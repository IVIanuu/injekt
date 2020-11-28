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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.android.ApplicationContext
import com.ivianuu.injekt.merge.ApplicationComponent
import java.io.File

typealias DatabaseFile = File
@Scoped(ApplicationComponent::class)
@Binding
fun databaseFile(applicationContext: ApplicationContext): DatabaseFile =
    applicationContext.cacheDir

@Scoped(ApplicationComponent::class)
@Binding
class Database(private val file: DatabaseFile)

@Scoped(ApplicationComponent::class)
@Binding
class Repo(
    private val database: Database,
    private val api: Api
) {
    fun refresh() {
    }
}

@FunBinding
fun refreshRepo(repo: Repo) {
    repo.refresh()
}

@Scoped(ApplicationComponent::class)
@Binding
class Api
