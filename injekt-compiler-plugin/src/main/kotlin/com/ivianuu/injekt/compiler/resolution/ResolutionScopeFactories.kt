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
            fileImports
                .toImportResolutionScope("FILE", null, context, trace)
                .also { trace.record(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, fileImports, it) }
        }

    return allScopes
        .filter { it !is ImportingScope }
        .reversed()
        .asSequence()
        .filter {
            it is LexicalScope && (
                    (it.ownerDescriptor is ClassDescriptor &&
                            it.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) ||
                            (it.ownerDescriptor is FunctionDescriptor &&
                                    it.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE) ||
                            (it.ownerDescriptor is PropertyDescriptor &&
                                    it.kind == LexicalScopeKind.PROPERTY_INITIALIZER_OR_DELEGATE) ||
                            it.kind == LexicalScopeKind.CODE_BLOCK ||
                            it.kind == LexicalScopeKind.CLASS_INITIALIZER
                    )
        }
        .fold(importsResolutionScope) { parent, next ->
            when {
                next is LexicalScope && next.ownerDescriptor is ClassDescriptor -> {
                    val clazz = next.ownerDescriptor as ClassDescriptor
                    val companionScope = clazz.companionObjectDescriptor
                        ?.let { companionDescriptor ->
                            trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, companionDescriptor)
                                ?: run {
                                    val finalParent = companionDescriptor
                                        .findPsi()
                                        .safeAs<KtClassOrObject>()
                                        ?.getGivenImports()
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.toImportResolutionScope("CLASS COMPANION ${clazz.fqNameSafe}", parent, context, trace)
                                        ?: parent
                                    ResolutionScope(
                                        name = "CLASS COMPANION ${clazz.fqNameSafe}",
                                        context = context,
                                        callContext = next.callContext(trace.bindingContext),
                                        parent = finalParent,
                                        ownerDescriptor = companionDescriptor,
                                        trace = trace,
                                        initialGivens = listOf(
                                            companionDescriptor
                                                .getGivenReceiver(context, trace)
                                        ),
                                        imports = emptyList(),
                                        typeParameters = emptyList()
                                    ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, companionDescriptor, it) }
                                }
                        }
                    val finalParent = clazz
                        .findPsi()
                        .safeAs<KtClassOrObject>()
                        ?.getGivenImports()
                        ?.takeIf { it.isNotEmpty() }
                        ?.toImportResolutionScope("CLASS ${clazz.fqNameSafe}",
                            companionScope ?: parent, context, trace)
                        ?: companionScope ?: parent
                    trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz)
                        ?: ResolutionScope(
                            name = "CLASS ${clazz.fqNameSafe}",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = finalParent,
                            ownerDescriptor = clazz,
                            trace = trace,
                            initialGivens = listOf(clazz.getGivenReceiver(context, trace)),
                            imports = emptyList(),
                            typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(context, trace) }
                        ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz, it) }
                }
                next is LexicalScope && next.ownerDescriptor is FunctionDescriptor &&
                        next.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE -> {
                    val function = next.ownerDescriptor as FunctionDescriptor
                    trace.get(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function)
                        ?: run {
                            val finalParent = function
                                .findPsi()
                                .safeAs<KtFunction>()
                                ?.getGivenImports()
                                ?.takeIf { it.isNotEmpty() }
                                ?.toImportResolutionScope("FUNCTION ${function.fqNameSafe}", parent, context, trace)
                                ?: parent
                            ResolutionScope(
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
                }
                next is LexicalScope && next.ownerDescriptor is PropertyDescriptor -> {
                    val property = next.ownerDescriptor as PropertyDescriptor
                    trace.get(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property)
                        ?: run {
                            val finalParent = property
                                .findPsi()
                                .safeAs<KtProperty>()
                                ?.getGivenImports()
                                ?.takeIf { it.isNotEmpty() }
                                ?.toImportResolutionScope("PROPERTY ${property.fqNameSafe}", parent, context, trace)
                                ?: parent
                            ResolutionScope(
                                name = "Hierarchical $next",
                                context = context,
                                callContext = next.callContext(trace.bindingContext),
                                parent = finalParent,
                                ownerDescriptor = property,
                                trace = trace,
                                initialGivens = next.collectGivens(context, trace),
                                imports = emptyList(),
                                typeParameters = property.typeParameters.map { it.toClassifierRef(context, trace) }
                            ).also { trace.record(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property, it) }
                        }
                }
                else -> {
                    val ownerDescriptor = next.parentsWithSelf
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
                        ?.toImportResolutionScope("BLOCK", parent, context, trace)
                        ?: parent
                    trace.get(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next)
                        ?: ResolutionScope(
                            name = "Hierarchical $next",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = finalParent,
                            ownerDescriptor = ownerDescriptor,
                            trace = trace,
                            initialGivens = next.collectGivens(context, trace),
                            imports = emptyList(),
                            typeParameters = emptyList()
                        )//.also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next, it) }
                }
            }
        }
        .also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, finalScope, it) }
}

private fun List<GivenImport>.toImportResolutionScope(
    namePrefix: String,
    parent: ResolutionScope?,
    context: InjektContext,
    trace: BindingTrace
): ResolutionScope {
    val (externalImportedGivens, internalImportedGivens) = this
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
        imports = this,
        typeParameters = emptyList()
    )
}
