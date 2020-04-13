package com.ivianuu.injekt.comparison

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import com.ivianuu.injekt.comparison.base.defaultConfig
import com.ivianuu.injekt.comparison.base.runInjectionTests
import com.ivianuu.injekt.comparison.injekt.InjektTest
import com.ivianuu.injekt.injekt

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injekt { initializeEndpoint() }
        runInjectionTests(InjektTest, config = defaultConfig.copy(rounds = 1000))
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })

    }

}
