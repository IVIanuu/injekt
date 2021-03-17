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

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

class ChildComponentModule0<P : Component, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<() -> C>() to {
        val factory: () -> C = {
            builderProvider()
                .dependency(parent)
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): () -> C = parent.element()
}

class ChildComponentModule1<P : Component, @ForTypeKey P1, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<(P1) -> C>() to {
        val factory: (P1) -> C = { p1 ->
            builderProvider()
                .dependency(parent)
                .element { p1 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1) -> C = parent.element()

    @Given
    fun p1(@Given component: C): P1 = component.element()
}

class ChildComponentModule2<P : Component, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<(P1, P2) -> C>() to {
        val factory: (P1, P2) -> C = { p1, p2 ->
            builderProvider()
                .dependency(parent)
                .element { p1 }
                .element { p2 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2) -> C = parent.element()

    @Given
    fun p1(@Given component: C): P1 = component.element()

    @Given
    fun p2(@Given component: C): P2 = component.element()
}

class ChildComponentModule3<P : Component, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<(P1, P2, P3) -> C>() to {
        val factory: (P1, P2, P3) -> C = { p1, p2, p3 ->
            builderProvider()
                .dependency(parent)
                .element { p1 }
                .element { p2 }
                .element { p3 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3) -> C = parent.element()

    @Given
    fun p1(@Given component: C): P1 = component.element()

    @Given
    fun p2(@Given component: C): P2 = component.element()

    @Given
    fun p3(@Given component: C): P3 = component.element()
}

class ChildComponentModule4<P : Component, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey P4, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<(P2, P2, P3, P4) -> C>() to {
        val factory: (P1, P2, P3, P4) -> C = { p1, p2, p3, p4 ->
            builderProvider()
                .dependency(parent)
                .element { p1 }
                .element { p2 }
                .element { p3 }
                .element { p4 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4) -> C = parent.element()

    @Given
    fun p1(@Given component: C): P1 = component.element()

    @Given
    fun p2(@Given component: C): P2 = component.element()

    @Given
    fun p3(@Given component: C): P3 = component.element()

    @Given
    fun p4(@Given component: C): P4 = component.element()
}

class ChildComponentModule5<P : Component, @ForTypeKey P1, @ForTypeKey P2, @ForTypeKey P3, @ForTypeKey P4, @ForTypeKey P5, @ForTypeKey C : Component> {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given builderProvider: () -> Component.Builder<C>
    ): ComponentElement<P> = typeKeyOf<(P1, P2, P3, P4, P5) -> C>() to {
        val factory: (P1, P2, P3, P4, P5) -> C = { p1, p2, p3, p4, p5 ->
            builderProvider()
                .dependency(parent)
                .element { p1 }
                .element { p2 }
                .element { p3 }
                .element { p4 }
                .element { p5 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4, P5) -> C = parent.element()

    @Given
    fun p1(@Given component: C): P1 = component.element()

    @Given
    fun p2(@Given component: C): P2 = component.element()

    @Given
    fun p3(@Given component: C): P3 = component.element()

    @Given
    fun p4(@Given component: C): P4 = component.element()

    @Given
    fun p5(@Given component: C): P5 = component.element()
}
