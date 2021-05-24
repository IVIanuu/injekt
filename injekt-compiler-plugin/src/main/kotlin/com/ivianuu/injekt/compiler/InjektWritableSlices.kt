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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.util.slicedMap.*

object InjektWritableSlices {
  val INJECTION_GRAPH =
    BasicWritableSlice<SourcePosition, InjectionGraph.Success>(RewritePolicy.DO_NOTHING)
  val USED_INJECTABLE = BasicWritableSlice<DeclarationDescriptor, Unit>(RewritePolicy.DO_NOTHING)
  val USED_IMPORT = BasicWritableSlice<SourcePosition, Unit>(RewritePolicy.DO_NOTHING)
  val INJECTIONS_OCCURRED_IN_FILE = BasicWritableSlice<String, Unit>(RewritePolicy.DO_NOTHING)
  val DECLARATION_INJECTABLES_SCOPE =
    BasicWritableSlice<DeclarationDescriptor, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val DECLARATION_IMPORTS_INJECTABLES_SCOPE =
    BasicWritableSlice<DeclarationDescriptor, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val FUNCTION_INJECTABLES_SCOPE =
    BasicWritableSlice<FunctionDescriptor, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val FUNCTION_PARAMETER_INJECTABLES_SCOPE =
    BasicWritableSlice<ParameterDescriptor, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val FUNCTION_PARAMETER_DEFAULT_VALUE_INJECTABLES_SCOPE =
    BasicWritableSlice<ParameterDescriptor, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val IMPORT_INJECTABLES_SCOPE =
    BasicWritableSlice<List<ProviderImport>, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val HIERARCHICAL_INJECTABLES_SCOPE =
    BasicWritableSlice<HierarchicalScope, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val TYPE_INJECTABLES_SCOPE =
    BasicWritableSlice<TypeRef, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val CALLABLE_REF_FOR_DESCRIPTOR =
    BasicWritableSlice<CallableDescriptor, CallableRef>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_REF_FOR_CLASSIFIER =
    BasicWritableSlice<ClassifierDescriptor, ClassifierRef>(RewritePolicy.DO_NOTHING)
  val IS_PROVIDE = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
  val IS_INJECT = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
  val INJECTABLE_CONSTRUCTORS =
    BasicWritableSlice<ClassDescriptor, List<CallableRef>>(RewritePolicy.DO_NOTHING)
  val CALLABLE_INFO = BasicWritableSlice<CallableDescriptor, CallableInfo>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_INFO =
    BasicWritableSlice<ClassifierDescriptor, ClassifierInfo>(RewritePolicy.DO_NOTHING)
  val CLASSIFIER_FOR_KEY =
    BasicWritableSlice<String, ClassifierDescriptor>(RewritePolicy.DO_NOTHING)
  val TYPE_SCOPE_INJECTABLES = BasicWritableSlice<TypeRef, List<CallableRef>>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)
