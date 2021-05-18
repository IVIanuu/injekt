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
import org.jetbrains.kotlin.incremental.components.*
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
  trace[InjektWritableSlices.HIERARCHICAL_RESOLUTION_SCOPE, finalScope]?.let { return it }

  val allScopes = finalScope.parentsWithSelf.toList()

  val file = allScopes
    .filterIsInstance<LexicalScope>()
    .first()
    .ownerDescriptor
    .findPsi()!!
    .cast<KtElement>()
    .containingKtFile

  val fileImports = (file.getProviderImports() + ProviderImport(null, "${file.packageFqName}.*"))
    .sortedBy { it.importPath }

  val importsResolutionScope = ImportResolutionScope(
    fileImports,
    "FILE ${file.name}",
    null,
    context,
    trace
  )

  return allScopes
    .filter { it !is ImportingScope }
    .reversed()
    .asSequence()
    .filter { it.isApplicableScope() }
    .fold(importsResolutionScope) { parent, next ->
      checkCancelled()
      when {
        next is LexicalScope && next.ownerDescriptor is ClassDescriptor ->
          ClassResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
        next is LexicalScope && next.ownerDescriptor is FunctionDescriptor &&
            // important to check this here to avoid bugs
            // related to local givens
            next.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE ->
          if (next.ownerDescriptor is PropertyAccessorDescriptor)
            PropertyResolutionScope(
              next.ownerDescriptor.cast<PropertyAccessorDescriptor>()
                .correspondingProperty, context, trace, parent
            )
          else FunctionResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
        next is LexicalScope && next.ownerDescriptor is PropertyDescriptor ->
          PropertyResolutionScope(next.ownerDescriptor.cast(), context, trace, parent)
        else -> CodeBlockResolutionScope(next, context, trace, parent)
      }
    }
    .also { trace.record(InjektWritableSlices.HIERARCHICAL_RESOLUTION_SCOPE, finalScope, it) }
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
  imports: List<ProviderImport>,
  namePrefix: String,
  parent: ResolutionScope?,
  context: InjektContext,
  trace: BindingTrace
): ResolutionScope {
  val resolvedImports = imports.collectImportGivens(context, trace)
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
      initialGivens = resolvedImports
          .filter { it.callable.isExternalDeclaration(context) },
      imports = emptyList(),
      typeParameters = emptyList()
    ),
    ownerDescriptor = null,
    trace = trace,
    initialGivens = resolvedImports
        .filterNot { it.callable.isExternalDeclaration(context) },
    imports = imports.map { it.resolve(context) },
    typeParameters = emptyList()
  )
}

private fun ClassResolutionScope(
  clazz: ClassDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: ResolutionScope?
): ResolutionScope {
  trace.get(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, clazz)
    ?.let { return it }
  val companionObjectScope = clazz.companionObjectDescriptor
    ?.let { ClassResolutionScope(it, context, trace, parent) }
  val name = if (clazz.isCompanionObject)
    "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
  else "CLASS ${clazz.fqNameSafe}"
  val finalParent = clazz
    .findPsi()
    .safeAs<KtClassOrObject>()
    ?.getProviderImports()
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
    initialGivens = listOf(clazz.givenReceiver(context, trace, false)),
    imports = emptyList(),
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(context, trace) }
  ).also { trace.record(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, clazz, it) }
}

private fun FunctionResolutionScope(
  function: FunctionDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: ResolutionScope?
): ResolutionScope {
  trace.get(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, function)
    ?.let { return it }
  val finalParent = function
    .findPsi()
    .safeAs<KtFunction>()
    ?.getProviderImports()
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
        .filter { it.isProvide(context, trace) || it === function.extensionReceiverParameter }
        .map { it.toCallableRef(context, trace).makeGiven() }
        .toList(),
    imports = emptyList(),
    typeParameters = function.typeParameters.map { it.toClassifierRef(context, trace) }
  ).also { trace.record(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, function, it) }
}

private fun PropertyResolutionScope(
  property: PropertyDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: ResolutionScope?
): ResolutionScope {
  trace.get(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, property)
    ?.let { return it }
  val finalParent = property
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
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
  ).also { trace.record(InjektWritableSlices.DECLARATION_RESOLUTION_SCOPE, property, it) }
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
    ?.map { it.toProviderImport() }
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
    initialGivens = scope.collectInstances(context, trace, true),
    imports = emptyList(),
    typeParameters = emptyList()
  )
}

fun TypeResolutionScope(
  context: InjektContext,
  trace: BindingTrace,
  type: TypeRef,
  lookupLocation: LookupLocation
): ResolutionScope {
  val finalType = type.withNullability(false)
  trace[InjektWritableSlices.TYPE_RESOLUTION_SCOPE, finalType]?.let { return it }

  val initialGivens = finalType.collectTypeScopeGivens(context, trace, lookupLocation)

  return ResolutionScope(
    name = "TYPE ${finalType.render()}",
    parent = null,
    context = context,
    callContext = CallContext.DEFAULT,
    ownerDescriptor = finalType.classifier.descriptor,
    trace = trace,
    initialGivens = initialGivens,
    imports = emptyList(),
    typeParameters = emptyList()
  ).also {
    trace.record(InjektWritableSlices.TYPE_RESOLUTION_SCOPE, finalType, it)
  }
}
