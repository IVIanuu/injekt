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

package com.ivianuu.injekt.sample.proto

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.transient

@Module
fun <T> genericTypeParameter() {
    transient<T>()
}

@Module
fun <T> genericTypeParameter_(provider: @Provider () -> T): genericClass<T> {
    return genericTypeParameter_(provider)
}

class genericClass<T>(val t: @Provider () -> T)

@Module
fun genericTypeParameterCaller() {
    genericTypeParameter<Foo>()
}

@Module
fun genericTypeParameterCaller_(): genericTypeParameterCallerClass {
    val genericTypeParameter1 = genericTypeParameter_ { Foo() }
    return genericTypeParameterCallerClass(genericTypeParameter1)
}

class genericTypeParameterCallerClass(
    val genericClass: genericClass<Foo>
)
