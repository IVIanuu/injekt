/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.utils.addToStdlib.*

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
@Serializable class ClassifierInfo(
  val primaryConstructorPropertyParameters: List<String>,
  val declaresInjectables: Boolean
)

fun ClassifierDescriptor.classifierInfo(ctx: Context): ClassifierInfo =
  ctx.trace!!.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
    if (isDeserializedDeclaration()) {
      annotations
        .findAnnotation(InjektFqNames.ClassifierInfo)
        ?.allValueArguments
        ?.values
        ?.single()
        ?.value
        ?.cast<String>()
        ?.decode<ClassifierInfo>()
        ?.let { return@getOrPut it }
    }

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        primaryConstructorPropertyParameters = emptyList(),
        declaresInjectables = false
      )
    } else {
      val primaryConstructorPropertyParameters = safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.transform {
          if (it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true)
            add(it.name.asString())
        }
        ?: emptyList()

      val info = ClassifierInfo(
        primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
        declaresInjectables = defaultType
          .memberScope
          .getContributedDescriptors()
          .any { it.isProvide(ctx) }
      )

      // important to cache the info before persisting it
      ctx.trace.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

      persistInfoIfNeeded(info, ctx)

      // no accident
      return info
    }
  }

private fun ClassifierDescriptor.persistInfoIfNeeded(
  info: ClassifierInfo,
  ctx: Context
) {
  if (this is DeclarationDescriptorWithVisibility) {
    if (!visibility.shouldPersistInfo()) return

    if (info.primaryConstructorPropertyParameters.isEmpty() && !info.declaresInjectables) return

    if (hasAnnotation(InjektFqNames.ClassifierInfo)) return

    val serializedInfo = info.encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        ctx.module.findClassAcrossModuleDependencies(
          ClassId.topLevel(InjektFqNames.ClassifierInfo)
        )?.defaultType ?: return,
        mapOf("value".asNameId() to StringValue(serializedInfo)),
        SourceElement.NO_SOURCE
      )
    )
  }
}

@OptIn(ExperimentalStdlibApi::class)
private fun Annotated.updateAnnotation(annotation: AnnotationDescriptor) {
  val newAnnotations = Annotations.create(
    buildList {
      for (existing in annotations)
        if (existing.type != annotation.type)
          add(existing)
      add(annotation)
    }
  )
  when (this) {
    is AnnotatedImpl -> updatePrivateFinalField<Annotations>(
      AnnotatedImpl::class,
      "annotations"
    ) { newAnnotations }
    is LazyClassDescriptor -> updatePrivateFinalField<Annotations>(
      LazyClassDescriptor::class,
      "annotations"
    ) { newAnnotations }
    is InjectFunctionDescriptor -> underlyingDescriptor.updateAnnotation(annotation)
    is FunctionImportedFromObject -> callableFromObject.updateAnnotation(annotation)
    else -> throw AssertionError("Cannot add annotation to $this $javaClass")
  }
}

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
