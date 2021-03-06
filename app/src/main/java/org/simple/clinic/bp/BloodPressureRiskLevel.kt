package org.simple.clinic.bp

import org.simple.clinic.R
import org.simple.clinic.util.Just
import org.simple.clinic.util.None
import org.simple.clinic.util.Optional

enum class BloodPressureRiskLevel(private val urgency: Int, val displayTextRes: Optional<Int>) {

  EXTREMELY_HIGH
      (4, Just(R.string.bloodpressure_risk_level_extremely_high)),

  VERY_HIGH
      (3, Just(R.string.bloodpressure_risk_level_very_high)),

  MODERATELY_HIGH
      (2, Just(R.string.bloodpressure_risk_level_moderately_high)),

  MILDLY_HIGH
      (1, None),

  NORMAL
      (0, None),

  LOW
      (-1, Just(R.string.bloodpressure_risk_level_low));

  fun isUrgent(): Boolean {
    return when (this) {
      LOW, NORMAL, MILDLY_HIGH -> false
      MODERATELY_HIGH, VERY_HIGH, EXTREMELY_HIGH -> true
    }
  }

  companion object {

    fun compute(measurement: BloodPressureMeasurement): BloodPressureRiskLevel {
      val systolicRiskLevel = computeSystolic(measurement)
      val diastolicRiskLevel = computeDiastolic(measurement)

      return if (systolicRiskLevel.urgency > diastolicRiskLevel.urgency) {
        systolicRiskLevel
      } else {
        diastolicRiskLevel
      }
    }

    private fun computeSystolic(measurement: BloodPressureMeasurement): BloodPressureRiskLevel {
      // TODO: Chart shows 90-139 for normal.
      return measurement.systolic.let {
        when {
          it <= 89 -> LOW
          it in 90..129 -> NORMAL
          it in 130..139 -> MILDLY_HIGH
          it in 140..159 -> MODERATELY_HIGH
          it in 160..199 -> VERY_HIGH
          it >= 200 -> EXTREMELY_HIGH
          else -> throw AssertionError("Shouldn't reach here: $measurement")
        }
      }
    }

    private fun computeDiastolic(measurement: BloodPressureMeasurement): BloodPressureRiskLevel {
      return measurement.diastolic.let {
        when {
          it <= 59 -> LOW
          it in 60..79 -> NORMAL
          it in 80..89 -> MILDLY_HIGH
          it in 90..99 -> MODERATELY_HIGH
          it in 100..119 -> VERY_HIGH
          it >= 120 -> EXTREMELY_HIGH
          else -> throw AssertionError("Shouldn't reach here: $measurement")
        }
      }
    }
  }
}
