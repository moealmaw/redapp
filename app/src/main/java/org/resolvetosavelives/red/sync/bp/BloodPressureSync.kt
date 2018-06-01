package org.resolvetosavelives.red.sync.bp

import io.reactivex.Completable
import org.resolvetosavelives.red.summary.bp.BloodPressureRepository
import org.resolvetosavelives.red.sync.DataSync
import org.resolvetosavelives.red.sync.SyncApiV1
import javax.inject.Inject

class BloodPressureSync @Inject constructor(
    private val api: SyncApiV1,
    private val repository: BloodPressureRepository,
    private val dataSync: DataSync
) {

  fun sync(): Completable {
    return Completable.mergeArrayDelayError(push(), pull())
  }

  private fun push(): Completable {
    // TODO.
    return Completable.complete()
  }

  private fun pull(): Completable {
    // TODO.
    return Completable.complete()
  }
}
