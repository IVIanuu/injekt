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

import com.google.j2objc.annotations.Weak
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.FactoryKind
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.SingleKind
import com.ivianuu.injekt.eager.Eager
import com.ivianuu.injekt.eager.EagerKind
import com.ivianuu.injekt.multi.Multi
import com.ivianuu.injekt.multi.MultiKind
import com.ivianuu.injekt.weak.WeakKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.Flags
import javax.lang.model.element.Element
import kotlin.reflect.KClass

enum class Kind(
    val annotation: KClass<out Annotation>,
    val impl: ClassName
) {
    EAGER(Eager::class, EagerKind::class.asClassName()),
    FACTORY(Factory::class, FactoryKind::class.asClassName()),
    MULTI(Multi::class, MultiKind::class.asClassName()),
    SINGLE(Single::class, SingleKind::class.asClassName()),
    WEAK(Weak::class, WeakKind::class.asClassName())
}

val Element.isObject: Boolean
    get() {
        return Flags.CLASS_KIND.get(
            (kotlinMetadata as? KotlinClassMetadata)
                ?.data?.classProto?.flags ?: 0
        ) == ProtoBuf.Class.Kind.OBJECT
    }