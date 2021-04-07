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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.ForTypeKey

class ChildGivenScopeModule0<P : GivenScope, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (@Given Unit) -> S // todo change type to () -> S once fixed
    ): @GivenScopeElementBinding<P> () -> S = { scopeFactory(Unit) }
}

class ChildGivenScopeModule1<P : GivenScope, P1, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (@Given @GivenScopeElementBinding<S> P1) -> S
    ): @GivenScopeElementBinding<P> (P1) -> S = scopeFactory
}

class ChildGivenScopeModule2<P : GivenScope, P1, P2, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2
        ) -> S
    ): @GivenScopeElementBinding<P> (P1, P2) -> S = scopeFactory
}

class ChildGivenScopeModule3<P : GivenScope, P1, P2, P3, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2,
            @Given @GivenScopeElementBinding<S> P3
        ) -> S
    ): @GivenScopeElementBinding<P> (P1, P2, P3) -> S = scopeFactory
}

class ChildGivenScopeModule4<P : GivenScope, P1, P2, P3, P4, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2,
            @Given @GivenScopeElementBinding<S> P3,
            @Given @GivenScopeElementBinding<S> P4
        ) -> S
    ): @GivenScopeElementBinding<P> (P1, P2, P3, P4) -> S = scopeFactory
}

class ChildGivenScopeModule5<P : GivenScope, P1, P2, P3, P4, P5, S : GivenScope> {
    @Given
    fun factory(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2,
            @Given @GivenScopeElementBinding<S> P3,
            @Given @GivenScopeElementBinding<S> P4,
            @Given @GivenScopeElementBinding<S> P5
        ) -> S
    ): @GivenScopeElementBinding<P> (P1, P2, P3, P4, P5) -> S = scopeFactory
}
