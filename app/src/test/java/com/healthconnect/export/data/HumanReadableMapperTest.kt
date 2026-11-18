package com.healthconnect.export.data

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Health Connect Int constant → human-readable string mapper functions.
 *
 * Each mapper is tested for:
 * - null input → null
 * - Every known Int value → expected string
 * - Unknown Int value → "Other (value)"
 *
 * Mappers covered:
 * - bodyPositionToString (5 known + unknown + null)
 * - specimenSourceToString (6 known + unknown + null)
 * - mealTypeToString (5 known + unknown + null)
 * - sleepStageToString (6 known + unknown + null)
 * - measurementLocationToString (12 known + unknown + null)
 * - menstruationFlowToString (4 known + unknown + null)
 * - nutritionMealTypeToString (5 known + unknown + null)
 * - exerciseTypeToString (every official Health Connect id + unknown + null)
 */
class HumanReadableMapperTest {
    // =============================================
    // bodyPositionToString
    // =============================================

    @Test
    fun `bodyPositionToString null returns null`() {
        assertNull(bodyPositionToString(null))
    }

    @Test
    fun `bodyPositionToString known values`() {
        assertEquals("Unknown", bodyPositionToString(0))
        assertEquals("Standing", bodyPositionToString(1))
        assertEquals("Sitting", bodyPositionToString(2))
        assertEquals("Lying down", bodyPositionToString(3))
        assertEquals("Reclining", bodyPositionToString(4))
    }

    @Test
    fun `bodyPositionToString unknown value returns Other`() {
        assertEquals("Other (5)", bodyPositionToString(5))
        assertEquals("Other (-1)", bodyPositionToString(-1))
        assertEquals("Other (99)", bodyPositionToString(99))
    }

    // =============================================
    // specimenSourceToString
    // =============================================

    @Test
    fun `specimenSourceToString null returns null`() {
        assertNull(specimenSourceToString(null))
    }

    @Test
    fun `specimenSourceToString known values`() {
        assertEquals("Unknown", specimenSourceToString(0))
        assertEquals("Interstitial fluid", specimenSourceToString(1))
        assertEquals("Capillary blood", specimenSourceToString(2))
        assertEquals("Plasma", specimenSourceToString(3))
        assertEquals("Serum", specimenSourceToString(4))
        assertEquals("Whole blood", specimenSourceToString(5))
    }

    @Test
    fun `specimenSourceToString unknown value returns Other`() {
        assertEquals("Other (6)", specimenSourceToString(6))
        assertEquals("Other (100)", specimenSourceToString(100))
    }

    // =============================================
    // mealTypeToString (blood glucose)
    // =============================================

    @Test
    fun `mealTypeToString null returns null`() {
        assertNull(mealTypeToString(null))
    }

    @Test
    fun `mealTypeToString known values`() {
        assertEquals("Unknown", mealTypeToString(0))
        assertEquals("Fasting", mealTypeToString(1))
        assertEquals("Before meal", mealTypeToString(2))
        assertEquals("After meal", mealTypeToString(3))
        assertEquals("General", mealTypeToString(4))
    }

    @Test
    fun `mealTypeToString unknown value returns Other`() {
        assertEquals("Other (5)", mealTypeToString(5))
        assertEquals("Other (-5)", mealTypeToString(-5))
    }

    // =============================================
    // sleepStageToString
    // =============================================

    @Test
    fun `sleepStageToString null returns null`() {
        assertNull(sleepStageToString(null))
    }

    @Test
    fun `sleepStageToString known values`() {
        assertEquals("Unknown", sleepStageToString(0))
        assertEquals("Awake", sleepStageToString(1))
        assertEquals("Deep sleep", sleepStageToString(2))
        assertEquals("Light sleep", sleepStageToString(3))
        assertEquals("REM sleep", sleepStageToString(4))
        assertEquals("Out of bed", sleepStageToString(5))
    }

    @Test
    fun `sleepStageToString unknown value returns Other`() {
        assertEquals("Other (6)", sleepStageToString(6))
        assertEquals("Other (42)", sleepStageToString(42))
    }

    // =============================================
    // measurementLocationToString
    // =============================================

    @Test
    fun `measurementLocationToString null returns null`() {
        assertNull(measurementLocationToString(null))
    }

    @Test
    fun `measurementLocationToString known values`() {
        assertEquals("Unknown", measurementLocationToString(0))
        assertEquals("Axillary", measurementLocationToString(1))
        assertEquals("Body", measurementLocationToString(2))
        assertEquals("Ear", measurementLocationToString(3))
        assertEquals("Finger", measurementLocationToString(4))
        assertEquals("Forehead", measurementLocationToString(5))
        assertEquals("Mouth", measurementLocationToString(6))
        assertEquals("Rectal", measurementLocationToString(7))
        assertEquals("Toe", measurementLocationToString(8))
        assertEquals("Tympanic", measurementLocationToString(9))
        assertEquals("Wrist", measurementLocationToString(10))
        assertEquals("Temporal artery", measurementLocationToString(11))
    }

    @Test
    fun `measurementLocationToString unknown value returns Other`() {
        assertEquals("Other (12)", measurementLocationToString(12))
        assertEquals("Other (50)", measurementLocationToString(50))
    }

    // =============================================
    // menstruationFlowToString
    // =============================================

    @Test
    fun `menstruationFlowToString null returns null`() {
        assertNull(menstruationFlowToString(null))
    }

    @Test
    fun `menstruationFlowToString known values`() {
        assertEquals("Unknown", menstruationFlowToString(0))
        assertEquals("Light", menstruationFlowToString(1))
        assertEquals("Medium", menstruationFlowToString(2))
        assertEquals("Heavy", menstruationFlowToString(3))
    }

    @Test
    fun `menstruationFlowToString unknown value returns Other`() {
        assertEquals("Other (4)", menstruationFlowToString(4))
        assertEquals("Other (10)", menstruationFlowToString(10))
    }

    // =============================================
    // nutritionMealTypeToString
    // =============================================

    @Test
    fun `nutritionMealTypeToString null returns null`() {
        assertNull(nutritionMealTypeToString(null))
    }

    @Test
    fun `nutritionMealTypeToString known values`() {
        assertEquals("Unknown", nutritionMealTypeToString(0))
        assertEquals("Breakfast", nutritionMealTypeToString(1))
        assertEquals("Lunch", nutritionMealTypeToString(2))
        assertEquals("Dinner", nutritionMealTypeToString(3))
        assertEquals("Snack", nutritionMealTypeToString(4))
    }

    @Test
    fun `nutritionMealTypeToString unknown value returns Other`() {
        assertEquals("Other (5)", nutritionMealTypeToString(5))
        assertEquals("Other (99)", nutritionMealTypeToString(99))
    }

    // =============================================
    // exerciseTypeToString — Health Connect's sparse exercise type ids
    // =============================================

    @Test
    fun `exerciseTypeToString null returns null`() {
        assertNull(exerciseTypeToString(null))
    }

    @Test
    fun `exerciseTypeToString maps every official Health Connect exercise type`() {
        // ExerciseSessionRecord constants in id order. The ids are sparse: a dense
        // 0…N table shifted every name after the first gap (issue #84).
        val expected =
            mapOf(
                0 to "Other workout",
                2 to "Badminton",
                4 to "Baseball",
                5 to "Basketball",
                8 to "Biking",
                9 to "Stationary biking",
                10 to "Boot camp",
                11 to "Boxing",
                13 to "Calisthenics",
                14 to "Cricket",
                16 to "Dancing",
                25 to "Elliptical",
                26 to "Exercise class",
                27 to "Fencing",
                28 to "American football",
                29 to "Australian football",
                31 to "Frisbee disc",
                32 to "Golf",
                33 to "Guided breathing",
                34 to "Gymnastics",
                35 to "Handball",
                36 to "HIIT",
                37 to "Hiking",
                38 to "Ice hockey",
                39 to "Ice skating",
                44 to "Martial arts",
                46 to "Paddling",
                47 to "Paragliding",
                48 to "Pilates",
                50 to "Racquetball",
                51 to "Rock climbing",
                52 to "Roller hockey",
                53 to "Rowing",
                54 to "Rowing machine",
                55 to "Rugby",
                56 to "Running",
                57 to "Treadmill running",
                58 to "Sailing",
                59 to "Scuba diving",
                60 to "Skating",
                61 to "Skiing",
                62 to "Snowboarding",
                63 to "Snowshoeing",
                64 to "Soccer",
                65 to "Softball",
                66 to "Squash",
                68 to "Stair climbing",
                69 to "Stair climbing machine",
                70 to "Strength training",
                71 to "Stretching",
                72 to "Surfing",
                73 to "Open water swimming",
                74 to "Pool swimming",
                75 to "Table tennis",
                76 to "Tennis",
                78 to "Volleyball",
                79 to "Walking",
                80 to "Water polo",
                81 to "Weightlifting",
                82 to "Wheelchair",
                83 to "Yoga",
            )

        expected.forEach { (id, name) -> assertEquals("exercise type id $id", name, exerciseTypeToString(id)) }
    }

    @Test
    fun `exerciseTypeToString decodes the ids from the bug report`() {
        // Sessions exported by third-party apps (Hevy) that were labelled wrongly.
        assertEquals("Hiking", exerciseTypeToString(37))
        assertEquals("Running", exerciseTypeToString(56))
        assertEquals("Strength training", exerciseTypeToString(70))
        // Types that were missing from the old table altogether.
        assertEquals("Walking", exerciseTypeToString(79))
        assertEquals("Water polo", exerciseTypeToString(80))
        assertEquals("Weightlifting", exerciseTypeToString(81))
        assertEquals("Wheelchair", exerciseTypeToString(82))
        assertEquals("Yoga", exerciseTypeToString(83))
    }

    @Test
    fun `exerciseTypeToString covers every ExerciseSessionRecord constant`() {
        // Guards the next Health Connect update: a new EXERCISE_TYPE_* value has to be
        // added to the mapper instead of silently becoming "Other (n)".
        val constants =
            ExerciseSessionRecord::class.java.declaredFields
                .filter { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.javaPrimitiveType }
                .map {
                    it.isAccessible = true
                    it.getInt(null)
                }

        assertTrue("expected at least 61 official exercise types, got ${constants.size}", constants.size >= 61)
        constants.forEach { id ->
            val name = exerciseTypeToString(id)
            assertNotNull("exercise type id $id must map to a name", name)
            assertFalse(
                "exercise type id $id still falls back to an unknown name: $name",
                name!!.startsWith("Other ("),
            )
        }
    }

    @Test
    fun `exerciseTypeToString ids that do not exist return Other`() {
        for (id in listOf(1, 3, 7, 12, 17, 45, 67, 77, 84, 1000, -1)) {
            assertEquals("Other ($id)", exerciseTypeToString(id))
        }
    }
}
