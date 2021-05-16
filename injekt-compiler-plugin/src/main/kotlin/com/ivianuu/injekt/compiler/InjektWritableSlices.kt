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

import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.compiler.resolution.ResolutionScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.util.slicedMap.*

object InjektWritableSlices {
  val GIVEN_GRAPH =
    BasicWritableSlice<SourcePosition, GivenGraph.Success>(RewritePolicy.DO_NOTHING)
  val USED_GIVEN = BasicWritableSlice<DeclarationDescriptor, Unit>(RewritePolicy.DO_NOTHING)
  val USED_IMPORT = BasicWritableSlice<SourcePosition, Unit>(RewritePolicy.DO_NOTHING)
  val FILE_HAS_GIVEN_CALLS = BasicWritableSlice<String, Unit>(RewritePolicy.DO_NOTHING)
  val DECLARATION_RESOLUTION_SCOPE =
    BasicWritableSlice<DeclarationDescriptor, ResolutionScope>(RewritePolicy.DO_NOTHING)
  val HIERARCHICAL_RESOLUTION_SCOPE =
    BasicWritableSlice<HierarchicalScope, ResolutionScope>(RewritePolicy.DO_NOTHING)
  val TYPE_RESOLUTION_SCOPE =
    BasicWritableSlice<TypeRef, ResolutionScope>(RewritePolicy.DO_NOTHING)
  val CALLABLE_REF_FOR_DESCRIPTOR =
    BasicWritableSlice<CallableDescriptor, CallableRef>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_REF_FOR_CLASSIFIER =
    BasicWritableSlice<ClassifierDescriptor, ClassifierRef>(RewritePolicy.DO_NOTHING)
  val IS_GIVEN = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
  val GIVEN_CONSTRUCTORS =
    BasicWritableSlice<ClassDescriptor, List<CallableRef>>(RewritePolicy.DO_NOTHING)
  val CALLABLE_INFO = BasicWritableSlice<CallableDescriptor, CallableInfo>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_INFO =
    BasicWritableSlice<ClassifierDescriptor, ClassifierInfo>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_FOR_KEY =
    BasicWritableSlice<String, ClassifierDescriptor>(RewritePolicy.DO_NOTHING)
  val TYPE_SCOPE_GIVENS = BasicWritableSlice<TypeRef, List<CallableRef>>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)
