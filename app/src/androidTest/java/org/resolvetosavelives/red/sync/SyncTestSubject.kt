package org.resolvetosavelives.red.sync

import io.reactivex.Completable
import io.reactivex.Single
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.threeten.bp.Instant

interface SyncTestSubject<T> {

  fun insertDummyRecords(count: Int): Completable

  fun push(): Completable

  fun pull(): Completable

  fun recordsWithSyncStatus(syncStatus: SyncStatus): Single<List<T>>

  fun updateLastPullTimestampTo(timestamp: Instant)

  fun recordCount(): Single<Int>
}
