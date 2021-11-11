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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.analysis.ComponentConstructorDescriptor
import com.ivianuu.injekt.compiler.analysis.EntryPointConstructorDescriptor
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableForUniqueKey
import com.ivianuu.injekt.compiler.classifierDescriptorForFqName
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.fastFlatMap
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.module
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.packageFragmentsForFqName
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.LinkedList

fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  import: ResolvedProviderImport?,
  @Inject ctx: Context,
  consumer: (CallableRef) -> Unit
) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val callable = classifier.descriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toCallableRef()
      .let { callable ->
        callable.copy(
          type = arguments.last(),
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectInjectables
          },
          scopeComponentType = scopeComponentType,
          isEager = isEager,
          import = import
        ).substitute(classifier.typeParameters.zip(arguments).toMap())
      }

    consumer(callable)
  }

  if (!classifier.declaresInjectables && !classBodyView) return

  classifier.descriptor!!
    .defaultType
    .memberScope
    .collectInjectables(classBodyView = classBodyView) { callable ->
      val substitutionMap = if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        val originalClassifier = callable.callable.cast<CallableMemberDescriptor>()
          .overriddenTreeAsSequence(false)
          .last()
          .containingDeclaration
          .cast<ClassDescriptor>()
          .toClassifierRef()
        classifier.typeParameters.zip(arguments).toMap() + originalClassifier.typeParameters
          .zip(subtypeView(originalClassifier)!!.arguments)
      } else classifier.typeParameters.zip(arguments).toMap()
      val substituted = callable.substitute(substitutionMap)

      consumer(
        substituted.copy(
          parameterTypes = if (substituted.parameterTypes[DISPATCH_RECEIVER_INDEX] != this) {
            substituted.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this }
          } else substituted.parameterTypes,
          import = import
        )
      )
    }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {},
  name: Name? = null,
  @Inject ctx: Context,
  consumer: (CallableRef) -> Unit
) {
  for (declaration in getContributedDescriptors()) {
    onEach(declaration)
    if (name != null && declaration.name != name) continue

    when (declaration) {
      is ClassDescriptor -> {
        if (declaration.kind == ClassKind.OBJECT &&
          (!classBodyView || !declaration.isCompanionObject))
          declaration
            .takeIf { it.isProvide() }
            ?.injectableReceiver(!classBodyView)
            ?.let(consumer)
        else {
          declaration.injectableConstructors().forEach(consumer)
          if (!classBodyView)
            declaration.companionObjectDescriptor
              ?.takeIf { it.classifierInfo().declaresInjectables }
              ?.injectableReceiver(false)
              ?.let(consumer)
        }
      }
      is CallableMemberDescriptor -> {
        if (declaration.isProvide() &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(injektFqNames().provide) ||
              declaration.primaryConstructorPropertyValueParameter()
                ?.hasAnnotation(injektFqNames().provide) == true)) {
          consumer(declaration.toCallableRef())
        }
      }
      is VariableDescriptor -> {
        if (declaration.isProvide())
          consumer(declaration.toCallableRef())
      }
    }
  }
}

fun Annotated.isProvide(@Inject ctx: Context): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return trace()!!.getOrPut(InjektWritableSlices.IS_PROVIDE, key) {
    var isProvide = hasAnnotation(injektFqNames().provide) ||
        hasAnnotation(injektFqNames().inject)

    if (!isProvide && this is PropertyDescriptor)
      isProvide = primaryConstructorPropertyValueParameter()?.isProvide() == true

    if (!isProvide && this is ParameterDescriptor)
      isProvide = type.isProvide() ||
          containingDeclaration.safeAs<FunctionDescriptor>()
            ?.let { containingFunction ->
              containingFunction.isProvide() ||
                  containingFunction.valueParameters.getOrNull(injektIndex() - 1)
                    ?.isInject() == true
            } == true

    if (!isProvide && this is ClassConstructorDescriptor && isPrimary)
      isProvide = constructedClass.isProvide()

    isProvide
  }
}

fun Annotated.isInject(@Inject ctx: Context): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return trace()!!.getOrPut(InjektWritableSlices.IS_INJECT, key) {
    var isInject = hasAnnotation(injektFqNames().inject)

    if (!isInject && this is PropertyDescriptor)
      isInject = primaryConstructorPropertyValueParameter()?.isInject() == true

    if (!isInject && this is ParameterDescriptor)
      isInject = type.isInject() ||
          containingDeclaration.safeAs<FunctionDescriptor>()
            ?.let { containingFunction ->
              containingFunction.isProvide() ||
                  containingFunction.valueParameters.getOrNull(injektIndex() - 1)
                      ?.isInject() == true
            } == true

    if (!isInject && this is ClassConstructorDescriptor && isPrimary)
      isInject = constructedClass.isProvide()

    isInject
  }
}

fun ClassDescriptor.injectableConstructors(@Inject ctx: Context): List<CallableRef> =
  trace()!!.getOrPut(InjektWritableSlices.INJECTABLE_CONSTRUCTORS, this) {
    (when {
      hasAnnotation(injektFqNames().component) ->
        listOf(
          ComponentConstructorDescriptor(this)
            .toCallableRef()
            .let { callable ->
              val info = classifierInfo()
              if (info.tags.isEmpty()) callable
              else {
                val taggedType = info.tags.wrap(callable.type)
                callable.copy(type = taggedType, originalType = taggedType)
              }
            }
        )
      classifierInfo().entryPointComponentType != null ->
        listOf(EntryPointConstructorDescriptor(this).toCallableRef())
      else -> constructors
        .filter { constructor ->
          constructor.hasAnnotation(injektFqNames().provide) ||
              (constructor.isPrimary && hasAnnotation(injektFqNames().provide))
        }
        .map { it.toCallableRef() }
    })
  }

fun ClassDescriptor.injectableReceiver(tagged: Boolean, @Inject ctx: Context): CallableRef {
  val callable = thisAsReceiverParameter.toCallableRef()
  return if (!tagged || callable.type.classifier.tags.isEmpty()) callable
  else {
    val taggedType = callable.type.classifier.tags.wrap(callable.type)
    callable.copy(type = taggedType, originalType = taggedType)
  }
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableRef) -> Unit,
  addComponent: (CallableRef) -> Unit,
  addEntryPoint: (CallableRef) -> Unit,
  import: ResolvedProviderImport? = this.import,
  seen: MutableSet<CallableRef> = mutableSetOf(),
  @Inject ctx: Context
) {
  if (this in seen) return
  seen += this

  if (!scope.canSee(this) || !scope.injectablesPredicate(this)) return

  if (callable is ComponentConstructorDescriptor) {
    addComponent(this)
    return
  }

  if (callable is EntryPointConstructorDescriptor) {
    addEntryPoint(this)
    return
  }

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.copy(frameworkKey = generateFrameworkKey()))
  } else this
  addInjectable(nextCallable)

  nextCallable
    .type
    .also { type ->
      type.classifier.descriptor?.findPackage()?.fqName?.let {
        addImport(type.classifier.fqName, it)
      }
    }
    .collectInjectables(
      scope.allScopes.any { it.ownerDescriptor == nextCallable.type.classifier.descriptor },
      import
    ) { innerCallable ->
      innerCallable.collectInjectables(
        scope = scope,
        addImport = addImport,
        addInjectable = addInjectable,
        addComponent = addComponent,
        addEntryPoint = addEntryPoint,
        import = import,
        seen = seen
      )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun List<ProviderImport>.collectImportedInjectables(
  @Inject ctx: Context,
  consumer: (CallableRef) -> Unit
) {
  for (import in this) {
    if (!import.isValidImport()) continue

    when {
      import.importPath!!.endsWith(".**") -> {
        val basePackage = FqName(import.importPath.removeSuffix(".**"))
        val resolvedImport = import.toResolvedImport(basePackage)

        val nextPackages = LinkedList<FqName>().also { it.add(basePackage) }

        val lookupLocation = import.element.lookupLocation

        while (nextPackages.isNotEmpty()) {
          val currentPackage = nextPackages.removeFirst()

          val (scope, packageObject) = memberScopeForFqName(currentPackage, lookupLocation)
            ?: continue

          fun collectInjectables(
            currentScope: MemberScope,
            currentPackageObject: ClassDescriptor?
          ) {
            if ((currentPackageObject != null &&
                  currentPackageObject.toClassifierRef().declaresInjectables) ||
              (currentPackageObject == null &&
                  injectablesLookupName in currentScope.getFunctionNames()))
              currentScope.collectInjectables(
                false,
                onEach = { declaration ->
                  if (declaration is ClassDescriptor)
                    collectInjectables(
                      if (declaration.kind == ClassKind.OBJECT) declaration.unsubstitutedMemberScope
                      else declaration.unsubstitutedInnerClassesScope,
                      declaration
                    )
                },
              ) {
                consumer(it.copy(import = resolvedImport))
              }
          }

          collectInjectables(scope, packageObject)

          nextPackages += module().getSubPackagesOf(currentPackage) { true }
        }
      }
      import.importPath.endsWith("*") -> {
        val packageFqName = FqName(import.importPath.removeSuffix(".*"))
        val resolvedImport = import.toResolvedImport(packageFqName)

        val (scope, packageObject) = memberScopeForFqName(packageFqName, import.element.lookupLocation)
          ?: continue

        // import all injectables in the package
        if ((packageObject != null && packageObject.toClassifierRef().declaresInjectables) ||
          (packageObject == null && injectablesLookupName in scope.getFunctionNames())) {
          scope.collectInjectables(false) {
            consumer(it.copy(import = resolvedImport))
          }
        }
      }
      else -> {
        val fqName = FqName(import.importPath)
        val parentFqName = fqName.parent()
        val name = fqName.shortName()

        val (scope, packageObject) = memberScopeForFqName(parentFqName, import.element.lookupLocation)
          ?: continue

        // import all injectables with the specified name
        if ((packageObject != null && packageObject.toClassifierRef().declaresInjectables) ||
          (packageObject == null && injectablesLookupName in scope.getFunctionNames())) {
          scope.collectInjectables(false, name = name) {
            consumer(it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)))
          }
        }
      }
    }
  }
}

fun TypeRef.collectTypeScopeInjectables(@Inject ctx: Context): InjectablesWithLookups =
  trace()!!.getOrPut(InjektWritableSlices.TYPE_SCOPE_INJECTABLES, key) {
    val injectables = mutableListOf<CallableRef>()
    val lookedUpPackages = mutableSetOf<FqName>()

    val processedTypes = mutableSetOf<TypeRef>()
    val nextTypes = allTypes.toCollection(LinkedList())

    while (nextTypes.isNotEmpty()) {
      val currentType = nextTypes.removeFirst()
      if (currentType.isStarProjection) continue
      if (currentType in processedTypes) continue
      processedTypes += currentType

      val resultForType = currentType.collectTypeScopeInjectablesForSingleType()

      injectables += resultForType.injectables
      lookedUpPackages += resultForType.lookedUpPackages

      resultForType.injectables.forEach { nextTypes.addAll(it.type.allTypes) }
    }

    InjectablesWithLookups(
      injectables = injectables.distinct(),
      lookedUpPackages = lookedUpPackages
    )
  }

data class InjectablesWithLookups(
  val injectables: List<CallableRef>,
  val lookedUpPackages: Set<FqName>
) {
  companion object {
    val Empty = InjectablesWithLookups(emptyList(), emptySet())
  }
}

private fun TypeRef.collectTypeScopeInjectablesForSingleType(
  @Inject ctx: Context
): InjectablesWithLookups {
  if (classifier.isTypeParameter) return InjectablesWithLookups.Empty

  trace()!!.bindingContext.get(InjektWritableSlices.TYPE_SCOPE_INJECTABLES_FOR_SINGLE_TYPE, key)
    ?.let { return it }

  val injectables = mutableListOf<CallableRef>()
  val lookedUpPackages = mutableSetOf<FqName>()

  val result = InjectablesWithLookups(injectables, lookedUpPackages)

  // we might recursively call our self so we make sure that we do not end up in a endless loop
  trace()!!.record(InjektWritableSlices.TYPE_SCOPE_INJECTABLES_FOR_SINGLE_TYPE, key, result)

  val packageResult = collectPackageTypeScopeInjectables()
  injectables += packageResult.injectables
  lookedUpPackages += packageResult.lookedUpPackages

  classifier.descriptor!!
    .safeAs<ClassDescriptor>()
    ?.let { clazz ->
      if (clazz.kind == ClassKind.OBJECT) {
        clazz
          .takeIf {
            it.hasAnnotation(injektFqNames().provide) ||
                it.classifierInfo().declaresInjectables
          }
          ?.injectableReceiver(true)
          ?.let { injectables += it }
      } else {
        injectables += clazz.injectableConstructors()
        clazz.companionObjectDescriptor
          ?.takeIf {
            it.hasAnnotation(injektFqNames().provide) ||
                it.classifierInfo().declaresInjectables
          }
          ?.let { injectables += it.injectableReceiver(true) }
      }

      clazz.classifierInfo().tags.forEach { tag ->
        val resultForTag = tag.classifier.defaultType.collectTypeScopeInjectables()
        injectables += resultForTag.injectables
        lookedUpPackages += resultForTag.lookedUpPackages
      }
    }

  return result
}

private fun TypeRef.collectPackageTypeScopeInjectables(
  @Inject ctx: Context
): InjectablesWithLookups {
  val packageFqName = classifier.descriptor!!.findPackage().fqName

  return trace()!!.getOrPut(InjektWritableSlices.PACKAGE_TYPE_SCOPE_INJECTABLES, packageFqName) {
    val lookedUpPackages = setOf(packageFqName)

    val packageFragments = packageFragmentsForFqName(packageFqName)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.none {
        injectablesLookupName in it.getMemberScope().getFunctionNames()
      }) return@getOrPut InjectablesWithLookups(emptyList(), lookedUpPackages)

    val injectables = mutableListOf<CallableRef>()

    fun collectInjectables(scope: MemberScope) {
      scope.collectInjectables(
        onEach = { declaration ->
          if (declaration is ClassDescriptor)
            collectInjectables(
              if (declaration.kind == ClassKind.OBJECT)
                declaration.unsubstitutedMemberScope
              else declaration.unsubstitutedInnerClassesScope
            )
        },
        classBodyView = false
      ) { injectables += it }
    }
    packageFragments.forEach { collectInjectables(it.getMemberScope()) }

    InjectablesWithLookups(injectables, lookedUpPackages)
  }
}

private fun InjectablesScope.canSee(callable: CallableRef, @Inject ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName() == ctx.module.moduleName()) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapTags().classifier.isObject) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any { it.ownerDescriptor == callableParent }
      } || (callable.callable.visibility == DescriptorVisibilities.PRIVATE &&
      callable.callable.containingDeclaration is PackageFragmentDescriptor &&
      run {
        val scopeFile = allScopes.firstNotNullOfOrNull { it.file }
        scopeFile == callable.callable.findPsi()
          ?.containingFile
      })

fun TypeRef.collectComponentCallables(@Inject ctx: Context): List<CallableRef> =
  classifier.descriptor!!.defaultType.memberScope
    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
    .filterIsInstance<CallableMemberDescriptor>()
    .filter { it.modality != Modality.FINAL }
    .filter {
      it.overriddenTreeAsSequence(false).none {
        it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() == true
      }
    }
    .map { it.toCallableRef() }
    .map {
      val substitutionMap = if (it.callable.safeAs<CallableMemberDescriptor>()?.kind ==
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        val originalClassifier = it.callable.cast<CallableMemberDescriptor>()
          .overriddenTreeAsSequence(false)
          .last()
          .containingDeclaration
          .cast<ClassDescriptor>()
          .toClassifierRef()
        classifier.typeParameters.zip(arguments).toMap() + originalClassifier.typeParameters
          .zip(subtypeView(originalClassifier)!!.arguments)
      } else classifier.typeParameters.zip(arguments).toMap()
      it.substitute(substitutionMap)
    }

fun List<CallableRef>.filterNotExistingIn(scope: InjectablesScope, @Inject ctx: Context): List<CallableRef> {
  val existingInjectables = scope.allScopes
    .fastFlatMap {
      addAll(it.injectables)
      it.spreadingInjectables.forEach { add(it.callable) }
    }
    .map { it.callable.uniqueKey() to it.originalType.withFrameworkKey(0) }

  return filter { callable ->
    val uniqueKey = callable.callable.uniqueKey()
    existingInjectables.none { it.first == uniqueKey && it.second == callable.originalType }
  }
}

fun InjectablesScope.collectImportSuggestionInjectables(@Inject ctx: Context): List<CallableRef> =
  collectAllInjectables().filterNotExistingIn(this)

fun collectAllInjectables(@Inject ctx: Context): List<CallableRef> =
  trace()!!.getOrPut(InjektWritableSlices.ALL_INJECTABLES, Unit) {
    memberScopeForFqName(injektFqNames().indicesPackage, NoLookupLocation.FROM_BACKEND)
      ?.first
      ?.getContributedFunctions("index".asNameId(), NoLookupLocation.FROM_BACKEND)
      ?.mapNotNull {
        val annotation = it.annotations.findAnnotation(injektFqNames().index)
          ?: return@mapNotNull null
        val fqName = FqName(annotation.allValueArguments["fqName".asNameId()]!!.value.toString())
        val uniqueKey = annotation.allValueArguments["uniqueKey".asNameId()]!!.value.toString()
        callableForUniqueKey(fqName, uniqueKey)!!.toCallableRef()
      }
      ?: emptyList()
  }
