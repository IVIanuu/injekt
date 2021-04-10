package com.ivianuu.injekt.android

import android.os.PowerManager
import androidx.test.core.app.ActivityScenario
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.scope.InstallElement
import com.ivianuu.injekt.scope.element
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
            it.activityGivenScope.element<SystemServiceComponent>()
                .powerManager
                .isPowerSaveMode
        }
    }
}

@Given
@InstallElement<ActivityGivenScope>
class SystemServiceComponent(@Given val powerManager: @SystemService PowerManager)
