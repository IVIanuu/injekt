package com.ivianuu.injekt.android

import android.os.PowerManager
import androidx.test.core.app.ActivityScenario
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import com.ivianuu.injekt.common.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
