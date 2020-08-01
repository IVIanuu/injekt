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

/*
import android.app.Application
import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.applicationStorage
import com.ivianuu.injekt.given

annotation class RunReaderInvocation

interface runApplicationReaderblockContext

inline fun <R> Application.runApplicationReader_(block: @Reader (runApplicationReaderblockContext) -> R): R =
    block(runApplicationReaderContextImpl(this))

class runApplicationReaderContextImpl(
    private val application: Application
) : runApplicationReaderblockContext, databaseContext {
    override fun application(): Application = application
}

@RunReaderInvocation
interface runApplicationReader {
    fun inputs(application: Application)
}

class Db(val application: Application)

@Given(ApplicationScoped::class)
fun database() = Db(given())

@Given(ApplicationScoped::class)
fun database_(context: databaseContext) = Db(context.application())

interface databaseContext {
    fun application(): Application
}

fun appMain(application: Application) {
    val db = application.runApplicationReader { given<Db>() }
    val db_ = database_(object : databaseContext {
        override fun application(): Application = application
    })
}

fun activityMain(application: Application) {
    val db = application.runApplicationReader { given<Db>() }

    val db_ = ApplicationScoped(135363) {
        database_(object : databaseContext {
            override fun application(): Application = application
        })
    }

    val db__ = applicationStorage.scope(135363) {
        database_(object : databaseContext {
            override fun application(): Application = application
        })
    }
}
*/