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

class ChildGivenScopeModule0<P : GivenScope, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(@Given scopeFactory: () -> S): () -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): () -> S = parent.element()
}

class ChildGivenScopeModule1<P : GivenScope, @ForTypeKey P1, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(
        @Given scopeFactory: (@Given @GivenScopeElementBinding<S> P1) -> S
    ): (P1) -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): (P1) -> S = parent.element()

    @Given
    fun p1(@Given scope: S): P1 = scope.element()
}

class ChildGivenScopeModule2<P : GivenScope, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2
        ) -> S
    ): (P1, P2) -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): (P1, P2) -> S = parent.element()

    @Given
    fun p1(@Given scope: S): P1 = scope.element()

    @Given
    fun p2(@Given scope: S): P2 = scope.element()
}

class ChildGivenScopeModule3<P : GivenScope, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(
        @Given scopeFactory: (@Given P1, @Given P2, @Given P3) -> S
    ): (P1, P2, P3) -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): (P1, P2, P3) -> S = parent.element()

    @Given
    fun p1(@Given scope: S): P1 = scope.element()

    @Given
    fun p2(@Given scope: S): P2 = scope.element()

    @Given
    fun p3(@Given scope: S): P3 = scope.element()
}

class ChildGivenScopeModule4<P : GivenScope, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey P4, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2,
            @Given @GivenScopeElementBinding<S> P3,
            @Given @GivenScopeElementBinding<S> P4
        ) -> S
    ): (P1, P2, P3, P4) -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4) -> S = parent.element()

    @Given
    fun p1(@Given scope: S): P1 = scope.element()

    @Given
    fun p2(@Given scope: S): P2 = scope.element()

    @Given
    fun p3(@Given scope: S): P3 = scope.element()

    @Given
    fun p4(@Given scope: S): P4 = scope.element()
}

class ChildGivenScopeModule5<P : GivenScope, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey P4, @ForTypeKey P5, @ForTypeKey S : GivenScope> {
    @GivenScopeElementBinding<P>
    @Given
    fun factoryElement(
        @Given scopeFactory: (
            @Given @GivenScopeElementBinding<S> P1,
            @Given @GivenScopeElementBinding<S> P2,
            @Given @GivenScopeElementBinding<S> P3,
            @Given @GivenScopeElementBinding<S> P4,
            @Given @GivenScopeElementBinding<S> P5
        ) -> S
    ): (P1, P2, P3, P4, P5) -> S = scopeFactory

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4, P5) -> S = parent.element()

    @Given
    fun p1(@Given scope: S): P1 = scope.element()

    @Given
    fun p2(@Given scope: S): P2 = scope.element()

    @Given
    fun p3(@Given scope: S): P3 = scope.element()

    @Given
    fun p4(@Given scope: S): P4 = scope.element()

    @Given
    fun p5(@Given scope: S): P5 = scope.element()
}
