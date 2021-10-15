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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.DescriptorWithParentScope
import com.ivianuu.injekt.compiler.resolution.InjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectablesWithLookups
import com.ivianuu.injekt.compiler.resolution.InjectionGraph
import com.ivianuu.injekt.compiler.resolution.TypeRefKey
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy

object InjektWritableSlices {
  val INJEKT_FQ_NAMES = BasicWritableSlice<Unit, InjektFqNames>(RewritePolicy.DO_NOTHING)
  val INJECTION_GRAPH_FOR_POSITION =
    BasicWritableSlice<SourcePosition, InjectionGraph.Success>(RewritePolicy.DO_NOTHING)
  val INJECTIONS_OCCURRED_IN_FILE = BasicWritableSlice<String, Unit>(RewritePolicy.DO_NOTHING)
  val USED_IMPORT = BasicWritableSlice<SourcePosition, Unit>(RewritePolicy.DO_NOTHING)
  val CALL_CONTEXT = BasicWritableSlice<CallableDescriptor, CallContext>(RewritePolicy.DO_NOTHING)

  val INJECTABLE_CONSTRUCTORS = BasicWritableSlice<ClassDescriptor, List<CallableRef>>(RewritePolicy.DO_NOTHING)
  val IS_PROVIDE = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
  val IS_INJECT = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
  val BLOCK_SCOPE = BasicWritableSlice<Pair<KtBlockExpression, DeclarationDescriptor>, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_REF = BasicWritableSlice<ClassifierDescriptor, ClassifierRef>(RewritePolicy.DO_NOTHING)
  val CALLABLE_REF = BasicWritableSlice<CallableDescriptor, CallableRef>(RewritePolicy.DO_NOTHING)
  val CALLABLE_INFO = BasicWritableSlice<CallableDescriptor, CallableInfo>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_INFO = BasicWritableSlice<ClassifierDescriptor, ClassifierInfo>(RewritePolicy.DO_NOTHING)
  val ELEMENT_SCOPE = BasicWritableSlice<KtElement, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val DECLARATION_SCOPE = BasicWritableSlice<DescriptorWithParentScope, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val TYPE_SCOPE_INJECTABLES = BasicWritableSlice<TypeRefKey, InjectablesWithLookups>(RewritePolicy.DO_NOTHING)
  val TYPE_SCOPE_INJECTABLES_FOR_SINGLE_TYPE = BasicWritableSlice<TypeRefKey, InjectablesWithLookups>(RewritePolicy.DO_NOTHING)
  val PACKAGE_TYPE_SCOPE_INJECTABLES = BasicWritableSlice<FqName, InjectablesWithLookups>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)
