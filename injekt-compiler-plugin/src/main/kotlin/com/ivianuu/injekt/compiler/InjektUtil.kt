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

import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.com.intellij.openapi.progress.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.types.*
import java.lang.reflect.*
import java.util.concurrent.*
import kotlin.collections.set
import kotlin.reflect.*

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

  // Check if the simple name is used, e.g. `@Given`.
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
    .asSequence()
    .filter { it.isAllUnder }
    .any { fqName.asString().startsWith(it.fqName.asString()) }
  if (hasStarImport) return annotationEntryShort

  val isSamePackage = fqName.parent() == annotationEntryShort.containingKtFile.packageFqName
  if (isSamePackage) return annotationEntryShort

  return null
}

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(
  bindingContext: BindingContext,
) = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(context: InjektContext): Boolean =
  module != context.module

fun DeclarationDescriptor.isDeserializedDeclaration(): Boolean = this is DeserializedDescriptor ||
    (this is PropertyAccessorDescriptor && correspondingProperty.isDeserializedDeclaration()) ||
    (this is GivenFunctionDescriptor && underlyingDescriptor.isDeserializedDeclaration()) ||
    this is DeserializedTypeParameterDescriptor

fun String.asNameId() = Name.identifier(this)

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
  annotations.hasAnnotation(fqName)

fun Annotated.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
  annotations.filter {
    val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
    inner.hasAnnotation(annotation)
  }

fun IrAnnotationContainer.getAnnotatedAnnotations(annotation: FqName): List<IrConstructorCall> =
  annotations.filter {
    val inner = it.type.classOrNull!!.owner
    inner.hasAnnotation(annotation)
  }

fun DeclarationDescriptor.uniqueKey(context: InjektContext): String {
  return when (val original = this.original) {
    is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:${
      original.valueParameters
        .joinToString(",") {
          it.type
            .fullyAbbreviatedType
            .uniqueTypeKey()
        }
    }"
    is ClassDescriptor -> "class:$fqNameSafe"
    is FunctionDescriptor -> "function:$fqNameSafe:${
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
    }"
    is PropertyDescriptor -> "property:$fqNameSafe:${
      listOfNotNull(
        original.dispatchReceiverParameter, original.extensionReceiverParameter
      )
        .joinToString(",") {
          it.type
            .fullyAbbreviatedType
            .uniqueTypeKey()
        }
    }"
    is TypeAliasDescriptor -> "typealias:$fqNameSafe"
    is TypeParameterDescriptor ->
      "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey(context)}"
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

fun checkCancelled() {
  try {
    ProgressManager.checkCanceled()
  } catch (e: CancellationException) {
    e.printStackTrace()
    throw e
  }
}

const val DISPATCH_RECEIVER_NAME = "\$dispatchReceiver"
const val EXTENSION_RECEIVER_NAME = "\$extensionReceiver"

fun ParameterDescriptor.injektName(): String {
  val callable = containingDeclaration as? CallableDescriptor
  return when {
    original == callable?.dispatchReceiverParameter?.original ||
        (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor) -> DISPATCH_RECEIVER_NAME
    original == callable?.extensionReceiverParameter?.original -> EXTENSION_RECEIVER_NAME
    else -> if (name.isSpecial)
      (type.getAbbreviation() ?: type)
        .constructor.declarationDescriptor!!.name
        .asString().decapitalize()
    else name.asString()
  }
}

inline fun <A, B> List<A>.forEachWith(
  other: List<B>,
  action: (A, B) -> Unit
) {
  check(size == other.size) {
    "Size not equal this: $this other: $other"
  }
  for (i in indices) action(this[i], other[i])
}

fun <K, V> List<K>.toMap(values: List<V>): Map<K, V> {
  if (this.isEmpty()) return emptyMap()
  val map = mutableMapOf<K, V>()
  forEachWith(values) { key, value -> map[key] = value }
  return map
}

private var currentFrameworkKey = 0
fun generateFrameworkKey() = ++currentFrameworkKey

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

fun givensLookupName(fqName: FqName, packageFqName: FqName): Name = fqName.asString()
  .removePrefix(packageFqName.asString())
  .replace(".", "_")
  .removePrefix("_")
  .takeIf { it.isNotEmpty() }
  ?.plus("_givens")
  ?.asNameId()
  ?: "givens".asNameId()

val KtElement?.lookupLocation: LookupLocation
  get() = if (this == null || isIde) NoLookupLocation.FROM_BACKEND
  else KotlinLookupLocation(this)
