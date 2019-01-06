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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class DefinitionDescriptor(
    val target: ClassName,
    val kind: Kind,
    val name: String?,
    val scope: String?,
    val override: Boolean,
    val createOnStart: Boolean?,
    val secondaryTypes: Set<TypeName>,
    val constructorParams: List<ParamDescriptor>
) {
    enum class Kind { FACTORY, SINGLE }
}