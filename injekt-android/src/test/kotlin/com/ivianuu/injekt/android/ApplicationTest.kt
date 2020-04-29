package com.ivianuu.injekt.android

import android.app.Application
import android.os.Build
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.injekt.ApplicationScoped
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.get
import junit.framework.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ApplicationTest {

    @Test
    fun testApplicationComponent() = withInitializedEndpoint {
        launchActivity<TestActivity>().onActivity { activity ->
            assertSame(
                activity.application,
                activity.application.applicationComponent.get<Application>(TestQualifier1::class)
            )
        }
    }

    companion object {
        @ApplicationScoped
        @Module
        fun testAppModule() {
            alias<Application>(aliasQualifier = TestQualifier1::class)
        }
    }
}
