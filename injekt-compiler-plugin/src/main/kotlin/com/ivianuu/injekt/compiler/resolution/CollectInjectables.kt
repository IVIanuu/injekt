/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

@OptIn(ExperimentalStdlibApi::class)
fun KotlinType.collectInjectables(
  classBodyView: Boolean,
  ctx: Context
): List<CallableRef> = ctx.trace!!.getOrPut(InjektWritableSlices.TYPE_CHAINED_INJECTABLES, toTypeKey() to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val callable = constructor
      .declarationDescriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toCallableRef(ctx)
      .let { callable ->
        callable.copy(
          type = arguments.last().type,
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectInjectables
          }
        ).substitute(
          TypeSubstitutor.create(
            constructor.parameters
              .map { it.typeConstructor }
              .zip(arguments)
              .toMap()
          )
        )
      }

    return@getOrPut listOf(callable)
  }

  // do not run any code for types which do not declare any injectables
  if (constructor.declarationDescriptor?.declaresInjectables(ctx) != true && !classBodyView)
    // at least include the companion object if it declares injectables
    return@getOrPut listOfNotNull(
      constructor.declarationDescriptor
        ?.safeAs<ClassDescriptor>()
        ?.companionObjectDescriptor
        ?.takeIf { it.declaresInjectables(ctx) }
        ?.injectableReceiver(ctx)
    )

  buildList {
    memberScope.collectInjectables(classBodyView = classBodyView, ctx = ctx) { callable ->
        add(
          if (callable.parameterTypes[DISPATCH_RECEIVER_INDEX]?.toTypeKey() != this@collectInjectables.toTypeKey()) callable.copy(
            parameterTypes = callable.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
          ) else callable
        )
      }
  }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {},
  name: Name? = null,
  ctx: Context,
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
                it.isProvide(ctx) ||
                    (includeNonProvideObjectsWithInjectables &&
                        it.declaresInjectables(ctx))
              }
              ?.injectableReceiver(ctx)
              ?.let(consumer)
        else {
          declaration.injectableConstructors(ctx).forEach(consumer)
          if (!classBodyView && !includeNonProvideObjectsWithInjectables)
            declaration.companionObjectDescriptor
              ?.takeIf { it.declaresInjectables(ctx) }
              ?.injectableReceiver(ctx)
              ?.let(consumer)
        }
      }
      is CallableMemberDescriptor -> {
        if (declaration.isProvide(ctx) &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(InjektFqNames.Provide))) {
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
  var isProvide = hasAnnotation(InjektFqNames.Provide)

  if (!isProvide && this is ClassConstructorDescriptor && isPrimary)
    isProvide = constructedClass.isProvide(ctx)

  if (!isProvide && this is ParameterDescriptor)
    isProvide = type.isProvide(ctx)

  return isProvide
}

fun ClassDescriptor.injectableConstructors(ctx: Context): List<CallableRef> =
  constructors
    .transform { constructor ->
      if (constructor.hasAnnotation(InjektFqNames.Provide) ||
        (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
        add(constructor.toCallableRef(ctx))
    }

fun ClassDescriptor.injectableReceiver(ctx: Context): CallableRef =
  thisAsReceiverParameter.toCallableRef(ctx)

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  addImport: (FqName, FqName) -> Unit,
  addInjectable: (CallableRef) -> Unit,
  import: ResolvedProviderImport? = this.import,
  chainLength: Int = 0,
  seen: MutableSet<InjectablesScope.InjectableKey> = mutableSetOf(),
  ctx: Context
) {
  if (!seen.add(InjectablesScope.InjectableKey(this, ctx))) return

  if (!scope.canSee(this, ctx) || !scope.injectablesPredicate(this)) return

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.withFrameworkKey(callable.uniqueKey(ctx), ctx))
  } else this
  addInjectable(nextCallable)

  nextCallable
    .type
    .also { type ->
      type.constructor.declarationDescriptor?.findPackage()?.fqName?.let {
        addImport(type.fqName, it)
      }
    }
    .collectInjectables(
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.constructor.declarationDescriptor
      },
      ctx
    )
    .forEach { innerCallable ->
      val nextChainLength = chainLength + 1
      innerCallable
        .copy(import = import, chainLength = nextChainLength)
        .collectInjectables(
          scope = scope,
          addImport = addImport,
          addInjectable = addInjectable,
          import = import,
          chainLength = nextChainLength,
          seen = seen,
          ctx = ctx
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun List<ProviderImport>.collectImportedInjectables(
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

          fun collectInjectables(
            currentScope: MemberScope,
            currentPackageObject: ClassDescriptor?
          ) {
            if ((currentPackageObject != null &&
                  currentPackageObject.declaresInjectables(ctx)) ||
              (currentPackageObject == null &&
                  injectablesLookupName in currentScope.getFunctionNames())) {
              if (currentPackageObject != null) {
                if (currentPackageObject.kind == ClassKind.OBJECT)
                  consumer(currentPackageObject.injectableReceiver(ctx).copy(import = resolvedImport))

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
                      consumer(innerClass.injectableReceiver(ctx).copy(import = resolvedImport))
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
                  ctx = ctx
                ) {
                  consumer(it.copy(import = resolvedImport))
                }
              }
            }
          }

          collectInjectables(scope, packageObject)

          nextPackages += ctx.module.getSubPackagesOf(currentPackage) { true }
        }
      }
      import.importPath.endsWith(".*") -> {
        val packageFqName = FqName(import.importPath.removeSuffix(".*"))
        val resolvedImport = import.toResolvedImport(packageFqName)

        val (scope, packageObject) = memberScopeForFqName(packageFqName, import.element.lookupLocation, ctx)
          ?: continue

        // import all injectables in the package
        if ((packageObject != null && packageObject.declaresInjectables(ctx)) ||
          (packageObject == null && injectablesLookupName in scope.getFunctionNames())) {
          if (packageObject != null) consumer(
            packageObject.injectableReceiver(ctx).copy(import = resolvedImport)
          )
          else scope.collectInjectables(false, ctx = ctx) {
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

        // import all injectables with the specified name
        if ((packageObject != null && packageObject.declaresInjectables(ctx)) ||
          (packageObject == null && injectablesLookupName in scope.getFunctionNames())) {
          scope.collectInjectables(false, name = name, ctx = ctx) {
            consumer(it.copy(import = import.toResolvedImport(it.callable.findPackage().fqName)))
          }
        }
      }
    }
  }
}

fun KotlinType.collectTypeScopeInjectables(ctx: Context): InjectablesWithLookups =
  ctx.trace!!.getOrPut(InjektWritableSlices.TYPE_SCOPE_INJECTABLES, toTypeKey()) {
    val injectables = mutableListOf<CallableRef>()
    val lookedUpPackages = mutableSetOf<FqName>()
    val nextPackages = LinkedList<FqName>()
    val seenTypes = mutableSetOf<TypeKey>()

    fun KotlinType.addNextPackages() {
      if (!seenTypes.add(toTypeKey())) return

      val packageFqName = constructor.declarationDescriptor?.findPackage()?.fqName
      if (packageFqName != null && lookedUpPackages.add(packageFqName))
        nextPackages += packageFqName

      allTypes.forEach { it.addNextPackages() }
    }

    addNextPackages()

    while (nextPackages.isNotEmpty()) {
      val currentPackage = nextPackages.removeFirst()

      val injectablesForPackage = collectPackageTypeScopeInjectables(currentPackage, ctx)

      injectables += injectablesForPackage

      injectablesForPackage.forEach { injectable ->
        injectable.type.addNextPackages()
        injectable.type.collectInjectables(false, ctx).forEach {
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
  ctx: Context
): List<CallableRef> =
  ctx.trace!!.getOrPut(InjektWritableSlices.PACKAGE_TYPE_SCOPE_INJECTABLES, packageFqName) {
    val packageFragments = packageFragmentsForFqName(packageFqName, ctx)
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
                !declaration.declaresInjectables(ctx)))
            collectInjectables(declaration.unsubstitutedInnerClassesScope)
        },
        classBodyView = false,
        includeNonProvideObjectsWithInjectables = true,
        ctx = ctx
      ) {
        injectables += it.copy(import = import)
      }
    }
    packageFragments.forEach { collectInjectables(it.getMemberScope()) }

    injectables
  }

private fun InjectablesScope.canSee(callable: CallableRef, ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.module.moduleName(ctx)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.kind == ClassKind.OBJECT) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any { it.ownerDescriptor == callableParent }
      } || (callable.callable.visibility == DescriptorVisibilities.PRIVATE &&
      callable.callable.containingDeclaration is PackageFragmentDescriptor &&
      run {
        val scopeFile = allScopes.firstNotNullOfOrNull { it.file }
        scopeFile == callable.callable.findPsi()?.containingFile
      })

private fun ClassifierDescriptor.declaresInjectables(ctx: Context): Boolean {
  if (this !is ClassDescriptor) return false
  return defaultType
    .memberScope
    .getContributedDescriptors()
    .any { it.isProvide(ctx) }
}

fun List<CallableRef>.filterNotExistingIn(scope: InjectablesScope, ctx: Context): List<CallableRef> {
  val existingInjectables: MutableSet<InjectablesScope.InjectableKey> = scope.allScopes
    .transformTo<InjectablesScope, InjectablesScope.InjectableKey, MutableSet<InjectablesScope.InjectableKey>>(mutableSetOf()) {
      for (injectable in it.injectables)
        add(InjectablesScope.InjectableKey(injectable, ctx))
    }

  return filter { existingInjectables.add(InjectablesScope.InjectableKey(it, ctx)) }
}
