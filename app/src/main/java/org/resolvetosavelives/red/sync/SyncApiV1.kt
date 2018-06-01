package org.resolvetosavelives.red.sync

import io.reactivex.Single
import org.resolvetosavelives.red.sync.patient.PatientPullResponse
import org.resolvetosavelives.red.sync.patient.PatientPushRequest
import org.threeten.bp.Instant
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApiV1 {

  companion object {
    const val version = "v1"
  }

  @POST("$version/patients/sync")
  fun push(
      @Body body: PatientPushRequest
  ): Single<DataPushResponse>

  @GET("$version/patients/sync")
  fun pull(
      @Query("limit") recordsToPull: Int,
      @Query("first_time") isFirstPull: Boolean
  ): Single<PatientPullResponse>

  @GET("$version/patients/sync")
  fun pull(
      @Query("limit") recordsToPull: Int,
      @Query("processed_since") latestRecordTimestamp: Instant? = null
  ): Single<PatientPullResponse>
}
