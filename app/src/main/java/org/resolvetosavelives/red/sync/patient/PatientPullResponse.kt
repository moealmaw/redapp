package org.resolvetosavelives.red.sync.patient

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.resolvetosavelives.red.sync.DataSync
import org.threeten.bp.Instant

@JsonClass(generateAdapter = true)
data class PatientPullResponse(

    @Json(name = "patients")
    override val records: List<PatientPayload>,

    @Json(name = "processed_since")
    override val latestRecordTimestamp: Instant

) : DataSync.DataPullResponse<PatientPayload>
