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

import android.content.Context
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.single
import java.io.File

@QualifierMarker
annotation class WebApiUrl {
    companion object : Qualifier.Element
}

@QualifierMarker
annotation class DatabaseFile {
    companion object : Qualifier.Element
}

fun ComponentBuilder.data() {
    single(qualifier = WebApiUrl) { "https://baseurl/" }
    single(qualifier = DatabaseFile) { File(get<Context>().cacheDir.absolutePath + "/db") }
}
