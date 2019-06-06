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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Single
import com.ivianuu.injekt.eager.Eager
import com.ivianuu.injekt.multi.Multi
import com.ivianuu.injekt.weak.Weak
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

enum class SpecialKind(
    val annotation: KClass<out Annotation>,
    val functionPackage: String,
    val functionName: String
) {
    EAGER(Eager::class, "com.ivianuu.injekt.eager", "asEagerBinding"),
    MULTI(Multi::class, "com.ivianuu.injekt.multi", "asMultiBinding"),
    SINGLE(Single::class, "com.ivianuu.injekt", "asSingleBinding"),
    WEAK(Weak::class, "com.ivianuu.injekt.weak", "asWeakBinding")
}

data class BindingFactoryDescriptor(
    val target: ClassName,
    val factoryName: ClassName,
    val kind: SpecialKind?,
    val scope: ClassName?,
    val constructorParams: List<ParamDescriptor>
)

sealed class ParamDescriptor {
    abstract val paramName: String

    data class Parameter(
        override val paramName: String,
        val index: Int
    ) : ParamDescriptor()

    data class Dependency(
        override val paramName: String,
        val paramType: TypeName,
        val qualifierName: ClassName?
    ) : ParamDescriptor()
}