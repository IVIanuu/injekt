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

package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.CompositionFactory
import com.ivianuu.injekt.EntryPoint
import com.ivianuu.injekt.InstallIn
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.entryPointOf
import com.ivianuu.injekt.parent
import com.ivianuu.injekt.scope

@Scope
annotation class RetainedActivityScoped

interface RetainedActivityComponent

val ComponentActivity.retainedActivityComponent: RetainedActivityComponent
    get() {
        val holder = ViewModelProvider(this, RetainedActivityComponentHolder.Factory)
            .get(RetainedActivityComponentHolder::class.java)

        synchronized(holder) {
            if (holder.component == null) {
                holder.component =
                    entryPointOf<RetainedActivityComponentFactoryOwner>(
                        application.applicationComponent
                    )
                        .retainedActivityComponentFactory()
            }
        }

        return holder.component!!
    }

@CompositionFactory
fun createRetainedActivityComponent(): RetainedActivityComponent {
    parent<ApplicationComponent>()
    scope<RetainedActivityScoped>()
    return createImpl()
}

@InstallIn<ApplicationComponent>
@EntryPoint
interface RetainedActivityComponentFactoryOwner {
    val retainedActivityComponentFactory: @ChildFactory () -> RetainedActivityComponent
}

private class RetainedActivityComponentHolder : ViewModel() {
    var component: RetainedActivityComponent? = null

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RetainedActivityComponentHolder() as T
    }
}
