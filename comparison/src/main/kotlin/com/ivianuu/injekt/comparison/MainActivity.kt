package com.ivianuu.injekt.comparison

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import com.ivianuu.injekt.comparison.injekt.InjektTest
import com.ivianuu.injekt.injekt

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injekt { initializeEndpoint() }
        runInjectionTests(InjektTest)
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })

    }

}
