package com.batteryexpert.data.assessment

import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentLogicTest {

    @Test
    fun testRecommendationLabels() {
        assertEquals("OK", Recommendation.OK.label)
        assertEquals("Beobachten", Recommendation.WATCH.label)
        assertEquals("Aussortieren", Recommendation.SORT_OUT.label)
    }

    @Test
    fun testIrRecommendation_normalAndBoundaryCases() {
        assertEquals(Recommendation.OK, AssessmentLogic.irRecommendation(25, 20))
        assertEquals(Recommendation.WATCH, AssessmentLogic.irRecommendation(35, 20))
        assertEquals(Recommendation.SORT_OUT, AssessmentLogic.irRecommendation(45, 20))

        assertEquals(Recommendation.OK, AssessmentLogic.irRecommendation(30, 0))
        assertEquals(Recommendation.OK, AssessmentLogic.irRecommendation(0, 20))
        assertEquals(Recommendation.OK, AssessmentLogic.irRecommendation(0, 0))
    }

    @Test
    fun testSohRecommendation_normalAndBoundaryCases() {
        val (soh1, rec1) = AssessmentLogic.sohRecommendation(2800, 3000)
        assertEquals(93.33f, soh1, 0.1f)
        assertEquals(Recommendation.OK, rec1)

        val (soh2, rec2) = AssessmentLogic.sohRecommendation(2500, 3000)
        assertEquals(83.33f, soh2, 0.1f)
        assertEquals(Recommendation.WATCH, rec2)

        val (soh3, rec3) = AssessmentLogic.sohRecommendation(2200, 3000)
        assertEquals(73.33f, soh3, 0.1f)
        assertEquals(Recommendation.SORT_OUT, rec3)

        val (sohZeroRated, recZeroRated) = AssessmentLogic.sohRecommendation(2000, 0)
        assertEquals(0f, sohZeroRated, 0.001f)
        assertEquals(Recommendation.WATCH, recZeroRated)

        val (sohZeroMeasured, recZeroMeasured) = AssessmentLogic.sohRecommendation(0, 3000)
        assertEquals(0f, sohZeroMeasured, 0.001f)
        assertEquals(Recommendation.SORT_OUT, recZeroMeasured)
    }

    @Test
    fun testComputeDefaults_normalAndLimits() {
        val currents1 = AssessmentLogic.computeDefaults(3000)
        assertEquals(3000, currents1.fastChargeMa)
        assertEquals(2000, currents1.fastDischargeMa)
        assertEquals(1500, currents1.slowChargeMa)
        assertEquals(600, currents1.slowDischargeMa)

        val currents2 = AssessmentLogic.computeDefaults(6000)
        assertEquals(5000, currents2.fastChargeMa)
        assertEquals(2000, currents2.fastDischargeMa)
        assertEquals(3000, currents2.slowChargeMa)
        assertEquals(1200, currents2.slowDischargeMa)

        val currents3 = AssessmentLogic.computeDefaults(800)
        assertEquals(800, currents3.fastChargeMa)
        assertEquals(800, currents3.fastDischargeMa)
        assertEquals(400, currents3.slowChargeMa)
        assertEquals(160, currents3.slowDischargeMa)

        val currentsZero = AssessmentLogic.computeDefaults(0)
        assertEquals(0, currentsZero.fastChargeMa)
        assertEquals(0, currentsZero.fastDischargeMa)
        assertEquals(0, currentsZero.slowChargeMa)
        assertEquals(0, currentsZero.slowDischargeMa)
    }
}
