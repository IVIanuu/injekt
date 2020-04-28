package com.ivianuu.injekt.android

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Injekt
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.get
import com.ivianuu.injekt.instance
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FragmentTest {

    @Factory
    class FragmentA(private val activity: TestActivity) : Fragment()
    class FragmentB(private val activity: TestActivity) : Fragment()

    @Test
    fun testFragmentFactory() {
        Injekt.initializeEndpoint()

        launchActivity<TestActivity>().onActivity { activity ->
            val component = Component(FragmentInjectionModule, Module {
                instance(activity)
                bindFragment<FragmentA>()
                fragment { FragmentB(get()) }
            })

            val factory = component.get<FragmentFactory>()
            assertTrue(
                factory.instantiate(
                    FragmentTest::class.java.classLoader!!,
                    FragmentA::class.java.name
                ) is FragmentA
            )
            assertTrue(
                factory.instantiate(
                    FragmentTest::class.java.classLoader!!,
                    FragmentB::class.java.name
                ) is FragmentB
            )
        }
    }

}