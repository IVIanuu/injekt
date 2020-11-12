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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentExpression
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: TypeRef,
    val originalType: TypeRef = type,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val contributionKind: ContributionKind?,
    val isCall: Boolean,
    val callableKind: CallableKind,
    val decorators: List<DecoratorDescriptor>,
    val effects: List<EffectDescriptor>,
    val isExternal: Boolean,
    val isInline: Boolean,
    val isFunBinding: Boolean,
    val visibility: Visibility,
    val modality: Modality,
    val receiver: ClassifierRef?
) {
    enum class ContributionKind {
        BINDING, DECORATOR, MAP_ENTRIES, SET_ELEMENTS, MODULE
    }
    enum class CallableKind {
        DEFAULT, SUSPEND, COMPOSABLE
    }
}

data class ValueParameterRef(
    val type: TypeRef,
    val isExtensionReceiver: Boolean,
    val inlineKind: InlineKind,
    val name: Name,
    val argName: Name?,
    val hasDefault: Boolean,
    val defaultExpression: ComponentExpression?
) {
    enum class InlineKind {
        NONE, NOINLINE, CROSSINLINE
    }
}

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)

data class DecoratorDescriptor(
    val annotationType: TypeRef?,
    val callables: List<Callable>,
    val typeArgs: Map<Name, TypeRef>,
    val valueArgs: Map<Name, ComponentExpression>
)

data class EffectDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
    val typeArgs: Map<Name, TypeRef>,
    val valueArgs: Map<Name, ComponentExpression>
)

data class QualifierDescriptor(
    val type: TypeRef,
    val args: Map<Name, String>
)

data class ImplBindingDescriptor(
    val callable: Callable,
    val implType: TypeRef,
    val superType: TypeRef
)
