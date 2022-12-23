/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.transform
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ElementContextScope(
  ctx: Context,
  element: KtElement,
  position: KtElement = element
): ContextScope {
  val scopeOwner = element.parentsWithSelf
    .first { (it as KtElement).isScopeOwner(position, ctx) }
    .cast<KtElement>()

  if (scopeOwner !is KtBlockExpression)
    ctx.trace!!.bindingContext.get(InjektWritableSlices.ELEMENT_SCOPE, scopeOwner)
      ?.let { return it }

  val parentScope = scopeOwner.parents
    .firstOrNull { (it as KtElement).isScopeOwner(position, ctx) }
    ?.let { ElementContextScope(ctx, it.cast(), position) }

  val scope = when (scopeOwner) {
    is KtFile -> FileContextScope(scopeOwner, ctx)
    is KtClassOrObject -> ClassContextScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtConstructor<*> -> {
      if (scopeOwner.bodyExpression.let { it == null || it !in position.parents }) {
        ConstructorPreInitContextScope(
          scopeOwner.descriptor(ctx)!!,
          parentScope!!,
          ctx
        )
      } else FunctionContextScope(
        scopeOwner.descriptor(ctx)!!,
        parentScope!!,
        ctx
      )
    }
    is KtFunction -> FunctionContextScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtParameter -> ValueParameterDefaultValueContextScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtProperty -> {
      when (val descriptor = scopeOwner.descriptor<VariableDescriptor>(ctx)!!) {
        is PropertyDescriptor -> {
          if (scopeOwner.delegateExpressionOrInitializer != null &&
            scopeOwner.delegateExpressionOrInitializer!! in element.parentsWithSelf)
            PropertyInitContextScope(descriptor, parentScope!!, position, ctx)
          else
            PropertyContextScope(descriptor, parentScope!!, ctx)
        }
        is LocalVariableDescriptor -> LocalVariableContextScope(descriptor, parentScope!!, ctx)
        else -> throw AssertionError("Unexpected variable descriptor $descriptor")
      }
    }
    is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>(ctx)
      ?.unsubstitutedPrimaryConstructor
      ?.let { ConstructorPreInitContextScope(it, parentScope!!, ctx) }
      ?: parentScope!!
    is KtClassInitializer -> ClassInitContextScope(
      clazz = scopeOwner.getParentOfType<KtClassOrObject>(false)!!.descriptor(ctx)!!,
      parent = parentScope!!,
      position = position,
      ctx = ctx
    )
    is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>(ctx)
      ?.unsubstitutedPrimaryConstructor
      ?.let { FunctionContextScope(it, parentScope!!, ctx) }
      ?: parentScope!!
    is KtBlockExpression -> BlockExpressionContextScope(scopeOwner, position, parentScope!!, ctx)
    is KtAnnotatedExpression -> ExpressionContextScope(scopeOwner, parentScope!!, ctx)
    else -> throw AssertionError("Unexpected scope owner $scopeOwner")
  }

  if (scopeOwner !is KtBlockExpression)
    ctx.trace!!.record(InjektWritableSlices.ELEMENT_SCOPE, scopeOwner, scope)

  return scope
}

private fun KtElement.isScopeOwner(position: KtElement, ctx: Context): Boolean {
  if (this is KtFile ||
    this is KtClassInitializer ||
    this is KtProperty ||
    this is KtParameter ||
    this is KtSuperTypeList ||
    this is KtBlockExpression)
    return true

  if (this is KtFunction && position.parents.none { it in valueParameters })
    return true

  if (this is KtClassOrObject) {
    val propertyInitializerOrDelegateExpression = position.getParentOfType<KtProperty>(false)
      ?.takeUnless { it.isLocal }
      ?.delegateExpressionOrInitializer

    if (propertyInitializerOrDelegateExpression != null) {
      val parentsBetweenInitializerAndPosition = position.parentsWithSelf
        .takeWhile { it != propertyInitializerOrDelegateExpression }
        .toList()
      if (parentsBetweenInitializerAndPosition.none {
          it is KtNamedDeclaration || it is KtClassOrObject || it is KtFunctionLiteral
        })
          return false
    }

    val classInitializer = position.getParentOfType<KtClassInitializer>(false)

    if (classInitializer != null) {
      val parentsBetweenInitializerAndPosition = position.parents
        .takeWhile { it != classInitializer }
        .toList()
      if (parentsBetweenInitializerAndPosition.none {
          it is KtNamedDeclaration || it is KtClassOrObject || it is KtFunctionLiteral
      })
        return false
    }
  }

  if (this is KtObjectDeclaration &&
    position.getParentOfType<KtSuperTypeList>(false)
      ?.getParentOfType<KtClassOrObject>(false) != this)
    return true

  if (this is KtAnnotatedExpression && annotationEntries.any {
      it.shortName == InjektFqNames.Providers.shortName()
    })
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

    return (nearestClassFromPosition !is KtObjectDeclaration ||
        nearestClassFromPosition.isObjectLiteral()) &&
        allClassesBetween.all {
          it.hasModifier(KtTokens.INNER_KEYWORD) ||
              it.isObjectLiteral()
        }
  }

  if (this is KtClassBody && position.parents
      .takeWhile { it != this }
      .none { parent ->
        parent is KtClassInitializer ||
            (parent is KtProperty && parent.delegateExpressionOrInitializer != null &&
                parent.delegateExpressionOrInitializer!! in position.parents
              .takeWhile { it !is KtFunctionLiteral }) ||
            parent is KtClass ||
            (parent is KtNamedFunction && parent.parent == this) ||
            (parent is KtPropertyAccessor && parent.property.parent == this)
      })
        return true

  return false
}

private fun FileContextScope(file: KtFile, ctx: Context): ContextScope =
  ctx.trace!!.getOrPut(InjektWritableSlices.ELEMENT_SCOPE, file) {
    ImportContextScopes(
      file = file,
      imports = file.getProviderImports() + ProviderImport(null, "${file.packageFqName.asString()}.*"),
      namePrefix = "FILE ${file.name}",
      parent = null,
      ctx = ctx
    )
  }

private fun FileInitContextScope(position: KtElement, ctx: Context): ContextScope {
  val file = position.containingKtFile

  val visibleProviderDeclarations = file
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
          declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }

  val providerDeclaration = visibleProviderDeclarations.lastOrNull()

  return ImportContextScopes(
    file = file,
    imports = file.getProviderImports() + ProviderImport(null, "${file.packageFqName.asString()}.*"),
    namePrefix = "FILE INIT ${file.name} at ${providerDeclaration?.name}",
    providersPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@ImportContextScopes true
      psiProperty.containingFile != file ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleProviderDeclarations
    },
    parent = null,
    ctx = ctx
  )
}

private fun ClassCompanionContextScope(
  clazz: ClassDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = clazz.companionObjectDescriptor
  ?.let { ClassContextScope(it, parent, ctx) } ?: parent

private fun ClassImportsContextScope(
  clazz: ClassDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope {
  val finalParent = ClassCompanionContextScope(clazz, parent, ctx)
  return (clazz
    .findPsi()
    .safeAs<KtClassOrObject>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportContextScopes(null, it, "CLASS ${clazz.fqNameSafe}", finalParent, ctx = ctx) }
    ?: finalParent)
}

private fun ClassContextScope(
  clazz: ClassDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(clazz, parent.name)
) {
  val finalParent = ClassImportsContextScope(clazz, parent, ctx)
  val name = if (clazz.isCompanionObject)
    "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
  else "CLASS ${clazz.fqNameSafe}"
  ContextScope(
    name = name,
    parent = finalParent,
    ownerDescriptor = clazz,
    initialProviders = buildList {
      add(clazz.receiverProvider(false, ctx))
      clazz.contextReceivers.forEach { add(it.toCallableRef(ctx)) }
    },
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun ClassInitContextScope(
  clazz: ClassDescriptor,
  parent: ContextScope,
  position: KtElement,
  ctx: Context
): ContextScope {
  val psiClass = clazz.findPsi()!!
  val visibleProviderDeclarations = psiClass
    .cast<KtClassOrObject>()
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }
  val finalParent = ClassImportsContextScope(clazz, parent, ctx)

  val providerDeclaration = visibleProviderDeclarations.lastOrNull()

  val name = if (clazz.isCompanionObject)
    "COMPANION INIT ${clazz.containingDeclaration.fqNameSafe} at ${providerDeclaration?.name}"
  else "CLASS INIT ${clazz.fqNameSafe} at ${providerDeclaration?.name}"

  val thisProvider = clazz.receiverProvider(false, ctx)

  val classInitScope = ContextScope(
    name = name,
    parent = finalParent,
    ownerDescriptor = clazz,
    initialProviders = buildList {
      add(clazz.receiverProvider(false, ctx))
      clazz.contextReceivers.forEach { add(it.toCallableRef(ctx)) }
    },
    providersPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@ContextScope true
      psiProperty.getParentOfType<KtClass>(false) != psiClass ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleProviderDeclarations
    },
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )

  val primaryConstructor = clazz.unsubstitutedPrimaryConstructor

  return if (primaryConstructor == null) classInitScope
  else FunctionContextScope(primaryConstructor, classInitScope, ctx)
}

private fun ConstructorPreInitContextScope(
  constructor: ConstructorDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope {
  val finalParent = ClassImportsContextScope(
    constructor.constructedClass,
    FunctionImportsContextScope(constructor, parent, ctx),
    ctx
  )
  val parameterScopes = FunctionParameterContextScopes(finalParent, constructor, null, ctx)
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toClassifierRef(ctx)
  }
  return ContextScope(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = constructor,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )
}

private fun FunctionImportsContextScope(
  function: FunctionDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = function
  .findPsi()
  .safeAs<KtFunction>()
  ?.getProviderImports()
  ?.takeIf { it.isNotEmpty() }
  ?.let {
    val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
    ImportContextScopes(null, it, "$baseName ${function.fqNameSafe}", parent, ctx = ctx)
  }
  ?: parent

private fun ValueParameterDefaultValueContextScope(
  valueParameter: ValueParameterDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope {
  val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val finalParent = FunctionImportsContextScope(function, parent, ctx)
  val parameterScopes = FunctionParameterContextScopes(finalParent, function, valueParameter, ctx)
  return ContextScope(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    callContext = function.callContext(ctx)
      // suspend functions cannot be called from a default value context
      .takeIf { it != CallContext.SUSPEND } ?: CallContext.DEFAULT,
    ownerDescriptor = function,
    typeParameters = function.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun FunctionContextScope(
  function: FunctionDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(function, parent.name)
) {
  val finalParent = FunctionImportsContextScope(function, parent, ctx)
  val parameterScopes = FunctionParameterContextScopes(finalParent, function, null, ctx)
  val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is ConstructorDescriptor)
    function.constructedClass.declaredTypeParameters
  else function.typeParameters)
    .map { it.toClassifierRef(ctx) }
  ContextScope(
    name = "$baseName ${function.fqNameSafe}",
    parent = parameterScopes,
    callContext = function.callContext(ctx),
    ownerDescriptor = function,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )
}

private fun FunctionParameterContextScopes(
  parent: ContextScope,
  function: FunctionDescriptor,
  until: ValueParameterDescriptor? = null,
  ctx: Context
): ContextScope {
  val maxIndex = until?.injektIndex(ctx)

  return function.allParametersWithContext
    .transform {
      if (it !== function.dispatchReceiverParameter &&
        (maxIndex == null || it.injektIndex(ctx) < maxIndex) &&
        (it === function.dispatchReceiverParameter ||
            it === function.extensionReceiverParameter ||
            it in function.contextReceiverParameters || it.isProvide(ctx)))
         add(it.toCallableRef(ctx))
    }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterContextScope(
        parent = acc,
        function = function,
        parameter = nextParameter,
        ctx = ctx
      )
    }
}

private fun FunctionParameterContextScope(
  parent: ContextScope,
  function: FunctionDescriptor,
  parameter: CallableRef,
  ctx: Context
): ContextScope {
  parameter.callable as ParameterDescriptor
  return ContextScope(
    name = "FUNCTION PARAMETER ${parameter.callable.fqNameSafe.parent()}.${parameter.callable.injektName(ctx)}",
    parent = parent,
    ownerDescriptor = function,
    initialProviders = listOf(parameter),
    nesting = if (parent.name.startsWith("FUNCTION PARAMETER")) parent.nesting
    else parent.nesting + 1,
    ctx = ctx
  )
}

private fun PropertyContextScope(
  property: PropertyDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(property, parent.name)
) {
  val finalParent = property
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportContextScopes(null, it, "PROPERTY ${property.fqNameSafe}", parent, ctx = ctx) }
    ?: parent

  ContextScope(
    name = "PROPERTY ${property.fqNameSafe}",
    callContext = property.callContext(ctx),
    parent = finalParent,
    ownerDescriptor = property,
    initialProviders = buildList {
      addIfNotNull(property.extensionReceiverParameter?.toCallableRef(ctx))
      property.contextReceiverParameters.forEach { add(it.toCallableRef(ctx)) }
    },
    typeParameters = property.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun PropertyInitContextScope(
  property: PropertyDescriptor,
  parent: ContextScope,
  position: KtElement,
  ctx: Context
): ContextScope {
  val containingDeclarationScope = if (property.containingDeclaration is ClassDescriptor) {
    ClassInitContextScope(
      clazz = property.containingDeclaration.cast(),
      parent = parent,
      position = position,
      ctx = ctx
    )
  } else {
    FileInitContextScope(position = position, ctx = ctx)
  }

  val finalParent = property
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let {
      ImportContextScopes(
        file = null,
        imports = it,
        namePrefix = "PROPERTY ${property.fqNameSafe}",
        parent = containingDeclarationScope,
        ctx = ctx
      )
    }
    ?: containingDeclarationScope

  return ContextScope(
    name = "PROPERTY INIT ${property.fqNameSafe}",
    parent = finalParent,
    ownerDescriptor = property,
    typeParameters = property.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun LocalVariableContextScope(
  variable: LocalVariableDescriptor,
  parent: ContextScope,
  ctx: Context
): ContextScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(variable, parent.name)
) {
  val finalParent = variable
    .findPsi()
    .safeAs<KtProperty>()
    ?.getProviderImports()
    ?.takeIf { it.isNotEmpty() }
    ?.let { ImportContextScopes(null, it, "LOCAL VARIABLE ${variable.fqNameSafe}", parent, ctx = ctx) }
    ?: parent

  ContextScope(
    name = "LOCAL VARIABLE ${variable.fqNameSafe}",
    callContext = parent.callContext,
    parent = finalParent,
    ownerDescriptor = variable,
    nesting = finalParent.nesting,
    ctx = ctx
  )
}

private fun ExpressionContextScope(
  expression: KtAnnotatedExpression,
  parent: ContextScope,
  ctx: Context
): ContextScope = ctx.trace!!.getOrPut(InjektWritableSlices.ELEMENT_SCOPE, expression) {
  val finalParent = expression
    .getProviderImports()
    .takeIf { it.isNotEmpty() }
    ?.let { ImportContextScopes(null, it, "EXPRESSION ${expression.startOffset}", parent, ctx = ctx) }
    ?: parent

  ContextScope(
    name = "EXPRESSION ${expression.startOffset}",
    callContext = finalParent.callContext,
    parent = finalParent,
    nesting = finalParent.nesting,
    ctx = ctx
  )
}

private fun BlockExpressionContextScope(
  block: KtBlockExpression,
  position: KtElement,
  parent: ContextScope,
  ctx: Context
): ContextScope {
  val visibleProviderDeclarations = block.statements
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }
  if (visibleProviderDeclarations.isEmpty()) return parent
  val providerDeclaration = visibleProviderDeclarations.last()
  val key = block to providerDeclaration
  return ctx.trace!!.getOrPut(InjektWritableSlices.BLOCK_SCOPE, key) {
    val finalParent = if (visibleProviderDeclarations.size > 1)
      BlockExpressionContextScope(block, providerDeclaration.findPsi().cast(), parent, ctx)
    else parent

    ContextScope(
      name = "BLOCK AT ${providerDeclaration.name}",
      callContext = finalParent.callContext,
      parent = finalParent,
      initialProviders = when (providerDeclaration) {
        is ClassDescriptor -> providerDeclaration.constructorProviders(ctx)
        is CallableDescriptor -> listOf(providerDeclaration.toCallableRef(ctx))
        else -> throw AssertionError("Unexpected provider $providerDeclaration")
      },
      nesting = if (visibleProviderDeclarations.size > 1) finalParent.nesting
      else finalParent.nesting + 1,
      ctx = ctx
    )
  }
}

fun TypeContextScope(
  type: TypeRef,
  parent: ContextScope,
  ctx: Context
): ContextScope {
  val finalParent = parent.scopeToUse
  return finalParent.typeScopes.getOrPut(type.key) {
    val providersWithLookups = type.collectTypeScopeProviders(ctx)

    val newProviders = providersWithLookups.providers
      .filterNotExistingIn(finalParent, ctx)

    val imports = providersWithLookups.lookedUpPackages
      .map { ResolvedProviderImport(null, "$it.*", it) }

    if (newProviders.isEmpty()) {
      return@getOrPut ContextScope(
        name = "EMPTY TYPE ${type.renderToString()}",
        parent = finalParent,
        imports = imports,
        isEmpty = true,
        isDeclarationContainer = false,
        ctx = ctx
      )
    }

    val externalProviders = mutableListOf<CallableRef>()
    val typeProviders = mutableListOf<CallableRef>()
    val internalProviders = mutableListOf<CallableRef>()

    val thisModuleName = ctx.module.moduleName(ctx)
    val typeModuleName = type.classifier.descriptor!!.moduleName(ctx)
    for (callable in newProviders) {
      when (callable.callable.moduleName(ctx)) {
        thisModuleName -> internalProviders += callable
        typeModuleName -> typeProviders += callable
        else -> externalProviders += callable
      }
    }

    var result = finalParent

    if (externalProviders.isNotEmpty()) {
      result = ContextScope(
        name = "EXTERNAL TYPE ${type.renderToString()}",
        parent = result,
        initialProviders = externalProviders,
        callContext = result.callContext,
        typeScopeType = type,
        isDeclarationContainer = false,
        imports = imports,
        ctx = ctx
      )
    }
    if (typeProviders.isNotEmpty()) {
      result = ContextScope(
        name = "TYPE TYPE ${type.renderToString()}",
        parent = result,
        initialProviders = typeProviders,
        callContext = result.callContext,
        typeScopeType = type,
        isDeclarationContainer = false,
        imports = imports,
        ctx = ctx
      )
    }
    if (internalProviders.isNotEmpty()) {
      result = ContextScope(
        name = "INTERNAL TYPE ${type.renderToString()}",
        parent = result,
        initialProviders = internalProviders,
        callContext = result.callContext,
        typeScopeType = type,
        isDeclarationContainer = false,
        imports = imports,
        ctx = ctx
      )
    }

    result
  }
}

private fun ImportContextScopes(
  file: KtFile?,
  imports: List<ProviderImport>,
  namePrefix: String,
  parent: ContextScope?,
  providersPredicate: (CallableRef) -> Boolean = { true },
  ctx: Context
): ContextScope {
  val externalStarProviders = mutableListOf<CallableRef>()
  val externalByNameProviders = mutableListOf<CallableRef>()
  val internalStarProviders = mutableListOf<CallableRef>()
  val internalByNameProviders = mutableListOf<CallableRef>()

  imports.collectImportedProviders(ctx) { callable ->
    if (callable.callable.isExternalDeclaration(ctx)) {
      if (callable.import!!.importPath!!.endsWith("*")) {
        externalStarProviders += callable
      } else {
        externalByNameProviders += callable
      }
    } else {
      if (callable.import!!.importPath!!.endsWith("*")) {
        internalStarProviders += callable
      } else {
        internalByNameProviders += callable
      }
    }
  }

  val resolvedImports = imports.mapNotNull { it.resolve(ctx) }
  if (externalStarProviders.isEmpty() &&
      internalStarProviders.isEmpty() &&
      externalByNameProviders.isEmpty() &&
      internalByNameProviders.isEmpty()) {
    return ContextScope(
      name = "$namePrefix EMPTY IMPORTS",
      parent = parent,
      file = file,
      isDeclarationContainer = false,
      ctx = ctx
    )
  }

  var current = parent
  if (externalStarProviders.isNotEmpty()) {
    current = ContextScope(
      name = "$namePrefix EXTERNAL STAR IMPORTS",
      parent = current,
      file = file,
      imports = resolvedImports,
      initialProviders = externalStarProviders,
      isDeclarationContainer = false,
      providersPredicate = providersPredicate,
      ctx = ctx
    )
  }
  if (internalStarProviders.isNotEmpty()) {
    current = ContextScope(
      name = "$namePrefix INTERNAL STAR IMPORTS",
      parent = current,
      file = file,
      imports = resolvedImports,
      initialProviders = internalStarProviders,
      isDeclarationContainer = false,
      providersPredicate = providersPredicate,
      ctx = ctx
    )
  }
  if (externalByNameProviders.isNotEmpty()) {
    current = ContextScope(
      name = "$namePrefix EXTERNAL BY NAME IMPORTS",
      parent = current,
      initialProviders = externalByNameProviders,
      file = file,
      imports = resolvedImports,
      isDeclarationContainer = false,
      providersPredicate = providersPredicate,
      ctx = ctx
    )
  }
  if (internalByNameProviders.isNotEmpty()) {
    current = ContextScope(
      name = "$namePrefix INTERNAL BY NAME IMPORTS",
      parent = current,
      file = file,
      imports = resolvedImports,
      initialProviders = internalByNameProviders,
      isDeclarationContainer = false,
      providersPredicate = providersPredicate,
      ctx = ctx
    )
  }

  return current!!
}

data class DescriptorWithParentScope(
  val declaration: DeclarationDescriptor,
  val parentName: String?
)
