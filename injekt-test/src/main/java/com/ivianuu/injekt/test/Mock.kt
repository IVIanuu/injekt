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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.BeanDefinition
import org.mockito.Mockito.mock
import kotlin.reflect.KClass

inline fun <reified T : Any> Component.declareMock(
    name: String? = null,
    noinline stubbing: (T.() -> Unit)? = null
) = declareMock(T::class, name, stubbing)

/**
 * Declares a mocked version of [type] and [name]
 */
fun <T : Any> Component.declareMock(
    type: KClass<T>,
    name: String? = null,
    stubbing: (T.() -> Unit)? = null
): T {
    val foundDefinition = beanRegistry.findDefinition(type, name) as BeanDefinition<T>

    val definition = foundDefinition.cloneForMock(type)
    beanRegistry.saveDefinition(definition)

    return applyStub(type, stubbing)
}

fun <T : Any> Component.applyStub(
    type: KClass<T>,
    stubbing: (T.() -> Unit)?
): T {
    val instance: T = get(type)
    stubbing?.let { instance.apply(stubbing) }
    return instance
}

fun <T : Any> BeanDefinition<T>.cloneForMock(type: KClass<T>) = copy().also {
    it.kind = kind
    it.override = override
    it.createOnStart = createOnStart
    it.attributes = attributes
    it.definition = { mock(type.java) }
    it.createInstanceHolder()
    it.moduleContext = moduleContext
}