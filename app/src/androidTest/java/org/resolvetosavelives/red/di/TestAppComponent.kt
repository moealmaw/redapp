package org.resolvetosavelives.red.di

import dagger.Component
import org.resolvetosavelives.red.sync.patient.PatientSyncAndroidTest
import org.resolvetosavelives.red.sync.SyncAndroidTest

@AppScope
@Component(modules = [AppModule::class])
interface TestAppComponent : AppComponent {

  fun inject(target: PatientSyncAndroidTest)
  fun inject(target: SyncAndroidTest)
}
