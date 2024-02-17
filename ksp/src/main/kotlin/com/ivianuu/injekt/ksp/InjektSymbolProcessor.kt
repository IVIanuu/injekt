/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ksp

import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

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

  private fun KSDeclaration.uniqueKey(): String = buildString {
    modifiers.forEach { append(it) }

    when (this@uniqueKey) {
      is KSClassDeclaration -> {
        superTypes.forEach { append(it.uniqueTypeKey()) }
        annotations
          .forEach { append(it.annotationType.uniqueTypeKey()) }
        primaryConstructor?.uniqueKey()?.let { append(it) }
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
