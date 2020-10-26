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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val contributionKind: ContributionKind?,
    val isCall: Boolean,
    val callableKind: CallableKind,
    val bindingAdapters: List<FqName>,
    val isEager: Boolean,
    val isExternal: Boolean,
    val isInline: Boolean,
    val isFunBinding: Boolean
) {
    enum class ContributionKind {
        BINDING, MAP_ENTRIES, SET_ELEMENTS, MODULE
    }
    enum class CallableKind {
        DEFAULT, SUSPEND, COMPOSABLE
    }
}

data class ValueParameterRef(
    val type: TypeRef,
    val isExtensionReceiver: Boolean = false,
    val inlineKind: InlineKind,
    val name: Name,
) {
    enum class InlineKind {
        NONE, NOINLINE, CROSSINLINE
    }
}

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)
