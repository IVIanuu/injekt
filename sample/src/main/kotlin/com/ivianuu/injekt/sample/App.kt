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

import android.app.Application
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.childFactory
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.scope

class App : Application() {
    val appComponent by lazy { AppComponent.create(this) }

    @Inject
    private lateinit var repo: Repo
    override fun onCreate() {
        super.onCreate()
        appComponent.injectApp(this)
        repo.refresh()
        println("injected app $repo")
    }
}

@Scope
annotation class ApplicationScoped

interface AppComponent {
    val injectApp: @MembersInjector (App) -> Unit
    val mainActivityComponentFactory: @ChildFactory (MainActivity) -> MainActivityComponent

    companion object {
        @Factory
        fun create(app: App): AppComponent {
            scope<ApplicationScoped>()
            instance(app)
            dataModule()
            childFactory(MainActivityComponent::create)
            return createImpl()
        }
    }
}
