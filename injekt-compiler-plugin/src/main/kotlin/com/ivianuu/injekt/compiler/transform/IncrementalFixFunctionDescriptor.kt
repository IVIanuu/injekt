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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.*

class IncrementalFixFunctionDescriptor(
  private val containingDeclaration: DeclarationDescriptor
) : SimpleFunctionDescriptor {
  override val annotations: Annotations
    get() = Annotations.EMPTY

  override fun getSource(): SourceElement = SourceElement.NO_SOURCE

  override fun hasStableParameterNames(): Boolean = true

  override fun hasSynthesizedParameterNames(): Boolean = false

  private var _owner: IrSimpleFunction? = null

  var owner: IrSimpleFunction
    get() {
      return _owner ?: error("$this is not bound")
    }
    private set(value) {
      _owner?.let { error("$this is already bound to ${it.dump()}") }
      _owner = value
    }

  fun bind(declaration: IrSimpleFunction) {
    owner = declaration
  }

  fun isBound(): Boolean = _owner != null

  override fun getOverriddenDescriptors() = owner.overriddenSymbols.map { it.descriptor }

  override fun getContainingDeclaration(): DeclarationDescriptor =
    containingDeclaration

  override fun getModality() = owner.modality
  override fun getName() = owner.name
  override fun getVisibility() = owner.visibility
  override fun getReturnType() = owner.returnType.toKotlinType()

  override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
    (containingDeclaration as ClassDescriptor).thisAsReceiverParameter
  }

  override fun getExtensionReceiverParameter() = owner.extensionReceiverParameter?.let {
    if (it.isHidden) null else
      it.descriptor as? ReceiverParameterDescriptor
  }
  override fun getTypeParameters() = owner.typeParameters.map { it.descriptor }
  override fun getValueParameters() = owner.valueParameters
    .asSequence()
    .filter { !it.isHidden }
    .mapNotNull { it.descriptor as? ValueParameterDescriptor }
    .toMutableList()

  override fun isExternal() = owner.isExternal
  override fun isSuspend() = owner.isSuspend
  override fun isTailrec() = owner.isTailrec
  override fun isInline() = owner.isInline

  override fun isExpect() = false
  override fun isActual() = false
  override fun isInfix() = false
  override fun isOperator() = false

  override fun getOriginal() = this

  override fun substitute(substitutor: TypeSubstitutor): SimpleFunctionDescriptor {
    TODO("")
  }

  override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
    TODO("not implemented")
  }

  override fun getKind() =
    if (owner.origin == IrDeclarationOrigin.FAKE_OVERRIDE) CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    else CallableMemberDescriptor.Kind.SYNTHESIZED

  override fun copy(
    newOwner: DeclarationDescriptor?,
    modality: Modality?,
    visibility: DescriptorVisibility?,
    kind: CallableMemberDescriptor.Kind?,
    copyOverrides: Boolean
  ): Nothing {
    TODO("not implemented")
  }

  override fun isHiddenToOvercomeSignatureClash(): Boolean = false
  override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean = false

  override fun getInitialSignatureDescriptor(): FunctionDescriptor? = null

  override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null

  override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor> {
    TODO("not implemented")
  }

  override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
    visitor!!.visitFunctionDescriptor(this, data)

  override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
    visitor!!.visitFunctionDescriptor(this, null)
  }
}
