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

package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun HierarchicalScope.isScopeForDefaultParameterValuesOf(enclosingModuleFunction: FunctionDescriptor) =
    this is LexicalScope && this.kind == LexicalScopeKind.DEFAULT_VALUE && this.ownerDescriptor == enclosingModuleFunction

private val ALLOWED_SCOPE_KINDS = setOf(
    LexicalScopeKind.FUNCTION_INNER_SCOPE,
    LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING
)

fun findDirectEnclosingFunctionContext(
    context: CallCheckerContext,
    predicate: (FunctionDescriptor) -> Boolean
): FunctionDescriptor? = context.scope
    .parentsWithSelf.firstOrNull {
        it is LexicalScope && it.kind in ALLOWED_SCOPE_KINDS &&
                it.ownerDescriptor.safeAs<FunctionDescriptor>()
                    ?.let { predicate(it) } == true
    }?.cast<LexicalScope>()?.ownerDescriptor?.cast()

fun findEnclosingContext(
    context: CallCheckerContext,
    predicate: (DeclarationDescriptor) -> Boolean
): DeclarationDescriptor? = context.scope
    .parentsWithSelf.firstOrNull {
        it is LexicalScope && predicate(it.ownerDescriptor)
    }?.cast<LexicalScope>()?.ownerDescriptor
