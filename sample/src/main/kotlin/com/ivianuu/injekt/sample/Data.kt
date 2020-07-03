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

import android.content.Context
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.ForApplication
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.composition.installIn
import com.ivianuu.injekt.composition.reader
import com.ivianuu.injekt.get
import com.ivianuu.injekt.transient
import java.io.File

@Module
fun dataModule() {
    installIn<ApplicationComponent>()
    transient<@DatabaseFile File> { get<@ForApplication Context>().cacheDir }
}

@Target(AnnotationTarget.TYPE)
@Qualifier
annotation class DatabaseFile

@Scoped<ApplicationComponent>
class Database(private val file: @DatabaseFile File)

@Scoped<ApplicationComponent>
class Repo(private val database: Database, private val api: Api) {
    fun refresh() {
    }
}

@Reader
fun refreshRepo() {
    get<Repo>().refresh()
}

@Scoped<ApplicationComponent>
class Api
