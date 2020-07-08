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

package com.ivianuu.injekt.sample.proof

import android.app.Application
import com.ivianuu.injekt.DistinctType
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.sample.Repo

private val _applicationStorage: ApplicationStorage = Storage()

@Given
@Reader
fun provideApplicationStorage(): ApplicationStorage = _applicationStorage

@DistinctType
typealias ApplicationStorage = Storage

@Reader
inline fun <T> applicationScoped(
    key: Any = sourceLocation(),
    init: () -> T
) = given<ApplicationStorage>().scope(key, init)

@Reader
inline fun <T> applicationScoped(
    key: Any = sourceLocation(),
    init: () -> T,
    context: applicationScopedContext
) = context.applicationStorage.scope(key, init)

interface applicationScopedContext : ApplicationStorageContext {
    override val applicationStorage: ApplicationStorage
}

interface ApplicationStorageContext {
    val applicationStorage: ApplicationStorage
}

interface ApplicationContext {
    val application: Application
}

interface withApplicationContextContext<R>

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runReader {
            val repo: Repo = given()
            repo.refresh()
        }
        runReader {
            val repo: Repo = provideRepo(object : provideRepoContext {
                override val applicationStorage: ApplicationStorage
                    get() = provideApplicationStorage()
            })
            repo.refresh()
        }
    }

}

@Given
@Reader
fun provideRepo() = applicationScoped { Repo(given(), given()) }

@Given
@Reader
fun provideRepo(
    context: provideRepoContext
) = applicationScoped { Repo(given(), given()) }

interface provideRepoContext : ApplicationStorageContext

interface MyApp_onCreate_1Context : provideRepoContext {
    val repo: Repo
}
