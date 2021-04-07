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
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class ChildGivenScopeFactory

class ChildGivenScopeModule<P : GivenScope, T, S : T> {
    @Given
    fun factory(
        @Given scopeFactory: S
    ): @GivenScopeElementBinding<P> @ChildGivenScopeFactory T = scopeFactory
}

fun <P : GivenScope, C : GivenScope> ChildGivenScopeModule0() =
    ChildGivenScopeModule<P, () -> C, () -> C>()

fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() =
    ChildGivenScopeModule<P, (P1) -> C, (@Given @GivenScopeElementBinding<C> P1) -> C>()

fun <P : GivenScope, P1, P2, C : GivenScope> ChildGivenScopeModule2() =
    ChildGivenScopeModule<P, (P1, P2) -> C,
                (@Given @GivenScopeElementBinding<C> P1,
                 @Given @GivenScopeElementBinding<C> P2) -> C>()

fun <P : GivenScope, P1, P2, P3, C : GivenScope> ChildGivenScopeModule3() =
    ChildGivenScopeModule<P, (P1, P2, P3) -> C,
                (@Given @GivenScopeElementBinding<C> P1,
                 @Given @GivenScopeElementBinding<C> P2,
                 @Given @GivenScopeElementBinding<C> P3) -> C>()

fun <P : GivenScope, P1, P2, P3, P4, C : GivenScope> ChildGivenScopeModule4() =
    ChildGivenScopeModule<P, (P1, P2, P3, P4) -> C,
            (@Given @GivenScopeElementBinding<C> P1,
             @Given @GivenScopeElementBinding<C> P2,
             @Given @GivenScopeElementBinding<C> P3,
             @Given @GivenScopeElementBinding<C> P4) -> C>()

fun <P : GivenScope, P1, P2, P3, P4, P5, C : GivenScope> ChildGivenScopeModule5() =
    ChildGivenScopeModule<P, (P1, P2, P3, P4, P5) -> C,
            (@Given @GivenScopeElementBinding<C> P1,
             @Given @GivenScopeElementBinding<C> P2,
             @Given @GivenScopeElementBinding<C> P3,
             @Given @GivenScopeElementBinding<C> P4,
             @Given @GivenScopeElementBinding<C> P5) -> C>()
