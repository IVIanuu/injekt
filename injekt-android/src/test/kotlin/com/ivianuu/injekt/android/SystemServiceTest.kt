package com.ivianuu.injekt.android

import android.os.*
import androidx.test.core.app.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import org.junit.*
import org.junit.runner.*
import org.robolectric.*
import org.robolectric.annotation.*

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class SystemServiceTest {
    @Test
    fun testCanRequestSystemService() {
        val scenario = ActivityScenario.launch(AndroidTestActivity::class.java)
        scenario.onActivity {
            with(it.application as AppContext) {
                given<@SystemService PowerManager>()
                    .isPowerSaveMode
            }
        }
    }
}
