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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class ComponentConstructorDescriptor(
  clazz: ClassDescriptor
) : FunctionDescriptorImpl(clazz, null, Annotations.EMPTY,
  Name.special("<init>"), CallableMemberDescriptor.Kind.SYNTHESIZED, clazz.source) {
  init {
    initialize(
      null,
      null,
      emptyList(),
      emptyList(),
      clazz.defaultType,
      null,
      clazz.visibility
    )
  }
  override fun createSubstitutedCopy(
    p0: DeclarationDescriptor,
    p1: FunctionDescriptor?,
    p2: CallableMemberDescriptor.Kind,
    p3: Name?,
    p4: Annotations,
    p5: SourceElement
  ): FunctionDescriptorImpl = TODO()
}

class EntryPointConstructorDescriptor(
  clazz: ClassDescriptor
) : FunctionDescriptorImpl(clazz, null, Annotations.EMPTY,
  Name.special("<init>"), CallableMemberDescriptor.Kind.SYNTHESIZED, clazz.source) {
  init {
    initialize(
      null,
      null,
      emptyList(),
      emptyList(),
      clazz.defaultType,
      null,
      clazz.visibility
    )
  }
  override fun createSubstitutedCopy(
    p0: DeclarationDescriptor,
    p1: FunctionDescriptor?,
    p2: CallableMemberDescriptor.Kind,
    p3: Name?,
    p4: Annotations,
    p5: SourceElement
  ): FunctionDescriptorImpl = TODO()
}

class InjectNParameterDescriptor(
  callable: CallableDescriptor,
  index: Int,
  type: KotlinType,
  val typeRef: TypeRef,
  @Inject context: InjektContext
) : ValueParameterDescriptorImpl(
  callable,
  null,
  index,
  Annotations.create(
    listOf(
      AnnotationDescriptorImpl(
        context.module.findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames().inject)
        )!!.defaultType,
        emptyMap(),
        SourceElement.NO_SOURCE
      )
    )
  ),
  Name.special("<\$inject_$index>"),
  type,
  false,
  false,
  false,
  null,
  SourceElement.NO_SOURCE
)
