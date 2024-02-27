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

  private fun processFile(file: KSFile, injectables: List<KSDeclaration>) =
    environment.codeGenerator.createNewFile(
      dependencies = Dependencies(false, file),
      packageName = InjektFqNames.InjectablesPackage.asString(),
      fileName = "${file.fileName.removeSuffix(".kt")}Injectables_" +
          file.filePath.hashCode().toString().filter { it.isLetterOrDigit() },
    ).write(
      buildString {
        val markerName = "${file.packageName.asString().replace(".", "__")}___${
          file.fileName.removeSuffix(".kt")
            .substringAfterLast(".")
            .substringAfterLast("/")
        }_InjectablesMarker_${file.filePath.hashCode().toString().filter { it.isLetterOrDigit() }}"

        appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
        appendLine()

        appendLine("package ${InjektFqNames.InjectablesPackage}")
        appendLine()

        appendLine("object $markerName")
        appendLine()

        for ((i, injectable) in injectables.withIndex()) {
          val hash = injectable.declarationHash()

          appendLine("// $hash")
          appendLine("fun `${InjektFqNames.InjectablesLookup.callableName}`(")
          appendLine("  marker: $markerName,")
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
      .toByteArray()
    )

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
    val visited = mutableSetOf<KSType>()
    fun KSType.append() {
      if (!visited.add(this)) return
      annotations.forEach { it.annotationType.resolve().append() }
      append(declaration.qualifiedName?.asString())
      arguments.forEach { argument ->
        append(argument.type?.let { append(it) })
        append(argument.variance)
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
