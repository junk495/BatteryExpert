package com.batteryexpert.data.assessment

import kotlin.math.round

object AssessmentLogic {

    fun irRecommendation(measuredIR: Int, targetIR: Int): Recommendation {
        if (targetIR <= 0) return Recommendation.OK
        val ratio = measuredIR.toFloat() / targetIR.toFloat()
        return when {
            ratio < 1.5f -> Recommendation.OK
            ratio <= 2.0f -> Recommendation.WATCH
            else -> Recommendation.SORT_OUT
        }
    }

    fun sohRecommendation(measuredCapacityMah: Int, ratedCapacityMah: Int): Pair<Float, Recommendation> {
        if (ratedCapacityMah <= 0) return 0f to Recommendation.WATCH
        val soh = (measuredCapacityMah.toFloat() * 100f) / ratedCapacityMah.toFloat()
        val recommendation = when {
            soh > 90f -> Recommendation.OK
            soh >= 80f -> Recommendation.WATCH
            else -> Recommendation.SORT_OUT
        }
        return soh to recommendation
    }

    fun computeDefaults(ratedCapacityMah: Int): TestCurrents {
        val c1 = maxOf(0, ratedCapacityMah)
        val fastCharge = minOf(c1, 5000)
        val fastDischarge = minOf(c1, 2000)
        val slowCharge = round(c1 * 0.5f).toInt()
        val slowDischarge = round(c1 * 0.2f).toInt()

        return TestCurrents(
            fastChargeMa = fastCharge,
            fastDischargeMa = fastDischarge,
            slowChargeMa = slowCharge,
            slowDischargeMa = slowDischarge
        )
    }
}
