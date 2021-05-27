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
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.cfg.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.lazy.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun TypeRef.collectInjectables(
  context: InjektContext,
  trace: BindingTrace,
  classBodyView: Boolean
): List<CallableRef> {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    return listOf(
      classifier.descriptor!!
        .defaultType
        .memberScope
        .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
        .first()
        .toCallableRef(context, trace)
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

  val callables = mutableListOf<CallableRef>()
  val seen = mutableSetOf<TypeRef>()
  fun collectInner(type: TypeRef, overriddenDepth: Int) {
    checkCancelled()
    if (type in seen) return
    seen += type
    val substitutionMap = type.classifier.typeParameters.toMap(type.arguments)
    callables += type.classifier.descriptor!!
      .defaultType
      .memberScope
      .collectInjectables(context, trace, classBodyView)
      .filter {
        (it.callable as CallableMemberDescriptor)
          .kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
      }
      .map { it.substitute(substitutionMap) }
      .map { callable ->
        callable.copy(
          overriddenDepth = overriddenDepth,
          owner = this.classifier,
          isProvide = true,
          parameterTypes = if (callable.callable.dispatchReceiverParameter != null) {
            callable.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this }
          } else callable.parameterTypes
        )
      }
    type.superTypes.forEach { collectInner(it, overriddenDepth + 1) }
  }
  collectInner(this, 0)
  return callables
}

fun ResolutionScope.collectInjectables(
  context: InjektContext,
  trace: BindingTrace,
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {}
): List<CallableRef> = getContributedDescriptors()
  .flatMap { declaration ->
    checkCancelled()
    onEach(declaration)
    when (declaration) {
      is ClassDescriptor -> declaration
        .provideConstructors(context, trace) + listOfNotNull(
        declaration.companionObjectDescriptor
          ?.injectableReceiver(context, trace, false)
      )
      is CallableMemberDescriptor -> {
        if (declaration.isProvide(context, trace) &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(InjektFqNames.Provide) ||
              declaration.primaryConstructorPropertyValueParameter(context, trace)
                ?.hasAnnotation(InjektFqNames.Provide) == true)) {
          listOf(
            declaration.toCallableRef(context, trace)
              .let { callable ->
                callable.copy(
                  isProvide = true,
                  parameterTypes = callable.parameterTypes.toMutableMap()
                )
              }
          )
        } else emptyList()
      }
      is VariableDescriptor -> if (declaration.isProvide(context, trace)) {
        listOf(declaration.toCallableRef(context, trace).makeProvide())
      } else emptyList()
      else -> emptyList()
    }
  }

fun Annotated.isProvide(context: InjektContext, trace: BindingTrace): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  trace.get(InjektWritableSlices.IS_PROVIDE, key)?.let { return it }
  var isProvided = hasAnnotation(InjektFqNames.Provide) ||
      hasAnnotation(InjektFqNames.Inject)
  if (!isProvided && this is PropertyDescriptor) {
    isProvided = primaryConstructorPropertyValueParameter(context, trace)
      ?.isProvide(context, trace) == true
  }
  if (!isProvided && this is ParameterDescriptor) {
    isProvided = type.isProvide(context, trace) ||
        containingDeclaration.safeAs<FunctionDescriptor>()
          ?.let { containingFunction ->
            containingFunction.isProvide(context, trace) ||
                containingFunction.isDeserializedDeclaration() &&
                injektIndex() in containingFunction.callableInfo(context, trace).injectParameters
          } == true
  }
  if (!isProvided && this is ClassConstructorDescriptor && isPrimary) {
    isProvided = constructedClass.isProvide(context, trace)
  }
  trace.record(InjektWritableSlices.IS_PROVIDE, key, isProvided)
  return isProvided
}

fun Annotated.isInject(context: InjektContext, trace: BindingTrace): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  trace.get(InjektWritableSlices.IS_INJECT, key)?.let { return it }
  var isInject = hasAnnotation(InjektFqNames.Inject)
  if (!isInject && this is PropertyDescriptor) {
    isInject = primaryConstructorPropertyValueParameter(context, trace)
      ?.isInject(context, trace) == true
  }
  if (!isInject && this is ParameterDescriptor) {
    isInject = type.isProvide(context, trace) ||
        containingDeclaration.safeAs<FunctionDescriptor>()
          ?.let { containingFunction ->
            containingFunction.isProvide(context, trace) ||
                containingFunction.isDeserializedDeclaration() &&
                injektIndex() in containingFunction.callableInfo(context, trace).injectParameters
          } == true
  }
  if (!isInject && this is ClassConstructorDescriptor && isPrimary) {
    isInject = constructedClass.isProvide(context, trace)
  }
  trace.record(InjektWritableSlices.IS_INJECT, key, isInject)
  return isInject
}

fun ClassDescriptor.provideConstructors(
  context: InjektContext,
  trace: BindingTrace
): List<CallableRef> {
  trace.get(InjektWritableSlices.INJECTABLE_CONSTRUCTORS, this)?.let { return it }
  val injectableConstructors = constructors
    .filter { constructor ->
      constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide))
    }
    .map { constructor ->
      val callable = constructor.toCallableRef(context, trace)
      val qualifiedType = callable.type.classifier.qualifiers.wrap(callable.type)
      callable.copy(
        isProvide = true,
        type = qualifiedType,
        originalType = qualifiedType
      )
    }
  trace.record(InjektWritableSlices.INJECTABLE_CONSTRUCTORS, this, injectableConstructors)
  return injectableConstructors
}

fun ClassDescriptor.injectableReceiver(
  context: InjektContext,
  trace: BindingTrace,
  qualified: Boolean
): CallableRef {
  val callable = thisAsReceiverParameter.toCallableRef(context, trace)
  val finalType = if (qualified) callable.type.classifier.qualifiers.wrap(callable.type)
  else callable.type
  return callable.copy(isProvide = true, type = finalType, originalType = finalType)
}

fun CallableRef.collectInjectables(
  context: InjektContext,
  scope: InjectablesScope,
  trace: BindingTrace,
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

  val nextCallable = if (type.isProvide) {
    addInjectable(this)
    copy(type = type.copy(frameworkKey = generateFrameworkKey()))
  } else this
  addInjectable(nextCallable)

  if (doNotIncludeChildren) return

  nextCallable
    .type
    .also { addImport(it.classifier.fqName, it.classifier.descriptor!!.findPackage().fqName) }
    .collectInjectables(
      context = context,
      trace = trace,
      classBodyView = scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.classifier.descriptor
      }
    )
    .forEach { innerCallable ->
      innerCallable.collectInjectables(
        context,
        scope,
        trace,
        addImport,
        addInjectable,
        addSpreadingInjectable,
        seen
      )
    }
}

fun List<ProviderImport>.collectImportedInjectables(
  context: InjektContext,
  trace: BindingTrace
): List<CallableRef> = flatMap { import ->
  buildList<CallableRef> {
    if (!import.isValidImport()) return@buildList
    checkCancelled()
    fun importObjectIfExists(
      fqName: FqName,
      doNotIncludeChildren: Boolean
    ) = context.classifierDescriptorForFqName(fqName, import.element.lookupLocation)
      ?.safeAs<ClassDescriptor>()
      ?.takeIf { it.kind == ClassKind.OBJECT }
      ?.let { clazz ->
        this += clazz.injectableReceiver(context, trace, false)
          .copy(
            doNotIncludeChildren = doNotIncludeChildren,
            import = import.toResolvedImport(clazz.findPackage().fqName)
          )
      }

    if (import.importPath!!.endsWith("*")) {
      val packageFqName = FqName(import.importPath.removeSuffix(".*"))

      // import all injectables in the package
      context.memberScopeForFqName(packageFqName, import.element.lookupLocation)
        ?.collectInjectables(context, trace, false)
        ?.map { it.copy(import = import.toResolvedImport(packageFqName)) }
        ?.let { this += it }

      // additionally add the object if the package is a object
      importObjectIfExists(packageFqName, true)
    } else {
      val fqName = FqName(import.importPath)
      val parentFqName = fqName.parent()
      val name = fqName.shortName()

      // import all injectables with the specified name
      context.memberScopeForFqName(parentFqName, import.element.lookupLocation)
        ?.collectInjectables(context, trace, false)
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
      context.classifierDescriptorForFqName(fqName, import.element.lookupLocation)
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
  context: InjektContext,
  trace: BindingTrace,
  lookupLocation: LookupLocation
): List<CallableRef> {
  val injectables = mutableListOf<CallableRef>()
  allTypes.forEach { currentType ->
    if (currentType.isStarProjection) return@forEach
    injectables += currentType.collectInjectablesForSingleType(context, trace, lookupLocation)
  }
  return injectables.filter {
    it.typeParameters.none { typeParameter ->
      typeParameter.isSpread
    }
  }
}

private fun TypeRef.collectInjectablesForSingleType(
  context: InjektContext,
  trace: BindingTrace,
  lookupLocation: LookupLocation
): List<CallableRef> {
  trace[InjektWritableSlices.TYPE_SCOPE_INJECTABLES, this]?.let { return it }
  val injectables = mutableListOf<CallableRef>()
  injectables += collectPackageTypeScopeInjectables(context, trace)

  when {
    classifier.isTypeAlias -> {
      context.classifierDescriptorForFqName(
        classifier.fqName.parent()
          .child("${classifier.fqName.shortName()}Module".asNameId()),
        lookupLocation
      )
        ?.safeAs<ClassDescriptor>()
        ?.takeIf { it.kind == ClassKind.OBJECT }
        ?.let { injectables += it.injectableReceiver(context, trace, false) }
    }
    else -> {
      classifier.descriptor!!
        .safeAs<ClassDescriptor>()
        ?.let { clazz ->
          if (clazz.kind == ClassKind.OBJECT) {
            injectables += clazz.injectableReceiver(context, trace, false)
          } else {
            injectables += clazz.provideConstructors(context, trace)
            clazz.companionObjectDescriptor
              ?.let { injectables += it.injectableReceiver(context, trace, false) }
          }
          clazz.classifierInfo(context, trace).qualifiers.forEach {
              injectables += it.collectTypeScopeInjectables(context, trace, lookupLocation)
            }
        }
    }
  }

  trace.record(InjektWritableSlices.TYPE_SCOPE_INJECTABLES, this, injectables)

  return injectables
}

private fun TypeRef.collectPackageTypeScopeInjectables(
  context: InjektContext,
  trace: BindingTrace
): List<CallableRef> {
  if (classifier.fqName == InjektFqNames.Any || classifier.isTypeParameter) return emptyList()

  val packageDescriptor = classifier.descriptor!!.findPackage()
  val module = packageDescriptor.module
  val injectables = mutableListOf<CallableRef>()
  fun collectInjectables(scope: MemberScope) {
    injectables += scope.collectInjectables(
      context = context,
      trace = trace,
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
            callable.type.buildContext(context, emptyList(), this).isOk
      }
  }
  collectInjectables(packageDescriptor.getMemberScope())
  return injectables
}

private fun InjectablesScope.canSee(callable: CallableRef): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          DescriptorVisibilities.INTERNAL.isVisible(null, callable.callable, context.module)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapQualifiers().classifier.isObject) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any {
          it.ownerDescriptor == callableParent ||
              (it.ownerDescriptor is ClassDescriptor &&
                  it.ownerDescriptor.toClassifierRef(
                    context,
                    trace
                  ) == callable.owner)
        }
      }
