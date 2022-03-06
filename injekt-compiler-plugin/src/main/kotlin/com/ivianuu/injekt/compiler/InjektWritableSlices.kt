/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.slicedMap.*

object InjektWritableSlices {
  val INJECTION_GRAPH = BasicWritableSlice<SourcePosition, InjectionGraph.Success>(RewritePolicy.DO_NOTHING)
  val INJECTIONS_OCCURRED_IN_FILE = BasicWritableSlice<String, Unit>(RewritePolicy.DO_NOTHING)
  val USED_IMPORT = BasicWritableSlice<SourcePosition, Unit>(RewritePolicy.DO_NOTHING)
  val BLOCK_SCOPE = BasicWritableSlice<Pair<KtBlockExpression, DeclarationDescriptor>, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val ELEMENT_SCOPE = BasicWritableSlice<KtElement, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val DECLARATION_SCOPE = BasicWritableSlice<DescriptorWithParentScope, InjectablesScope>(RewritePolicy.DO_NOTHING)
  val TYPE_CHAINED_INJECTABLES = BasicWritableSlice<Pair<TypeKey, Boolean>, List<CallableRef>>(RewritePolicy.DO_NOTHING)
  val TYPE_SCOPE_INJECTABLES = BasicWritableSlice<TypeKey, InjectablesWithLookups>(RewritePolicy.DO_NOTHING)
  val PACKAGE_TYPE_SCOPE_INJECTABLES = BasicWritableSlice<FqName, List<CallableRef>>(RewritePolicy.DO_NOTHING)
  val UNIQUE_KEY = BasicWritableSlice<DeclarationDescriptor, String>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)
