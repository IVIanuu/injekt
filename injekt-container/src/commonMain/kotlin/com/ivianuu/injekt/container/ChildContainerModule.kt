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

package com.ivianuu.injekt.container

import com.ivianuu.injekt.*

@Tag annotation class ChildContainerFactory

abstract class AbstractChildContainerModule<P, T, S : T> {
  @Provide fun factory(scopeFactory: S):
      @ContainerElement<P> @ChildContainerFactory T = scopeFactory
}

class ChildContainerModule0<P, C> :
  AbstractChildContainerModule<P, () -> Container<C>, () -> Container<C>>()

class ChildContainerModule1<P, P1, C> : AbstractChildContainerModule<P,
      (P1) -> Container<C>,
      (@Provide @ContainerElement<C> P1) -> Container<C>>()

class ChildContainerModule2<P, P1, P2, C> : AbstractChildContainerModule<P,
      (P1, P2) -> Container<C>,
      (
  @Provide @ContainerElement<C> P1,
  @Provide @ContainerElement<C> P2
) -> Container<C>>()

class ChildContainerModule3<P, P1, P2, P3, C> : AbstractChildContainerModule<P,
      (P1, P2, P3) -> Container<C>,
      (
  @Provide @ContainerElement<C> P1,
  @Provide @ContainerElement<C> P2,
  @Provide @ContainerElement<C> P3
) -> Container<C>>()

class ChildContainerModule4<P, P1, P2, P3, P4, C> :
  AbstractChildContainerModule<P,
        (P1, P2, P3, P4) -> Container<C>,
        (
    @Provide @ContainerElement<C> P1,
    @Provide @ContainerElement<C> P2,
    @Provide @ContainerElement<C> P3,
    @Provide @ContainerElement<C> P4
  ) -> Container<C>>()

class ChildContainerModule5<P, P1, P2, P3, P4, P5, C> :
  AbstractChildContainerModule<P,
        (P1, P2, P3, P4, P5) -> Container<C>,
        (
    @Provide @ContainerElement<C> P1,
    @Provide @ContainerElement<C> P2,
    @Provide @ContainerElement<C> P3,
    @Provide @ContainerElement<C> P4,
    @Provide @ContainerElement<C> P5
  ) -> Container<C>>()
