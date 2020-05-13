package com.ivianuu.injekt.comparison

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import com.ivianuu.injekt.comparison.base.defaultConfig
import com.ivianuu.injekt.comparison.base.runInjectionTests
import com.ivianuu.injekt.comparison.injekt.InjektTest

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runInjectionTests(InjektTest, config = defaultConfig.copy(rounds = 100000))
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })
    }
}
