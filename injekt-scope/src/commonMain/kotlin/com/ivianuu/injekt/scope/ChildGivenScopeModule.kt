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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.*

@Qualifier
annotation class ChildScopeFactory

abstract class AbstractChildScopeModule<P : GivenScope, T, S : T> {
    @Given
    fun factory(
        @Given scopeFactory: S
    ): @InstallElement<P> @ChildScopeFactory T = scopeFactory
}

class ChildScopeModule0<P : GivenScope, C : GivenScope> :
    AbstractChildScopeModule<P, () -> C, () -> C>()

class ChildScopeModule1<P : GivenScope, P1, C : GivenScope> : AbstractChildScopeModule<P,
            (P1) -> C,
            (@Given @InstallElement<C> P1) -> C>()

class ChildScopeModule2<P : GivenScope, P1, P2, C : GivenScope> : AbstractChildScopeModule<P,
            (P1, P2) -> C,
            (@Given @InstallElement<C> P1,
             @Given @InstallElement<C> P2) -> C>()

class ChildScopeModule3<P : GivenScope, P1, P2, P3, C : GivenScope> : AbstractChildScopeModule<P,
            (P1, P2, P3) -> C,
            (@Given @InstallElement<C> P1,
             @Given @InstallElement<C> P2,
             @Given @InstallElement<C> P3) -> C>()

class ChildScopeModule4<P : GivenScope, P1, P2, P3, P4, C : GivenScope> : AbstractChildScopeModule<P,
            (P1, P2, P3, P4) -> C,
            (@Given @InstallElement<C> P1,
             @Given @InstallElement<C> P2,
             @Given @InstallElement<C> P3,
             @Given @InstallElement<C> P4) -> C>()

class ChildScopeModule5<P : GivenScope, P1, P2, P3, P4, P5, C : GivenScope> : AbstractChildScopeModule<P,
            (P1, P2, P3, P4, P5) -> C,
            (@Given @InstallElement<C> P1,
             @Given @InstallElement<C> P2,
             @Given @InstallElement<C> P3,
             @Given @InstallElement<C> P4,
             @Given @InstallElement<C> P5) -> C>()
