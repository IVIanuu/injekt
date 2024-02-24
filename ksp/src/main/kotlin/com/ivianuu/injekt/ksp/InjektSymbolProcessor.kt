/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ivianuu.injekt.ksp

import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

class InjektSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(InjektFqNames.Provide.asFqNameString(), true)
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
        appendLine("fun `${InjektFqNames.InjectablesLookup.callableName}`(")
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
    annotations.forEach { append(it.annotationType.uniqueTypeKey()) }

    when (this@uniqueKey) {
      is KSClassDeclaration -> {
        superTypes.forEach { append(it.uniqueTypeKey()) }
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

  private fun KSTypeReference.uniqueTypeKey(): String = buildString {
    fun KSType.append() {
      annotations.forEach { it.annotationType.resolve().append() }
      append(declaration.qualifiedName!!.asString())
      arguments.forEach {
        append(it.type?.uniqueTypeKey())
        append(it.variance)
      }
      append(isMarkedNullable)
    }

    resolve().append()
  }

  @AutoService(SymbolProcessorProvider::class)
  class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
      InjektSymbolProcessor(environment)
  }
}
