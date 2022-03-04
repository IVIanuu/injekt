/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.builtins.functions.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.slicedMap.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.lang.reflect.*
import kotlin.experimental.*
import kotlin.reflect.*

fun PropertyDescriptor.primaryConstructorPropertyValueParameter(
  ctx: Context
): ValueParameterDescriptor? = overriddenTreeUniqueAsSequence(false)
  .map { it.containingDeclaration }
  .filterIsInstance<ClassDescriptor>()
  .mapNotNull { clazz ->
    if (clazz.isDeserializedDeclaration()) {
      clazz.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.firstOrNull {
          it.name == name &&
              it.name.asString() in clazz.primaryConstructorPropertyParameters()
        }
    } else {
      clazz.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.firstOrNull {
          it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true &&
              it.name == name
        }
    }
  }
  .firstOrNull()

val isIde = Project::class.java.name == "com.intellij.openapi.project.Project"

fun KtAnnotated.hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

fun KtAnnotated.findAnnotation(fqName: FqName): KtAnnotationEntry? {
  val annotationEntries = annotationEntries
  if (annotationEntries.isEmpty()) return null

  // Check if the fully qualified name is used, e.g. `@dagger.Module`.
  val annotationEntry = annotationEntries.firstOrNull {
    it.text.startsWith("@${fqName.asString()}")
  }
  if (annotationEntry != null) return annotationEntry

  // Check if the simple name is used, e.g. `@Provide`.
  val annotationEntryShort = annotationEntries
    .firstOrNull {
      it.shortName == fqName.shortName()
    }
    ?: return null

  val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

  // If the simple name is used, check that the annotation is imported.
  val hasImport = importPaths.any { it.fqName == fqName }
  if (hasImport) return annotationEntryShort

  // Look for star imports and make a guess.
  val hasStarImport = importPaths
    .any { it.isAllUnder && fqName.asString().startsWith(it.fqName.asString()) }
  if (hasStarImport) return annotationEntryShort

  val isSamePackage = fqName.parent() == annotationEntryShort.containingKtFile.packageFqName
  if (isSamePackage) return annotationEntryShort

  return null
}

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(ctx: Context) =
  ctx.trace!!.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(ctx: Context): Boolean =
  moduleName(ctx) != ctx.module.moduleName(ctx)

fun DeclarationDescriptor.isDeserializedDeclaration(): Boolean = this is DeserializedDescriptor ||
    (this is PropertyAccessorDescriptor && correspondingProperty.isDeserializedDeclaration()) ||
    (this is InjectFunctionDescriptor && underlyingDescriptor.isDeserializedDeclaration()) ||
    this is DeserializedTypeParameterDescriptor ||
    this is JavaClassDescriptor ||
    this is FunctionClassDescriptor

fun String.asNameId() = Name.identifier(this)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
  annotations.hasAnnotation(fqName)

fun DeclarationDescriptor.uniqueKey(ctx: Context): String =
  ctx.trace!!.getOrPut(InjektWritableSlices.UNIQUE_KEY, original) {
    when (val original = this.original) {
      is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:${
        original.valueParameters
          .joinToString(",") {
            it.type
              .fullyAbbreviatedType
              .uniqueTypeKey()
          }
      }"
      is ClassDescriptor -> "class:$fqNameSafe"
      is AnonymousFunctionDescriptor -> "anonymous_function:${findPsi()!!.let {
        "${it.containingFile.cast<KtFile>().virtualFilePath}_${it.startOffset}_${it.endOffset}"
      }}"
      is FunctionDescriptor -> "function:$fqNameSafe:" +
          original.typeParameters.joinToString {
            buildString {
              append(it.name.asString())
              it.upperBounds.forEach { upperBound ->
                append(
                  upperBound
                    .fullyAbbreviatedType
                    .uniqueTypeKey()
                )
              }
            }
          } +
          listOfNotNull(original.dispatchReceiverParameter, original.extensionReceiverParameter)
            .plus(original.valueParameters)
            .joinToString(",") { parameter ->
              buildString {
                when {
                  parameter === original.dispatchReceiverParameter -> append("d:")
                  parameter === original.extensionReceiverParameter -> append("e:")
                  else -> append("p:")
                }
                append(
                  parameter.type
                    .fullyAbbreviatedType
                    .uniqueTypeKey()
                )
              }
            }
      is PropertyDescriptor -> "property:$fqNameSafe:" +
          original.typeParameters.joinToString {
            buildString {
              append(it.name.asString())
              it.upperBounds.forEach { upperBound ->
                append(
                  upperBound
                    .fullyAbbreviatedType
                    .uniqueTypeKey()
                )
              }
            }
          } +
          listOfNotNull(
            original.dispatchReceiverParameter, original.extensionReceiverParameter
          )
            .joinToString(",") {
              it.type
                .fullyAbbreviatedType
                .uniqueTypeKey()
            }
      is TypeAliasDescriptor -> "typealias:$fqNameSafe"
      is TypeParameterDescriptor ->
        "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey(ctx)}"
      is ReceiverParameterDescriptor -> "receiver:$fqNameSafe"
      is ValueParameterDescriptor -> "value_parameter:$fqNameSafe"
      is VariableDescriptor -> "variable:${fqNameSafe}"
      else -> error("Unexpected declaration $this")
    }
  }

private fun KotlinType.uniqueTypeKey(depth: Int = 0): String {
  if (depth > 15) return ""
  return buildString {
    append(constructor.declarationDescriptor!!.fqNameSafe)
    arguments.forEachIndexed { index, typeArgument ->
      if (index == 0) append("<")
      append(typeArgument.type.uniqueTypeKey(depth + 1))
      if (index != arguments.lastIndex) append(", ")
      else append(">")
    }
    if (isMarkedNullable) append("?")
  }
}

private val KotlinType.fullyAbbreviatedType: KotlinType
  get() {
    val abbreviatedType = getAbbreviatedType()
    return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
  }

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R> Collection<T>.transform(@BuilderInference block: MutableList<R>.(T) -> Unit): List<R> =
  transformTo(mutableListOf(), block)

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R, C : MutableCollection<in R>> Collection<T>.transformTo(
  destination: C,
  @BuilderInference block: C.(T) -> Unit
) = destination.apply {
  for (item in this@transformTo)
    block(item)
}

val DISPATCH_RECEIVER_NAME = Name.identifier("\$dispatchReceiver")
val EXTENSION_RECEIVER_NAME = Name.identifier("\$extensionReceiver")

fun ParameterDescriptor.injektName(): Name = when (injektIndex()) {
  DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
  EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
  else -> name
}

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1

fun ParameterDescriptor.injektIndex(): Int = if (this is ValueParameterDescriptor) {
  index
} else {
  val callable = containingDeclaration as? CallableDescriptor
  when {
    original == callable?.dispatchReceiverParameter?.original ||
        (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor) -> DISPATCH_RECEIVER_INDEX
    original == callable?.extensionReceiverParameter?.original -> EXTENSION_RECEIVER_INDEX
    else -> throw AssertionError("Unexpected descriptor $this")
  }
}

fun <T> Any.readPrivateFinalField(clazz: KClass<*>, fieldName: String): T {
  val field = clazz.java.declaredFields
    .single { it.name == fieldName }
  field.isAccessible = true
  val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
  modifiersField.isAccessible = true
  modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
  return field.get(this) as T
}

fun <T> Any.updatePrivateFinalField(clazz: KClass<*>, fieldName: String, transform: T.() -> T): T {
  val field = clazz.java.declaredFields
    .single { it.name == fieldName }
  field.isAccessible = true
  val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
  modifiersField.isAccessible = true
  modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
  val currentValue = field.get(this)
  val newValue = transform(currentValue as T)
  field.set(this, newValue)
  return newValue
}

val injectablesLookupName = "_injectables".asNameId()
val subInjectablesLookupName = "_subInjectables".asNameId()

val KtElement?.lookupLocation: LookupLocation
  get() = if (this == null || isIde) NoLookupLocation.FROM_BACKEND
  else KotlinLookupLocation(this)

fun DeclarationDescriptor.moduleName(ctx: Context): String =
  getJvmModuleNameForDeserializedDescriptor(this)
    ?.removeSurrounding("<", ">")
    ?: ctx.module.name.asString().removeSurrounding("<", ">")

inline fun <K, V> BindingTrace?.getOrPut(
  slice: WritableSlice<K, V>,
  key: K,
  computation: () -> V
): V {
  this?.get(slice, key)?.let { return it }
  return computation()
    .also { this?.record(slice, key, it) }
}

fun classifierDescriptorForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: Context
): ClassifierDescriptor? {
  return if (fqName.isRoot) null
  else memberScopeForFqName(fqName.parent(), lookupLocation, ctx)?.first
    ?.getContributedClassifier(fqName.shortName(), lookupLocation)
}

fun injectablesForFqName(
  fqName: FqName,
  ctx: Context
): List<CallableRef> =
  memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND, ctx)
    ?.first
    ?.getContributedDescriptors(nameFilter = { it == fqName.shortName() })
    ?.transform { declaration ->
      when (declaration) {
        is ClassDescriptor -> addAll(declaration.injectableConstructors(ctx))
        is CallableDescriptor -> {
          if (declaration.isProvide(ctx))
            this += declaration.toCallableRef(ctx)
        }
      }
    }
    ?: emptyList()

fun memberScopeForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: Context
): Pair<MemberScope, ClassDescriptor?>? {
  val pkg = ctx.module.getPackage(fqName)

  if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope to null

  val (parentMemberScope) = memberScopeForFqName(fqName.parent(), lookupLocation, ctx) ?: return null

  val classDescriptor = parentMemberScope.getContributedClassifier(
    fqName.shortName(),
    lookupLocation
  ) as? ClassDescriptor ?: return null

  return classDescriptor.unsubstitutedMemberScope to classDescriptor
}

fun packageFragmentsForFqName(
  fqName: FqName,
  ctx: Context
): List<PackageFragmentDescriptor> = ctx.module.getPackage(fqName).fragments

val composeCompilerInClasspath = try {
  Class.forName("androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices")
  true
} catch (e: ClassNotFoundException) {
  false
}

fun ClassifierDescriptor.declaresInjectables(ctx: Context): Boolean {
  if (this !is ClassDescriptor) return false
  if (hasAnnotation(InjektFqNames.DeclaresInjectables)) return true

  if (this !is LazyClassDescriptor) return false

  val declaresInjectables = defaultType
    .memberScope
    .getContributedDescriptors()
    .any { it.isProvide(ctx) }

  if (declaresInjectables && visibility.shouldPersistInfo())
    addAnnotation(
      AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.DeclaresInjectables))
          ?.defaultType ?: return false,
        emptyMap(),
        SourceElement.NO_SOURCE
      )
    )

  return declaresInjectables
}

fun ClassifierDescriptor.primaryConstructorPropertyParameters(): List<String> {
  if (this !is ClassDescriptor) return emptyList()

  annotations.findAnnotation(InjektFqNames.PrimaryConstructorPropertyParameters)
    ?.allValueArguments
    ?.values
    ?.single()
    ?.cast<ArrayValue>()
    ?.value
    ?.map { it.value as String }
    ?.let { return it }

  if (this !is LazyClassDescriptor) return emptyList()

  val primaryConstructorPropertyParameters = safeAs<ClassDescriptor>()
    ?.unsubstitutedPrimaryConstructor
    ?.valueParameters
    ?.transform {
      if (it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true)
        add(it.name.asString())
    }
    ?: emptyList()

  if (primaryConstructorPropertyParameters.isNotEmpty() && visibility.shouldPersistInfo())
    addAnnotation(
      AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.PrimaryConstructorPropertyParameters))
          ?.defaultType ?: return emptyList(),
        mapOf(
          "value".asNameId() to ArrayValue(
            primaryConstructorPropertyParameters
              .map { StringValue(it) }
          ) {
            it.builtIns.array.defaultType.replace(
              newArguments = listOf(
                it.builtIns.stringType.asTypeProjection()
              )
            )
          }
        ),
        SourceElement.NO_SOURCE
      )
    )

  return primaryConstructorPropertyParameters
}

@OptIn(ExperimentalStdlibApi::class)
private fun Annotated.addAnnotation(annotation: AnnotationDescriptor) {
  updatePrivateFinalField<Annotations>(
    LazyClassDescriptor::class,
    "annotations"
  ) { Annotations.create(annotations + annotation) }
}

fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED

fun KotlinType.renderToString() = asTypeProjection().renderToString()

fun TypeProjection.renderToString() = buildString {
  render { append(it) }
}

fun TypeProjection.render(
  depth: Int = 0,
  renderType: (TypeProjection) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  fun TypeProjection.inner() {
    if (!renderType(this)) return

    if (type.isComposable) append("${InjektFqNames.Composable}<")

    when {
      isStarProjection -> append("*")
      else -> append(type.fqName.asString())
    }
    if (type.arguments.isNotEmpty()) {
      append("<")
      type.arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != type.arguments.lastIndex) append(", ")
      }
      append(">")
    }
    if (type.isMarkedNullable && !isStarProjection) append("?")
    if (type.isComposable) append(">")
  }
  inner()
}

val KotlinType.fqName: FqName
  get() = constructor.declarationDescriptor?.fqNameSafe ?: FqName.ROOT

val KotlinType.typeSize: Int
  get() {
    var typeSize = 0
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      typeSize++
      if (seen.add(type))
        type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return typeSize
  }

val KotlinType.coveringSet: Set<ClassifierDescriptor>
  get() {
    val classifiers = mutableSetOf<ClassifierDescriptor>()
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      if (!seen.add(type)) return
      type.constructor.declarationDescriptor
        ?.let { classifiers += it }
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return classifiers
  }

val KotlinType.allTypes: Set<KotlinType>
  get() {
    val result = mutableSetOf<KotlinType>()
    fun collect(inner: KotlinType) {
      if (!result.add(inner)) return
      inner.arguments.forEach { collect(it.type) }
      inner.constructor.supertypes.forEach { collect(it) }
    }
    collect(this)
    return result
  }

val KotlinType.allVisibleTypes: Set<KotlinType>
  get() {
    val result = mutableSetOf<KotlinType>()
    fun collect(inner: KotlinType) {
      if (!result.add(inner)) return
      inner.arguments.forEach { collect(it.type) }
    }
    collect(this)
    return result
  }


fun KotlinType.subtypeView(classifier: ClassifierDescriptor): KotlinType? {
  if (constructor.declarationDescriptor == classifier) return this
  return constructor.supertypes
    .firstNotNullOfOrNull { it.subtypeView(classifier) }
    ?.let { return it }
}

fun KotlinType.buildSystem(
  superType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>
): ConstraintSystem {
  val system = ConstraintSystemBuilderImpl()

  val typeVariables = buildList {
    for (visibleType in allVisibleTypes)
      if (visibleType.constructor.declarationDescriptor is TypeParameterDescriptor &&
        visibleType.constructor.declarationDescriptor !in staticTypeParameters)
        add(visibleType.constructor.declarationDescriptor as TypeParameterDescriptor)
  }

  val substitutor = system.registerTypeVariables(
    CallHandle.NONE,
    typeVariables
  )

  system.addSubtypeConstraint(
    substitutor.substitute(this, Variance.INVARIANT) ?: this,
    superType,
    ConstraintPositionKind.SPECIAL.position()
  )

  system.fixVariables()

  return system.build()
}

val KotlinType.frameworkKey: String
  get() = annotations.findAnnotation(InjektFqNames.FrameworkKey)
    ?.allValueArguments?.values?.single()?.value?.cast() ?: ""

fun KotlinType.withFrameworkKey(key: String, ctx: Context) = replace(
  newAnnotations = Annotations.create(
    annotations
      .filter { it.fqName != InjektFqNames.FrameworkKey } +
        AnnotationDescriptorImpl(
          ctx.frameworkKeyClassifier.defaultType,
          mapOf("value".asNameId() to StringValue(key)),
          SourceElement.NO_SOURCE
        )
  )
)

val KotlinType.isComposable: Boolean
  get() = hasAnnotation(InjektFqNames.Composable)

val KotlinType.isComposableType: Boolean
  get() = isComposable || constructor.supertypes.any { it.isComposableType }

val KotlinType.isProvideFunctionType: Boolean
  get() = hasAnnotation(InjektFqNames.Provide) && isFunctionType

val KotlinType.isFunctionType: Boolean
  get() = fqName.asString().startsWith("kotlin.Function") ||
      fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")
