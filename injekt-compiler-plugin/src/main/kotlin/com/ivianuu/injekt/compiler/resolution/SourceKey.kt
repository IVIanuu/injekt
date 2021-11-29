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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SourceKeyFakeConstructorDescriptor(
  val sourceKey: SourceKey,
  @Inject ctx: Context
) : FunctionDescriptorImpl(
  ctx.module.getPackage(injektFqNames().commonPackage),
  null,
  Annotations.EMPTY,
  "SourceKey".asNameId(),
  CallableMemberDescriptor.Kind.SYNTHESIZED,
  SourceElement.NO_SOURCE
) {
  init {
    initialize(
      null,
      null,
      emptyList(),
      emptyList(),
      ctx.sourceKeyDescriptor!!.defaultType,
      null,
      DescriptorVisibilities.PUBLIC
    )
  }

  override fun createSubstitutedCopy(
    p0: DeclarationDescriptor,
    p1: FunctionDescriptor?,
    p2: CallableMemberDescriptor.Kind,
    p3: Name?,
    p4: Annotations,
    p5: SourceElement
  ): FunctionDescriptorImpl = this
}

data class SourceKey(
  val fileName: String,
  val lineNumber: Int,
  val columnNumber: Int
)

fun KtElement.toSourceKey(): SourceKey {
  val file = containingKtFile
  val document = file.viewProvider.document!!
  val lineNumber = document.getLineNumber(startOffset)
  return SourceKey(
    fileName = file.name,
    lineNumber = lineNumber + 1,
    columnNumber = startOffset - document.getLineStartOffset(lineNumber)
  )
}
