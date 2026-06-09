package com.example.logicflow

import android.app.Application
import com.example.logicflow.data.di.AppContainer

class LogicFlowApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
