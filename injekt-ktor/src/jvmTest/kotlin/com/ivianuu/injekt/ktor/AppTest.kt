/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.booleans.*
import io.ktor.server.testing.*
import org.junit.*

@GivenImports(
    "com.ivianuu.injekt.common.*",
    "com.ivianuu.injekt.scope.*"
)
class AppTest {
    @Test
    fun testServerLifecycle() {
        lateinit var listener: ScopeDisposeListener
        withTestApplication({
            initializeAppGivenScope()
            listener = appGivenScope.element()
            listener.disposed.shouldBeFalse()
        }) {
        }
        listener.disposed.shouldBeTrue()
    }
}

@Given
@Scoped<AppGivenScope>
@InstallElement<AppGivenScope>
class ScopeDisposeListener : GivenScopeDisposable {
    var disposed = false
    override fun dispose() {
        disposed = true
    }
}
