/*
 * Copyright 2018 Manuel Wrage
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

/**
package com.ivianuu.injekt

import com.ivianuu.injekt.util.TestDep1
import com.ivianuu.injekt.util.TestDep2
import junit.framework.Assert.*
import org.junit.Test

class BeanRegistryTest {

@Test
fun testLoadModules() {
val registry = component { }.beanRegistry
val module = module {
factory { TestDep1() }
factory { TestDep2(get()) }
}

registry.modules(listOf(module))

assertEquals(registry.getAllDefinitions(), module.getDefinitions().toSet())
}

@Test
fun testLoadComponents() {
val registry = component { }.beanRegistry

val component = component {
modules(
module {
factory { TestDep1() }
factory { TestDep2(get()) }
}
)
}

registry.linkComponents(listOf(component))

assertEquals(
registry.getAllDefinitions(),
component.beanRegistry.getAllDefinitions()
)
}

@Test
fun testSaveDefinition() {
val registry = component { }.beanRegistry
val definition =
BeanDefinition.create(TestDep1::class, null, BeanDefinition.Kind.FACTORY) { TestDep1() }
assertEquals(null, registry.findDefinition(TestDep1::class))
registry.addDefinition(definition)
assertEquals(definition, registry.findDefinition(TestDep1::class))
}

@Test
fun testAllowsValidOverride() {
val registry = component { }.beanRegistry
val definition1 =
BeanDefinition.create(TestDep1::class, null, BeanDefinition.Kind.FACTORY) { TestDep1() }
val definition2 =
BeanDefinition.create(TestDep1::class, null, BeanDefinition.Kind.FACTORY) { TestDep1() }
.apply { override = true }

val throwed = try {
registry.addDefinition(definition1)
registry.addDefinition(definition2)
false
} catch (e: OverrideException) {
true
}

assertFalse(throwed)
}

@Test
fun testThrowsOnInvalidOverride() {
val registry = component { }.beanRegistry
val definition1 =
BeanDefinition.create(TestDep1::class, null, BeanDefinition.Kind.FACTORY) { TestDep1() }
val definition2 =
BeanDefinition.create(TestDep1::class, null, BeanDefinition.Kind.FACTORY) { TestDep1() }

val throwed = try {
registry.addDefinition(definition1)
registry.addDefinition(definition2)
false
} catch (e: OverrideException) {
true
}

assertTrue(throwed)
}

@Test
fun testGetEagerInstance() {
val module = module {
factory { TestDep1() }
single(eager = true) { TestDep2(TestDep1()) }
}

val eagerInstances = module.getDefinitions().filter { it.eager }.toSet()

val registry = component {
modules(module)
}.beanRegistry

assertEquals(eagerInstances, registry.getEagerInstances())
}

}*/