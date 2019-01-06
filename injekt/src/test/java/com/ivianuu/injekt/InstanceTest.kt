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
import com.ivianuu.injekt.util.getDefinition
import org.junit.Assert.*
import org.junit.Test

class InstanceTest {

@Test
fun testSingleCreatesOnce() {
val component = component {
modules(
module {
single { TestDep1() }
}
)
}

val definition = component.getDefinition<TestDep1>()

assertTrue(definition.instance is SingleInstance)
assertFalse(definition.instance.isCreated)

val value1 = definition.instance.get()
assertTrue(definition.instance.isCreated)
val value2 = definition.instance.get()
assertTrue(definition.instance.isCreated)

assertEquals(value1, value2)
}

@Test
fun testFactoryCreatesNew() {
val component = component {
modules(
module {
factory { TestDep1() }
}
)
}

val definition = component.getDefinition<TestDep1>()

assertTrue(definition.instance is FactoryInstance)
assertFalse(definition.instance.isCreated)

val value1 = definition.instance.get()
assertFalse(definition.instance.isCreated)
val value2 = definition.instance.get()
assertFalse(definition.instance.isCreated)

assertNotEquals(value1, value2)
}

@Test
fun testInstanceCreationFailed() {
val component = component {
modules(
module {
factory<TestDep1> { throw error("error") }
}
)
}

val definition = component.getDefinition<TestDep1>()

val throwed = try {
definition.instance.get()
false
} catch (e: InstanceCreationException) {
true
}

assertTrue(throwed)
}
}*/