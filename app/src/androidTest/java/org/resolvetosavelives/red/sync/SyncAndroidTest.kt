package org.resolvetosavelives.red.sync

import android.support.test.runner.AndroidJUnit4
import com.google.common.truth.Truth
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.resolvetosavelives.red.AppDatabase
import org.resolvetosavelives.red.TestRedApp
import org.resolvetosavelives.red.newentry.search.SyncStatus
import org.threeten.bp.Instant
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class SyncAndroidTest {

  @Inject
  lateinit var configProvider: Single<SyncConfig>

  @Inject
  lateinit var database: AppDatabase

  private lateinit var subject: SyncTestSubject<Any>

  @Before
  fun setUp() {
    TestRedApp.appComponent().inject(this)
  }

  @Test
  fun when_pending_sync_records_are_present_then_they_should_be_pushed_to_the_server_and_marked_as_synced_on_success() {
    subject.insertDummyRecords(count = 5).blockingAwait()

    subject.push().blockingAwait()

    subject.recordsWithSyncStatus(SyncStatus.DONE)
        .test()
        .await()
        .assertValue({ records -> records.size == 5 })
        .assertComplete()
        .assertNoErrors()
  }

  @Test
  fun when_pulling_records_then_paginate_till_the_server_does_not_have_anymore_records() {
    subject.updateLastPullTimestampTo(Instant.now().minusSeconds(1))

    val recordsToInsert = 2 * configProvider.blockingGet().batchSize + 7

    subject.insertDummyRecords(count = recordsToInsert)
        .andThen(subject.push())
        .andThen(Completable.fromAction({ database.clearAllTables() }))
        .blockingAwait()

    subject.pull().blockingAwait()

    val patientCountAfterPull = subject.recordCount().blockingGet()
    Truth.assertThat(patientCountAfterPull).isAtLeast(recordsToInsert)
  }

  @After
  fun tearDown() {
    database.clearAllTables()
    database.close()
  }
}
