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

package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Given(val component: KClass<*> = Nothing::class)

@Reader
inline fun <reified T : Any> given(
    key: TypeKey<T> = typeKeyOf()
): T = getFactory(key)(emptyArguments())

@Reader
inline fun <reified T : Any> given(
    vararg arguments: Any?,
    key: TypeKey<T> = typeKeyOf()
): T = getFactory(key)(arguments)

inline fun <reified T : Any> ReaderContextBuilder.given(
    noinline factory: @Reader (Arguments) -> T
) {
    given(key = typeKeyOf(), factory = factory)
}

fun <T : Any> ReaderContextBuilder.given(
    key: TypeKey<T>,
    factory: @Reader (Arguments) -> T
) {
    set(FactoryKey(key), factory)
}

inline fun <reified T : Any> ReaderContextBuilder.memoGiven(
    noinline factory: @Reader (Arguments) -> T
) {
    memoGiven(key = typeKeyOf(), factory = factory)
}

fun <T : Any> ReaderContextBuilder.memoGiven(
    key: TypeKey<T>,
    factory: @Reader (Arguments) -> T
) {
    given(key) { arguments ->
        memo(key) {
            factory(arguments)
        }
    }
}
