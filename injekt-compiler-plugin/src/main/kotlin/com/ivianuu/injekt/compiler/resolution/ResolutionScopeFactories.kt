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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun HierarchicalResolutionScope(
    context: InjektContext,
    scope: HierarchicalScope,
    trace: BindingTrace
): ResolutionScope {
    val finalScope = scope.takeSnapshot()
    trace[InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, finalScope]?.let { return it }

    val allScopes = finalScope.parentsWithSelf.toList()

    val file = allScopes
        .filterIsInstance<LexicalScope>()
        .first()
        .ownerDescriptor
        .findPsi()!!
        .cast<KtElement>()
        .containingKtFile

    val fileImports = (file.getGivenImports() + GivenImport(null, "${file.packageFqName}.*"))
        .sortedBy { it.importPath }

    val importsResolutionScope = trace.get(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, fileImports)
        ?: run {
            ImportResolutionScope(
                fileImports,
                "FILE",
                null,
                context,
                trace
            ).also { trace.record(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, fileImports, it) }
        }

    return allScopes
        .filter { it !is ImportingScope }
        .reversed()
        .asSequence()
        .filter { it.isApplicableScope() }
        .fold(importsResolutionScope) { parent, next ->
            when {
                next is LexicalScope && next.ownerDescriptor is ClassDescriptor ->
                    ClassResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
                next is LexicalScope && next.ownerDescriptor is FunctionDescriptor &&
                        next.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE ->
                    FunctionResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
                next is LexicalScope && next.ownerDescriptor is PropertyDescriptor ->
                    PropertyResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
                else -> CodeBlockResolutionScope(next, context, trace, parent)
            }
        }
        .also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, finalScope, it) }
}

private fun HierarchicalScope.isApplicableScope() = this is LexicalScope && (
        (ownerDescriptor is ClassDescriptor &&
                kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) ||
                (ownerDescriptor is FunctionDescriptor &&
                        kind == LexicalScopeKind.FUNCTION_INNER_SCOPE) ||
                (ownerDescriptor is PropertyDescriptor &&
                        kind == LexicalScopeKind.PROPERTY_INITIALIZER_OR_DELEGATE) ||
                kind == LexicalScopeKind.CODE_BLOCK ||
                kind == LexicalScopeKind.CLASS_INITIALIZER
        )

private fun ImportResolutionScope(
    imports: List<GivenImport>,
    namePrefix: String,
    parent: ResolutionScope?,
    context: InjektContext,
    trace: BindingTrace
): ResolutionScope {
    val (externalImportedGivens, internalImportedGivens) = imports
        .collectImportGivens(context, trace)
        .partition { it.callable.isExternalDeclaration(context) }
    return ResolutionScope(
        name = "$namePrefix INTERNAL IMPORTS",
        context = context,
        callContext = CallContext.DEFAULT,
        parent = ResolutionScope(
            name = "$namePrefix EXTERNAL IMPORTS",
            context = context,
            callContext = CallContext.DEFAULT,
            parent = parent,
            ownerDescriptor = null,
            trace = trace,
            initialGivens = externalImportedGivens,
            imports = emptyList(),
            typeParameters = emptyList()
        ),
        ownerDescriptor = null,
        trace = trace,
        initialGivens = internalImportedGivens,
        imports = imports,
        typeParameters = emptyList()
    )
}

private fun ClassResolutionScope(
    clazz: ClassDescriptor,
    context: InjektContext,
    trace: BindingTrace,
    parent: ResolutionScope?
): ResolutionScope {
    trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz)
        ?.let { return it }
    val companionObjectScope = clazz.companionObjectDescriptor
        ?.let { ClassResolutionScope(it, context, trace, parent) }
    val name = if (clazz.isCompanionObject)
        "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
    else "CLASS ${clazz.fqNameSafe}"
    val finalParent = clazz
        .findPsi()
        .safeAs<KtClassOrObject>()
        ?.getGivenImports()
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            ImportResolutionScope(
                it,
                name,
                parent,
                context,
                trace
            )
        } ?: companionObjectScope ?: parent

    return ResolutionScope(
        name = name,
        context = context,
        callContext = CallContext.DEFAULT,
        parent = finalParent,
        ownerDescriptor = clazz,
        trace = trace,
        initialGivens = listOf(clazz.getGivenReceiver(context, trace)),
        imports = emptyList(),
        typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(context, trace) }
    ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz, it) }
}

private fun FunctionResolutionScope(
    function: FunctionDescriptor,
    context: InjektContext,
    trace: BindingTrace,
    parent: ResolutionScope?
): ResolutionScope {
    trace.get(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function)
        ?.let { return it }
    val finalParent = function
        .findPsi()
        .safeAs<KtFunction>()
        ?.getGivenImports()
        ?.takeIf { it.isNotEmpty() }
        ?.let { ImportResolutionScope(it, "FUNCTION ${function.fqNameSafe}", parent, context, trace) }
        ?: parent
    return ResolutionScope(
        name = "FUNCTION ${function.fqNameSafe}",
        context = context,
        callContext = function.callContext(trace.bindingContext),
        parent = finalParent,
        ownerDescriptor = function,
        trace = trace,
        initialGivens = function.allParameters
            .asSequence()
            .filter { it.isGiven(context, trace) || it === function.extensionReceiverParameter }
            .map { it.toCallableRef(context, trace).makeGiven() }
            .toList(),
        imports = emptyList(),
        typeParameters = function.typeParameters.map { it.toClassifierRef(context, trace) }
    ).also { trace.record(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function, it) }
}

private fun PropertyResolutionScope(
    property: PropertyDescriptor,
    context: InjektContext,
    trace: BindingTrace,
    parent: ResolutionScope?
): ResolutionScope {
    trace.get(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property)
        ?.let { return it }
    val finalParent = property
        .findPsi()
        .safeAs<KtProperty>()
        ?.getGivenImports()
        ?.takeIf { it.isNotEmpty() }
        ?.let { ImportResolutionScope(it, "PROPERTY ${property.fqNameSafe}", parent, context, trace) }
        ?: parent
    return ResolutionScope(
        name = "PROPERTY ${property.fqNameSafe}",
        context = context,
        callContext = property.callContext(trace.bindingContext),
        parent = finalParent,
        ownerDescriptor = property,
        trace = trace,
        initialGivens = listOfNotNull(
            property.extensionReceiverParameter
                ?.toCallableRef(context, trace)
                ?.makeGiven()
        ),
        imports = emptyList(),
        typeParameters = property.typeParameters.map { it.toClassifierRef(context, trace) }
    ).also { trace.record(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property, it) }
}

private fun CodeBlockResolutionScope(
    scope: HierarchicalScope,
    context: InjektContext,
    trace: BindingTrace,
    parent: ResolutionScope?
): ResolutionScope {
    val ownerDescriptor = scope.parentsWithSelf
        .firstIsInstance<LexicalScope>()
        .ownerDescriptor
    val finalParent = ownerDescriptor
        .safeAs<AnonymousFunctionDescriptor>()
        ?.findPsi()
        ?.getParentOfType<KtCallExpression>(false)
        ?.getResolvedCall(trace.bindingContext)
        ?.valueArguments
        ?.values
        ?.firstOrNull()
        ?.safeAs<VarargValueArgument>()
        ?.arguments
        ?.map { it.toGivenImport() }
        ?.takeIf { it.isNotEmpty() }
        ?.let { ImportResolutionScope(it, "BLOCK", parent, context, trace) }
        ?: parent
    return ResolutionScope(
        name = "Hierarchical $scope",
        context = context,
        callContext = scope.callContext(trace.bindingContext),
        parent = finalParent,
        ownerDescriptor = ownerDescriptor,
        trace = trace,
        initialGivens = scope.collectGivens(context, trace),
        imports = emptyList(),
        typeParameters = emptyList()
    )
}
