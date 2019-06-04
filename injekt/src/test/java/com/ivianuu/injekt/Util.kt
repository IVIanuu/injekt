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

package com.ivianuu.injekt

object NameOne : Qualifier
object NameTwo : Qualifier
object NameThree : Qualifier

object Values : Qualifier

object TestScope : Scope

inline fun <reified T> Component.getBinding(
    name: Qualifier? = null
): Binding<T> = getBinding(typeOf(), name)

fun <T> Component.getBinding(
    type: Type<T>,
    name: Qualifier? = null
): Binding<T> {
    val key = Key(type, name)
    return instances.entries.firstOrNull { it.key == key }?.value?.binding as? Binding<T>
        ?: error("binding not found")
}


class TestDep1
class TestDep2(val dep1: TestDep1)
class TestDep3(val dep1: TestDep1, val dep2: TestDep2)