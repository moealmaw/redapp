package org.resolvetosavelives.red.sync.patient

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.resolvetosavelives.red.newentry.search.Gender
import org.resolvetosavelives.red.newentry.search.OngoingPatientEntry
import org.resolvetosavelives.red.newentry.search.PatientRepository
import org.resolvetosavelives.red.newentry.search.PatientWithAddress
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.resolvetosavelives.red.sync.SyncTestSubject
import org.resolvetosavelives.red.util.Just
import org.resolvetosavelives.red.util.Optional
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Named

class PatientSyncTestSubject @Inject constructor(
    private val repository: PatientRepository,
    private val patientSync: PatientSync,
    @Named("last_patient_pull_timestamp") private val lastPullTimestamp: Preference<Optional<Instant>>
) : SyncTestSubject<PatientWithAddress> {

  override fun insertDummyRecords(count: Int): Completable {
    return Observable.range(0, count)
        .flatMapCompletable({ index ->
          repository
              .saveOngoingEntry(OngoingPatientEntry(
                  personalDetails = OngoingPatientEntry.PersonalDetails("Ashok Kumar #$index", "12-04-1993", null, Gender.TRANSGENDER),
                  address = OngoingPatientEntry.Address("colony-or-village", "district", "state")))
              .andThen(repository.saveOngoingEntryAsPatient())
        })
  }

  override fun push(): Completable {
    return patientSync.push()
  }

  override fun pull(): Completable {
    return patientSync.pull()
  }

  override fun recordsWithSyncStatus(syncStatus: SyncStatus): Single<List<PatientWithAddress>> {
    return repository.patientsWithSyncStatus(syncStatus)
  }

  override fun updateLastPullTimestampTo(timestamp: Instant) {
    lastPullTimestamp.set(Just(timestamp))
  }

  override fun recordCount(): Single<Int> {
    return repository.patientCount()
  }
}
