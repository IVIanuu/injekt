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
import com.ivianuu.injekt.compiler.analysis.InjectNParameterDescriptor
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.fixTypes
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectNTypes
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.isDeserializedDeclaration
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.module
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.packageFragmentsForFqName
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  import: ResolvedProviderImport?,
  @Inject ctx: Context
): List<CallableRef> {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val functionType = if (isSuspendFunctionType)
      module().builtIns.getSuspendFunction(arguments.size - 1 + injectNTypes.size)
    else
      module().builtIns.getFunction(arguments.size - 1 + injectNTypes.size)

    return listOf(
      functionType
        .defaultType
        .memberScope
        .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
        .first()
        .toCallableRef()
        .let { callable ->
          val lambdaInjectParameters = injectNTypes.mapIndexed { index, injectNType ->
            InjectNParameterDescriptor(
              callable.callable,
              arguments.size - 1 + index,
              injectNType
            )
          }

          callable.copy(
            type = arguments.last(),
            isProvide = true,
            parameterTypes = callable.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this } + lambdaInjectParameters
              .map { it.index to it.typeRef },
            scopeComponentType = scopeComponentType,
            isEager = isEager,
            import = import,
            injectNParameters = lambdaInjectParameters
          ).substitute(classifier.typeParameters.zip(arguments).toMap())
        }
    )
  }

  if (!classifier.declaresInjectables && !classBodyView) return emptyList()

  return ((classifier.descriptor ?: error("Wtf $classifier"))
    .defaultType
    .memberScope
    .collectInjectables(classBodyView) + if (classBodyView) classifier.injectNParameters
    .map { it.toCallableRef() } else emptyList())
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
    .map { callable ->
      callable.copy(
        isProvide = true,
        parameterTypes = if (callable.callable.dispatchReceiverParameter != null &&
          callable.parameterTypes.isNotEmpty()) {
          callable.parameterTypes.toMutableMap()
            .also { it[DISPATCH_RECEIVER_INDEX] = this }
        } else callable.parameterTypes,
        import = import
      )
    }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  nameFilter: (Name) -> Boolean = { true },
  onEach: (DeclarationDescriptor) -> Unit = {},
  @Inject ctx: Context
): List<CallableRef> = getContributedDescriptors(nameFilter = nameFilter)
  .flatMap { declaration ->
    onEach(declaration)
    when (declaration) {
      is ClassDescriptor -> declaration
        .injectableConstructors() + listOfNotNull(
        declaration.companionObjectDescriptor
          ?.takeIf { it.classifierInfo().declaresInjectables }
          ?.injectableReceiver(false)
      )
      is CallableMemberDescriptor -> {
        if (declaration.isProvide() &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(injektFqNames().provide) ||
              declaration.primaryConstructorPropertyValueParameter()
                ?.hasAnnotation(injektFqNames().provide) == true)) {
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
                  (this is ValueParameterDescriptor && (if (containingFunction.isDeserializedDeclaration())
                    containingFunction.callableInfo().injectParameterIndex?.let {
                      injektIndex() >= it
                    } == true
                  else
                    containingFunction.valueParameters.getOrNull(injektIndex() - 1)
                      ?.isInject() == true))
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
                  (this is ValueParameterDescriptor && (if (containingFunction.isDeserializedDeclaration())
                    containingFunction.callableInfo().injectParameterIndex?.let {
                      injektIndex() >= it
                    } == true
                  else
                    containingFunction.valueParameters.getOrNull(injektIndex() - 1)
                      ?.isInject() == true))
            } == true

    if (!isInject && this is ClassConstructorDescriptor && isPrimary)
      isInject = constructedClass.isProvide()

    isInject
  }
}

fun CallableDescriptor.injectNParameters(@Inject ctx: Context): List<InjectNParameterDescriptor> =
  trace()!!.getOrPut(InjektWritableSlices.INJECT_N_PARAMETERS, this) {
    findPsi()?.safeAs<KtDeclaration>()?.let { declaration ->
      annotations.forEach {
        fixTypes(it.type, declaration)
      }
    }
    ((safeAs<ConstructorDescriptor>()?.constructedClass
      ?.classifierInfo()?.injectNParameters?.map { it.typeRef } ?: emptyList()) + injectNTypes())
      .mapIndexed { index, parameterType ->
        InjectNParameterDescriptor(
          this,
          valueParameters.size + index,
          parameterType
        )
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
  val finalType = if (tagged) callable.type.classifier.tags.wrap(callable.type)
  else callable.type
  return callable.copy(isProvide = true, type = finalType, originalType = finalType)
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableRef) -> Unit,
  addSpreadingInjectable: (CallableRef) -> Unit,
  addComponent: (TypeRef) -> Unit,
  addEntryPoint: (TypeRef) -> Unit,
  import: ResolvedProviderImport? = this.import,
  seen: MutableSet<CallableRef> = mutableSetOf(),
  @Inject ctx: Context
) {
  if (this in seen) return
  seen += this
  if (!scope.canSee(this) || !scope.injectablesPredicate(this)) return

  if (typeParameters.any { it.isSpread && typeArguments[it] == it.defaultType }) {
    addSpreadingInjectable(this)
    return
  }

  if (callable is ComponentConstructorDescriptor) {
    addComponent(type)
    return
  }

  if (callable is EntryPointConstructorDescriptor) {
    addEntryPoint(type)
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
    )
    .forEach { innerCallable ->
      innerCallable.collectInjectables(
        scope = scope,
        addImport = addImport,
        addInjectable = addInjectable,
        addSpreadingInjectable = addSpreadingInjectable,
        addComponent = addComponent,
        addEntryPoint = addEntryPoint,
        import = import,
        seen = seen
      )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun List<ProviderImport>.collectImportedInjectables(
  @Inject ctx: Context
): List<CallableRef> = flatMap { import ->
  buildList<CallableRef> {
    if (!import.isValidImport()) return@buildList

    if (import.importPath!!.endsWith("*")) {
      val packageFqName = FqName(import.importPath.removeSuffix(".*"))

      // import all injectables in the package
      memberScopeForFqName(packageFqName, import.element.lookupLocation)
        ?.takeIf {
          it.getContributedFunctions(injectablesLookupName, NoLookupLocation.FROM_BACKEND)
            .isNotEmpty()
        }
        ?.collectInjectables(false)
        ?.map { it.copy(import = import.toResolvedImport(packageFqName)) }
        ?.let { this += it }
    } else {
      val fqName = FqName(import.importPath)
      val parentFqName = fqName.parent()
      val name = fqName.shortName()

      // import all injectables with the specified name
      memberScopeForFqName(parentFqName, import.element.lookupLocation)
        ?.collectInjectables(false, { it == name })
        ?.map { it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)) }
        ?.let { this += it }
    }
  }
}

fun TypeRef.collectTypeScopeInjectables(@Inject ctx: Context): InjectablesWithLookups =
  trace()!!.getOrPut(InjektWritableSlices.TYPE_SCOPE_INJECTABLES, key) {
    val injectables = mutableListOf<CallableRef>()
    val lookedUpPackages = mutableSetOf<FqName>()

    val processedTypes = mutableSetOf<TypeRef>()
    val nextTypes = allTypes.toMutableList()

    while (nextTypes.isNotEmpty()) {
      val currentType = nextTypes.removeFirst()
      if (currentType.isStarProjection) continue
      if (currentType in processedTypes) continue
      processedTypes += currentType

      val resultForType = currentType.collectTypeScopeInjectablesForSingleType()

      injectables += resultForType.injectables
      lookedUpPackages += resultForType.lookedUpPackages

      nextTypes += resultForType.injectables
        .flatMap { it.type.allTypes }
    }

    injectables.removeAll { callable ->
      if (callable.callable !is CallableMemberDescriptor &&
          callable.callable !is ComponentConstructorDescriptor &&
          callable.callable !is EntryPointConstructorDescriptor) return@removeAll false
      val containingObjectClassifier = callable.callable.containingDeclaration
        .safeAs<ClassDescriptor>()
        ?.takeIf { it.kind == ClassKind.OBJECT }
        ?.toClassifierRef()

      containingObjectClassifier != null && injectables.any { other ->
        other.callable is LazyClassReceiverParameterDescriptor &&
            other.type.isSubTypeOf(containingObjectClassifier.defaultType)
      }
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
          .takeIf { it.classifierInfo().declaresInjectables }
          ?.injectableReceiver(true)
          ?.let { injectables += it }
      } else {
        injectables += clazz.injectableConstructors()
        clazz.companionObjectDescriptor
          ?.takeIf { it.classifierInfo().declaresInjectables }
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

    if (memberScopeForFqName(packageFqName, NoLookupLocation.FROM_BACKEND)
        ?.getContributedFunctions(injectablesLookupName, NoLookupLocation.FROM_BACKEND)
        ?.isEmpty() == true)
          return@getOrPut InjectablesWithLookups(emptyList(), lookedUpPackages)

    val packageFragments = packageFragmentsForFqName(packageFqName)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.isEmpty())
      return@getOrPut InjectablesWithLookups(emptyList(), lookedUpPackages)

    val injectables = mutableListOf<CallableRef>()

    fun collectInjectables(scope: MemberScope) {
      injectables += scope.collectInjectables(
        onEach = { declaration ->
          if (declaration is ClassDescriptor && declaration
              .toClassifierRef()
              .declaresInjectables)
                collectInjectables(declaration.unsubstitutedMemberScope)
        },
        classBodyView = false
      )
        .filter { callable ->
          callable.callable is ConstructorDescriptor || callable.callable.containingDeclaration
            .safeAs<ClassDescriptor>()
            ?.let { it.kind == ClassKind.OBJECT } != false
        }
    }
    packageFragments.forEach { collectInjectables(it.getMemberScope()) }

    InjectablesWithLookups(injectables, lookedUpPackages)
  }
}

private fun InjectablesScope.canSee(callable: CallableRef, @Inject ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName() == this.ctx.module.moduleName()) ||
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
