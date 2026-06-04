package com.drejo.openeksin

import android.app.Application
import com.drejo.openeksin.data.SessionCookieImporter

class OpeneksinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionCookieImporter.applyIfPresent(this)
    }
}
