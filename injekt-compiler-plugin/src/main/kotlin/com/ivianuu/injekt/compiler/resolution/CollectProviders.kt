/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.providersLookupName
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.packageFragmentsForFqName
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.injekt.compiler.transformTo
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

fun TypeRef.collectProviders(
  classBodyView: Boolean,
  ctx: Context
): List<CallableRef> = ctx.trace!!.getOrPut(InjektWritableSlices.TYPE_PROVIDERS, this to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val unwrappedFunctionType = unwrapTags()
    val callable = unwrappedFunctionType
      .classifier
      .descriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toCallableRef(ctx)
      .let { callable ->
        callable.copy(
          type = unwrappedFunctionType.arguments.last(),
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectProviders
          }
        ).substitute(
          unwrappedFunctionType.classifier.typeParameters
            .zip(unwrappedFunctionType.arguments)
            .toMap()
        )
      }

    return@getOrPut listOf(callable)
  }

  // do not run any code for types which do not declare any providers
  if (!classifier.declaresProviders && !classBodyView)
    // at least include the companion object if it declares providers
    return@getOrPut listOfNotNull(
      classifier.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.companionObjectDescriptor
        ?.toClassifierRef(ctx)
        ?.takeIf { it.declaresProviders }
        ?.descriptor
        ?.cast<ClassDescriptor>()
        ?.receiverProvider(false, ctx)
    )

  buildList {
    classifier
      .descriptor
      ?.defaultType
      ?.memberScope
      ?.collectProviders(classBodyView = classBodyView, ctx = ctx) { callable ->
        val substitutionMap = if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
          CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
          val originalClassifier = callable.callable.cast<CallableMemberDescriptor>()
            .overriddenTreeAsSequence(false)
            .last()
            .containingDeclaration
            .cast<ClassDescriptor>()
            .toClassifierRef(ctx)
          classifier.typeParameters.zip(arguments).toMap() + originalClassifier.typeParameters
            .zip(subtypeView(originalClassifier)!!.arguments)
        } else classifier.typeParameters.zip(arguments).toMap()
        val substituted = callable.substitute(substitutionMap)

        add(
          substituted.copy(
            parameterTypes = if (substituted.parameterTypes[DISPATCH_RECEIVER_INDEX] != this@collectProviders) {
              substituted.parameterTypes.toMutableMap()
                .also { it[DISPATCH_RECEIVER_INDEX] = this@collectProviders }
            } else substituted.parameterTypes
          )
        )
      }
  }
}

fun ResolutionScope.collectProviders(
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {},
  name: Name? = null,
  ctx: Context,
  includeNonProvideObjectsWithProviders: Boolean = false,
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
                it.isProvide(ctx) ||
                    (includeNonProvideObjectsWithProviders &&
                        it.toClassifierRef(ctx).declaresProviders)
              }
              ?.receiverProvider(!classBodyView, ctx)
              ?.let(consumer)
        else {
          declaration.constructorProviders(ctx).forEach(consumer)
          if (!classBodyView && !includeNonProvideObjectsWithProviders)
            declaration.companionObjectDescriptor
              ?.takeIf { it.classifierInfo(ctx).declaresProviders }
              ?.receiverProvider(false, ctx)
              ?.let(consumer)
        }
      }
      is CallableMemberDescriptor -> {
        if (declaration.isProvide(ctx) &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(InjektFqNames.Provide) ||
              declaration.primaryConstructorPropertyValueParameter(ctx)
                ?.hasAnnotation(InjektFqNames.Provide) == true)) {
          consumer(declaration.toCallableRef(ctx))
        }
      }
      is VariableDescriptor -> {
        if (declaration.isProvide(ctx))
          consumer(declaration.toCallableRef(ctx))
      }
    }
  }
}

fun Annotated.isProvide(ctx: Context): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return ctx.trace!!.getOrPut(InjektWritableSlices.IS_PROVIDE, key) {
    var isProvide = hasAnnotation(InjektFqNames.Provide) ||
        hasAnnotation(InjektFqNames.Context)

    if (!isProvide && this is PropertyDescriptor)
      isProvide = primaryConstructorPropertyValueParameter(ctx)?.isProvide(ctx) == true

    if (!isProvide && this is ParameterDescriptor)
      isProvide = type.isProvide(ctx) ||
          containingDeclaration.safeAs<FunctionDescriptor>()?.isProvide(ctx) == true

    if (!isProvide && this is ClassConstructorDescriptor && isPrimary)
      isProvide = constructedClass.isProvide(ctx)

    isProvide
  }
}

fun Annotated.isContext(ctx: Context): Boolean {
  @Suppress("IMPLICIT_CAST_TO_ANY")
  val key = if (this is KotlinType) System.identityHashCode(this) else this
  return ctx.trace!!.getOrPut(InjektWritableSlices.IS_CONTEXT, key) {
    var isContext = hasAnnotation(InjektFqNames.Context)

    if (!isContext)
      isContext = this is ReceiverParameterDescriptor &&
          containingDeclaration.safeAs<CallableDescriptor>()?.contextReceiverParameters?.any {
            it.type == type
          } == true

    if (!isContext && this is PropertyDescriptor)
      isContext = primaryConstructorPropertyValueParameter(ctx)?.isContext(ctx) == true

    if (!isContext && this is ParameterDescriptor)
      isContext = type.isContext(ctx) ||
          containingDeclaration.safeAs<FunctionDescriptor>()?.isProvide(ctx) == true

    if (!isContext && this is ClassConstructorDescriptor && isPrimary)
      isContext = constructedClass.isProvide(ctx)

    isContext
  }
}

fun ClassDescriptor.constructorProviders(ctx: Context): List<CallableRef> =
  ctx.trace!!.getOrPut(InjektWritableSlices.CONSTRUCTOR_PROVIDERS, this) {
    constructors
      .transform { constructor ->
        if (constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
            add(constructor.toCallableRef(ctx))
      }
  }

fun ClassDescriptor.receiverProvider(tagged: Boolean, ctx: Context): CallableRef {
  val callable = ReceiverParameterDescriptorImpl(
    this,
    ImplicitClassReceiver(this),
    Annotations.EMPTY
  ).toCallableRef(ctx)
  return if (!tagged || callable.type.classifier.tags.isEmpty()) callable
  else {
    val taggedType = callable.type.classifier.tags.wrap(callable.type)
    callable.copy(type = taggedType, originalType = taggedType)
  }
}

fun CallableRef.collectProviders(
  scope: ContextScope,
  addImport: (FqName, FqName) -> Unit,
  addProvider: (CallableRef) -> Unit,
  addSpreadingProvider: (CallableRef) -> Unit,
  import: ResolvedProviderImport? = this.import,
  chainLength: Int = 0,
  seen: MutableSet<ContextScope.ProviderKey> = mutableSetOf(),
  ctx: Context
) {
  if (!seen.add(ContextScope.ProviderKey(this, ctx))) return

  if (!scope.canSee(this, ctx) || !scope.providersPredicate(this)) return

  if (typeParameters.any { it.isSpread && typeArguments[it] == it.defaultType }) {
    addSpreadingProvider(this)
    return
  }

  val nextCallable = if (type.isProvideFunctionType) {
    addProvider(this)
    copy(type = type.copy(frameworkKey = callable.uniqueKey(ctx)))
  } else this
  addProvider(nextCallable)

  nextCallable
    .type
    .also { type ->
      type.classifier.descriptor?.findPackage()?.fqName?.let {
        addImport(type.classifier.fqName, it)
      }
    }
    .collectProviders(
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.classifier.descriptor
      },
      ctx
    )
    .forEach { innerCallable ->
      val nextChainLength = chainLength + 1
      innerCallable
        .copy(import = import, chainLength = nextChainLength)
        .collectProviders(
          scope = scope,
          addImport = addImport,
          addProvider = addProvider,
          addSpreadingProvider = addSpreadingProvider,
          import = import,
          chainLength = nextChainLength,
          seen = seen,
          ctx = ctx
        )
    }
}

fun List<ProviderImport>.collectImportedProviders(
  ctx: Context,
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

          val (scope, packageObject) = memberScopeForFqName(currentPackage, lookupLocation, ctx)
            ?: continue

          fun collectProviders(
            currentScope: MemberScope,
            currentPackageObject: ClassDescriptor?
          ) {
            if ((currentPackageObject != null &&
                  currentPackageObject.toClassifierRef(ctx).declaresProviders) ||
              (currentPackageObject == null &&
                  providersLookupName in currentScope.getFunctionNames())) {
              if (currentPackageObject != null) {
                if (currentPackageObject.kind == ClassKind.OBJECT)
                  consumer(currentPackageObject.receiverProvider(false, ctx).copy(import = resolvedImport))

                fun collectPackageObjects(packageObject: ClassDescriptor) {
                  for (innerClass in packageObject.unsubstitutedInnerClassesScope
                    .getContributedDescriptors()) {
                    innerClass as ClassDescriptor
                    // only include the inner class if the class is a
                    // object which is not the companion or @Provide
                    // because otherwise it will be included when collecting the enclosing package object
                    if (innerClass.kind == ClassKind.OBJECT &&
                      !innerClass.isCompanionObject &&
                      !innerClass.isProvide(ctx))
                      consumer(innerClass.receiverProvider(false, ctx).copy(import = resolvedImport))
                    collectPackageObjects(innerClass)
                  }
                }

                collectPackageObjects(currentPackageObject)
              } else {
                currentScope.collectProviders(
                  false,
                  onEach = { declaration ->
                    if (declaration is ClassDescriptor)
                      collectProviders(
                        declaration.unsubstitutedInnerClassesScope,
                        declaration
                      )
                  },
                  ctx = ctx
                ) {
                  consumer(it.copy(import = resolvedImport))
                }
              }
            }
          }

          collectProviders(scope, packageObject)

          nextPackages += ctx.module.getSubPackagesOf(currentPackage) { true }
        }
      }
      import.importPath.endsWith(".*") -> {
        val packageFqName = FqName(import.importPath.removeSuffix(".*"))
        val resolvedImport = import.toResolvedImport(packageFqName)

        val (scope, packageObject) = memberScopeForFqName(packageFqName, import.element.lookupLocation, ctx)
          ?: continue

        // import all providers in the package
        if ((packageObject != null && packageObject.toClassifierRef(ctx).declaresProviders) ||
          (packageObject == null && providersLookupName in scope.getFunctionNames())) {
          if (packageObject != null) consumer(
            packageObject.receiverProvider(false, ctx).copy(import = resolvedImport)
          )
          else scope.collectProviders(false, ctx = ctx) {
            consumer(it.copy(import = resolvedImport))
          }
        }
      }
      else -> {
        val fqName = FqName(import.importPath)
        val parentFqName = fqName.parent()
        val name = fqName.shortName()

        val (scope, packageObject) = memberScopeForFqName(parentFqName, import.element.lookupLocation, ctx)
          ?: continue

        // import all providers with the specified name
        if ((packageObject != null && packageObject.toClassifierRef(ctx).declaresProviders) ||
          (packageObject == null && providersLookupName in scope.getFunctionNames())) {
          scope.collectProviders(false, name = name, ctx = ctx) {
            consumer(it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)))
          }
        }
      }
    }
  }
}

fun TypeRef.collectTypeScopeProviders(ctx: Context): ProvidersWithLookups =
  ctx.trace!!.getOrPut(InjektWritableSlices.TYPE_SCOPE_PROVIDERS, key) {
    val providers = mutableListOf<CallableRef>()
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

      val providersForPackage = collectPackageTypeScopeProviders(currentPackage, ctx)

      providers += providersForPackage

      providersForPackage.forEach { provider ->
        provider.type.addNextPackages()
        provider.type.collectProviders(false, ctx).forEach {
          it.type.addNextPackages()
        }
      }
    }

    ProvidersWithLookups(providers, lookedUpPackages)
  }

data class ProvidersWithLookups(
  val providers: List<CallableRef>,
  val lookedUpPackages: Set<FqName>
)

private fun collectPackageTypeScopeProviders(
  packageFqName: FqName,
  ctx: Context
): List<CallableRef> =
  ctx.trace!!.getOrPut(InjektWritableSlices.PACKAGE_TYPE_SCOPE_PROVIDERS, packageFqName) {
    val packageFragments = packageFragmentsForFqName(packageFqName, ctx)
      .filterNot { it is BuiltInsPackageFragment }

    if (packageFragments.none {
        providersLookupName in it.getMemberScope().getFunctionNames()
    }) return@getOrPut emptyList()

    val providers = mutableListOf<CallableRef>()

    val import = ResolvedProviderImport(null, "$packageFqName.*", packageFqName)
    fun collectProviders(scope: MemberScope) {
      scope.collectProviders(
        onEach = { declaration ->
          // only collect in nested scopes if the declaration does NOT declare any providers
          // otherwise they will be included later in the providers scope itself
          if (declaration is ClassDescriptor &&
            (declaration.kind != ClassKind.OBJECT ||
                !declaration.toClassifierRef(ctx).declaresProviders))
            collectProviders(declaration.unsubstitutedInnerClassesScope)
        },
        classBodyView = false,
        includeNonProvideObjectsWithProviders = true,
        ctx = ctx
      ) {
        providers += it.copy(import = import)
      }
    }
    packageFragments.forEach { collectProviders(it.getMemberScope()) }

    providers
  }

private fun ContextScope.canSee(callable: CallableRef, ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.module.moduleName(ctx)) ||
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

fun List<CallableRef>.filterNotExistingIn(scope: ContextScope, ctx: Context): List<CallableRef> {
  val existingProviders: MutableSet<ContextScope.ProviderKey> = scope.allScopes
    .transformTo<ContextScope, ContextScope.ProviderKey, MutableSet<ContextScope.ProviderKey>>(mutableSetOf()) {
      for (provider in it.providers)
        add(ContextScope.ProviderKey(provider, ctx))
      addAll(it.spreadingProviderKeys)
    }

  return filter { existingProviders.add(ContextScope.ProviderKey(it, ctx)) }
}
