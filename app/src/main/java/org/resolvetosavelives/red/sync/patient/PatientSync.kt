package org.resolvetosavelives.red.sync.patient

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Completable
import org.resolvetosavelives.red.newentry.search.PatientRepository
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.resolvetosavelives.red.sync.DataSync
import org.resolvetosavelives.red.sync.SyncApiV1
import org.resolvetosavelives.red.util.Just
import org.resolvetosavelives.red.util.None
import org.resolvetosavelives.red.util.Optional
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Named

class PatientSync @Inject constructor(
    private val api: SyncApiV1,
    private val repository: PatientRepository,
    private val dataSync: DataSync,
    @Named("last_patient_pull_timestamp") private val lastPullTimestampPref: Preference<Optional<Instant>>
) {

  fun sync(): Completable {
    return Completable.mergeArrayDelayError(push(), pull())
  }

  fun push(): Completable {
    return dataSync.push(
        recordsToSync = repository
            .patientsWithSyncStatus(SyncStatus.PENDING)
            .map { records -> records.map { it.toPayload() } },
        switchSyncStatus = { fromStatus, toStatus -> repository.updatePatientsSyncStatus(fromStatus, toStatus) },
        setSyncStatus = { recordUuids, toStatus -> repository.updatePatientsSyncStatus(recordUuids, toStatus) },
        api = { records -> api.push(PatientPushRequest(records)) })
  }

  fun pull(): Completable {
    return dataSync.pull(
        lastPullTimestamp = lastPullTimestampPref,
        saveRecords = { records -> repository.save(records) },
        syncStatusOfLocalRecord = { uuid -> repository.patient(uuid)?.syncStatus },
        api = { recordsToPull: Int ->
          val timestamp = lastPullTimestampPref.get()
          when (timestamp) {
            is Just -> api.pull(recordsToPull = recordsToPull, latestRecordTimestamp = timestamp.value)
            is None -> api.pull(recordsToPull = recordsToPull, isFirstPull = true)
          }
        }
    )
  }
}
