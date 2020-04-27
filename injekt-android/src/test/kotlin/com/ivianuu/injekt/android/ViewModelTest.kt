package com.ivianuu.injekt.android

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import junit.framework.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ViewModelTest {

    @Test
    fun testViewModel() {
        val activityScenario = launchActivity<TestActivity>()
        var viewModelA: TestViewModel? = null
        activityScenario.onActivity { activity ->
            val component = Component(Module {
                factory { activity.viewModelStore }
                viewModel { TestViewModel(get()) }
            })

            viewModelA = component.get<TestViewModel>()
        }
        activityScenario.recreate()
        var viewModelB: TestViewModel? = null
        activityScenario.onActivity { activity ->
            val component = Component(Module {
                factory { activity.viewModelStore }
                viewModel { TestViewModel(get()) }
            })

            viewModelB = component.get<TestViewModel>()
        }

        assertSame(viewModelA, viewModelB)
    }

    class TestViewModel(private val component: Component) : ViewModel()

}