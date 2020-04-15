package com.ivianuu.injekt.comparison

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //injekt { initializeEndpoint() }
        //runInjectionTests(InjektTest, config = defaultConfig.copy(rounds = 1000))
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })

    }

}
