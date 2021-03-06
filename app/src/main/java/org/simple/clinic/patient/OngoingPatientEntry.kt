package org.simple.clinic.patient

/**
 * Represents loggedInUser input on the UI, which is why every field is a String.
 * Parsing of loggedInUser input happens later when this data class is converted
 * into a Patient object in [PatientRepository.saveOngoingEntryAsPatient].
 */
data class OngoingPatientEntry(
    val personalDetails: PersonalDetails? = null,
    val address: Address? = null,
    val phoneNumber: PhoneNumber? = null
) {

  fun validateForSaving(): ValidationResult {
    val invalidResult: (message: String) -> ValidationResult = {
      ValidationResult.Invalid(AssertionError(it))
    }

    if (personalDetails == null) {
      return invalidResult("Personal details cannot be empty")
    }
    if (address == null) {
      return invalidResult("Address cannot be empty")
    }

    with(personalDetails) {
      if (dateOfBirth.isNullOrBlank() && age.isNullOrBlank()) {
        return invalidResult("Both age and dateOfBirth cannot be null.")
      }
      if (dateOfBirth.isNullOrBlank() == age.isNullOrBlank()
          || (dateOfBirth == null) == (age == null)) {
        return invalidResult("Both age and dateOfBirth cannot be present.")
      }
      if (fullName.isBlank()) {
        return invalidResult("Full name cannot be empty")
      }
      if (gender == null) {
        return invalidResult("Gender cannot be empty")
      }
    }

    with(address) {
      if (colonyOrVillage != null && colonyOrVillage.isBlank()) {
        return invalidResult("Colony/district cannot be both non-null and blank")
      }
      if (district.isBlank()) {
        return invalidResult("Address district cannot be empty")
      }
      if (state.isBlank()) {
        return invalidResult("Address state cannot be empty")
      }
    }

    return ValidationResult.Valid()
  }

  sealed class ValidationResult {
    class Valid : ValidationResult()
    data class Invalid(val error: Throwable) : ValidationResult()
  }

  /**
   * [age] is stored as a String instead of an Int because it's easy
   * to forget that [Int.toString] will return literal "null" for null Ints.
   */
  data class PersonalDetails(val fullName: String, val dateOfBirth: String?, val age: String?, val gender: Gender?)

  data class PhoneNumber(val number: String, val type: PatientPhoneNumberType = PatientPhoneNumberType.MOBILE, val active: Boolean = true)

  data class Address(val colonyOrVillage: String?, val district: String, val state: String)
}
