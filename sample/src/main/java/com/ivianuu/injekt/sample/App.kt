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

import android.app.Application
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.AndroidLogger
import com.ivianuu.injekt.android.applicationComponent

@KindRegistry([Fancy::class])
object MyScope : Scope

@ScopeAnnotation(MyScope::class)
@KindAnnotation(FancyKind::class)
annotation class Fancy

object FancyKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): com.ivianuu.injekt.Instance<T> =
        Instance(binding)

    private class Instance<T>(override val binding: Binding<T>) : com.ivianuu.injekt.Instance<T>() {
        override fun get(
            requestingContext: DefinitionContext,
            parameters: ParametersDefinition?
        ): T = create(requestingContext, parameters)
    }
}

@Fancy
class AClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), InjektTrait {

    override val component by lazy { applicationComponent() }

    override fun onCreate() {
        InjektPlugins.logger = AndroidLogger()

        d { "Injected app dependency ${get<AppDependency>()}" }

        super.onCreate()
    }
}

@Single @ScopeAnnotation(ApplicationScope::class)
class AppDependency(val app: App)