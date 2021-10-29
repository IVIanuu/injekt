/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.types.KotlinType

class InjectSymbolRemapper(@Inject private val context: InjektContext) : DeepCopySymbolRemapper(
  object : DescriptorsRemapper {
    override fun remapDeclaredConstructor(
      descriptor: ClassConstructorDescriptor
    ): ClassConstructorDescriptor? =
      descriptor.takeUnless { it.isTransformed() }

    override fun remapDeclaredSimpleFunction(
      descriptor: FunctionDescriptor
    ): FunctionDescriptor? =
      descriptor.takeUnless { it.isTransformed() }

    override fun remapDeclaredValueParameter(
      descriptor: ParameterDescriptor
    ): ParameterDescriptor? =
      descriptor.takeUnless { it.isTransformed() }

    override fun remapDeclaredTypeParameter(
      descriptor: TypeParameterDescriptor
    ): TypeParameterDescriptor? =
      descriptor.takeUnless { it.isTransformed() }

    private fun ClassConstructorDescriptor.isTransformed(): Boolean =
      this is IrBasedDeclarationDescriptor<*> ||
          valueParameters.any { it.type.containsComposable() }

    private fun FunctionDescriptor.isTransformed(): Boolean =
      this is IrBasedDeclarationDescriptor<*> ||
          valueParameters.any { it.type.containsComposable() } ||
          returnType?.containsComposable() == true

    private fun ParameterDescriptor.isTransformed(): Boolean =
      this is IrBasedDeclarationDescriptor<*> ||
          type.containsComposable() ||
          containingDeclaration.let { it is FunctionDescriptor && it.isTransformed() }

    private fun TypeParameterDescriptor.isTransformed(): Boolean =
      this is IrBasedDeclarationDescriptor<*> ||
          containingDeclaration.let { it is FunctionDescriptor && it.isTransformed() }

    private fun KotlinType.containsComposable(): Boolean =
      hasAnnotation(injektFqNames().inject2) ||
          arguments.any { it.type.containsComposable() }
  }
)
