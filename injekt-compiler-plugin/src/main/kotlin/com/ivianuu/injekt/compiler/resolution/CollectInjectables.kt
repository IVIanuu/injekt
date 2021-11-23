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
import com.ivianuu.injekt.compiler.analysis.SyntheticInterfaceConstructorDescriptor
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injectablesForFqName
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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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

@OptIn(ExperimentalStdlibApi::class)
fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  @Inject ctx: Context
): List<CallableRef> = trace()!!.getOrPut(InjektWritableSlices.TYPE_INJECTABLES, this to classBodyView) {
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
          isEager = isEager
        ).substitute(classifier.typeParameters.zip(arguments).toMap())
      }

    return@getOrPut listOf(callable)
  }

  if (!classifier.declaresInjectables && !classBodyView) return@getOrPut emptyList()

  buildList {
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

        add(
          substituted.copy(
            parameterTypes = if (substituted.parameterTypes[DISPATCH_RECEIVER_INDEX] != this@collectInjectables) {
              substituted.parameterTypes.toMutableMap()
                .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
            } else substituted.parameterTypes
          )
        )
      }
  }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {},
  name: Name? = null,
  @Inject ctx: Context,
  includeNonProvideObjectsWithInjectables: Boolean = false,
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
              .takeIf {
                it.isProvide() ||
                    (includeNonProvideObjectsWithInjectables &&
                        it.toClassifierRef().declaresInjectables)
              }
              ?.injectableReceiver(!classBodyView)
              ?.let(consumer)
        else {
          declaration.injectableConstructors().forEach(consumer)
          if (!classBodyView && !includeNonProvideObjectsWithInjectables)
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
    constructors
      .filter { constructor ->
        constructor.hasAnnotation(injektFqNames().provide) ||
            (constructor.isPrimary && hasAnnotation(injektFqNames().provide))
      }
      .map { it.toCallableRef() }
      .takeIf { it.isNotEmpty() }
      ?: if (isProvide() && kind == ClassKind.INTERFACE)
        listOf(
          SyntheticInterfaceConstructorDescriptor(this)
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
      else emptyList()
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
  addSpreadingInjectable: (CallableRef) -> Unit,
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
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.classifier.descriptor ||
            it.componentType == nextCallable.type
      }
    )
    .map { it.copy(import = import) }
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
    val nextPackages = LinkedList<FqName>()
    val seenTypes = mutableSetOf<TypeRef>()

    fun TypeRef.addNextPackages() {
      if (this in seenTypes) return
      seenTypes += this

      val packageFqName = classifier.descriptor?.findPackage()?.fqName
      if (packageFqName != null && packageFqName !in lookedUpPackages) {
        lookedUpPackages += packageFqName
        nextPackages += packageFqName
      }

      allTypes.forEach { it.addNextPackages() }
      classifier.tags.forEach { it.addNextPackages() }
    }

    addNextPackages()

    while (nextPackages.isNotEmpty()) {
      val currentPackage = nextPackages.removeFirst()

      val injectablesForPackage = collectPackageTypeScopeInjectables(currentPackage)

      injectables += injectablesForPackage

      injectablesForPackage.forEach { injectable ->
        injectable.type.addNextPackages()
        injectable.type.collectInjectables(false).forEach {
          it.type.addNextPackages()
        }
      }
    }

    InjectablesWithLookups(injectables, lookedUpPackages)
  }

data class InjectablesWithLookups(
  val injectables: List<CallableRef>,
  val lookedUpPackages: Set<FqName>
)

private fun collectPackageTypeScopeInjectables(
  packageFqName: FqName,
  @Inject ctx: Context
): List<CallableRef> =
  trace()!!.getOrPut(InjektWritableSlices.PACKAGE_TYPE_SCOPE_INJECTABLES, packageFqName) {
    val packageFragments = packageFragmentsForFqName(packageFqName)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.none {
        injectablesLookupName in it.getMemberScope().getFunctionNames()
      }) return@getOrPut emptyList()

    val injectables = mutableListOf<CallableRef>()

    val import = ResolvedProviderImport(null, "$packageFqName.*", packageFqName)
    fun collectInjectables(scope: MemberScope) {
      scope.collectInjectables(
        onEach = { declaration ->
          if (declaration is ClassDescriptor)
            collectInjectables(declaration.unsubstitutedInnerClassesScope)
        },
        classBodyView = false,
        includeNonProvideObjectsWithInjectables = true
      ) {
        injectables += it.copy(import = import)
      }
    }
    packageFragments.forEach { collectInjectables(it.getMemberScope()) }

    injectables
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
        scopeFile == callable.callable.findPsi()?.containingFile
      }) ||
      (callable.callable.visibility == DescriptorVisibilities.PROTECTED && run {
        val ownerType = callable.callable.containingDeclaration
          .cast<ClassDescriptor>().toClassifierRef().defaultType
        allScopes.any { it.componentType?.isSubTypeOf(ownerType) == true }
      })

fun TypeRef.collectAbstractInjectableCallables(@Inject ctx: Context): List<CallableRef> =
  classifier.descriptor!!.defaultType.memberScope
    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
    .filterIsInstance<CallableMemberDescriptor>()
    .filter { it.modality != Modality.FINAL }
    .filter {
      it.overriddenTreeAsSequence(false).none {
        val dispatchReceiverType = it.dispatchReceiverParameter?.type
        dispatchReceiverType?.isAnyOrNullableAny() == true ||
            dispatchReceiverType?.constructor?.declarationDescriptor?.fqNameSafe ==
            injektFqNames().disposable
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
    .mapTo(mutableSetOf()) { it.callable.uniqueKey() to it.originalType }

  return filter { callable ->
    val uniqueKey = callable.callable.uniqueKey()
    existingInjectables.none {
      it.first == uniqueKey && it.second == callable.originalType
    }.also {
      if (it) existingInjectables += uniqueKey to callable.originalType
    }
  }
}

fun InjectablesScope.collectImportSuggestionInjectables(@Inject ctx: Context): List<CallableRef> =
  collectAllInjectables().filterNotExistingIn(this)

fun collectAllInjectables(@Inject ctx: Context): List<CallableRef> =
  trace()!!.getOrPut(InjektWritableSlices.ALL_INJECTABLES, Unit) {
    memberScopeForFqName(injektFqNames().indicesPackage, NoLookupLocation.FROM_BACKEND)
      ?.first
      ?.getContributedFunctions("index".asNameId(), NoLookupLocation.FROM_BACKEND)
      ?.flatMap {
        val annotation = it.annotations.findAnnotation(injektFqNames().index)
          ?: return@flatMap emptyList()
        val fqName = FqName(annotation.allValueArguments["fqName".asNameId()]!!.value.toString())
        injectablesForFqName(fqName)
      }
      ?: emptyList()
  }
