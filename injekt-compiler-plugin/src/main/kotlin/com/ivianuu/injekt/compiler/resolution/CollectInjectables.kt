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
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.isDeserializedDeclaration
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
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
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KotlinType.collectInjectables(
  classBodyView: Boolean,
  import: ResolvedProviderImport?,
  @Inject context: AnalysisContext
): List<CallableDescriptor> {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    return listOf(
      constructor.declarationDescriptor!!
        .defaultType
        .memberScope
        .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
        .first()
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
): List<CallableDescriptor> = getContributedDescriptors()
  .flatMap { declaration ->
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
  return context.trace.getOrPut(InjektWritableSlices.IS_PROVIDE, key) {
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
  return context.trace.getOrPut(InjektWritableSlices.IS_INJECT, key) {
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
): List<CallableDescriptor> = context.trace.getOrPut(InjektWritableSlices.INJECTABLE_CONSTRUCTORS, this) {
  constructors
    .filter { constructor ->
      constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide))
    }
    .map { constructor ->
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
): CallableDescriptor {
  val callable = thisAsReceiverParameter.toCallableRef()
  val finalType = if (tagged) callable.type.classifier.tags.wrap(callable.type)
  else callable.type
  return callable.copy(isProvide = true, type = finalType, originalType = finalType)
}

fun CallableDescriptor.collectInjectables(
  scope: InjectablesScope,
  @Inject context: AnalysisContext,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableDescriptor) -> Unit,
  addSpreadingInjectable: (CallableDescriptor) -> Unit,
  import: ResolvedProviderImport? = this.import,
  seen: MutableSet<CallableDescriptor> = mutableSetOf()
) {
  if (this in seen) return
  seen += this
  if (!scope.canSee(this) || !scope.injectablesPredicate(this)) return

  if (origin == null && typeParameters.any { it.isSpread }) {
    addSpreadingInjectable(this)
    return
  }

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.copy(frameworkKey = generateFrameworkKey()))
  } else this
  addInjectable(nextCallable)

  // todo if (doNotIncludeChildren) return

  nextCallable
    .returnType!!
    .also { type ->
      type.constructor.declarationDescriptor?.findPackage()?.fqName?.let {
        addImport(type.constructor.declarationDescriptor!!.fqNameSafe, it)
      }
    }
    .collectInjectables(
      scope.allScopes.any {
        it.ownerDescriptor ==
            nextCallable.returnType!!.constructor.declarationDescriptor
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
): List<CallableDescriptor> = flatMap { import ->
  buildList<CallableDescriptor> {
    if (!import.isValidImport()) return@buildList

    fun importObjectIfExists(
      fqName: FqName,
      doNotIncludeChildren: Boolean
    ) = context.injektContext.classifierDescriptorForFqName(fqName, import.element.lookupLocation)
      ?.safeAs<ClassDescriptor>()
      ?.takeIf { it.kind == ClassKind.OBJECT }
      ?.let { clazz ->
        this += clazz.injectableReceiver(false)
          /*.copy(
            doNotIncludeChildren = doNotIncludeChildren,
            import = import.toResolvedImport(clazz.findPackage().fqName)
          )*/ // todo
      }

    if (import.importPath!!.endsWith("*")) {
      val packageFqName = FqName(import.importPath.removeSuffix(".*"))

      // import all injectables in the package
      context.injektContext.memberScopeForFqName(packageFqName, import.element.lookupLocation)
        ?.collectInjectables(false)
        // todo ?.map { it.copy(import = import.toResolvedImport(packageFqName)) }
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
          it.name == name ||
              it.safeAs<ClassConstructorDescriptor>()
                ?.constructedClass
                ?.name == name ||
              it.safeAs<ReceiverParameterDescriptor>()
                ?.value
                ?.type
                ?.constructor
                ?.declarationDescriptor
                ?.safeAs<ClassDescriptor>()
                ?.containingDeclaration
                ?.name == name
        }
        // todo ?.map { it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)) }
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

fun KotlinType.collectTypeScopeInjectables(
  @Inject context: AnalysisContext
): InjectablesWithLookups = context.trace.getOrPut(InjektWritableSlices.TYPE_SCOPE_INJECTABLES, key) {
  val injectables = mutableListOf<CallableDescriptor>()
  val lookedUpPackages = mutableSetOf<FqName>()

  val processedTypes = mutableSetOf<KotlinType>()
  val nextTypes = allTypes.toMutableList()

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
      .flatMap { it.returnType!!.allTypes }
  }

  injectables.removeAll { callable ->
    if (callable !is CallableMemberDescriptor) return@removeAll false
    val containingObjectClassifier = callable.containingDeclaration
      .safeAs<ClassDescriptor>()
      ?.takeIf { it.kind == ClassKind.OBJECT }

    containingObjectClassifier != null && injectables.any { other ->
      other is LazyClassReceiverParameterDescriptor &&
          other.buildContext(emptyList(), containingObjectClassifier.defaultType).isOk
    }
  }

  InjectablesWithLookups(
    injectables = injectables.distinct(),
    lookedUpPackages = lookedUpPackages
  )
}

data class InjectablesWithLookups(
  val injectables: List<CallableDescriptor>,
  val lookedUpPackages: Set<FqName>
) {
  companion object {
    val Empty = InjectablesWithLookups(emptyList(), emptySet())
  }
}

private fun KotlinType.collectInjectablesForSingleType(
  @Inject context: AnalysisContext
): InjectablesWithLookups {
  if (isTypeParameter()) return InjectablesWithLookups.Empty

  context.trace?.bindingContext?.get(InjektWritableSlices.TYPE_SCOPE_INJECTABLES_FOR_SINGLE_TYPE, key)
    ?.let { return it }

  val injectables = mutableListOf<CallableDescriptor>()
  val lookedUpPackages = mutableSetOf<FqName>()

  val result = InjectablesWithLookups(injectables, lookedUpPackages)

  // we might recursively call our self so we make sure that we do not end up in a endless loop
  context.trace?.record(InjektWritableSlices.TYPE_SCOPE_INJECTABLES_FOR_SINGLE_TYPE, this, result)

  val packageResult = collectPackageTypeScopeInjectables()
  injectables += packageResult.injectables
  lookedUpPackages += packageResult.lookedUpPackages

  constructor.declarationDescriptor!!
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

private fun KotlinType.collectPackageTypeScopeInjectables(
  @Inject context: AnalysisContext
): InjectablesWithLookups {
  val packageFqName = constructor.declarationDescriptor!!.findPackage().fqName

  return context.trace.getOrPut(InjektWritableSlices.PACKAGE_TYPE_SCOPE_INJECTABLES, packageFqName) {
    val lookedUpPackages = setOf(packageFqName)

    val packageFragments = context.injektContext.packageFragmentsForFqName(packageFqName)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.isEmpty())
      return@getOrPut InjectablesWithLookups(emptyList(), lookedUpPackages)

    val injectables = mutableListOf<CallableDescriptor>()

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
          callable is ConstructorDescriptor || callable.containingDeclaration
            .safeAs<ClassDescriptor>()
            ?.let { it.kind == ClassKind.OBJECT } != false
        }
    }
    packageFragments.forEach { collectInjectables(it.getMemberScope()) }

    InjectablesWithLookups(injectables, lookedUpPackages)
  }
}

private fun InjectablesScope.canSee(callable: CallableDescriptor): Boolean =
  callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.moduleName() == context.injektContext.module.name.asString()) ||
      (callable is ClassConstructorDescriptor &&
          callable.returnType.constructor.declarationDescriptor
            .safeAs<ClassDescriptor>()?.kind == ClassKind.OBJECT) ||
      callable.parents.any { callableParent ->
        allScopes.any {
          it.ownerDescriptor == callableParent/* ||
              (it.ownerDescriptor is ClassDescriptor &&
                  it.ownerDescriptor == callable.owner)*/ // todo
        }
      } || (callable.visibility == DescriptorVisibilities.PRIVATE &&
      callable.containingDeclaration is PackageFragmentDescriptor &&
      run {
        val scopeFile = allScopes.firstNotNullOfOrNull { it.file }
        scopeFile == callable.findPsi()
          ?.containingFile
      })
