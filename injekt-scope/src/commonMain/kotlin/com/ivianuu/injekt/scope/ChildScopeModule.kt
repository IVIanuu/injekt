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

@Qualifier annotation class ChildScopeFactory

abstract class AbstractChildScopeModule<P : Scope, T, S : T> {
  @Provide fun factory(scopeFactory: S): @ScopeElement<P> @ChildScopeFactory T = scopeFactory
}

class ChildScopeModule0<P : Scope, C : Scope> :
  AbstractChildScopeModule<P, () -> C, () -> C>()

class ChildScopeModule1<P : Scope, P1, C : Scope> : AbstractChildScopeModule<P,
      (P1) -> C,
      (@Provide @ScopeElement<C> P1) -> C>()

class ChildScopeModule2<P : Scope, P1, P2, C : Scope> : AbstractChildScopeModule<P,
      (P1, P2) -> C,
      (
  @Provide @ScopeElement<C> P1,
  @Provide @ScopeElement<C> P2
) -> C>()

class ChildScopeModule3<P : Scope, P1, P2, P3, C : Scope> : AbstractChildScopeModule<P,
      (P1, P2, P3) -> C,
      (
  @Provide @ScopeElement<C> P1,
  @Provide @ScopeElement<C> P2,
  @Provide @ScopeElement<C> P3
) -> C>()

class ChildScopeModule4<P : Scope, P1, P2, P3, P4, C : Scope> :
  AbstractChildScopeModule<P,
        (P1, P2, P3, P4) -> C,
        (
    @Provide @ScopeElement<C> P1,
    @Provide @ScopeElement<C> P2,
    @Provide @ScopeElement<C> P3,
    @Provide @ScopeElement<C> P4
  ) -> C>()

class ChildScopeModule5<P : Scope, P1, P2, P3, P4, P5, C : Scope> :
  AbstractChildScopeModule<P,
        (P1, P2, P3, P4, P5) -> C,
        (
    @Provide @ScopeElement<C> P1,
    @Provide @ScopeElement<C> P2,
    @Provide @ScopeElement<C> P3,
    @Provide @ScopeElement<C> P4,
    @Provide @ScopeElement<C> P5
  ) -> C>()
