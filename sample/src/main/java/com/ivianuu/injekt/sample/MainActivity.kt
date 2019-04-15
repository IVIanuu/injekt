/*
 * Copyright 2018 Manuel Wrage
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
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.PerActivity
import com.ivianuu.injekt.android.activityComponent

class MainActivity : AppCompatActivity(), InjektTrait {

    override val component by lazy {
        activityComponent {
            modules(mainActivityModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ParentFragment())
                .commit()
        }
    }

}

val mainActivityModule = module {
    single(scope = PerActivity) { MainActivityDependency(get(), get()) }
}

class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)