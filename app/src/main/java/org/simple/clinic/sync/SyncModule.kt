package org.simple.clinic.sync

import dagger.Module
import dagger.Provides
import io.reactivex.Single
import org.simple.clinic.BuildConfig
import org.simple.clinic.bp.BloodPressureModule
import org.simple.clinic.drugs.PrescriptionModule
import org.simple.clinic.patient.sync.PatientSyncModule
import org.threeten.bp.Duration
import retrofit2.Retrofit

@Module(includes = [PatientSyncModule::class, BloodPressureModule::class, PrescriptionModule::class])
open class SyncModule {

  @Provides
  fun retrofit(commonRetrofitBuilder: Retrofit.Builder): Retrofit {
    val baseUrl = BuildConfig.API_ENDPOINT
    return commonRetrofitBuilder
        .baseUrl(baseUrl)
        .build()
  }

  @Provides
  open fun syncConfig(): Single<SyncConfig> {
    // In the future, this may come from the server.
    return Single.just(SyncConfig(frequency = Duration.ofHours(1), batchSize = 50))
  }
}
