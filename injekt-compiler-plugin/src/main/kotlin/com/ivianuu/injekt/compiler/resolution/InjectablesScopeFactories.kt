/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
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
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ElementInjectablesScope(
  ctx: Context,
  element: KtElement,
  position: KtElement = element
): InjectablesScope {
  val scopeOwner = element.parentsWithSelf
    .first { (it as KtElement).isScopeOwner(position, ctx) }
    .cast<KtElement>()

  if (scopeOwner !is KtBlockExpression)
    ctx.trace!!.bindingContext.get(InjektWritableSlices.ELEMENT_SCOPE, scopeOwner)
      ?.let { return it }

  val parentScope = scopeOwner.parents
    .firstOrNull { (it as KtElement).isScopeOwner(position, ctx) }
    ?.let { ElementInjectablesScope(ctx, it.cast(), position) }

  val scope = when (scopeOwner) {
    is KtFile -> FileInjectablesScope(scopeOwner, ctx)
    is KtClassOrObject -> ClassInjectablesScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtConstructor<*> -> {
      if (scopeOwner.bodyExpression.let { it == null || it !in position.parents }) {
        ConstructorPreInitInjectablesScope(
          scopeOwner.descriptor(ctx)!!,
          parentScope!!,
          ctx
        )
      } else FunctionInjectablesScope(
        scopeOwner.descriptor(ctx)!!,
        parentScope!!,
        ctx
      )
    }
    is KtFunction -> FunctionInjectablesScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtParameter -> ValueParameterDefaultValueInjectablesScope(
      scopeOwner.descriptor(ctx)!!,
      parentScope!!,
      ctx
    )
    is KtProperty -> {
      when (val descriptor = scopeOwner.descriptor<VariableDescriptor>(ctx)!!) {
        is PropertyDescriptor -> {
          if (scopeOwner.delegateExpressionOrInitializer != null &&
            scopeOwner.delegateExpressionOrInitializer!! in element.parentsWithSelf)
            PropertyInitInjectablesScope(descriptor, parentScope!!, position, ctx)
          else
            PropertyInjectablesScope(descriptor, parentScope!!, ctx)
        }
        is LocalVariableDescriptor -> LocalVariableInjectablesScope(descriptor, parentScope!!, ctx)
        else -> throw AssertionError("Unexpected variable descriptor $descriptor")
      }
    }
    is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>(ctx)
      ?.unsubstitutedPrimaryConstructor
      ?.let { ConstructorPreInitInjectablesScope(it, parentScope!!, ctx) }
      ?: parentScope!!
    is KtClassInitializer -> ClassInitInjectablesScope(
      clazz = scopeOwner.getParentOfType<KtClassOrObject>(false)!!.descriptor(ctx)!!,
      parent = parentScope!!,
      position = position,
      ctx = ctx
    )
    is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
      ?.descriptor<ClassDescriptor>(ctx)
      ?.unsubstitutedPrimaryConstructor
      ?.let { FunctionInjectablesScope(it, parentScope!!, ctx) }
      ?: parentScope!!
    is KtBlockExpression -> BlockExpressionInjectablesScope(scopeOwner, position, parentScope!!, ctx)
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
      if (parentsBetweenInitializerAndPosition.none { it is KtNamedDeclaration       })
          return false
    }

    val classInitializer = position.getParentOfType<KtClassInitializer>(false)

    if (classInitializer != null) {
      val parentsBetweenInitializerAndPosition = position.parents
        .takeWhile { it != classInitializer }
        .toList()
      if (parentsBetweenInitializerAndPosition.none { it is KtNamedDeclaration })
        return false
    }
  }

  if (this is KtObjectDeclaration &&
    position.getParentOfType<KtSuperTypeList>(false)
      ?.getParentOfType<KtClassOrObject>(false) != this)
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

private fun FileInjectablesScope(file: KtFile, ctx: Context): InjectablesScope =
  ctx.trace!!.getOrPut(InjektWritableSlices.ELEMENT_SCOPE, file) {
    InjectablesScope(
      file = file,
      name = "FILE ${file.name}",
      parent = GlobalInjectablesScope(ctx),
      ctx = ctx,
      initialInjectables = collectPackageInjectables(file.packageFqName, ctx)
    )
  }

private fun FileInitInjectablesScope(position: KtElement, ctx: Context): InjectablesScope {
  val file = position.containingKtFile

  val visibleInjectableDeclarations = file
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
          declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  return InjectablesScope(
    file = file,
    name = "FILE INIT ${file.name} at ${injectableDeclaration?.name}",
    injectablesPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@InjectablesScope true
      psiProperty.containingFile != file ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    parent = GlobalInjectablesScope(ctx),
    ctx = ctx,
    initialInjectables = collectPackageInjectables(file.packageFqName, ctx)
  )
}

private fun ClassCompanionInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope = clazz.companionObjectDescriptor
  ?.let { ClassInjectablesScope(it, parent, ctx) } ?: parent

private fun ClassInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(clazz, parent.name)
) {
  val finalParent = ClassCompanionInjectablesScope(clazz, parent, ctx)
  val name = if (clazz.isCompanionObject)
    "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
  else "CLASS ${clazz.fqNameSafe}"
  InjectablesScope(
    name = name,
    parent = finalParent,
    ownerDescriptor = clazz,
    initialInjectables = listOf(clazz.injectableReceiver(false, ctx)),
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun ClassInitInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  position: KtElement,
  ctx: Context
): InjectablesScope {
  val psiClass = clazz.findPsi()!!
  val visibleInjectableDeclarations = psiClass
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

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  val name = if (clazz.isCompanionObject)
    "COMPANION INIT ${clazz.containingDeclaration.fqNameSafe} at ${injectableDeclaration?.name}"
  else "CLASS INIT ${clazz.fqNameSafe} at ${injectableDeclaration?.name}"

  val classInitScope = InjectablesScope(
    name = name,
    parent = parent,
    ownerDescriptor = clazz,
    initialInjectables = listOf(clazz.injectableReceiver(false, ctx)),
    injectablesPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@InjectablesScope true
      psiProperty.getParentOfType<KtClass>(false) != psiClass ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )

  val primaryConstructor = clazz.unsubstitutedPrimaryConstructor

  return if (primaryConstructor == null) classInitScope
  else FunctionInjectablesScope(primaryConstructor, classInitScope, ctx)
}

private fun ConstructorPreInitInjectablesScope(
  constructor: ConstructorDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, constructor, null, ctx)
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toClassifierRef(ctx)
  }
  return InjectablesScope(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = constructor,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )
}

private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: ValueParameterDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope {
  val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, valueParameter, ctx)
  return InjectablesScope(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = function,
    typeParameters = function.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun FunctionInjectablesScope(
  function: FunctionDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(function, parent.name)
) {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, null, ctx)
  val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is ConstructorDescriptor)
    function.constructedClass.declaredTypeParameters
  else function.typeParameters)
    .map { it.toClassifierRef(ctx) }
  InjectablesScope(
    name = "$baseName ${function.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = function,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )
}

private fun FunctionParameterInjectablesScopes(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  until: ValueParameterDescriptor? = null,
  ctx: Context
): InjectablesScope {
  val maxIndex = until?.injektIndex()

  return function.allParameters
    .transform {
      if (it !== function.dispatchReceiverParameter &&
        (maxIndex == null || it.injektIndex() < maxIndex) &&
        (it === function.dispatchReceiverParameter ||
            it === function.extensionReceiverParameter ||
            it.isProvide(ctx)))
        add(it.toCallableRef(ctx))
    }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        parent = acc,
        function = function,
        parameter = nextParameter,
        ctx = ctx
      )
    }
}

private fun FunctionParameterInjectablesScope(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  parameter: CallableRef,
  ctx: Context
): InjectablesScope {
  parameter.callable as ParameterDescriptor
  return InjectablesScope(
    name = "FUNCTION PARAMETER ${parameter.callable.fqNameSafe.parent()}.${parameter.callable.injektName()}",
    parent = parent,
    ownerDescriptor = function,
    initialInjectables = listOf(parameter),
    nesting = if (parent.name.startsWith("FUNCTION PARAMETER")) parent.nesting
    else parent.nesting + 1,
    ctx = ctx
  )
}

private fun PropertyInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(property, parent.name)
) {
  InjectablesScope(
    name = "PROPERTY ${property.fqNameSafe}",
    parent = parent,
    ownerDescriptor = property,
    initialInjectables = listOfNotNull(property.extensionReceiverParameter?.toCallableRef(ctx)),
    typeParameters = property.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun PropertyInitInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  position: KtElement,
  ctx: Context
): InjectablesScope {
  val finalParent = if (property.containingDeclaration is ClassDescriptor) {
    ClassInitInjectablesScope(
      clazz = property.containingDeclaration.cast(),
      parent = parent,
      position = position,
      ctx = ctx
    )
  } else {
    FileInitInjectablesScope(position = position, ctx = ctx)
  }

  return InjectablesScope(
    name = "PROPERTY INIT ${property.fqNameSafe}",
    parent = finalParent,
    ownerDescriptor = property,
    typeParameters = property.typeParameters.map { it.toClassifierRef(ctx) },
    ctx = ctx
  )
}

private fun LocalVariableInjectablesScope(
  variable: LocalVariableDescriptor,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope = ctx.trace!!.getOrPut(
  InjektWritableSlices.DECLARATION_SCOPE,
  DescriptorWithParentScope(variable, parent.name)
) {
  InjectablesScope(
    name = "LOCAL VARIABLE ${variable.fqNameSafe}",
    parent = parent,
    ownerDescriptor = variable,
    nesting = parent.nesting,
    ctx = ctx
  )
}

private fun BlockExpressionInjectablesScope(
  block: KtBlockExpression,
  position: KtElement,
  parent: InjectablesScope,
  ctx: Context
): InjectablesScope {
  val visibleInjectableDeclarations = block.statements
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }
  if (visibleInjectableDeclarations.isEmpty()) return parent
  val injectableDeclaration = visibleInjectableDeclarations.last()
  val key = block to injectableDeclaration
  return ctx.trace!!.getOrPut(InjektWritableSlices.BLOCK_SCOPE, key) {
    val finalParent = if (visibleInjectableDeclarations.size > 1)
      BlockExpressionInjectablesScope(block, injectableDeclaration.findPsi().cast(), parent, ctx)
    else parent

    InjectablesScope(
      name = "BLOCK AT ${injectableDeclaration.name}",
      parent = finalParent,
      initialInjectables = when (injectableDeclaration) {
        is ClassDescriptor -> injectableDeclaration.injectableConstructors(ctx)
        is CallableDescriptor -> listOf(injectableDeclaration.toCallableRef(ctx))
        else -> throw AssertionError("Unexpected injectable $injectableDeclaration")
      },
      nesting = if (visibleInjectableDeclarations.size > 1) finalParent.nesting
      else finalParent.nesting + 1,
      ctx = ctx
    )
  }
}

fun GlobalInjectablesScope(ctx: Context): InjectablesScope =
  ctx.trace.getOrPut(InjektWritableSlices.GLOBAL_SCOPE, Unit) {
    val (externalInjectables, internalInjectables) = collectGlobalInjectables(ctx)
      .partition { it.callable.isExternalDeclaration(ctx) }

    val externalScope = InjectablesScope(
      name = "EXTERNAL GLOBAL",
      parent = null,
      initialInjectables = externalInjectables,
      ctx = ctx
    )

    if (internalInjectables.isEmpty()) externalScope
    else InjectablesScope(
      name = "INTERNAL GLOBAL",
      parent = externalScope,
      initialInjectables = internalInjectables,
      ctx = ctx
    )
  }

data class DescriptorWithParentScope(
  val declaration: DeclarationDescriptor,
  val parentName: String?,
)
