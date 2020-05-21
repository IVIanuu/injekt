/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivianuu.injekt.test.codegen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AndroidEntryPointTest {

    @Test
    fun testAppEntryPoint() = codegen(
        """
        @AndroidEntryPoint
        class MyApp : android.app.Application() { 
            val foo: Foo by inject()
        }
        
        @Module
        fun fooModule() {
            installIn<ApplicationComponent>()
            transient<Foo>()
        }
    """
    )

    @Test
    fun testActivityEntryPoint() = codegen(
        """
        @AndroidEntryPoint
        class MyActivity : androidx.activity.ComponentActivity() { 
            val foo: Foo by inject()
        }
        
        @Module
        fun fooModule() {
            installIn<ActivityComponent>()
            transient<Foo>()
        }
    """
    )

    @Test
    fun testFragmentEntryPoint() = codegen(
        """
        @AndroidEntryPoint
        class MyFragment : androidx.fragment.app.Fragment() { 
            val foo: Foo by inject()
        }
        
        @Module
        fun fooModule() {
            installIn<FragmentComponent>()
            transient<Foo>()
        }
    """
    )

    @Test
    fun testReceiverEntryPoint() = codegen(
        """
        @AndroidEntryPoint
        class MyReceiver : android.content.BroadcastReceiver() { 
            val foo: Foo by inject()
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                
            }
        }
        
        @Module
        fun fooModule() {
            installIn<ReceiverComponent>()
            transient<Foo>()
        }
    """
    )

    @Test
    fun testServiceEntryPoint() = codegen(
        """
        @AndroidEntryPoint
        class MyService : android.app.Service() { 
            val foo: Foo by inject()
            override fun onBind(intent: android.content.Intent?): android.os.IBinder? = null
        }
        
        @Module
        fun fooModule() {
            installIn<ServiceComponent>()
            transient<Foo>()
        }
    """
    )

}
