package org.resolvetosavelives.red.sync

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toCompletable
import io.reactivex.rxkotlin.toObservable
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.resolvetosavelives.red.util.Just
import org.resolvetosavelives.red.util.Optional
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

/**
 * Helper class for syncing,
 * Patient
 * BP
 * User
 * Facility
 * Prescription drug
 */
class DataSync @Inject constructor(private val configProvider: Single<SyncConfig>) {

  /** TODO */
  interface DataPullResponse<T : Synceable> {
    val records: List<T>
    val latestRecordTimestamp: Instant
  }

  /** TODO */
  interface Synceable {
    val uuid: String
  }

  /** [T] Type of data records to push. */
  fun <T : Synceable> push(
      recordsToSync: Single<List<T>>,
      switchSyncStatus: (fromStatus: SyncStatus, toStatus: SyncStatus) -> Completable,
      setSyncStatus: (recordUuids: List<String>, toStatus: SyncStatus) -> Completable,
      api: (records: List<T>) -> Single<DataPushResponse>
  ): Completable {
    val cachedPendingSyncRecords = recordsToSync
        // Converting to an Observable because Single#filter() returns a Maybe.
        // And Maybe#flatMapSingle() throws a NoSuchElementException on completion.
        .toObservable()
        .filter { it.isNotEmpty() }
        .cache()

    val pendingToInFlight = cachedPendingSyncRecords
        .flatMapCompletable {
          switchSyncStatus(SyncStatus.PENDING, SyncStatus.IN_FLIGHT)
        }

    val networkCall = cachedPendingSyncRecords
        .flatMapSingle { api(it) }
        .doOnNext { response -> logValidationErrorsIfAny(response) }
        .map { it.validationErrors }
        .map { errors -> errors.map { it.uuid } }
        .flatMapCompletable { recordUuidsWithErrors ->
          switchSyncStatus(SyncStatus.IN_FLIGHT, SyncStatus.DONE)
              .andThen(recordUuidsWithErrors.let {
                when {
                  it.isEmpty() -> Completable.complete()
                  else -> setSyncStatus(recordUuidsWithErrors, SyncStatus.INVALID)
                }
              })
        }

    return pendingToInFlight.andThen(networkCall)
  }

  /** [T] Type of data records to push. */
  fun <T : Synceable> pull(
      lastPullTimestamp: Preference<Optional<Instant>>,
      saveRecords: (records: List<T>) -> Completable,
      syncStatusOfLocalRecord: (uuid: String) -> SyncStatus?,
      api: (recordsToPull: Int) -> Single<out DataPullResponse<T>>
  ): Completable {
    return configProvider
        .flatMapCompletable { config ->
          lastPullTimestamp
              .asObservable()
              .take(1)
              .flatMapSingle { api(config.batchSize) }
              .flatMap { response ->
                filterRecordsThatCanBeSaved(syncStatusOfLocalRecord, response.records)
                    .flatMapCompletable { saveRecords(it) }
                    .andThen({ lastPullTimestamp.set(Just(response.latestRecordTimestamp)) }.toCompletable())
                    .andThen(Observable.just(response))
              }
              .repeat()
              .takeWhile({ response -> response.records.size >= config.batchSize })
              .ignoreElements()
        }
  }

  private fun <T : Synceable> filterRecordsThatCanBeSaved(
      syncStatusOfLocalRecord: (String) -> SyncStatus?,
      recordsFromServer: List<T>
  ): Single<List<T>> {
    return recordsFromServer
        .toObservable()
        .filter { serverRecord ->
          when (syncStatusOfLocalRecord(serverRecord.uuid)) {
            SyncStatus.PENDING -> false
            SyncStatus.IN_FLIGHT -> false
            SyncStatus.INVALID -> true
            SyncStatus.DONE -> true
            null -> true
          }
        }
        .toList()
  }
}

private fun logValidationErrorsIfAny(response: DataPushResponse) {
  if (response.validationErrors.isNotEmpty()) {
    Timber.e("Server sent validation errors for patients: ${response.validationErrors}")
  }
}
