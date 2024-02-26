/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ksp

import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

class InjektSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(InjektFqNames.Provide.asFqNameString(), false)
      .filterIsInstance<KSDeclaration>()
      .groupBy { it.containingFile }
      .forEach { processFile(it.key!!, it.value) }
    return emptyList()
  }

  private fun processFile(file: KSFile, injectables: List<KSDeclaration>) {
    val markerName = "_${
      file.fileName.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_InjectablesMarker"

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

      for ((i, injectable) in injectables.withIndex()) {
        val hash = injectable.declarationHash()

        appendLine("// $hash")
        appendLine("fun `${InjektFqNames.InjectablesLookup.callableName}`(")
        appendLine("  marker: ${file.packageName.asString()}.${markerName},")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        val finalHash = String(Base64.getEncoder().encode(hash.toByteArray()))

        finalHash
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

  private fun KSDeclaration.declarationHash(): String = buildString {
    modifiers.forEach { append(it) }
    annotations.forEach { append(it.annotationType.typeHash()) }

    when (this@declarationHash) {
      is KSClassDeclaration -> {
        superTypes.forEach { append(it.typeHash()) }
        primaryConstructor?.declarationHash()?.let { append(it) }
      }
      is KSFunctionDeclaration -> {
        append(extensionReceiver?.typeHash())
        parameters.forEach {
          append(it.type.typeHash())
          append(it.hasDefault)
        }
        append(returnType?.typeHash())
      }
      is KSPropertyDeclaration -> {
        append(extensionReceiver?.typeHash())
        append(type.typeHash())
      }
    }
  }

  private fun KSTypeReference.typeHash(): String = buildString {
    fun KSType.append() {
      annotations.forEach { it.annotationType.resolve().append() }
      append(declaration.qualifiedName!!.asString())
      arguments.forEach {
        append(it.type?.typeHash())
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
