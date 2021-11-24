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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.injectableConstructors
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

fun PropertyDescriptor.primaryConstructorPropertyValueParameter(
  @Inject ctx: Context
): ValueParameterDescriptor? = overriddenTreeUniqueAsSequence(false)
  .map { it.containingDeclaration }
  .filterIsInstance<ClassDescriptor>()
  .mapNotNull { clazz ->
    if (clazz.isDeserializedDeclaration()) {
      val clazzClassifier = clazz.toClassifierRef()
      clazz.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.firstOrNull {
          it.name == name &&
              it.name in clazzClassifier.primaryConstructorPropertyParameters
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

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(@Inject ctx: Context) =
  trace()!!.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(@Inject ctx: Context): Boolean =
  moduleName() != module().moduleName()

fun DeclarationDescriptor.isDeserializedDeclaration(): Boolean = this is DeserializedDescriptor ||
    (this is PropertyAccessorDescriptor && correspondingProperty.isDeserializedDeclaration()) ||
    (this is InjectFunctionDescriptor && underlyingDescriptor.isDeserializedDeclaration()) ||
    this is DeserializedTypeParameterDescriptor ||
    this is JavaClassDescriptor ||
    this is FunctionClassDescriptor

fun String.asNameId() = Name.identifier(this)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
  annotations.hasAnnotation(fqName)

fun Annotated.getTags(injektFqNames: InjektFqNames): List<AnnotationDescriptor> =
  annotations.filter {
    val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
    inner.hasAnnotation(injektFqNames.tag) || it.fqName == injektFqNames.composable
  }

fun DeclarationDescriptor.uniqueKey(@Inject ctx: Context): String =
  trace()!!.getOrPut(InjektWritableSlices.UNIQUE_KEY, this) {
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
        "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey()}"
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

private val KotlinType.fullyAbbreviatedType: KotlinType
  get() {
    val abbreviatedType = getAbbreviatedType()
    return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
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

private var currentFrameworkKey = 0
fun generateFrameworkKey() = ++currentFrameworkKey

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

fun DeclarationDescriptor.moduleName(@Inject ctx: Context): String =
  getJvmModuleNameForDeserializedDescriptor(this)
    ?.removeSurrounding("<", ">")
    ?: module().name.asString().removeSurrounding("<", ">")

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
  @Inject ctx: Context
): ClassifierDescriptor? {
  return if (fqName.isRoot) null
  else memberScopeForFqName(fqName.parent(), lookupLocation)?.first
    ?.getContributedClassifier(fqName.shortName(), lookupLocation)
}

fun classifierDescriptorForKey(key: String, @Inject ctx: Context): ClassifierDescriptor =
  trace().getOrPut(InjektWritableSlices.CLASSIFIER_FOR_KEY, key) {
    val fqName = FqName(key.split(":")[1])
    val classifier = memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
      ?.first
      ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
      ?.takeIf { it.uniqueKey() == key }
      ?: functionDescriptorsForFqName(fqName.parent())
        .transform { addAll(it.typeParameters) }
        .firstOrNull {
          it.uniqueKey() == key
        }
      ?: propertyDescriptorsForFqName(fqName.parent())
        .transform { addAll(it.typeParameters) }
        .firstOrNull { it.uniqueKey() == key }
      ?: classifierDescriptorForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
        .safeAs<ClassifierDescriptorWithTypeParameters>()
        ?.declaredTypeParameters
        ?.firstOrNull { it.uniqueKey() == key }
      ?: error("Could not get for $fqName $key")
    classifier
  }

fun injectablesForFqName(
  fqName: FqName,
  @Inject ctx: Context
): List<CallableRef> =
  memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
    ?.first
    ?.getContributedDescriptors(nameFilter = { it == fqName.shortName() })
    ?.transform { declaration ->
      when (declaration) {
        is ClassDescriptor -> addAll(declaration.injectableConstructors())
        is CallableDescriptor -> {
          if (declaration.isProvide())
            this += declaration.toCallableRef()
        }
      }
    }
    ?: emptyList()

private fun functionDescriptorsForFqName(
  fqName: FqName,
  @Inject ctx: Context
): Collection<FunctionDescriptor> =
  memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
    ?.first
    ?.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
    ?: emptyList()

private fun propertyDescriptorsForFqName(
  fqName: FqName,
  @Inject ctx: Context
): Collection<PropertyDescriptor> =
  memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.first
    ?.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
    ?: emptyList()

fun memberScopeForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  @Inject ctx: Context
): Pair<MemberScope, ClassDescriptor?>? {
  val pkg = module().getPackage(fqName)

  if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope to null

  val (parentMemberScope) = memberScopeForFqName(fqName.parent(), lookupLocation) ?: return null

  val classDescriptor = parentMemberScope.getContributedClassifier(
    fqName.shortName(),
    lookupLocation
  ) as? ClassDescriptor ?: return null

  return classDescriptor.unsubstitutedMemberScope to classDescriptor
}

fun packageFragmentsForFqName(
  fqName: FqName,
  @Inject ctx: Context
): List<PackageFragmentDescriptor> = module().getPackage(fqName).fragments
