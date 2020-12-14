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
import com.ivianuu.injekt.android.ApplicationContext
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Storage
import com.ivianuu.injekt.component.memo
import com.ivianuu.injekt.given
import java.io.File

typealias DatabaseFile = File

@Given fun databaseFile(
    context: ApplicationContext = given,
    storage: Storage<ApplicationScoped> = given,
): DatabaseFile = storage.memo("db_file") { context.cacheDir!! }

@Given fun database(file: DatabaseFile = given, storage: Storage<ApplicationScoped> = given) =
    storage.memo("db") {
        Database()
    }

class Database(private val file: DatabaseFile = given)

@Given fun repo(storage: Storage<ApplicationScoped> = given) = storage.memo("repo") {
    Repo()
}

class Repo(private val api: Api = given) {
    fun refresh() {
    }
}

fun refreshRepo(repo: Repo = given) {
    repo.refresh()
}

@Given object Api
