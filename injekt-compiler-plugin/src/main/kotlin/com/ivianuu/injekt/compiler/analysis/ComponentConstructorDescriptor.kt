package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name

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
