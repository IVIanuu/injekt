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

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class InjectTypeValueParameter(
  val type: TypeRef,
  val typeIndex: Int,
  containingDeclaration: CallableDescriptor,
  index: Int
) : ValueParameterDescriptorImpl(
  containingDeclaration,
  null,
  index,
  Annotations.create(
    listOf(
      AnnotationDescriptorImpl(
        containingDeclaration.module.injektContext.classifierDescriptorForFqName(
          InjektFqNames.Inject,
          NoLookupLocation.FROM_BACKEND
        )!!.defaultType,
        emptyMap(),
        SourceElement.NO_SOURCE
      )
    )
  ),
  "\$injectType${typeIndex}".asNameId(),
  type.toKotlinType(containingDeclaration.module.injektContext),
  false,
  false,
  false,
  null,
  SourceElement.NO_SOURCE
)

class InjectTypeProperty(
  val type: TypeRef,
  containingDeclaration: CallableDescriptor
) : PropertyDescriptorImpl(
  containingDeclaration,
  null,
  Annotations.EMPTY,
  Modality.FINAL,
  DescriptorVisibilities.PRIVATE,
  false,
  "injectType${type.renderKotlinLikeToString()}".asNameId(),
  CallableMemberDescriptor.Kind.SYNTHESIZED,
  SourceElement.NO_SOURCE,
  false,
  false,
  false,
  false,
  false,
  false
) {
  init {
    initialize(
      DescriptorFactory.createDefaultGetter(this, Annotations.EMPTY),
      null,
      null,
      null
    )
  }
}
