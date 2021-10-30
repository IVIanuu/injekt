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

import com.ivianuu.injekt.compiler.WithInjektContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.module
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.substitute
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

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

@WithInjektContext class InjectNParameterDescriptor(
  private val _containingDeclaration: DeclarationDescriptor,
  val index: Int,
  val typeRef: TypeRef
) : DeclarationDescriptorImpl(
  Annotations.create(
    listOf(
      AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames.inject)
        )!!.defaultType,
        emptyMap(),
        SourceElement.NO_SOURCE
      )
    )
  ),
  "_inject$index".asNameId()
), ParameterDescriptor {
  override fun getOriginal(): ParameterDescriptor = this

  override fun <R : Any?, D : Any?> accept(p0: DeclarationDescriptorVisitor<R, D>?, p1: D): R =
    TODO()

  override fun getContainingDeclaration(): DeclarationDescriptor = _containingDeclaration

  override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

  override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

  override fun getOverriddenDescriptors(): MutableCollection<out CallableDescriptor> =
    mutableListOf()

  override fun getReturnType(): KotlinType = type

  override fun getSource(): SourceElement = SourceElement.NO_SOURCE

  override fun getType(): KotlinType = module.builtIns.nullableAnyType

  override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()

  override fun <V : Any?> getUserData(p0: CallableDescriptor.UserDataKey<V>?): V? = null

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> = mutableListOf()

  override fun getVisibility(): DescriptorVisibility = DescriptorVisibilities.PRIVATE

  override fun hasStableParameterNames(): Boolean = true

  override fun hasSynthesizedParameterNames(): Boolean = false

  override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor = this
}

@WithInjektContext fun InjectNParameterDescriptor.substitute(
  map: Map<ClassifierRef, TypeRef>
): InjectNParameterDescriptor {
  if (map.isEmpty()) return this
  return InjectNParameterDescriptor(containingDeclaration, index, typeRef.substitute(map))
}
