/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
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
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

context(Context) fun ElementInjectablesScope(
  element: KtElement,
  position: KtElement = element
): InjectablesScope {
  val scopeOwner = element.parentsWithSelf
    .first { (it as KtElement).isScopeOwner(position) }
    .cast<KtElement>()

  fun createScope(): InjectablesScope {
    val parentScope = scopeOwner.parents
      .firstOrNull { (it as KtElement).isScopeOwner(position) }
      ?.let { ElementInjectablesScope(it.cast(), position) }

    return when (scopeOwner) {
      is KtFile -> FileInjectablesScope(scopeOwner)
      is KtClassOrObject -> ClassInjectablesScope(
        scopeOwner.descriptor()!!,
        parentScope!!
      )
      is KtConstructor<*> -> {
        if (scopeOwner.bodyExpression.let { it == null || it !in position.parents }) {
          ConstructorPreInitInjectablesScope(
            scopeOwner.descriptor()!!,
            parentScope!!
          )
        } else FunctionInjectablesScope(scopeOwner.descriptor()!!, parentScope!!)
      }
      is KtFunction -> FunctionInjectablesScope(scopeOwner.descriptor()!!, parentScope!!)
      is KtParameter -> ValueParameterDefaultValueInjectablesScope(
        scopeOwner.descriptor()!!,
        parentScope!!
      )
      is KtProperty -> {
        when (val descriptor = scopeOwner.descriptor<VariableDescriptor>()!!) {
          is PropertyDescriptor -> {
            if (scopeOwner.delegateExpressionOrInitializer != null &&
              scopeOwner.delegateExpressionOrInitializer!! in element.parentsWithSelf)
              PropertyInitInjectablesScope(descriptor, parentScope!!, position)
            else
              PropertyInjectablesScope(descriptor, parentScope!!)
          }
          is LocalVariableDescriptor -> LocalVariableInjectablesScope(descriptor, parentScope!!)
          else -> throw AssertionError("Unexpected variable descriptor $descriptor")
        }
      }
      is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.let { ConstructorPreInitInjectablesScope(it, parentScope!!) }
        ?: parentScope!!
      is KtClassInitializer -> ClassInitInjectablesScope(
        clazz = scopeOwner.getParentOfType<KtClassOrObject>(false)!!.descriptor()!!,
        parent = parentScope!!,
        position = position
      )
      is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.let { FunctionInjectablesScope(it, parentScope!!) }
        ?: parentScope!!
      is KtBlockExpression -> BlockExpressionInjectablesScope(scopeOwner, position, parentScope!!)
      else -> throw AssertionError("Unexpected scope owner $scopeOwner")
    }
  }

  return if (scopeOwner !is KtBlockExpression)
    cached("element_scope", scopeOwner) { createScope() }
  else
    createScope()
}

private fun KtElement.isScopeOwner(position: KtElement): Boolean {
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

context(Context) private fun FileInjectablesScope(file: KtFile): InjectablesScope =
  cached("file_scope", file) {
    InjectableScopeOrParent(
      file = file,
      name = "FILE ${file.name}",
      parent = GlobalInjectablesScope(),
      initialInjectables = collectPackageInjectables(file.packageFqName)
    )
  }

context(Context) private fun FileInitInjectablesScope(position: KtElement): InjectablesScope {
  val file = position.containingKtFile

  val visibleInjectableDeclarations = file
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
          declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>()
          ?.takeIf { it.isProvide() }
          ?.let { add(it) }
      }
    }

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  return InjectableScopeOrParent(
    file = file,
    name = "FILE INIT ${file.name} at ${injectableDeclaration?.name}",
    injectablesPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@InjectableScopeOrParent true
      psiProperty.containingFile != file ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    parent = GlobalInjectablesScope(),
    initialInjectables = collectPackageInjectables(file.packageFqName)
  )
}

context(Context) private fun ClassCompanionInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope
): InjectablesScope = clazz.companionObjectDescriptor
  ?.let { ClassInjectablesScope(it, parent) } ?: parent

context(Context) private fun ClassInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope
): InjectablesScope = cached(
  "class_scope",
  DescriptorWithParentScope(clazz, parent.name)
) {
  val finalParent = ClassCompanionInjectablesScope(clazz, parent)
  val name = if (clazz.isCompanionObject)
    "COMPANION ${clazz.containingDeclaration.fqNameSafe}"
  else "CLASS ${clazz.fqNameSafe}"
  InjectableScopeOrParent(
    name = name,
    parent = finalParent,
    ownerDescriptor = clazz,
    initialInjectables = listOf(clazz.injectableReceiver(false)),
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef() }
  )
}

context(Context) private fun ClassInitInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  position: KtElement
): InjectablesScope {
  val psiClass = clazz.findPsi()!!
  val visibleInjectableDeclarations = psiClass
    .cast<KtClassOrObject>()
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>()
          ?.takeIf { it.isProvide() }
          ?.let { add(it) }
      }
    }

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  val name = if (clazz.isCompanionObject)
    "COMPANION INIT ${clazz.containingDeclaration.fqNameSafe} at ${injectableDeclaration?.name}"
  else "CLASS INIT ${clazz.fqNameSafe} at ${injectableDeclaration?.name}"

  val classInitScope = InjectableScopeOrParent(
    name = name,
    parent = parent,
    ownerDescriptor = clazz,
    initialInjectables = listOf(clazz.injectableReceiver(false)),
    injectablesPredicate = {
      val psiProperty = it.callable.findPsi().safeAs<KtProperty>() ?: return@InjectableScopeOrParent true
      psiProperty.getParentOfType<KtClass>(false) != psiClass ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef() }
  )

  val primaryConstructor = clazz.unsubstitutedPrimaryConstructor

  return if (primaryConstructor == null) classInitScope
  else FunctionInjectablesScope(primaryConstructor, classInitScope)
}

context(Context) private fun ConstructorPreInitInjectablesScope(
  constructor: ConstructorDescriptor,
  parent: InjectablesScope
): InjectablesScope {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, constructor, null)
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toClassifierRef()
  }
  return InjectableScopeOrParent(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = constructor,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting
  )
}

context(Context) private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: ValueParameterDescriptor,
  parent: InjectablesScope
): InjectablesScope {
  val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, valueParameter)
  return InjectableScopeOrParent(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = function,
    typeParameters = function.typeParameters.map { it.toClassifierRef() }
  )
}

context(Context) private fun FunctionInjectablesScope(
  function: FunctionDescriptor,
  parent: InjectablesScope
): InjectablesScope = cached(
  "function_scope",
  DescriptorWithParentScope(function, parent.name)
) {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, null)
  val baseName = if (function is ConstructorDescriptor) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is ConstructorDescriptor)
    function.constructedClass.declaredTypeParameters
  else function.typeParameters)
    .map { it.toClassifierRef() }
  InjectableScopeOrParent(
    name = "$baseName ${function.fqNameSafe}",
    parent = parameterScopes,
    ownerDescriptor = function,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting
  )
}

context(Context) private fun FunctionParameterInjectablesScopes(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  until: ValueParameterDescriptor? = null
): InjectablesScope {
  val maxIndex = until?.injektIndex()

  return function.allParametersWithContext
    .transform {
      if (it !== function.dispatchReceiverParameter &&
        (maxIndex == null || it.injektIndex() < maxIndex) &&
        (it === function.extensionReceiverParameter ||
            it in function.contextReceiverParameters ||
            it.isProvide()))
        add(it.toCallableRef())
    }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        parent = acc,
        function = function,
        parameter = nextParameter
      )
    }
}

context(Context) private fun FunctionParameterInjectablesScope(
  parent: InjectablesScope,
  function: FunctionDescriptor,
  parameter: CallableRef
): InjectablesScope {
  parameter.callable as ParameterDescriptor
  return InjectableScopeOrParent(
    name = "FUNCTION PARAMETER ${parameter.callable.fqNameSafe.parent()}.${parameter.callable.injektName()}",
    parent = parent,
    ownerDescriptor = function,
    initialInjectables = listOf(parameter),
    nesting = if (parent.name.startsWith("FUNCTION PARAMETER")) parent.nesting
    else parent.nesting + 1
  )
}

context(Context) private fun PropertyInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope
): InjectablesScope = cached(
  "property_scope",
  DescriptorWithParentScope(property, parent.name)
) {
  InjectableScopeOrParent(
    name = "PROPERTY ${property.fqNameSafe}",
    parent = parent,
    ownerDescriptor = property,
    initialInjectables = buildList {
      addIfNotNull(property.extensionReceiverParameter?.toCallableRef())
      property.contextReceiverParameters.forEach { add(it.toCallableRef()) }
    },
    typeParameters = property.typeParameters.map { it.toClassifierRef() }
  )
}

context(Context) private fun PropertyInitInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  position: KtElement
): InjectablesScope {
  val finalParent = if (property.containingDeclaration is ClassDescriptor) {
    ClassInitInjectablesScope(
      clazz = property.containingDeclaration.cast(),
      parent = parent,
      position = position
    )
  } else {
    FileInitInjectablesScope(position = position)
  }

  return InjectableScopeOrParent(
    name = "PROPERTY INIT ${property.fqNameSafe}",
    parent = finalParent,
    ownerDescriptor = property,
    typeParameters = property.typeParameters.map { it.toClassifierRef() }
  )
}

context(Context) private fun LocalVariableInjectablesScope(
  variable: LocalVariableDescriptor,
  parent: InjectablesScope
): InjectablesScope = cached(
  "local_variable_scope",
  DescriptorWithParentScope(variable, parent.name)
) {
  InjectableScopeOrParent(
    name = "LOCAL VARIABLE ${variable.fqNameSafe}",
    parent = parent,
    ownerDescriptor = variable,
    nesting = parent.nesting
  )
}

context(Context) private fun BlockExpressionInjectablesScope(
  block: KtBlockExpression,
  position: KtElement,
  parent: InjectablesScope
): InjectablesScope {
  val visibleInjectableDeclarations = block.statements
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>()
          ?.takeIf { it.isProvide() }
          ?.let { add(it) }
      }
    }
  if (visibleInjectableDeclarations.isEmpty()) return parent
  val injectableDeclaration = visibleInjectableDeclarations.last()
  val key = block to injectableDeclaration
  return cached("block_scope", key) {
    val finalParent = if (visibleInjectableDeclarations.size > 1)
      BlockExpressionInjectablesScope(block, injectableDeclaration.findPsi().cast(), parent)
    else parent

    InjectableScopeOrParent(
      name = "BLOCK AT ${injectableDeclaration.name}",
      parent = finalParent,
      initialInjectables = when (injectableDeclaration) {
        is ClassDescriptor -> injectableDeclaration.injectableConstructors()
        is CallableDescriptor -> listOf(injectableDeclaration.toCallableRef())
        else -> throw AssertionError("Unexpected injectable $injectableDeclaration")
      },
      nesting = if (visibleInjectableDeclarations.size > 1) finalParent.nesting
      else finalParent.nesting + 1
    )
  }
}

context(Context) fun GlobalInjectablesScope(): InjectablesScope =
  cached("global_scope", Unit) {
    val (externalInjectables, internalInjectables) = collectGlobalInjectables()
      .partition { it.callable.isExternalDeclaration() }

    val externalScope = InjectablesScope(
      name = "EXTERNAL GLOBAL",
      parent = null,
      initialInjectables = externalInjectables
    )

    InjectableScopeOrParent(
      name = "INTERNAL GLOBAL",
      parent = externalScope,
      initialInjectables = internalInjectables
    )
  }

data class DescriptorWithParentScope(
  val declaration: DeclarationDescriptor,
  val parentName: String?,
)

context(Context) fun InjectableScopeOrParent(
  name: String,
  parent: InjectablesScope,
  ownerDescriptor: DeclarationDescriptor? = null,
  file: KtFile? = null,
  initialInjectables: List<CallableRef> = emptyList(),
  injectablesPredicate: (CallableRef) -> Boolean = { true },
  typeParameters: List<ClassifierRef> = emptyList(),
  nesting: Int = parent.nesting.inc()
): InjectablesScope {
  return if (typeParameters.isEmpty() && initialInjectables.isEmpty()) parent
  else InjectablesScope(name, parent, ownerDescriptor, file, initialInjectables, injectablesPredicate, typeParameters, nesting)
}
