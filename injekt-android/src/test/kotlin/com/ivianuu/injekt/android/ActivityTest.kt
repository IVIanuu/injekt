package com.ivianuu.injekt.android

import android.app.Activity
import android.os.Build
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.get
import com.ivianuu.injekt.scoped
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ActivityTest {

    @Test
    fun testActivityComponent() = withInitializedEndpoint {
        launchActivity<TestActivity>().onActivity { activity ->
            assertSame(
                activity,
                activity.activityComponent.get<Activity>(TestQualifier1::class)
            )
        }
    }

    @Test
    fun testRetainedActivityComponent() = withInitializedEndpoint {
        var dep1: TestDep1? = null
        val scenario = launchActivity<TestActivity>()
        scenario.onActivity { dep1 = it.retainedActivityComponent.get<TestDep1>() }
        var dep2: TestDep1? = null
        scenario.onActivity { dep2 = it.retainedActivityComponent.get<TestDep1>() }
        assertNotNull(dep1)
        assertNotNull(dep2)
        assertSame(dep1, dep2)
    }

    companion object {
        @ActivityScoped
        @Module
        fun testActivityModule() {
            alias<Activity>(aliasQualifier = TestQualifier1::class)
        }

        @RetainedActivityScoped
        @Module
        fun testRetainedActivityModule() {
            scoped { TestDep1() }
        }
    }
}
