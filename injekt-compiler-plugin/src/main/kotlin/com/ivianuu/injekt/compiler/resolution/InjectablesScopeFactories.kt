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
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun ElementInjectablesScope(
  context: InjektContext,
  trace: BindingTrace,
  element: KtElement,
  position: KtElement = element
): InjectablesScope {
  synchronized(context.elementScopes) {
    val scopeOwner = element.parentsWithSelf
      .first { (it as KtElement).isScopeOwner(position) }
      .cast<KtElement>()

    if (scopeOwner !is KtBlockExpression)
      context.elementScopes[scopeOwner]?.let { return it }

    val parentScope = scopeOwner.parents
      .firstOrNull { (it as KtElement).isScopeOwner(position) }
      ?.let { ElementInjectablesScope(context, trace, it.cast(), position) }

    val scope = when (scopeOwner) {
      is KtFile -> FileInjectablesScope(scopeOwner, context, trace)
      is KtClassOrObject -> ClassInjectablesScope(
        scopeOwner.descriptor(trace.bindingContext)!!,
        context,
        trace,
        parentScope!!
      )
      is KtConstructor<*> -> {
        if (scopeOwner.bodyExpression.let { it == null || it !in position.parents }) {
          ConstructorPreInitInjectablesScope(
            scopeOwner.descriptor(trace.bindingContext)!!,
            context,
            trace,
            parentScope!!
          )
        } else FunctionInjectablesScope(
          scopeOwner.descriptor(trace.bindingContext)!!,
          context,
          trace,
          parentScope!!
        )
      }
      is KtFunction -> FunctionInjectablesScope(
        scopeOwner.descriptor(trace.bindingContext)!!,
        context,
        trace,
        parentScope!!
      )
      is KtParameter -> ValueParameterDefaultValueInjectablesScope(
        scopeOwner.descriptor(trace.bindingContext)!!,
        context,
        trace,
        parentScope!!
      )
      is KtProperty -> {
        when (val descriptor = scopeOwner.descriptor<VariableDescriptor>(trace.bindingContext)!!) {
          is PropertyDescriptor -> PropertyInjectablesScope(descriptor, context, trace, parentScope!!)
          is LocalVariableDescriptor -> LocalVariableInjectablesScope(descriptor, context, trace, parentScope!!)
          else -> throw AssertionError("Unexpected variable descriptor $descriptor")
        }
      }
      is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>(trace.bindingContext)
        ?.unsubstitutedPrimaryConstructor
        ?.let { ConstructorPreInitInjectablesScope(it, context, trace, parentScope!!) }
        ?: parentScope!!
      is KtClassInitializer, is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>(trace.bindingContext)
        ?.unsubstitutedPrimaryConstructor
        ?.let { FunctionInjectablesScope(it, context, trace, parentScope!!) }
        ?: parentScope!!
      is KtBlockExpression -> BlockExpressionInjectablesScope(scopeOwner, position, context, trace, parentScope!!)
      is KtAnnotatedExpression -> ExpressionInjectablesScope(scopeOwner, context, trace, parentScope!!)
      else -> throw AssertionError("Unexpected scope owner $scopeOwner")
    }

    if (scopeOwner !is KtBlockExpression)
      context.elementScopes[scopeOwner] = scope

    return scope
  }
}

private fun KtElement.isScopeOwner(position: KtElement): Boolean =
  this is KtFile ||
      (this is KtClassOrObject && position.getParentOfType<KtSuperTypeList>(false) == null &&
          run {
            val constructor = position.getParentOfType<KtConstructor<*>>(false)
            constructor == null ||
                constructor.bodyExpression.let { it != null && it in position.parents }
          }) ||
      this is KtClassInitializer ||
      (this is KtFunction && position.parents.none { it in valueParameters }) ||
      this is KtProperty ||
      this is KtParameter ||
      this is KtSuperTypeList ||
      this is KtBlockExpression ||
      (this is KtClassBody && position.parents
        .takeWhile { it != this }
        .none {
          (it is KtFunction && it.parent == this) ||
              (it is KtPropertyAccessor && it.property.parent == this)
        }) ||
      (this is KtAnnotatedExpression && hasAnnotation(InjektFqNames.Providers))

private fun FileInjectablesScope(
  file: KtFile,
  context: InjektContext,
  trace: BindingTrace
): InjectablesScope {
  synchronized(context.elementScopes) {
    context.elementScopes[file]?.let { return it }
    return ImportInjectablesScope(
      imports = file.getProviderImports() + ProviderImport(null, "${file.packageFqName.asString()}.*"),
      namePrefix = "FILE ${file.name}",
      parent = null,
      context = context,
      trace = trace
    ).also {
      context.elementScopes[file] = it
    }
  }
}

private fun ClassCompanionInjectablesScope(
  clazz: ClassDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope = clazz.companionObjectDescriptor
  ?.let { ClassInjectablesScope(it, context, trace, parent) } ?: parent

private fun ClassImportsInjectablesScope(
  clazz: ClassDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  val finalParent = ClassCompanionInjectablesScope(clazz, context, trace, parent)
  return (clazz
    .findPsi()
    .safeAs<KtClassOrObject>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(it, "CLASS ${clazz.fqNameSafe}", finalParent, context, trace) }
    ?: finalParent)
}

private fun ClassInjectablesScope(
  clazz: ClassDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  synchronized(context.declarationScopes) {
    context.declarationScopes[clazz]?.let { return it }
    val finalParent = ClassImportsInjectablesScope(clazz, context, trace, parent)
    val name = if (clazz.isCompanionObject)
      "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
    else "CLASS ${clazz.fqNameSafe}"
    return InjectablesScope(
      name = name,
      context = context,
      callContext = CallContext.DEFAULT,
      parent = finalParent,
      ownerDescriptor = clazz,
      trace = trace,
      initialInjectables = listOf(clazz.injectableReceiver(context, trace, false)),
      imports = emptyList(),
      typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(context, trace) },
      nesting = finalParent.nesting + 1
    ).also { context.declarationScopes[clazz] = it }
  }
}

private fun ConstructorPreInitInjectablesScope(
  constructor: ConstructorDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  val finalParent = ClassImportsInjectablesScope(
    constructor.constructedClass,
    context,
    trace,
    FunctionImportsInjectablesScope(constructor, context, trace, parent)
  )
  val parameterScopes = FunctionParameterInjectablesScopes(context, trace, finalParent, constructor, null)
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toClassifierRef(context, trace)
  }
  return InjectablesScope(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    context = context,
    trace = trace,
    callContext = CallContext.DEFAULT,
    ownerDescriptor = constructor,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting
  )
}

private fun FunctionImportsInjectablesScope(
  function: FunctionDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  val finalParent = parent
  val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
  return function
    .findPsi()
    .safeAs<KtFunction>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(it, "$baseName ${function.fqNameSafe}", finalParent, context, trace) }
    ?: finalParent
}

private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: ValueParameterDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val finalParent = FunctionImportsInjectablesScope(function, context, trace, parent)
  val parameterScopes = FunctionParameterInjectablesScopes(context, trace, finalParent, function, valueParameter)
  return InjectablesScope(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    context = context,
    trace = trace,
    callContext = function.callContext(trace.bindingContext)
      // suspend functions cannot be called from a default value context
      .takeIf { it != CallContext.SUSPEND } ?: CallContext.DEFAULT,
    ownerDescriptor = function,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = function.typeParameters.map { it.toClassifierRef(context, trace) },
    nesting = finalParent.nesting + 1
  )
}

private fun FunctionInjectablesScope(
  function: FunctionDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  synchronized(context.declarationScopes) {
    context.declarationScopes[function]?.let { return it }
    val finalParent = FunctionImportsInjectablesScope(function, context, trace, parent)
    val parameterScopes = FunctionParameterInjectablesScopes(context, trace, finalParent, function, null)
    val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
    val typeParameters = (if (function is ConstructorDescriptor)
      function.constructedClass.declaredTypeParameters
    else function.typeParameters)
      .map { it.toClassifierRef(context, trace) }
    return InjectablesScope(
      name = "$baseName ${function.fqNameSafe}",
      parent = parameterScopes,
      context = context,
      trace = trace,
      callContext = function.callContext(trace.bindingContext),
      ownerDescriptor = function,
      initialInjectables = emptyList(),
      imports = emptyList(),
      typeParameters = typeParameters,
      nesting = parameterScopes.nesting
    ).also { context.declarationScopes[function] = it }
  }
}

private fun FunctionParameterInjectablesScopes(
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope,
  function: FunctionDescriptor,
  until: ValueParameterDescriptor? = null
): InjectablesScope {
  val maxIndex = until?.injektIndex()
  return function.allParameters
    .asSequence()
    .filter {
      (maxIndex == null || it.injektIndex() < maxIndex) &&
          (it.isProvide(context, trace) || it === function.extensionReceiverParameter)
    }
    .map { it.toCallableRef(context, trace).makeProvide() }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        context = context,
        trace = trace,
        parent = acc,
        function = function,
        parameter = nextParameter
      )
    }
}

private fun FunctionParameterInjectablesScope(
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope,
  function: FunctionDescriptor,
  parameter: CallableRef
): InjectablesScope {
  parameter.callable as ParameterDescriptor
  return InjectablesScope(
    name = "FUNCTION PARAMETER ${parameter.callable.fqNameSafe.parent()}.${parameter.callable.injektName()}",
    context = context,
    callContext = CallContext.DEFAULT,
    parent = parent,
    ownerDescriptor = function,
    trace = trace,
    initialInjectables = listOf(parameter),
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = if (parent.name.startsWith("FUNCTION_PARAMETER")) parent.nesting
    else parent.nesting + 1
  )
}

private fun PropertyInjectablesScope(
  property: PropertyDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  synchronized(context.declarationScopes) {
    context.declarationScopes[property]?.let { return it }
    val finalParent = property
      .findPsi()
      .safeAs<KtProperty>()
      ?.getProviderImports()
      ?.takeIf { it.isNotEmpty() }
      ?.let { ImportInjectablesScope(it, "PROPERTY ${property.fqNameSafe}", parent, context, trace) }
      ?: parent
    return InjectablesScope(
      name = "PROPERTY ${property.fqNameSafe}",
      context = context,
      callContext = property.callContext(trace.bindingContext),
      parent = finalParent,
      ownerDescriptor = property,
      trace = trace,
      initialInjectables = listOfNotNull(
        property.extensionReceiverParameter
          ?.toCallableRef(context, trace)
          ?.makeProvide()
      ),
      imports = emptyList(),
      typeParameters = property.typeParameters.map { it.toClassifierRef(context, trace) },
      nesting = finalParent.nesting + 1
    ).also {
      context.declarationScopes[property] = it
    }
  }
}

private fun LocalVariableInjectablesScope(
  variable: LocalVariableDescriptor,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  synchronized(context.declarationScopes) {
    context.declarationScopes[variable]?.let { return it }
    val finalParent = variable
      .findPsi()
      .safeAs<KtProperty>()
      ?.getProviderImports()
      ?.takeIf { it.isNotEmpty() }
      ?.let { ImportInjectablesScope(it, "LOCAL VARIABLE ${variable.fqNameSafe}", parent, context, trace) }
      ?: parent
    return InjectablesScope(
      name = "LOCAL VARIABLE ${variable.fqNameSafe}",
      context = context,
      callContext = parent.callContext,
      parent = finalParent,
      ownerDescriptor = variable,
      trace = trace,
      initialInjectables = emptyList(),
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = finalParent.nesting
    ).also {
      context.declarationScopes[variable] = it
    }
  }
}

private fun ExpressionInjectablesScope(
  expression: KtAnnotatedExpression,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  synchronized(context.elementScopes) {
    context.elementScopes[expression]?.let { return it }
    val finalParent = expression
      .getProviderImports()
      .takeIf { it.isNotEmpty() }
      ?.let { ImportInjectablesScope(it, "EXPRESSION ${expression.startOffset}", parent, context, trace) }
      ?: parent
    return InjectablesScope(
      name = "EXPRESSION ${expression.startOffset}",
      context = context,
      callContext = finalParent.callContext,
      parent = finalParent,
      ownerDescriptor = null,
      trace = trace,
      initialInjectables = emptyList(),
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = finalParent.nesting
    ).also {
      context.elementScopes[expression] = it
    }
  }
}

private fun BlockExpressionInjectablesScope(
  block: KtBlockExpression,
  position: KtElement,
  context: InjektContext,
  trace: BindingTrace,
  parent: InjectablesScope
): InjectablesScope {
  val positionOffset = position.getStartOffsetIn(block)
  val visibleInjectableDeclarations = block.statements
    .filter { it.getStartOffsetIn(block) < positionOffset }
    .filterIsInstance<KtNamedDeclaration>()
    .mapNotNull { it.descriptor(trace.bindingContext) }
    .filter { it.isProvide(context, trace) }
  if (visibleInjectableDeclarations.isEmpty()) return parent
  val injectableDeclaration = visibleInjectableDeclarations.last()
  val key = block to injectableDeclaration
  val finalParent = if (visibleInjectableDeclarations.size > 1)
    BlockExpressionInjectablesScope(block, injectableDeclaration.findPsi().cast(), context, trace, parent)
  else parent
  return InjectablesScope(
    name = "BLOCK AT ${injectableDeclaration.name}",
    context = context,
    callContext = finalParent.callContext,
    parent = finalParent,
    ownerDescriptor = null,
    trace = trace,
    initialInjectables = when (injectableDeclaration) {
      is ClassDescriptor -> injectableDeclaration.injectableConstructors(context, trace)
      is CallableDescriptor -> listOf(injectableDeclaration.toCallableRef(context, trace))
      else -> throw AssertionError("Unexpected injectable $injectableDeclaration")
    },
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = if (visibleInjectableDeclarations.size > 1) finalParent.nesting
    else finalParent.nesting + 1
  )
}

fun TypeInjectablesScope(
  context: InjektContext,
  trace: BindingTrace,
  type: TypeRef,
  lookupLocation: LookupLocation
): InjectablesScope {
  val finalType = type.withNullability(false)
  synchronized(context.typeScopes) {
    context.typeScopes[finalType]?.let { return it }

    val initialInjectables = finalType.collectTypeScopeInjectables(context, trace, lookupLocation)

    return InjectablesScope(
      name = "TYPE ${finalType.renderToString()}",
      parent = null,
      context = context,
      callContext = CallContext.DEFAULT,
      ownerDescriptor = finalType.classifier.descriptor,
      trace = trace,
      initialInjectables = initialInjectables,
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = 0
    ).also {
      context.typeScopes[finalType] = it
    }
  }
}

private fun ImportInjectablesScope(
  imports: List<ProviderImport>,
  namePrefix: String,
  parent: InjectablesScope?,
  context: InjektContext,
  trace: BindingTrace
): InjectablesScope {
  val resolvedImports = imports.collectImportedInjectables(context, trace)
  return InjectablesScope(
    name = "$namePrefix INTERNAL IMPORTS",
    context = context,
    callContext = CallContext.DEFAULT,
    parent = InjectablesScope(
      name = "$namePrefix EXTERNAL IMPORTS",
      context = context,
      callContext = CallContext.DEFAULT,
      parent = parent,
      ownerDescriptor = null,
      trace = trace,
      initialInjectables = resolvedImports
        .filter { it.callable.isExternalDeclaration(context) },
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = parent?.nesting?.inc() ?: 0
    ),
    ownerDescriptor = null,
    trace = trace,
    initialInjectables = resolvedImports
      .filterNot { it.callable.isExternalDeclaration(context) },
    imports = imports.mapNotNull { it.resolve(context) },
    typeParameters = emptyList(),
    nesting = parent?.nesting?.inc()?.inc() ?: 1
  )
}

