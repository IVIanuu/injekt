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

package com.ivianuu.injekt.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.asSingle
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.getBinding

fun ModuleBuilder.sharedPreferences(
    sharedPreferencesName: String,
    sharedPreferencesMode: Int = Context.MODE_PRIVATE,
    name: Any? = null
) {
    bind(SharedPreferencesBinding(sharedPreferencesName, sharedPreferencesMode).asSingle(), name)
}

private class SharedPreferencesBinding(
    private val sharedPreferencesName: String,
    private val sharedPreferencesMode: Int
) : Binding<SharedPreferences> {
    private lateinit var appBinding: Binding<Application>
    override fun attach(component: Component) {
        appBinding = component.getBinding()
    }

    override fun get(parameters: ParametersDefinition?): SharedPreferences {
        return appBinding().getSharedPreferences(
            sharedPreferencesName,
            sharedPreferencesMode
        )
    }
}