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

package com.ivianuu.injekt.sample.data

import com.ivianuu.injekt.ApplicationScope
import com.ivianuu.injekt.Single
import java.io.File

@ApplicationScope
@Single
class Database(@DatabaseFile private val file: File) {

    private var _cached: List<String>? = null

    fun getItems(): List<String>? = _cached

    fun setItems(items: List<String>) {
        _cached = items
    }
}
