package com.tutozz.blespam

import android.app.Application

class BleSpamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.init(this)
    }
}