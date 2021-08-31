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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.checkCancelled
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.isDeserializedDeclaration
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
import com.ivianuu.injekt.compiler.toMap
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  import: ResolvedProviderImport?,
  @Inject context: AnalysisContext
): List<CallableRef> {
  if (isStarProjection) return emptyList()

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
              .also { it[DISPATCH_RECEIVER_INDEX] = this },
            import = import
          ).substitute(classifier.typeParameters.toMap(arguments))
        }
    )
  }

  return (classifier.descriptor ?: error("Wtf $classifier"))
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
        } else callable.parameterTypes,
        import = import
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
      val taggedType = callable.type.classifier.tags.wrap(callable.type)
      callable.copy(
        isProvide = true,
        type = taggedType,
        originalType = taggedType
      )
    }
}

fun ClassDescriptor.injectableReceiver(
  tagged: Boolean,
  @Inject context: AnalysisContext
): CallableRef {
  val callable = thisAsReceiverParameter.toCallableRef()
  val finalType = if (tagged) callable.type.classifier.tags.wrap(callable.type)
  else callable.type
  return callable.copy(isProvide = true, type = finalType, originalType = finalType)
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  @Inject context: AnalysisContext,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableRef) -> Unit,
  addSpreadingInjectable: (CallableRef) -> Unit,
  import: ResolvedProviderImport? = this.import,
  seen: MutableSet<CallableRef> = mutableSetOf()
) {
  checkCancelled()
  if (this in seen) return
  seen += this
  if (!scope.canSee(this)) return

  if (origin == null && typeParameters.any { it.isSpread }) {
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
    .also { type ->
      type.classifier.descriptor?.findPackage()?.fqName?.let {
        addImport(type.classifier.fqName, it)
      }
    }
    .collectInjectables(
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.classifier.descriptor
      },
      import
    )
    .forEach { innerCallable ->
      innerCallable.collectInjectables(
        scope = scope,
        addImport = addImport,
        addInjectable = addInjectable,
        addSpreadingInjectable = addSpreadingInjectable,
        import = import,
        seen = seen
      )
    }
}

@OptIn(ExperimentalStdlibApi::class)
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
  @Inject context: AnalysisContext
): InjectablesWithLookups {
  val finalType = withNullability(false)

  return context.injektContext.typeScopeInjectables.getOrPut(finalType) {
    val injectables = mutableListOf<CallableRef>()
    val lookedUpPackages = mutableSetOf<FqName>()

    val processedTypes = mutableSetOf<TypeRef>()
    val nextTypes = finalType.allTypes.toMutableList()

    while (nextTypes.isNotEmpty()) {
      val currentType = nextTypes.removeFirst()
      if (currentType.isStarProjection) continue
      if (currentType in processedTypes) continue
      processedTypes += currentType

      val resultForType = currentType.collectInjectablesForSingleType()

      injectables += resultForType.injectables
      lookedUpPackages += resultForType.lookedUpPackages

      nextTypes += resultForType.injectables
        .toList()
        .flatMap { it.type.allTypes }
    }

    injectables.removeAll { callable ->
      if (callable.callable !is CallableMemberDescriptor) return@removeAll false
      val containingObjectClassifier = callable.callable.containingDeclaration
        .safeAs<ClassDescriptor>()
        ?.takeIf { it.kind == ClassKind.OBJECT }
        ?.toClassifierRef()

      containingObjectClassifier != null && injectables.any { other ->
        other.callable is LazyClassReceiverParameterDescriptor &&
            other.buildContext(emptyList(), containingObjectClassifier.defaultType).isOk
      }
    }

    InjectablesWithLookups(
      injectables = injectables.distinct(),
      lookedUpPackages = lookedUpPackages
    )
  }
}

data class InjectablesWithLookups(
  val injectables: List<CallableRef>,
  val lookedUpPackages: Set<FqName>
) {
  companion object {
    val Empty = InjectablesWithLookups(emptyList(), emptySet())
  }
}

private fun TypeRef.collectInjectablesForSingleType(
  @Inject context: AnalysisContext
): InjectablesWithLookups {
  if (classifier.isTypeParameter) return InjectablesWithLookups.Empty

  val finalType = withNullability(false)

  context.injektContext.typeScopeInjectablesForSingleType[finalType]?.let { return it }

  val injectables = mutableListOf<CallableRef>()
  val lookedUpPackages = mutableSetOf<FqName>()

  val result = InjectablesWithLookups(injectables, lookedUpPackages)

  // we might recursively call our self so we make sure that we do not end up in a endless loop
  context.injektContext.typeScopeInjectablesForSingleType[finalType] = result

  val packageResult = collectPackageTypeScopeInjectables()
  injectables += packageResult.injectables
  lookedUpPackages += packageResult.lookedUpPackages

  classifier.descriptor!!
    .safeAs<ClassDescriptor>()
    ?.let { clazz ->
      if (clazz.kind == ClassKind.OBJECT) {
        injectables += clazz.injectableReceiver(true)
      } else {
        injectables += clazz.injectableConstructors()
        clazz.companionObjectDescriptor
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
  @Inject context: AnalysisContext
): InjectablesWithLookups {
  val packageFqName = classifier.descriptor!!.findPackage().fqName

  return context.injektContext.packageTypeScopeInjectables.getOrPut(packageFqName) {
    val lookedUpPackages = setOf(packageFqName)

    val packageFragments = context.injektContext.packageFragmentsForFqName(packageFqName)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.isEmpty())
      return@getOrPut InjectablesWithLookups(emptyList(), lookedUpPackages)

    val injectables = mutableListOf<CallableRef>()

    fun collectInjectables(scope: MemberScope) {
      injectables += scope.collectInjectables(
        onEach = { declaration ->
          if (declaration is ClassDescriptor &&
            declaration !is LazyJavaClassDescriptor
          )
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

private fun InjectablesScope.canSee(callable: CallableRef): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          DescriptorVisibilities.INTERNAL.isVisible(null,
            callable.callable, context.injektContext.module)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapTags().classifier.isObject) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any {
          it.ownerDescriptor == callableParent ||
              (it.ownerDescriptor is ClassDescriptor &&
                  it.ownerDescriptor.toClassifierRef() == callable.owner)
        }
      } || (callable.callable.visibility == DescriptorVisibilities.PRIVATE &&
      callable.callable.containingDeclaration is PackageFragmentDescriptor &&
      run {
        val scopeFile = allScopes.firstNotNullResult { it.file }

        callable.callable.containingDeclaration
          .safeAs<LazyPackageDescriptor>()
          ?.declarationProvider
          ?.getPackageFiles()
          ?.any { it == scopeFile }
          ?: false
      })
