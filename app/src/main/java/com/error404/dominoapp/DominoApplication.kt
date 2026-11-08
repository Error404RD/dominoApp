package com.error404.dominoapp

import android.app.Application
import com.error404.dominoapp.data.local.DominoDatabase

/**
 * Created once when the app starts. Holds the single database instance;
 * ViewModels get it from here.
 */
class DominoApplication : Application() {

    // `by lazy`: the database is opened the first time someone uses it, not before.
    val database: DominoDatabase by lazy { DominoDatabase.create(this) }
}
