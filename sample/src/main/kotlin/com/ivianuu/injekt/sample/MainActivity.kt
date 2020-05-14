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

package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.android.ApplicationComponent
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.entryPoint
import com.ivianuu.injekt.entryPointOf
import com.ivianuu.injekt.installIn
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.scope

class MainActivity : AppCompatActivity() {
    val activityComponent by lazy {
        entryPointOf<MainActivityFactoryHolder>(application.applicationComponent)
            .mainActivityComponentFactory(this)
    }

    @Inject
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent.injectMainActivity(this)
        println("Got view model $viewModel")
    }
}

interface MainActivityFactoryHolder {
    val mainActivityComponentFactory: @ChildFactory (MainActivity) -> MainActivityComponent
}

@Module
fun mainActivityFactoryHolderModule() {
    installIn<ApplicationComponent>()
    entryPoint<MainActivityFactoryHolder>()
}

interface MainActivityComponent {
    val injectMainActivity: @MembersInjector (MainActivity) -> Unit

    companion object {
        @ChildFactory
        fun create(mainActivity: MainActivity): MainActivityComponent {
            scope<ActivityScoped>()
            instance(mainActivity)
            return createImpl()
        }
    }
}

@Scope
annotation class ActivityScoped

@ActivityScoped
class MainViewModel(private val repo: Repo)
