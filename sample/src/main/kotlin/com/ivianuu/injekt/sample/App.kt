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
import androidx.fragment.app.Fragment
import androidx.work.Worker
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.MapEntries
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import kotlin.reflect.KClass

@MapEntries(ApplicationComponent::class)
@Reader
fun mySpecialWorker() = mapOf(
    "a" to "a",
    "b" to "b"
)

@Reader
inline fun <reified T : Worker> worker(): Map<KClass<out Worker>, () -> Worker> =
    mapOf(T::class to { given<T>() })

@Distinct
typealias Fragments = Map<String, Fragment>

@MapEntries(ApplicationComponent::class)
fun fragments(): Fragments = emptyMap()

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applicationComponent.runReader {
            initializeWorkers()
            startAppServices()
            refreshRepo()
        }
    }

}

