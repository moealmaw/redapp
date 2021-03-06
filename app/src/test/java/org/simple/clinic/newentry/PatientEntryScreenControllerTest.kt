package org.simple.clinic.newentry

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.OngoingPatientEntry
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.util.Just
import org.simple.clinic.util.None
import org.simple.clinic.widgets.ActivityLifecycle
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent
import java.util.UUID

class PatientEntryScreenControllerTest {

  private val screen = mock<PatientEntryScreen>()
  private val patientRepository = mock<PatientRepository>()
  private val dateOfBirthValidator = mock<DateOfBirthFormatValidator>()
  private val facilityRepository = mock<FacilityRepository>()

  private val uiEvents = PublishSubject.create<UiEvent>()
  private val controller = PatientEntryScreenController(patientRepository, facilityRepository, dateOfBirthValidator)

  private lateinit var errorConsumer: (Throwable) -> Unit

  @Before
  fun setUp() {
    whenever(facilityRepository.currentFacility()).thenReturn(Observable.just(PatientMocker.facility()))

    errorConsumer = { throw it }

    uiEvents
        .compose(controller)
        .subscribe({ uiChange -> uiChange(screen) }, { e -> errorConsumer(e) })
  }

  @Test
  fun `when screen is created then existing data should be pre-filled`() {
    whenever(patientRepository.ongoingEntry()).thenReturn(Single.just(OngoingPatientEntry()))

    uiEvents.onNext(ScreenCreated())

    verify(screen).preFillFields(OngoingPatientEntry(
        address = OngoingPatientEntry.Address(
            colonyOrVillage = null,
            district = "district",
            state = "state")))
  }

  @Test
  fun `save button should remain disabled until the form has sufficient input`() {
    whenever(dateOfBirthValidator.validate("")).thenReturn(DateOfBirthFormatValidator.Result.VALID)

    // Default, empty values when the screen starts.
    uiEvents.onNext(PatientFullNameTextChanged(""))
    uiEvents.onNext(PatientNoPhoneNumberToggled(noneSelected = false))
    uiEvents.onNext(PatientPhoneNumberTextChanged(""))
    uiEvents.onNext(PatientDateOfBirthTextChanged(""))
    uiEvents.onNext(PatientAgeTextChanged(""))
    uiEvents.onNext(PatientGenderChanged(None))
    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))
    uiEvents.onNext(PatientNoColonyOrVillageToggled(noneSelected = false))
    uiEvents.onNext(PatientDistrictTextChanged(""))
    uiEvents.onNext(PatientStateTextChanged(""))

    // Valid values entered later.
    uiEvents.onNext(PatientFullNameTextChanged("Ashok"))
    uiEvents.onNext(PatientNoPhoneNumberToggled(noneSelected = true))
    uiEvents.onNext(PatientPhoneNumberTextChanged(""))
    uiEvents.onNext(PatientDateOfBirthTextChanged(""))
    uiEvents.onNext(PatientAgeTextChanged("125"))
    uiEvents.onNext(PatientGenderChanged(Just(Gender.TRANSGENDER)))
    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))
    uiEvents.onNext(PatientNoColonyOrVillageToggled(noneSelected = true))
    uiEvents.onNext(PatientDistrictTextChanged("district"))
    uiEvents.onNext(PatientStateTextChanged("state"))

    verify(screen, times(1)).setSaveButtonEnabled(false)
    verify(screen, times(1)).setSaveButtonEnabled(true)
  }

  @Test
  fun `when save button is clicked then a patient record should be created from the form input`() {
    whenever(patientRepository.saveOngoingEntry(any())).thenReturn(Completable.complete())
    whenever(dateOfBirthValidator.validate(any())).thenReturn(DateOfBirthFormatValidator.Result.VALID)
    val savedPatient = PatientMocker.patient(uuid = UUID.randomUUID())
    whenever(patientRepository.saveOngoingEntryAsPatient()).thenReturn(Single.just(savedPatient))

    uiEvents.onNext(PatientFullNameTextChanged("Ashok"))
    uiEvents.onNext(PatientNoPhoneNumberToggled(noneSelected = false))
    uiEvents.onNext(PatientPhoneNumberTextChanged("1234567890"))
    uiEvents.onNext(PatientDateOfBirthTextChanged("12/04/1993"))
    uiEvents.onNext(PatientAgeTextChanged(""))
    uiEvents.onNext(PatientGenderChanged(Just(Gender.TRANSGENDER)))
    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))
    uiEvents.onNext(PatientNoColonyOrVillageToggled(noneSelected = false))
    uiEvents.onNext(PatientDistrictTextChanged("district"))
    uiEvents.onNext(PatientStateTextChanged("state"))
    uiEvents.onNext(PatientEntrySaveClicked())

    verify(patientRepository).saveOngoingEntry(OngoingPatientEntry(
        personalDetails = OngoingPatientEntry.PersonalDetails("Ashok", "12/04/1993", age = null, gender = Gender.TRANSGENDER),
        address = OngoingPatientEntry.Address(colonyOrVillage = null, district = "district", state = "state"),
        phoneNumber = OngoingPatientEntry.PhoneNumber("1234567890")
    ))
    verify(patientRepository).saveOngoingEntryAsPatient()
    verify(screen).openSummaryScreenForBpEntry(savedPatient.uuid)
  }

  @Test
  fun `when none is selected then their associated fields should be reset`() {
    uiEvents.onNext(PatientNoPhoneNumberToggled(noneSelected = true))
    uiEvents.onNext(PatientNoColonyOrVillageToggled(noneSelected = true))

    verify(screen).resetPhoneNumberField()
    verify(screen).resetColonyOrVillageField()
  }

  @Test
  fun `none checkboxes should only be visible while their associated fields are empty`() {
    uiEvents.onNext(PatientPhoneNumberTextChanged(""))
    uiEvents.onNext(PatientPhoneNumberTextChanged("1"))
    uiEvents.onNext(PatientPhoneNumberTextChanged("12"))
    uiEvents.onNext(PatientPhoneNumberTextChanged(""))

    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))
    uiEvents.onNext(PatientColonyOrVillageTextChanged("C"))
    uiEvents.onNext(PatientColonyOrVillageTextChanged("Co"))
    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))

    verify(screen, times(2)).setNoPhoneNumberCheckboxVisible(true)
    verify(screen, times(1)).setNoPhoneNumberCheckboxVisible(false)

    verify(screen, times(2)).setNoVillageOrColonyCheckboxVisible(true)
    verify(screen, times(1)).setNoVillageOrColonyCheckboxVisible(false)
  }

  @Test
  fun `date-of-birth and age fields should only be visible while one of them is empty`() {
    whenever(dateOfBirthValidator.validate(any())).thenReturn(DateOfBirthFormatValidator.Result.VALID)

    uiEvents.onNext(PatientAgeTextChanged(""))
    uiEvents.onNext(PatientDateOfBirthTextChanged(""))
    verify(screen).setDateOfBirthAndAgeVisibility(DateOfBirthAndAgeVisibility.BOTH_VISIBLE)

    uiEvents.onNext(PatientDateOfBirthTextChanged("1"))
    verify(screen).setDateOfBirthAndAgeVisibility(DateOfBirthAndAgeVisibility.DATE_OF_BIRTH_VISIBLE)

    uiEvents.onNext(PatientDateOfBirthTextChanged(""))
    uiEvents.onNext(PatientAgeTextChanged("1"))
    verify(screen).setDateOfBirthAndAgeVisibility(DateOfBirthAndAgeVisibility.AGE_VISIBLE)
  }

  @Test()
  fun `when both date-of-birth and age fields have text then an assertion error should be thrown`() {
    whenever(dateOfBirthValidator.validate(any())).thenReturn(DateOfBirthFormatValidator.Result.VALID)

    errorConsumer = { assertThat(it).isInstanceOf(AssertionError::class.java) }

    uiEvents.onNext(PatientDateOfBirthTextChanged("1"))
    uiEvents.onNext(PatientAgeTextChanged("1"))
  }

  @Test
  fun `while date-of-birth has focus or has some input then date format should be shown in the label`() {
    whenever(dateOfBirthValidator.validate(any())).thenReturn(DateOfBirthFormatValidator.Result.VALID)

    uiEvents.onNext(PatientDateOfBirthTextChanged(""))
    uiEvents.onNext(PatientDateOfBirthFocusChanged(hasFocus = false))
    uiEvents.onNext(PatientDateOfBirthFocusChanged(hasFocus = true))
    uiEvents.onNext(PatientDateOfBirthTextChanged("1"))
    uiEvents.onNext(PatientDateOfBirthFocusChanged(hasFocus = false))

    verify(screen, times(1)).setShowDatePatternInDateOfBirthLabel(false)
    verify(screen).setShowDatePatternInDateOfBirthLabel(true)
  }

  @Test
  fun `when screen is paused then ongoing patient entry should be saved`() {
    whenever(patientRepository.saveOngoingEntry(any())).thenReturn(Completable.complete())
    whenever(dateOfBirthValidator.validate(any())).thenReturn(DateOfBirthFormatValidator.Result.VALID)
    val savedPatient = PatientMocker.patient(uuid = UUID.randomUUID())
    whenever(patientRepository.saveOngoingEntryAsPatient()).thenReturn(Single.just(savedPatient))

    uiEvents.onNext(PatientFullNameTextChanged("Ashok"))
    uiEvents.onNext(PatientNoPhoneNumberToggled(noneSelected = false))
    uiEvents.onNext(PatientPhoneNumberTextChanged("1234567890"))
    uiEvents.onNext(PatientDateOfBirthTextChanged("12/04/1993"))
    uiEvents.onNext(PatientAgeTextChanged(""))
    uiEvents.onNext(PatientGenderChanged(Just(Gender.TRANSGENDER)))
    uiEvents.onNext(PatientColonyOrVillageTextChanged(""))
    uiEvents.onNext(PatientNoColonyOrVillageToggled(noneSelected = false))
    uiEvents.onNext(PatientDistrictTextChanged("district"))
    uiEvents.onNext(PatientStateTextChanged("state"))

    uiEvents.onNext(ActivityLifecycle.Paused())

    verify(patientRepository).saveOngoingEntry(OngoingPatientEntry(
        personalDetails = OngoingPatientEntry.PersonalDetails("Ashok", "12/04/1993", age = null, gender = Gender.TRANSGENDER),
        address = OngoingPatientEntry.Address(colonyOrVillage = null, district = "district", state = "state"),
        phoneNumber = OngoingPatientEntry.PhoneNumber("1234567890")
    ))
  }
}
