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

import androidx.fragment.app.Fragment
import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.GenerateDsl
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.SideEffectBehavior
import com.ivianuu.injekt.map

@GenerateDsl(
    generateBuilder = true,
    builderName = "fragment",
    generateDelegate = true
)
@BehaviorMarker
val BindFragment = SideEffectBehavior {
    map<String, Fragment> {
        put(it.key.classifier.java.name, it.key as Key<out Fragment>)
    }
} + Factory

