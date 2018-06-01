package org.resolvetosavelives.red.newentry.search

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import com.squareup.moshi.Json
import io.reactivex.Flowable
import org.threeten.bp.Instant

@Entity
data class PatientBloodPressure(
    @PrimaryKey
    @Json(name = "id")
    val uuid: String,

    @Json(name = "patient_id")
    val patientUuid: String,

    @Json(name = "systolic")
    val systolic: Int,

    @Json(name = "diastolic")
    val diastolic: Int,

    @Json(name = "created_at")
    val createdAt: Instant,

    @Json(name = "updated_at")
    val updatedAt: Instant,

    val syncStatus: SyncStatus
) {

  @Dao
  interface RoomDao {

    @Query("SELECT * FROM patientbloodpressure WHERE syncStatus = :syncStatus")
    fun withSyncStatus(syncStatus: SyncStatus): Flowable<List<PatientBloodPressure>>
  }
}
