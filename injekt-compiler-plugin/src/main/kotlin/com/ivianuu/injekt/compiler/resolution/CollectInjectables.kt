/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

@OptIn(ExperimentalStdlibApi::class)
fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  @Inject ctx: Context
): List<CallableRef> = trace()!!.getOrPut(InjektWritableSlices.TYPE_INJECTABLES, this to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val callable = unwrapTags()
      .classifier
      .descriptor!!
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
          }
        ).substitute(classifier.typeParameters.zip(arguments).toMap())
      }

    return@getOrPut listOf(callable)
  }

  // do not run any code for types which do not declare any injectables
  if (!classifier.declaresInjectables && !classBodyView)
    // at least include the companion object if it declares injectables
    return@getOrPut listOfNotNull(
      classifier.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.companionObjectDescriptor
        ?.toClassifierRef()
        ?.takeIf { it.declaresInjectables }
        ?.descriptor
        ?.cast<ClassDescriptor>()
        ?.injectableReceiver(false)
    )

  buildList {
    classifier
      .descriptor
      ?.defaultType
      ?.memberScope
      ?.collectInjectables(classBodyView = classBodyView) { callable ->
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
      .transform { constructor ->
        if (constructor.hasAnnotation(injektFqNames().provide) ||
          (constructor.isPrimary && hasAnnotation(injektFqNames().provide)))
            add(constructor.toCallableRef())
      }
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
  if (!seen.add(this)) return

  if (!scope.canSee(this) || !scope.injectablesPredicate(this)) return

  if (typeParameters.any { it.isSpread && typeArguments[it] == it.defaultType }) {
    addSpreadingInjectable(this)
    return
  }

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.copy(frameworkKey = callable.uniqueKey()))
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
        it.ownerDescriptor == nextCallable.type.classifier.descriptor
      }
    )
    .forEach { innerCallable ->
      innerCallable
        .copy(import = import)
        .collectInjectables(
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
                  injectablesLookupName in currentScope.getFunctionNames())) {
              if (currentPackageObject != null) {
                if (currentPackageObject.kind == ClassKind.OBJECT)
                  consumer(currentPackageObject.injectableReceiver(false).copy(import = resolvedImport))

                fun collectPackageObjects(packageObject: ClassDescriptor) {
                  for (innerClass in packageObject.unsubstitutedInnerClassesScope
                    .getContributedDescriptors()) {
                    innerClass as ClassDescriptor
                    // only include the inner class if the class is a
                    // object which is not the companion or @Provide
                    // because otherwise it will be included when collecting the enclosing package object
                    if (innerClass.kind == ClassKind.OBJECT &&
                      !innerClass.isCompanionObject &&
                      !innerClass.isProvide())
                      consumer(innerClass.injectableReceiver(false).copy(import = resolvedImport))
                    collectPackageObjects(innerClass)
                  }
                }

                collectPackageObjects(currentPackageObject)
              } else {
                currentScope.collectInjectables(
                  false,
                  onEach = { declaration ->
                    if (declaration is ClassDescriptor)
                      collectInjectables(
                        declaration.unsubstitutedInnerClassesScope,
                        declaration
                      )
                  },
                ) {
                  consumer(it.copy(import = resolvedImport))
                }
              }
            }
          }

          collectInjectables(scope, packageObject)

          nextPackages += module().getSubPackagesOf(currentPackage) { true }
        }
      }
      import.importPath.endsWith(".*") -> {
        val packageFqName = FqName(import.importPath.removeSuffix(".*"))
        val resolvedImport = import.toResolvedImport(packageFqName)

        val (scope, packageObject) = memberScopeForFqName(packageFqName, import.element.lookupLocation)
          ?: continue

        // import all injectables in the package
        if ((packageObject != null && packageObject.toClassifierRef().declaresInjectables) ||
          (packageObject == null && injectablesLookupName in scope.getFunctionNames())) {
          if (packageObject != null) consumer(
            packageObject.injectableReceiver(false).copy(import = resolvedImport)
          )
          else scope.collectInjectables(false) {
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
      if (!seenTypes.add(this)) return

      val packageFqName = classifier.descriptor?.findPackage()?.fqName
      if (packageFqName != null && lookedUpPackages.add(packageFqName))
        nextPackages += packageFqName

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
          // only collect in nested scopes if the declaration does NOT declare any injectables
          if (declaration is ClassDescriptor &&
            (declaration.kind != ClassKind.OBJECT ||
                !declaration.toClassifierRef().declaresInjectables))
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
      })

fun List<CallableRef>.filterNotExistingIn(scope: InjectablesScope, @Inject ctx: Context): List<CallableRef> {
  val existingInjectables: MutableSet<InjectablesScope.InjectableKey> = scope.allScopes
    .transformTo<InjectablesScope, InjectablesScope.InjectableKey, MutableSet<InjectablesScope.InjectableKey>>(mutableSetOf()) {
      for (injectable in it.injectables)
        add(InjectablesScope.InjectableKey(injectable))
      addAll(it.spreadingInjectableKeys)
    }

  return filter { existingInjectables.add(InjectablesScope.InjectableKey(it)) }
}

fun InjectablesScope.collectImportSuggestionInjectables(@Inject ctx: Context): List<CallableRef> =
  collectAllInjectables().filterNotExistingIn(this)

fun collectAllInjectables(@Inject ctx: Context): List<CallableRef> =
  trace()!!.getOrPut(InjektWritableSlices.ALL_INJECTABLES, Unit) {
    memberScopeForFqName(injektFqNames().indicesPackage, NoLookupLocation.FROM_BACKEND)
      ?.first
      ?.getContributedFunctions("index".asNameId(), NoLookupLocation.FROM_BACKEND)
      ?.transform {
        val annotation = it.annotations.findAnnotation(injektFqNames().index)
          ?: return@transform
        val fqName = FqName(annotation.allValueArguments["fqName".asNameId()]!!.value.toString())
        for (injectable in injectablesForFqName(fqName)) {
          val dispatchReceiverType = injectable.parameterTypes[DISPATCH_RECEIVER_INDEX]
          if (dispatchReceiverType == null ||
              dispatchReceiverType.classifier.isObject)
                add(injectable)
        }
      }
      ?: emptyList()
  }
