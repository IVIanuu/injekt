/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeImpl
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.Base64

@OptIn(UnsafeCastFunction::class)
class InjektSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(InjektFqNames.Provide.asString())
      .filterIsInstance<KSDeclaration>()
      .groupBy { it.containingFile }
      .forEach { processFile(it.key!!, it.value) }
    return emptyList()
  }

  private fun processFile(file: KSFile, providers: List<KSDeclaration>) {
    val markerName = "_${
      file.fileName.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_ProvidersMarker"

    val markerCode = buildString {
      if (file.packageName.asString().isNotEmpty()) {
        appendLine("package ${file.packageName.asString()}")
        appendLine()
      }

      appendLine("object $markerName")
    }

    environment.codeGenerator.createNewFile(
      dependencies = Dependencies(false, file),
      packageName = file.packageName.asString(),
      fileName = file.fileName.removeSuffix(".kt") + "Marker",
    ).write(markerCode.toByteArray())

    val injectablesLookupCode = buildString {
      appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
      appendLine()

      appendLine("package ${InjektFqNames.InjectablesPackage}")
      appendLine()

      for ((i, provider) in providers.withIndex()) {
        val key = provider.uniqueKey()

        appendLine("// $key")
        appendLine("fun `${InjektFqNames.InjectablesLookup.shortName()}`(")
        appendLine("  marker: ${file.packageName.asString()}.${markerName},")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        val finalKey = String(Base64.getEncoder().encode(key.toByteArray()))

        finalKey
          .filter { it.isLetterOrDigit() }
          .chunked(256)
          .forEachIndexed { index, value ->
            appendLine("  hash_${index}_$value: Int,")
          }

        appendLine(") = Unit")
        appendLine()
      }
    }

    environment.codeGenerator.createNewFile(
      dependencies = Dependencies(false, file),
      packageName = InjektFqNames.InjectablesPackage.asString(),
      fileName = "${file.fileName.removeSuffix(".kt")}Injectables_" +
          file.filePath.hashCode().toString().filter { it.isLetterOrDigit() },
    ).write(injectablesLookupCode.toByteArray())
  }

  private fun KSDeclaration.uniqueKey() = buildString {
    modifiers.forEach { append(it) }

    when (this@uniqueKey) {
      is KSClassDeclaration -> {
        superTypes.forEach { append(it.uniqueTypeKey()) }

        annotations
          .filter {
            it.annotationType.resolve().annotations.any {
              it.annotationType.resolve().declaration.qualifiedName?.asString() == InjektFqNames.Tag.asString()
            }
          }
          .forEach { append(it.annotationType.uniqueTypeKey()) }
      }
      is KSFunctionDeclaration -> {
        append(extensionReceiver?.uniqueTypeKey())
        parameters.forEach {
          append(it.type.uniqueTypeKey())
          append(it.hasDefault)
        }
        append(returnType?.uniqueTypeKey())
      }
      is KSPropertyDeclaration -> {
        append(extensionReceiver?.uniqueTypeKey())
        append(type.uniqueTypeKey())
      }
    }
  }

  private fun KSTypeReference.uniqueTypeKey() = resolve().safeAs<KSTypeImpl>()
    ?.kotlinType?.uniqueTypeKey() ?: "error"

  @AutoService(SymbolProcessorProvider::class)
  class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
      InjektSymbolProcessor(environment)
  }
}

fun KotlinType.uniqueTypeKey(depth: Int = 0): String {
  if (depth > 15) return ""
  return buildString {
    append(constructor.declarationDescriptor!!.fqNameSafe)
    arguments.forEachIndexed { index, typeArgument ->
      if (index == 0) append("<")
      append(typeArgument.type.fullyAbbreviatedType.uniqueTypeKey(depth + 1))
      if (index != arguments.lastIndex) append(", ")
      else append(">")
    }
    if (isMarkedNullable) append("?")
  }
}

val KotlinType.fullyAbbreviatedType: KotlinType
  get() {
    val abbreviatedType = getAbbreviatedType()
    return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
  }
