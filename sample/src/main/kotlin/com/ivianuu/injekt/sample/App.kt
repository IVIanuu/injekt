package com.ivianuu.injekt.sample

import android.app.Application
import com.ivianuu.injekt.Injekt

class App : Application() {
    override fun onCreate() {
        Injekt.initializeEndpoint()
        super.onCreate()
    }
}
