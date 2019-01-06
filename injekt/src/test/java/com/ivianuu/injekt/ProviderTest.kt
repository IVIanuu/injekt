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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProviderTest {

@Test
fun testProviderNotReturnsSameValue() {
val component = component {
modules(
module {
factory { TestDep1() }
}
)
}
val provider = component.provider<TestDep1>()
val value1 = provider.get()
val value2 = provider.get()
assertNotEquals(value1, value2)
}

@Test
fun testProviderUsesDefaultParams() {
lateinit var usedParams: Parameters

val component = component {
modules(
module {
factory {
usedParams = it
TestDep1()
}
}
)
}

val defaultParams = parametersOf("one", "two")

val provider = component.provider<TestDep1> { defaultParams }

provider.get()

assertEquals(defaultParams, usedParams)
}

@Test
fun testProviderUsesExplicitParams() {
lateinit var usedParams: Parameters

val component = component {
modules(
module {
factory {
usedParams = it
TestDep1()
}
}
)
}

val params = parametersOf("one", "two")

val provider = component.provider<TestDep1>()

provider.get { params }

assertEquals(params, usedParams)
}

@Test
fun testProviderPrefersExplicitParams() {
lateinit var usedParams: Parameters

val component = component {
modules(
module {
factory {
usedParams = it
TestDep1()
}
}
)
}

val defaultParams = parametersOf("default")
val explicitParams = parametersOf("explicit")

val provider = component.provider<TestDep1> { defaultParams }

provider.get { explicitParams }

assertEquals(explicitParams, usedParams)
}

}*/