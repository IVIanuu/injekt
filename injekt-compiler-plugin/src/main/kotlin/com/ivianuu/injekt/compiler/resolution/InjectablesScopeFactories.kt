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

@file:Providers("com.ivianuu.injekt.compiler.bindingContext")

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun ElementInjectablesScope(
  element: KtElement,
  position: KtElement = element,
  @Inject context: AnalysisContext
): InjectablesScope {
  val scopeOwner = element.parentsWithSelf
    .first { (it as KtElement).isScopeOwner(position) }
    .cast<KtElement>()

  if (scopeOwner !is KtBlockExpression)
    context.injektContext.elementScopes[scopeOwner]?.let { return it }

  val parentScope = scopeOwner.parents
    .firstOrNull { (it as KtElement).isScopeOwner(position) }
    ?.let { ElementInjectablesScope(it.cast(), position) }

  val scope = when {
    scopeOwner is KtFile -> FileInjectablesScope(scopeOwner)
    scopeOwner is KtClassOrObject -> ClassInjectablesScope(
      scopeOwner.descriptor()!!,
      parentScope!!
    )
    scopeOwner is KtConstructor<*> -> {
      if (scopeOwner.bodyExpression.let { it == null || it !in position.parents }) {
        ConstructorPreInitInjectablesScope(
          scopeOwner.descriptor()!!,
          parentScope!!
        )
      } else FunctionInjectablesScope(
        scopeOwner.descriptor()!!,
        parentScope!!
      )
    }
    scopeOwner is KtFunction -> FunctionInjectablesScope(
      scopeOwner.descriptor()!!,
      parentScope!!
    )
    scopeOwner is KtParameter -> ValueParameterDefaultValueInjectablesScope(
      scopeOwner.descriptor()!!,
      parentScope!!
    )
    scopeOwner is KtPropertyAccessor -> InitializedPropertyInjectablesScope(
      scopeOwner.descriptor<PropertyGetterDescriptor>()!!.correspondingProperty,
      parentScope!!
    )
    scopeOwner is KtExpression && scopeOwner.parent is KtProperty && !scopeOwner.parent.cast<KtProperty>().isLocal ->
      PropertyInitializerInjectablesScope(scopeOwner.parent.cast<KtProperty>().descriptor()!!, parentScope!!)
    scopeOwner is KtProperty -> LocalVariableInjectablesScope(scopeOwner.descriptor()!!, parentScope!!)
    scopeOwner is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>()
      ?.unsubstitutedPrimaryConstructor
      ?.let { ConstructorPreInitInjectablesScope(it, parentScope!!) }
      ?: parentScope!!
    scopeOwner is KtClassInitializer || scopeOwner is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>()
      ?.unsubstitutedPrimaryConstructor
      ?.let { FunctionInjectablesScope(it, parentScope!!) }
      ?: parentScope!!
    scopeOwner is KtBlockExpression -> BlockExpressionInjectablesScope(scopeOwner, position, parentScope!!)
    scopeOwner is KtAnnotatedExpression -> ExpressionInjectablesScope(scopeOwner, parentScope!!)
    else -> throw AssertionError("Unexpected scope owner $scopeOwner")
  }

  if (scopeOwner !is KtBlockExpression)
    context.injektContext.elementScopes[scopeOwner] = scope

  return scope
}

private fun KtElement.isScopeOwner(position: KtElement): Boolean {
  if (this is KtFile ||
    this is KtObjectDeclaration ||
    this is KtClassInitializer ||
    (this is KtProperty && isLocal) ||
    this is KtPropertyAccessor ||
    this is KtPropertyDelegate ||
    this is KtParameter ||
    this is KtSuperTypeList ||
    this is KtBlockExpression)
      return true

  if (this is KtExpression && parent.safeAs<KtProperty>()?.let {
    !it.isLocal && it.initializer == this
    } == true) return true

  if (this is KtFunction && position.parents.none { it in valueParameters })
    return true

  if (this is KtAnnotatedExpression && hasAnnotation(InjektFqNames.Providers))
    return true

  if (this is KtClass && position.getParentOfType<KtSuperTypeList>(false) == null) {
    val constructor = position.getParentOfType<KtConstructor<*>>(false)
    if (constructor != null &&
      constructor.bodyExpression.let { it == null || it !in position.parents })
        return false

    val allClassesBetween = position.parentsWithSelf
      .filterIsInstance<KtClassOrObject>()
      .takeWhile { it != this }
      .toList()
    val nearestClassFromPosition = allClassesBetween.firstOrNull()
      ?: return true

    return nearestClassFromPosition !is KtObjectDeclaration &&
        allClassesBetween.all {
          it.hasModifier(KtTokens.INNER_KEYWORD)
        }
  }

  if (this is KtClassBody && position.parents
      .takeWhile { it != this }
      .none {
        (it is KtClass && it != this) ||
            (it is KtFunction && it.parent == this) ||
            (it is KtPropertyAccessor && it.property.parent == this)
      }) return true

  return false
}

private fun FileInjectablesScope(
  file: KtFile,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.elementScopes.getOrPut(file) {
  ImportInjectablesScope(
    file = file,
    imports = file.getProviderImports() + ProviderImport(null, "${file.packageFqName.asString()}.*"),
    namePrefix = "FILE ${file.name}",
    parent = null
  )
}

private fun ClassCompanionInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = clazz.companionObjectDescriptor
  ?.let { ClassInjectablesScope(it, parent) } ?: parent

private fun ClassImportsInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope {
  val finalParent = ClassCompanionInjectablesScope(clazz, parent)
  return (clazz
    .findPsi()
    .safeAs<KtClassOrObject>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(null, it, "CLASS ${clazz.fqNameSafe}", finalParent) }
    ?: finalParent)
}

private fun ClassInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.declarationScopes.getOrPut(clazz) {
  val finalParent = ClassImportsInjectablesScope(clazz, parent)
  val name = if (clazz.isCompanionObject)
    "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
  else "CLASS ${clazz.fqNameSafe}"
  InjectablesScope(
    name = name,
    callContext = CallContext.DEFAULT,
    parent = finalParent,
    ownerDescriptor = clazz,
    file = null,
    initialInjectables = listOf(clazz.injectableReceiver(false)),
    imports = emptyList(),
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef() },
    nesting = finalParent.nesting + 1,
    ignoredInjectables = emptyList()
  )
}

private fun ConstructorPreInitInjectablesScope(
  constructor: ConstructorDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope {
  val finalParent = ClassImportsInjectablesScope(
    constructor.constructedClass,
    FunctionImportsInjectablesScope(constructor, parent)
  )
  val parameterScopes = FunctionParameterInjectablesScopes(finalParent, constructor, null)
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toClassifierRef()
  }
  return InjectablesScope(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    callContext = CallContext.DEFAULT,
    ownerDescriptor = constructor,
    file = null,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ignoredInjectables = emptyList()
  )
}

private fun FunctionImportsInjectablesScope(
  function: FunctionDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = function
  .findPsi()
  .safeAs<KtFunction>()
  ?.getProviderImports()
  ?.takeIf { it.isNotEmpty() }
  ?.let {
    val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
    ImportInjectablesScope(null, it, "$baseName ${function.fqNameSafe}", parent)
  }
  ?: parent

private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: ValueParameterDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope {
  val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val finalParent = FunctionImportsInjectablesScope(function, parent)
  val parameterScopes = FunctionParameterInjectablesScopes(finalParent, function, valueParameter)
  return InjectablesScope(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    callContext = function.callContext()
      // suspend functions cannot be called from a default value context
      .takeIf { it != CallContext.SUSPEND } ?: CallContext.DEFAULT,
    ownerDescriptor = function,
    file = null,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = function.typeParameters.map { it.toClassifierRef() },
    nesting = finalParent.nesting + 1,
    ignoredInjectables = emptyList()
  )
}

private fun FunctionInjectablesScope(
  function: FunctionDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.declarationScopes.getOrPut(function) {
  val finalParent = FunctionImportsInjectablesScope(function, parent)
  val parameterScopes = FunctionParameterInjectablesScopes(finalParent, function, null)
  val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is ConstructorDescriptor)
    function.constructedClass.declaredTypeParameters
  else function.typeParameters)
    .map { it.toClassifierRef() }
  InjectablesScope(
    name = "$baseName ${function.fqNameSafe}",
    parent = parameterScopes,
    callContext = function.callContext(),
    ownerDescriptor = function,
    file = null,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ignoredInjectables = emptyList()
  )
}

private fun FunctionParameterInjectablesScopes(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  until: ValueParameterDescriptor? = null,
  @Inject context: AnalysisContext
): InjectablesScope {
  val maxIndex = until?.injektIndex()
  return function.allParameters
    .asSequence()
    .filter {
      (maxIndex == null || it.injektIndex() < maxIndex) &&
          (it.isProvide() || it === function.extensionReceiverParameter)
    }
    .map { it.toCallableRef().makeProvide() }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        parent = acc,
        function = function,
        parameter = nextParameter
      )
    }
}

private fun FunctionParameterInjectablesScope(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  parameter: CallableRef,
  @Inject context: AnalysisContext
): InjectablesScope {
  parameter.callable as ParameterDescriptor
  return InjectablesScope(
    name = "FUNCTION PARAMETER ${parameter.callable.fqNameSafe.parent()}.${parameter.callable.injektName()}",
    callContext = CallContext.DEFAULT,
    parent = parent,
    ownerDescriptor = function,
    file = null,
    initialInjectables = listOf(parameter),
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = if (parent.name.startsWith("FUNCTION_PARAMETER")) parent.nesting
    else parent.nesting + 1,
    ignoredInjectables = emptyList()
  )
}

private fun InitializedPropertyInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.declarationScopes.getOrPut(property) {
  val finalParent = property
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(null, it, "INITIALIZED PROPERTY ${property.fqNameSafe}", parent) }
    ?: parent

  InjectablesScope(
    name = "INITIALIZED PROPERTY ${property.fqNameSafe}",
    callContext = property.callContext(),
    parent = finalParent,
    ownerDescriptor = property,
    file = null,
    initialInjectables = listOfNotNull(
      property.extensionReceiverParameter
        ?.toCallableRef()
        ?.makeProvide()
    ),
    imports = emptyList(),
    typeParameters = property.typeParameters.map { it.toClassifierRef() },
    nesting = finalParent.nesting + 1,
    ignoredInjectables = emptyList()
  )
}

private fun PropertyInitializerInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.declarationScopes.getOrPut(property) {
  val finalParent = property
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(null, it, "PROPERTY INITIALIZER ${property.fqNameSafe}", parent) }
    ?: parent

  InjectablesScope(
    name = "PROPERTY INITIALIZER ${property.fqNameSafe}",
    callContext = property.callContext(),
    parent = finalParent,
    ownerDescriptor = property,
    file = null,
    initialInjectables = listOfNotNull(
      property.extensionReceiverParameter
        ?.toCallableRef()
        ?.makeProvide()
    ),
    imports = emptyList(),
    typeParameters = property.typeParameters.map { it.toClassifierRef() },
    nesting = finalParent.nesting + 1,
    ignoredInjectables = listOf(property)
  )
}

private fun LocalVariableInjectablesScope(
  variable: LocalVariableDescriptor,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.declarationScopes.getOrPut(variable) {
  val finalParent = variable
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(null, it, "LOCAL VARIABLE ${variable.fqNameSafe}", parent) }
    ?: parent

  InjectablesScope(
    name = "LOCAL VARIABLE ${variable.fqNameSafe}",
    callContext = parent.callContext,
    parent = finalParent,
    ownerDescriptor = variable,
    file = null,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = finalParent.nesting,
    ignoredInjectables = emptyList()
  )
}

private fun ExpressionInjectablesScope(
  expression: KtAnnotatedExpression,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope = context.injektContext.elementScopes.getOrPut(expression) {
  val finalParent = expression
    .getProviderImports()
    .takeIf { it.isNotEmpty() }
    ?.let { ImportInjectablesScope(null, it, "EXPRESSION ${expression.startOffset}", parent) }
    ?: parent

  InjectablesScope(
    name = "EXPRESSION ${expression.startOffset}",
    callContext = finalParent.callContext,
    parent = finalParent,
    ownerDescriptor = null,
    file = null,
    initialInjectables = emptyList(),
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = finalParent.nesting,
    ignoredInjectables = emptyList()
  )
}

private fun BlockExpressionInjectablesScope(
  block: KtBlockExpression,
  position: KtElement,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope {
  val visibleInjectableDeclarations = block.statements
    .filter { it.endOffset < position.startOffset }
    .filterIsInstance<KtNamedDeclaration>()
    .mapNotNull { it.descriptor() }
    .filter { it.isProvide() }
  if (visibleInjectableDeclarations.isEmpty()) return parent
  val injectableDeclaration = visibleInjectableDeclarations.last()
  val key = block to injectableDeclaration
  return context.injektContext.blockScopes.getOrPut(key) {
    val finalParent = if (visibleInjectableDeclarations.size > 1)
      BlockExpressionInjectablesScope(block, injectableDeclaration.findPsi().cast(), parent)
    else parent

    InjectablesScope(
      name = "BLOCK AT ${injectableDeclaration.name}",
      callContext = finalParent.callContext,
      parent = finalParent,
      ownerDescriptor = null,
      file = null,
      initialInjectables = when (injectableDeclaration) {
        is ClassDescriptor -> injectableDeclaration.injectableConstructors()
        is CallableDescriptor -> listOf(injectableDeclaration.toCallableRef())
        else -> throw AssertionError("Unexpected injectable $injectableDeclaration")
      },
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = if (visibleInjectableDeclarations.size > 1) finalParent.nesting
      else finalParent.nesting + 1,
      ignoredInjectables = emptyList()
    )
  }
}

fun TypeInjectablesScope(
  type: TypeRef,
  parent: InjectablesScope,
  @Inject context: AnalysisContext
): InjectablesScope {
  val finalType = type.withNullability(false)
  return parent.typeScopes.getOrPut(finalType) {
    val injectablesWithLookups = finalType.collectTypeScopeInjectables()
    InjectablesScope(
      name = "TYPE ${finalType.renderToString()}",
      parent = parent,
      callContext = CallContext.DEFAULT,
      ownerDescriptor = finalType.classifier.descriptor,
      file = null,
      initialInjectables = injectablesWithLookups.injectables,
      imports = injectablesWithLookups.lookedUpPackages
        .map { ResolvedProviderImport(null, "$it.*", it) },
      typeParameters = emptyList(),
      nesting = parent.nesting,
      ignoredInjectables = emptyList()
    )
  }
}

private fun ImportInjectablesScope(
  file: KtFile?,
  imports: List<ProviderImport>,
  namePrefix: String,
  parent: InjectablesScope?,
  @Inject context: AnalysisContext
): InjectablesScope {
  val resolvedImports = imports.collectImportedInjectables()
  return InjectablesScope(
    name = "$namePrefix INTERNAL IMPORTS",
    callContext = CallContext.DEFAULT,
    parent = InjectablesScope(
      name = "$namePrefix EXTERNAL IMPORTS",
      callContext = CallContext.DEFAULT,
      parent = parent,
      ownerDescriptor = null,
      file = file,
      initialInjectables = resolvedImports
        .filter { it.callable.isExternalDeclaration() },
      imports = emptyList(),
      typeParameters = emptyList(),
      nesting = parent?.nesting?.inc() ?: 0,
      ignoredInjectables = emptyList()
    ),
    ownerDescriptor = null,
    file = file,
    initialInjectables = resolvedImports
      .filterNot { it.callable.isExternalDeclaration() },
    imports = imports.mapNotNull { it.resolve() },
    typeParameters = emptyList(),
    nesting = parent?.nesting?.inc()?.inc() ?: 1,
    ignoredInjectables = emptyList()
  )
}
