package com.error404.dominoapp

import android.app.Application
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.repository.MatchRepository

/**
 * Created once when the app starts. Holds the single database and repository
 * instances; ViewModels get the repository from here.
 */
open class DominoApplication : Application() {

    // `by lazy`: created the first time someone uses it, not before.
    // `open`: instrumented tests override it with an in-memory database.
    open val database: DominoDatabase by lazy { DominoDatabase.create(this) }
    val matchRepository: MatchRepository by lazy { MatchRepository(database) }
}
