package org.simple.clinic.drugs.selection.entry

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import org.simple.clinic.drugs.PrescriptionRepository
import org.simple.clinic.util.nullIfBlank
import org.simple.clinic.widgets.UiEvent
import javax.inject.Inject

private typealias Ui = CustomPrescriptionEntrySheet
private typealias UiChange = (Ui) -> Unit

class CustomPrescriptionEntryController @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = events.replay().refCount()

    return Observable.mergeArray(
        toggleSaveButton(replayedEvents),
        savePrescriptionsAndDismiss(replayedEvents))
  }

  private fun toggleSaveButton(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<CustomPrescriptionDrugNameTextChanged>()
        .map { it.name.isNotBlank() }
        .distinctUntilChanged()
        .map { canBeSaved -> { ui: Ui -> ui.setSaveButtonEnabled(canBeSaved) } }
  }

  private fun savePrescriptionsAndDismiss(events: Observable<UiEvent>): Observable<UiChange> {
    val patientUuids = events
        .ofType<CustomPrescriptionSheetCreated>()
        .map { it.patientUuid }
        .take(1)

    val nameChanges = events
        .ofType<CustomPrescriptionDrugNameTextChanged>()
        .map { it.name }

    val dosageChanges = events
        .ofType<CustomPrescriptionDrugDosageTextChanged>()
        .map { it.dosage }

    val saveClicks = events
        .ofType<SaveCustomPrescriptionClicked>()

    return Observables
        .combineLatest(saveClicks, patientUuids, nameChanges, dosageChanges) { _, uuid, name, dosage -> Triple(uuid, name, dosage) }
        .flatMap { (patientUuid, name, dosage) ->
          prescriptionRepository
              .savePrescription(patientUuid, name, dosage.nullIfBlank(), rxNormCode = null, isProtocolDrug = false)
              .andThen(Observable.just({ ui: Ui -> ui.finish() }))
        }
  }
}
