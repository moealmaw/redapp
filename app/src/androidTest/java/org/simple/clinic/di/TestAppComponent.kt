package org.simple.clinic.di

import dagger.Component
import org.simple.clinic.TestClinicApp
import org.simple.clinic.bp.sync.BloodPressureSyncAndroidTest
import org.simple.clinic.drugs.PrescriptionRepositoryAndroidTest
import org.simple.clinic.drugs.sync.PrescriptionSyncAndroidTest
import org.simple.clinic.patient.PatientRepositoryAndroidTest
import org.simple.clinic.patient.PatientSyncAndroidTest
import org.simple.clinic.user.UserSessionAndroidTest

@AppScope
@Component(modules = [AppModule::class])
interface TestAppComponent : AppComponent {

  fun inject(target: TestClinicApp)
  fun inject(target: UserSessionAndroidTest)
  fun inject(target: PatientSyncAndroidTest)
  fun inject(target: PrescriptionSyncAndroidTest)
  fun inject(target: BloodPressureSyncAndroidTest)
  fun inject(target: PatientRepositoryAndroidTest)
  fun inject(target: PrescriptionRepositoryAndroidTest)
}
