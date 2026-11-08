package com.error404.dominoapp

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.runner.AndroidJUnitRunner
import com.error404.dominoapp.data.local.DominoDatabase

/**
 * Starts the app under test with [TestDominoApplication] instead of the real one,
 * so instrumented tests never touch the matches saved on the device.
 * Registered as `testInstrumentationRunner` in app/build.gradle.
 */
class DominoTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestDominoApplication::class.java.name, context)
}

/** Same app, but the database lives in memory and disappears when the tests end. */
class TestDominoApplication : DominoApplication() {
    override val database: DominoDatabase by lazy {
        Room.inMemoryDatabaseBuilder(this, DominoDatabase::class.java).build()
    }
}
