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

import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.ApplicationContext
import com.ivianuu.injekt.given
import java.io.File

@Distinct
typealias DatabaseFile = File

@Given
@Reader
fun databaseFile(): DatabaseFile = given<ApplicationContext>().cacheDir

@Given(ApplicationComponent::class)
class Database(private val file: DatabaseFile)

@Given(ApplicationComponent::class)
class Repo(
    private val database: Database = given(),
    private val api: Api = given()
) {
    fun refresh() {
    }
}

@Reader
fun refreshRepo() {
    given<Repo>().refresh()
}

@Given(ApplicationComponent::class)
class Api
