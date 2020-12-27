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

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.AppContext
import com.ivianuu.injekt.component.AppScoped
import java.io.File

typealias DatabaseFile = File

@AppScoped @Given fun databaseFile(@Given context: AppContext): DatabaseFile =
    context.cacheDir!!

@AppScoped @Given class Database(@Given private val file: DatabaseFile)

@AppScoped @Given class Repo(@Given private val api: Api) {
    fun refresh() {
    }
}

fun refreshRepo(@Given repo: Repo) {
    repo.refresh()
}

@Given object Api
