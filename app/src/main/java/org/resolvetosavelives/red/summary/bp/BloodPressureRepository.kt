package org.resolvetosavelives.red.summary.bp

import io.reactivex.Single
import org.resolvetosavelives.red.AppDatabase
import org.resolvetosavelives.red.newentry.search.PatientBloodPressure
import org.resolvetosavelives.red.newentry.search.SyncStatus
import javax.inject.Inject

class BloodPressureRepository @Inject constructor(private val database: AppDatabase) {

  fun bloodPressuresWithSyncStatus(syncStatus: SyncStatus): Single<List<PatientBloodPressure>>? {
    return database.bloodPressureDao()
        .withSyncStatus(syncStatus)
        .firstOrError()
  }
}
