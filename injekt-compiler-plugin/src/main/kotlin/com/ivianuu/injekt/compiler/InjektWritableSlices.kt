/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.ResolutionScope
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy

object InjektWritableSlices {
    val GIVEN_GRAPH =
        BasicWritableSlice<SourcePosition, GivenGraph.Success>(RewritePolicy.DO_NOTHING)
    val USED_GIVEN = BasicWritableSlice<CallableDescriptor, Unit>(RewritePolicy.DO_NOTHING)
    val GIVEN_CALLS_IN_FILE = BasicWritableSlice<String, Unit>(RewritePolicy.DO_NOTHING)
    val USED_GIVENS_FOR_FILE = BasicWritableSlice<KtFile, List<CallableDescriptor>>(RewritePolicy.DO_NOTHING)
    val RESOLUTION_SCOPE_FOR_SCOPE = BasicWritableSlice<HierarchicalScope, ResolutionScope>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(
    val filePath: String,
    val startOffset: Int,
    val endOffset: Int,
)
