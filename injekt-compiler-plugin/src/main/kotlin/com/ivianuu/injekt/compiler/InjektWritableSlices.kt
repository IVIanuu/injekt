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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.slicedMap.*

object InjektWritableSlices {
  val INJECTION_GRAPH_FOR_POSITION =
    BasicWritableSlice<SourcePosition, InjectionGraph.Success>(RewritePolicy.DO_NOTHING)
  val INJECTION_GRAPH_FOR_CALL =
    BasicWritableSlice<KtElement, InjectionGraph>(RewritePolicy.DO_NOTHING)
  val USED_INJECTABLE = BasicWritableSlice<DeclarationDescriptor, Unit>(RewritePolicy.DO_NOTHING)
  val USED_IMPORT = BasicWritableSlice<SourcePosition, Unit>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)
