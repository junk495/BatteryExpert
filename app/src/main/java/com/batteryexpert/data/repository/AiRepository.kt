package com.batteryexpert.data.repository

import com.batteryexpert.data.ApiKeyStore
import com.batteryexpert.data.ApiKeyStore.Provider
import com.batteryexpert.data.assessment.AssessmentLogic
import com.batteryexpert.data.db.CellTypeEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiRepository(private val apiKeyStore: ApiKeyStore) {

    suspend fun researchCellType(query: String): Result<CellTypeEntity> = withContext(Dispatchers.IO) {
        val provider = apiKeyStore.getProvider()
        val apiKey = apiKeyStore.getApiKey(provider)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Kein API-Key für ${provider.name} vorhanden. Bitte in den Einstellungen hinterlegen.")
            )
        }

        val prompt = buildPrompt(query)

        try {
            val responseText = when (provider) {
                Provider.GEMINI -> callGemini(apiKey, prompt)
                Provider.DEEPSEEK -> callDeepSeek(apiKey, prompt)
            }

            val jsonString = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(jsonString)

            val cap = json.optInt("nominalCapacityMah", 2000)
            val defaults = AssessmentLogic.computeDefaults(cap)
            val chemistry = json.optString("chemistry", "Li-Ion").ifBlank { "Li-Ion" }
            val isNiMx = chemistry.lowercase() in listOf("nimh", "nicd", "eneloop")

            val deltaPeak = if (!json.isNull("deltaPeakMv")) json.optInt("deltaPeakMv") else if (isNiMx) 3 else null
            val capCutoff = if (!json.isNull("capacityCutoffMah")) json.optInt("capacityCutoffMah") else if (isNiMx) (cap * 1.15f).toInt() else null
            val trickle = if (!json.isNull("trickleChargeMa")) json.optInt("trickleChargeMa") else if (isNiMx) (cap * 0.03f).toInt().coerceAtLeast(30) else null
            val keepVolt = if (!json.isNull("keepVoltageMv")) json.optInt("keepVoltageMv") else if (isNiMx) 1350 else null

            val cellType = CellTypeEntity(
                id = 0L,
                manufacturer = json.optString("manufacturer", "Unbekannt").ifBlank { "Unbekannt" },
                model = json.optString("model", query).ifBlank { query },
                aliases = if (json.isNull("aliases")) null else json.optString("aliases"),
                chemistry = chemistry,
                size = json.optString("size", "18650").ifBlank { "18650" },
                nominalVoltageV = json.optDouble("nominalVoltageV", if (isNiMx) 1.2 else 3.6).toFloat(),
                nominalCapacityMah = cap,
                nominalEnergyWh = if (json.isNull("nominalEnergyWh")) null else json.optDouble("nominalEnergyWh").toFloat(),
                typicalInternalResistanceMOhm = if (json.isNull("typicalInternalResistanceMOhm")) 25 else json.optInt("typicalInternalResistanceMOhm"),
                chargeEndVoltageV = if (json.isNull("chargeEndVoltageV")) null else json.optDouble("chargeEndVoltageV").toFloat(),
                chargeCurrentStandardMa = if (json.isNull("chargeCurrentStandardMa")) null else json.optInt("chargeCurrentStandardMa"),
                chargeCurrentMaxMa = if (json.isNull("chargeCurrentMaxMa")) null else json.optInt("chargeCurrentMaxMa"),
                deltaPeakMv = deltaPeak,
                capacityCutoffMah = capCutoff,
                trickleChargeMa = trickle,
                keepVoltageMv = keepVolt,
                dischargeCutoffRecommendedV = if (json.isNull("dischargeCutoffRecommendedV")) null else json.optDouble("dischargeCutoffRecommendedV").toFloat(),
                dischargeCurrentStandardMa = if (json.isNull("dischargeCurrentStandardMa")) null else json.optInt("dischargeCurrentStandardMa"),
                dischargeCurrentMaxContinuousMa = if (json.isNull("dischargeCurrentMaxContinuousMa")) null else json.optInt("dischargeCurrentMaxContinuousMa"),
                cycleLifeTo80Percent = if (json.isNull("cycleLifeTo80Percent")) null else json.optInt("cycleLifeTo80Percent"),
                fastChargeCurrentMa = defaults.fastChargeMa,
                fastDischargeCurrentMa = defaults.fastDischargeMa,
                slowChargeCurrentMa = defaults.slowChargeMa,
                slowDischargeCurrentMa = defaults.slowDischargeMa,
                notes = if (json.isNull("notes")) "KI-Recherche Daten (${provider.name})" else json.optString("notes")
            )

            Result.success(cellType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(query: String): String = """
        Du bist ein Akku-Experte. Recherchiere die Datenblatt-Spezifikationen für den Zelltyp '$query'.
        Antworte AUSSCHLIESSLICH im JSON-Format gemäß diesem Schema (unbekannte/nicht vorhandene Werte als null):
        {
          "manufacturer": "String (z.B. Panasonic, Samsung, Sony, Eneloop)",
          "model": "String (z.B. NCR18650B, BK-3MCCE)",
          "aliases": "String oder null",
          "chemistry": "String (z.B. Li-Ion, Li-Ion HV, LiFePO4, NiMH)",
          "size": "String (z.B. 18650, 21700, AA, AAA)",
          "nominalVoltageV": Float (z.B. 3.6 oder 1.2),
          "nominalCapacityMah": Int (z.B. 3400 oder 2000),
          "nominalEnergyWh": Float oder null,
          "typicalInternalResistanceMOhm": Int oder null,
          "chargeEndVoltageV": Float oder null (z.B. 4.2 oder 1.45),
          "chargeCurrentStandardMa": Int oder null,
          "chargeCurrentMaxMa": Int oder null,
          "deltaPeakMv": Int oder null (NiMH -dV in mV, z.B. 3..5),
          "capacityCutoffMah": Int oder null (Sicherheitsanker in mAh, z.B. 2300),
          "trickleChargeMa": Int oder null (Erhaltungsladung in mA, z.B. 30..50),
          "keepVoltageMv": Int oder null (Erhaltungsspannung in mV, z.B. 1350),
          "dischargeCutoffRecommendedV": Float oder null (z.B. 2.5 oder 1.0),
          "dischargeCurrentStandardMa": Int oder null,
          "dischargeCurrentMaxContinuousMa": Int oder null,
          "cycleLifeTo80Percent": Int oder null,
          "notes": "String"
        }
        Hinweis für NiMH/Eneloop: Gib stets typische Werte für deltaPeakMv (3..5), capacityCutoffMah (~115% Nennkapazität), trickleChargeMa (30..50 mA) und keepVoltageMv (1350 mV) an.
    """.trimIndent()

    private suspend fun callGemini(apiKey: String, prompt: String): String {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig { responseMimeType = "application/json" }
        )
        return model.generateContent(prompt).text
            ?: throw IllegalStateException("Keine Antwort vom Gemini Modell.")
    }

    private fun callDeepSeek(apiKey: String, prompt: String): String {
        val url = URL("https://api.deepseek.com/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true

        val body = JSONObject()
            .put("model", "deepseek-chat")
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("temperature", 0)
            .toString()

        conn.outputStream.use { it.write(body.toByteArray()) }

        val responseText = conn.inputStream.bufferedReader().readText()
        return JSONObject(responseText)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }
}
