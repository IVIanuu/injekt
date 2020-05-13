package com.ivianuu.injekt.compiler.analysis

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

val ALLOWED_SCOPE_KINDS = setOf(
    LexicalScopeKind.FUNCTION_INNER_SCOPE, LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING
)

fun findEnclosingModuleFunctionContext(
    context: CallCheckerContext,
    predicate: (FunctionDescriptor) -> Boolean
): FunctionDescriptor? = context.scope
    .parentsWithSelf.firstOrNull {
    it is LexicalScope && it.kind in ALLOWED_SCOPE_KINDS &&
            it.ownerDescriptor.safeAs<FunctionDescriptor>()
                ?.let { predicate(it) } == true
}?.cast<LexicalScope>()?.ownerDescriptor?.cast()
