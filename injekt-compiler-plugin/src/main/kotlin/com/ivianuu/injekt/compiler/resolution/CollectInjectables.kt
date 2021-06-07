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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.cfg.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.lazy.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  @Inject context: AnalysisContext
): List<CallableRef> {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    return listOf(
      classifier.descriptor!!
        .defaultType
        .memberScope
        .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
        .first()
        .toCallableRef()
        .let { callable ->
          callable.copy(
            type = arguments.last(),
            isProvide = true,
            parameterTypes = callable.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this }
          ).substitute(classifier.typeParameters.toMap(arguments))
        }
    )
  }

  return classifier.descriptor!!
    .defaultType
    .memberScope
    .collectInjectables(classBodyView)
    .map {
      val substitutionMap = if (it.callable.safeAs<CallableMemberDescriptor>()?.kind ==
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        val originalClassifier = it.callable.cast<CallableMemberDescriptor>()
          .overriddenTreeAsSequence(false)
          .last()
          .containingDeclaration
          .cast<ClassDescriptor>()
          .toClassifierRef()
        classifier.typeParameters.toMap(arguments) + originalClassifier.typeParameters
          .toMap(subtypeView(originalClassifier)!!.arguments)
      } else classifier.typeParameters.toMap(arguments)
      it.substitute(substitutionMap)
    }
    .map { callable ->
      callable.copy(
        owner = classifier,
        isProvide = true,
        parameterTypes = if (callable.callable.dispatchReceiverParameter != null &&
          callable.parameterTypes.isNotEmpty()) {
          callable.parameterTypes.toMutableMap()
            .also { it[DISPATCH_RECEIVER_INDEX] = this }
        } else callable.parameterTypes
      )
    }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  @Inject context: AnalysisContext,
  onEach: (DeclarationDescriptor) -> Unit = {}
): List<CallableRef> = getContributedDescriptors()
  .flatMap { declaration ->
    checkCancelled()
    onEach(declaration)
    when (declaration) {
      is ClassDescriptor -> declaration
        .injectableConstructors() + listOfNotNull(
        declaration.companionObjectDescriptor
          ?.injectableReceiver(false)
      )
      is CallableMemberDescriptor -> {
        if (declaration.isProvide() &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(InjektFqNames.Provide) ||
              declaration.primaryConstructorPropertyValueParameter()
                ?.hasAnnotation(InjektFqNames.Provide) == true)) {
          listOf(
            declaration.toCallableRef()
              .let { callable ->
                callable.copy(
                  isProvide = true,
                  parameterTypes = callable.parameterTypes.toMutableMap()
                )
              }
          )
        } else emptyList()
      }
      is VariableDescriptor -> if (declaration.isProvide()) {
        listOf(declaration.toCallableRef().makeProvide())
      } else emptyList()
      else -> emptyList()
    }
  }

fun Annotated.isProvide(@Inject context: AnalysisContext): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return context.injektContext.isProvide.getOrPut(key) {
    var isProvide = hasAnnotation(InjektFqNames.Provide) ||
        hasAnnotation(InjektFqNames.Inject)
    if (!isProvide && this is PropertyDescriptor) {
      isProvide = primaryConstructorPropertyValueParameter()?.isProvide() == true
    }
    if (!isProvide && this is ParameterDescriptor) {
      isProvide = type.isProvide() ||
          containingDeclaration.safeAs<FunctionDescriptor>()
            ?.let { containingFunction ->
              containingFunction.isProvide() ||
                  containingFunction.isDeserializedDeclaration() &&
                  injektIndex() in containingFunction.callableInfo().injectParameters
            } == true
    }
    if (!isProvide && this is ClassConstructorDescriptor && isPrimary) {
      isProvide = constructedClass.isProvide()
    }
    isProvide
  }
}

fun Annotated.isInject(@Inject context: AnalysisContext): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return context.injektContext.isInject.getOrPut(key) {
    var isInject = hasAnnotation(InjektFqNames.Inject)
    if (!isInject && this is PropertyDescriptor) {
      isInject = primaryConstructorPropertyValueParameter()?.isInject() == true
    }
    if (!isInject && this is ParameterDescriptor) {
      isInject = type.isProvide() ||
          containingDeclaration.safeAs<FunctionDescriptor>()
            ?.let { containingFunction ->
              containingFunction.isProvide() ||
                  containingFunction.isDeserializedDeclaration() &&
                  injektIndex() in containingFunction.callableInfo().injectParameters
            } == true
    }
    if (!isInject && this is ClassConstructorDescriptor && isPrimary) {
      isInject = constructedClass.isProvide()
    }
    isInject
  }
}

fun ClassDescriptor.injectableConstructors(
  @Inject context: AnalysisContext
): List<CallableRef> = context.injektContext.injectableConstructors.getOrPut(this) {
  constructors
    .filter { constructor ->
      constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide))
    }
    .map { constructor ->
      val callable = constructor.toCallableRef()
      val qualifiedType = callable.type.classifier.qualifiers.wrap(callable.type)
      callable.copy(
        isProvide = true,
        type = qualifiedType,
        originalType = qualifiedType
      )
    }
}

fun ClassDescriptor.injectableReceiver(
  qualified: Boolean,
  @Inject context: AnalysisContext
): CallableRef {
  val callable = thisAsReceiverParameter.toCallableRef()
  val finalType = if (qualified) callable.type.classifier.qualifiers.wrap(callable.type)
  else callable.type
  return callable.copy(isProvide = true, type = finalType, originalType = finalType)
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  @Inject context: AnalysisContext,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableRef) -> Unit,
  addSpreadingInjectable: (CallableRef) -> Unit,
  seen: MutableSet<CallableRef> = mutableSetOf()
) {
  checkCancelled()
  if (this in seen) return
  seen += this
  if (!scope.canSee(this)) return

  if (source == null && typeParameters.any { it.isSpread }) {
    addSpreadingInjectable(this)
    return
  }

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.copy(frameworkKey = generateFrameworkKey()))
  } else this
  addInjectable(nextCallable)

  if (doNotIncludeChildren) return

  nextCallable
    .type
    .also { addImport(it.classifier.fqName, it.classifier.descriptor!!.findPackage().fqName) }
    .collectInjectables(
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.classifier.descriptor
      }
    )
    .forEach { innerCallable ->
      innerCallable.collectInjectables(
        scope = scope,
        addImport = addImport,
        addInjectable = addInjectable,
        addSpreadingInjectable = addSpreadingInjectable,
        seen = seen
      )
    }
}

fun List<ProviderImport>.collectImportedInjectables(
  @Inject context: AnalysisContext
): List<CallableRef> = flatMap { import ->
  buildList<CallableRef> {
    if (!import.isValidImport()) return@buildList
    checkCancelled()
    fun importObjectIfExists(
      fqName: FqName,
      doNotIncludeChildren: Boolean
    ) = context.injektContext.classifierDescriptorForFqName(fqName, import.element.lookupLocation)
      ?.safeAs<ClassDescriptor>()
      ?.takeIf { it.kind == ClassKind.OBJECT }
      ?.let { clazz ->
        this += clazz.injectableReceiver(false)
          .copy(
            doNotIncludeChildren = doNotIncludeChildren,
            import = import.toResolvedImport(clazz.findPackage().fqName)
          )
      }

    if (import.importPath!!.endsWith("*")) {
      val packageFqName = FqName(import.importPath.removeSuffix(".*"))

      // import all injectables in the package
      context.injektContext.memberScopeForFqName(packageFqName, import.element.lookupLocation)
        ?.collectInjectables(false)
        ?.map { it.copy(import = import.toResolvedImport(packageFqName)) }
        ?.let { this += it }

      // additionally add the object if the package is a object
      importObjectIfExists(packageFqName, true)
    } else {
      val fqName = FqName(import.importPath)
      val parentFqName = fqName.parent()
      val name = fqName.shortName()

      // import all injectables with the specified name
      context.injektContext.memberScopeForFqName(parentFqName, import.element.lookupLocation)
        ?.collectInjectables(false)
        ?.filter {
          it.callable.name == name ||
              it.callable.safeAs<ClassConstructorDescriptor>()
                ?.constructedClass
                ?.name == name ||
              it.callable.safeAs<ReceiverParameterDescriptor>()
                ?.value
                ?.type
                ?.constructor
                ?.declarationDescriptor
                ?.safeAs<ClassDescriptor>()
                ?.containingDeclaration
                ?.name == name
        }
        ?.map { it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)) }
        ?.let { this += it }

      // additionally add the object if the package is a object
      importObjectIfExists(parentFqName, true)

      // include injectables from the module object of a type alias with the fq name
      context.injektContext.classifierDescriptorForFqName(fqName, import.element.lookupLocation)
        ?.safeAs<TypeAliasDescriptor>()
        ?.let { typeAlias ->
          importObjectIfExists(
            typeAlias.fqNameSafe.parent()
              .child("${typeAlias.fqNameSafe.shortName()}Module".asNameId()),
            false
          )
        }
    }
  }
}

fun TypeRef.collectTypeScopeInjectables(
  @Inject lookupLocation: LookupLocation,
  @Inject context: AnalysisContext
): List<CallableRef> {
  val injectables = mutableListOf<CallableRef>()
  allTypes.forEach { currentType ->
    if (currentType.isStarProjection) return@forEach
    injectables += currentType.collectInjectablesForSingleType()
  }
  return injectables
    .filter { callable ->
      if (callable.callable !is CallableMemberDescriptor) return@filter true
      if (callable.typeParameters.any { it.isSpread }) return@filter false

      val containingObjectClassifier = callable.callable.containingDeclaration
        .safeAs<ClassDescriptor>()
        ?.takeIf { it.kind == ClassKind.OBJECT }
        ?.toClassifierRef()

      containingObjectClassifier == null || injectables.none { other ->
        other.callable is LazyClassReceiverParameterDescriptor &&
            other.buildContext(emptyList(), containingObjectClassifier.defaultType).isOk
      }
    }
}

private fun TypeRef.collectInjectablesForSingleType(
  @Inject lookupLocation: LookupLocation,
  @Inject context: AnalysisContext
): List<CallableRef> = context.injektContext.typeScopeInjectables.getOrPut(this) {
  val injectables = mutableListOf<CallableRef>()
  injectables += collectPackageTypeScopeInjectables()

  when {
    classifier.isTypeAlias -> {
      context.injektContext.classifierDescriptorForFqName(
        classifier.fqName.parent()
          .child("${classifier.fqName.shortName()}Module".asNameId()),
        lookupLocation
      )
        ?.safeAs<ClassDescriptor>()
        ?.takeIf { it.kind == ClassKind.OBJECT }
        ?.let { injectables += it.injectableReceiver(false) }
    }
    else -> {
      classifier.descriptor!!
        .safeAs<ClassDescriptor>()
        ?.let { clazz ->
          if (clazz.kind == ClassKind.OBJECT) {
            injectables += clazz.injectableReceiver(false)
          } else {
            injectables += clazz.injectableConstructors()
            clazz.companionObjectDescriptor
              ?.let { injectables += it.injectableReceiver(false) }
          }
          clazz.classifierInfo().qualifiers.forEach {
            injectables += it.collectTypeScopeInjectables()
          }
        }
    }
  }

  injectables
}

private fun TypeRef.collectPackageTypeScopeInjectables(
  @Inject context: AnalysisContext
): List<CallableRef> {
  if (classifier.fqName == InjektFqNames.Any || classifier.isTypeParameter) return emptyList()

  val packageDescriptor = classifier.descriptor!!.findPackage()
  val module = packageDescriptor.module
  val injectables = mutableListOf<CallableRef>()
  fun collectInjectables(scope: MemberScope) {
    injectables += scope.collectInjectables(
      onEach = { declaration ->
        if (declaration is ClassDescriptor &&
            declaration !is LazyJavaClassDescriptor)
          collectInjectables(declaration.unsubstitutedMemberScope)
      },
      classBodyView = false
    )
      .filter { callable ->
        module.shouldSeeInternalsOf(callable.callable.module) &&
            callable.callable.containingDeclaration
              .safeAs<ClassDescriptor>()
              ?.let { it.kind == ClassKind.OBJECT } != false &&
            callable.buildContext(emptyList(), this).isOk
      }
  }
  collectInjectables(packageDescriptor.getMemberScope())
  return injectables
}

private fun InjectablesScope.canSee(callable: CallableRef): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          DescriptorVisibilities.INTERNAL.isVisible(null,
            callable.callable, context.injektContext.module)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapQualifiers().classifier.isObject) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any {
          it.ownerDescriptor == callableParent ||
              (it.ownerDescriptor is ClassDescriptor &&
                  it.ownerDescriptor.toClassifierRef(context) == callable.owner)
        }
      }
