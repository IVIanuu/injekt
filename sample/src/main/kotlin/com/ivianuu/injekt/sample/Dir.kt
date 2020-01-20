/*
 * Copyright 2019 Manuel Wrage
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

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.android.ForActivity
import com.ivianuu.injekt.keyOf

@Factory
object Dir : UnlinkedBinding<DetailViewModel>() {
    private val stringKey = keyOf<String>(ForActivity)
    private val listKey = keyOf<Map<String, CharSequence>>()
    override fun link(linker: Linker): LinkedBinding<DetailViewModel> {
        return Linked(
            linker.get(stringKey),
            linker.get(listKey)
        )
    }

    private class Linked internal constructor(
        private val stringBinding: LinkedBinding<String>,
        private val listKey: LinkedBinding<List<Map<String, CharSequence>>>
    ) : LinkedBinding<DetailViewModel>() {
        override fun invoke(parameters: ParametersDefinition?): DetailViewModel {
            val parametersVar = parameters!!()
            return DetailViewModel(
                parametersVar[0],
                stringBinding(),
                listKey()
            )
        }
    }
}