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

package com.ivianuu.injekt.samples.android

import android.app.Application
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.GenerateMergeComponents
import com.ivianuu.injekt.merge.MergeInto
import com.ivianuu.injekt.merge.mergeComponent

@GenerateMergeComponents
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        applicationComponent
            .mergeComponent<SampleAppComponent>()
            .run {
                initializeWorkers()
                refreshRepo()
            }
    }
}

@MergeInto(ApplicationComponent::class)
interface SampleAppComponent {
    val initializeWorkers: initializeWorkers
    val refreshRepo: refreshRepo
}
