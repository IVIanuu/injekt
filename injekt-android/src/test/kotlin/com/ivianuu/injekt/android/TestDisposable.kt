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

package com.ivianuu.injekt.android

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.scope.GivenScope
import com.ivianuu.injekt.scope.InstallElement
import com.ivianuu.injekt.scope.GivenScopeDisposable
import com.ivianuu.injekt.scope.Scoped

class TestGivenScopeDisposable<S : GivenScope> : GivenScopeDisposable {
    var disposed = false
    override fun dispose() {
        disposed = true
    }
}

@Given
inline fun <S : GivenScope> testGivenScopeDisposable():
        @InstallElement<S> @Scoped<S> TestGivenScopeDisposable<S> =
    TestGivenScopeDisposable()
