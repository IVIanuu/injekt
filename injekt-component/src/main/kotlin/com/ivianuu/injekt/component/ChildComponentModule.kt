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

class ChildComponentModule0<P : Component, C : Component>(
    private val childComponentFactoryKey: TypeKey<() -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: () -> Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: () -> C = {
            componentBuilder()
                .dependency(parent)
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): () -> C = parent.element(childComponentFactoryKey)

    companion object {
        operator fun <P : Component, @ForTypeKey C : Component> invoke() = ChildComponentModule0<P, C>(
            typeKeyOf()
        )
    }
}

class ChildComponentModule1<P : Component, P1, C : Component>(
    private val p1Key: TypeKey<P1>,
    private val childComponentFactoryKey: TypeKey<(P1) -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: (P1) -> C = { p1 ->
            componentBuilder
                .dependency(parent)
                .element(p1Key) { p1 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1) -> C = parent.element(childComponentFactoryKey)

    @Given
    fun p1(@Given component: C): P1 = component.element(p1Key)!!

    companion object {
        operator fun <P : Component, @ForTypeKey P1, @ForTypeKey C : Component> invoke() = ChildComponentModule1<P, P1, C>(
            typeKeyOf(),
            typeKeyOf()
        )
    }
}

class ChildComponentModule2<P : Component, P1, P2, C : Component>(
    private val p1Key: TypeKey<P1>,
    private val p2Key: TypeKey<P2>,
    private val childComponentFactoryKey: TypeKey<(P1, P2) -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: (P1, P2) -> C = { p1, p2 ->
            componentBuilder
                .dependency(parent)
                .element(p1Key) { p1 }
                .element(p2Key) { p2 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2) -> C = parent.element(childComponentFactoryKey)

    @Given
    fun p1(@Given component: C): P1 = component.element(p1Key)!!

    @Given
    fun p2(@Given component: C): P2 = component.element(p2Key)!!

    companion object {
        operator fun <@ForTypeKey P : Component,
                @ForTypeKey P1,
                @ForTypeKey P2,
                @ForTypeKey C : Component> invoke() = ChildComponentModule2<P, P1, P2, C>(
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf()
        )
    }
}

class ChildComponentModule3<P : Component, P1, P2, P3, C : Component>(
    private val p1Key: TypeKey<P1>,
    private val p2Key: TypeKey<P2>,
    private val p3Key: TypeKey<P3>,
    private val childComponentFactoryKey: TypeKey<(P1, P2, P3) -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: (P1, P2, P3) -> C = { p1, p2, p3 ->
            componentBuilder
                .dependency(parent)
                .element(p1Key) { p1 }
                .element(p2Key) { p2 }
                .element(p3Key) { p3 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3) -> C = parent.element(childComponentFactoryKey)

    @Given
    fun p1(@Given component: C): P1 = component.element(p1Key)!!

    @Given
    fun p2(@Given component: C): P2 = component.element(p2Key)!!

    @Given
    fun p3(@Given component: C): P3 = component.element(p3Key)!!

    companion object {
        operator fun <P : Component,
                @ForTypeKey P1,
                @ForTypeKey P2,
                @ForTypeKey P3,
                @ForTypeKey C : Component> invoke() = ChildComponentModule3<P, P1, P2, P3, C>(
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf()
        )
    }
}

class ChildComponentModule4<P : Component, P1, P2, P3, P4, C : Component>(
    private val p1Key: TypeKey<P1>,
    private val p2Key: TypeKey<P2>,
    private val p3Key: TypeKey<P3>,
    private val p4Key: TypeKey<P4>,
    private val childComponentFactoryKey: TypeKey<(P1, P2, P3, P4) -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: (P1, P2, P3, P4) -> C = { p1, p2, p3, p4 ->
            componentBuilder
                .dependency(parent)
                .element(p1Key) { p1 }
                .element(p2Key) { p2 }
                .element(p3Key) { p3 }
                .element(p4Key) { p4 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4) -> C = parent.element(childComponentFactoryKey)

    @Given
    fun p1(@Given component: C): P1 = component.element(p1Key)!!

    @Given
    fun p2(@Given component: C): P2 = component.element(p2Key)!!

    @Given
    fun p3(@Given component: C): P3 = component.element(p3Key)!!

    @Given
    fun p4(@Given component: C): P4 = component.element(p4Key)!!

    companion object {
        operator fun <P : Component,
                @ForTypeKey P1,
                @ForTypeKey P2,
                @ForTypeKey P3,
                @ForTypeKey P4,
                @ForTypeKey C : Component> invoke() = ChildComponentModule4<P, P1, P2, P3, P4, C>(
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf()
        )
    }
}

class ChildComponentModule5<P : Component, P1, P2, P3, P4, P5, C : Component>(
    private val p1Key: TypeKey<P1>,
    private val p2Key: TypeKey<P2>,
    private val p3Key: TypeKey<P3>,
    private val p4Key: TypeKey<P4>,
    private val p5Key: TypeKey<P5>,
    private val childComponentFactoryKey: TypeKey<(P1, P2, P3, P4, P5) -> C>
) {
    @Given
    fun factoryElement(
        @Given parent: P,
        @Given componentBuilder: Component.Builder<C>
    ): ComponentElement<P> = childComponentFactoryKey to {
        val factory: (P1, P2, P3, P4, P5) -> C = { p1, p2, p3, p4, p5 ->
            componentBuilder
                .dependency(parent)
                .element(p1Key) { p1 }
                .element(p2Key) { p2 }
                .element(p3Key) { p3 }
                .element(p4Key) { p4 }
                .element(p5Key) { p5 }
                .build()
        }
        factory
    }

    @Given
    fun factory(@Given parent: P): (P1, P2, P3, P4, P5) -> C = parent.element(childComponentFactoryKey)

    @Given
    fun p1(@Given component: C): P1 = component.element(p1Key)!!

    @Given
    fun p2(@Given component: C): P2 = component.element(p2Key)!!

    @Given
    fun p3(@Given component: C): P3 = component.element(p3Key)!!

    @Given
    fun p4(@Given component: C): P4 = component.element(p4Key)!!

    @Given
    fun p5(@Given component: C): P5 = component.element(p5Key)!!

    companion object {
        operator fun <P : Component,
                @ForTypeKey P1,
                @ForTypeKey P2,
                @ForTypeKey P3,
                @ForTypeKey P4,
                @ForTypeKey P5,
                @ForTypeKey C : Component> invoke() = ChildComponentModule5<P, P1, P2, P3, P4, P5, C>(
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf(),
            typeKeyOf()
        )
    }
}
