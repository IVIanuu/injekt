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

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.AndroidApplicationContext
import com.ivianuu.injekt.given
import java.io.File

typealias DatabaseFile = File

@Given
fun databaseFile(): DatabaseFile = given<AndroidApplicationContext>().cacheDir

@Given(ApplicationContext::class)
class Database {
    private val file: DatabaseFile = given()
}

@Given(ApplicationContext::class)
class Repo {
    private val database: Database = given()
    private val api: Api = given()
    fun refresh() {
    }
}

@Reader
fun refreshRepo() {
    given<Repo>().refresh()
}

@Given(ApplicationContext::class)
class Api
