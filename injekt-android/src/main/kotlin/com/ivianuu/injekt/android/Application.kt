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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.create
import com.ivianuu.injekt.merge.ApplicationComponent

val Application.applicationComponent: ApplicationComponent
    get() = ProcessLifecycleOwner.get().lifecycle.singleton { create(this) }

typealias ApplicationContext = Context

@Binding inline fun Application.provideAppContext(): ApplicationContext = this

typealias ApplicationResources = Resources

@Binding inline fun Application.provideApplicationResources(): ApplicationResources = resources

typealias ApplicationLifecycleOwner = LifecycleOwner

@Binding inline fun Application.provideApplicationLifecycleOwner(): ApplicationLifecycleOwner =
    ProcessLifecycleOwner.get()
