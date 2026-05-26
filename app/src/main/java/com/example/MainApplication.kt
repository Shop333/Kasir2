package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    // A long-running scope for application-level database operations
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { PosRepository(database.posDao()) }
}
