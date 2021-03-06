package org.simple.clinic.summary

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.drugs.PrescriptionRepository
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.util.Just
import org.simple.clinic.util.None
import org.simple.clinic.widgets.UiEvent
import java.util.UUID

@RunWith(JUnitParamsRunner::class)
class PatientSummaryScreenControllerTest {

  private val screen = mock<PatientSummaryScreen>()
  private val patientRepository = mock<PatientRepository>()
  private val bpRepository = mock<BloodPressureRepository>()
  private val prescriptionRepository = mock<PrescriptionRepository>()
  private val patientUuid = UUID.randomUUID()

  private val uiEvents = PublishSubject.create<UiEvent>()
  private lateinit var controller: PatientSummaryScreenController

  @Before
  fun setUp() {
    val timestampGenerator = mock<RelativeTimestampGenerator>()
    whenever(timestampGenerator.generate(any())).thenReturn(Today())

    controller = PatientSummaryScreenController(patientRepository, bpRepository, prescriptionRepository, timestampGenerator)

    uiEvents
        .compose(controller)
        .subscribe { uiChange -> uiChange(screen) }

    whenever(patientRepository.patient(patientUuid)).thenReturn(Observable.never())
    whenever(patientRepository.phoneNumbers(patientUuid)).thenReturn(Observable.never())
    whenever(bpRepository.recentMeasurementsForPatient(patientUuid)).thenReturn(Observable.never())
    whenever(prescriptionRepository.newestPrescriptionsForPatient(patientUuid)).thenReturn(Observable.never())
  }

  @Test
  fun `patient's profile should be populated`() {
    val addressUuid = UUID.randomUUID()
    val patient = PatientMocker.patient(uuid = patientUuid, addressUuid = addressUuid)
    val address = PatientMocker.address(uuid = addressUuid)
    val phoneNumber = None

    whenever(patientRepository.patient(patientUuid)).thenReturn(Observable.just(Just(patient)))
    whenever(patientRepository.address(addressUuid)).thenReturn(Observable.just(Just(address)))
    whenever(patientRepository.phoneNumbers(patientUuid)).thenReturn(Observable.just(phoneNumber))
    whenever(bpRepository.recentMeasurementsForPatient(patientUuid)).thenReturn(Observable.never())

    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.NEW_PATIENT))

    verify(screen).populatePatientProfile(patient, address, phoneNumber)
  }

  @Test
  fun `patient's prescription summary should be populated`() {
    val prescriptions = listOf(
        PatientMocker.prescription(name = "Amlodipine", dosage = "10mg"),
        PatientMocker.prescription(name = "Telmisartan", dosage = "9000mg"),
        PatientMocker.prescription(name = "Randomzole", dosage = "2 packets"))
    whenever(prescriptionRepository.newestPrescriptionsForPatient(patientUuid)).thenReturn(Observable.just(prescriptions))

    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.SEARCH))

    verify(screen).populatePrescribedDrugsSummary(SummaryPrescribedDrugsItem(prescriptions))
  }

  @Test
  fun `patient's blood pressure history should be populated`() {
    val bloodPressureMeasurements = listOf(
        PatientMocker.bp(patientUuid, systolic = 120, diastolic = 85),
        PatientMocker.bp(patientUuid, systolic = 164, diastolic = 95),
        PatientMocker.bp(patientUuid, systolic = 144, diastolic = 90))
    whenever(bpRepository.recentMeasurementsForPatient(patientUuid)).thenReturn(Observable.just(bloodPressureMeasurements))

    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.NEW_PATIENT))

    verify(screen).populateBloodPressureHistory(check {
      it.forEachIndexed { i, item -> assertThat(item.measurement == bloodPressureMeasurements[i]) }
    })
  }

  @Test
  fun `when new-BP is clicked then BP entry sheet should be shown`() {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.SEARCH))
    uiEvents.onNext(PatientSummaryNewBpClicked())

    verify(screen).showBloodPressureEntrySheet(patientUuid)
  }

  @Test
  fun `when screen was opened after saving a new patient then BP entry sheet should be shown`() {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.SEARCH))
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.NEW_PATIENT))

    verify(screen, times(1)).showBloodPressureEntrySheet(any())
  }

  @Test
  fun `when screen was opened from search and up button is pressed then the user should be taken back to search`() {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.NEW_PATIENT))
    uiEvents.onNext(PatientSummaryBackClicked())

    verify(screen).goBackToHome()
  }

  @Test
  fun `when screen was opened after saving a new patient and up button is pressed then the user should be taken back to home`() {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.SEARCH))
    uiEvents.onNext(PatientSummaryBackClicked())

    verify(screen).goBackToPatientSearch()
  }

  @Test
  fun `when update medicines is clicked then BP medicines screen should be shown`() {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller = PatientSummaryCaller.SEARCH))
    uiEvents.onNext(PatientSummaryUpdateDrugsClicked())

    verify(screen).showUpdatePrescribedDrugsScreen(patientUuid)
  }

  @Test
  @Parameters(value = ["SEARCH", "NEW_PATIENT"])
  fun `done button should only be shown when summary was opened after saving a new patient`(caller: PatientSummaryCaller) {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller))

    verify(screen).setDoneButtonVisible(caller == PatientSummaryCaller.NEW_PATIENT)
  }

  @Test
  @Parameters(value = ["SEARCH", "NEW_PATIENT"])
  fun `done button clicks should be handled in the same way as back clicks`(caller: PatientSummaryCaller) {
    uiEvents.onNext(PatientSummaryScreenCreated(patientUuid, caller))
    uiEvents.onNext(PatientSummaryDoneClicked())

    when (caller) {
      PatientSummaryCaller.NEW_PATIENT -> verify(screen).goBackToHome()
      else -> verify(screen).goBackToPatientSearch()
    }
  }
}
