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
import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.subInjectablesLookupName
import com.ivianuu.injekt.compiler.uniqueTypeKey
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.TypeReference
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
    if (providers.isEmpty()) return

    val markerName = "_${
      file.fileName.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_ProvidersMarker"

    generateProviders(file, markerName, providers)
    generateSubProviders(file, markerName, providers)
  }

  private fun generateProviders(
    file: KSFile,
    markerName: String,
    providers: List<KSDeclaration>
  ) {
    val code = buildString {
      appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
      appendLine()

      if (file.packageName.asString().isNotEmpty()) {
        appendLine("package ${file.packageName.asString()}")
        appendLine()
      }

      appendLine("object $markerName")

      appendLine()

      for ((i, provider) in providers.withIndex()) {
        val key = provider.uniqueKey()

        appendLine("// $key")

        appendLine("fun `$injectablesLookupName`(")
        appendLine(" marker: $markerName,")
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

        appendLine(") {")
        appendLine("}")
        appendLine()
      }
    }

    environment.codeGenerator.createNewFile(
      dependencies = Dependencies(false, file),
      packageName = file.packageName.asString(),
      fileName = file.fileName.removeSuffix(".kt") + "Providers",
    ).write(code.toByteArray())
  }

  private fun generateSubProviders(
    file: KSFile,
    markerName: String,
    providers: List<KSDeclaration>
  ) {
    if (file.packageName.asString().isEmpty()) return
    var current = FqName(file.packageName.asString())
    while (!current.isRoot) {
      current = current.parent()

      val code = buildString {
        appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
        appendLine()

        if (!current.isRoot) {
          appendLine("package $current")
          appendLine()
        }

        for ((i, provider) in providers.withIndex()) {
          val key = provider.uniqueKey()

          appendLine("// $key")
          appendLine("fun `$subInjectablesLookupName`(")
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

          appendLine(") {")
          appendLine("}")
          appendLine()
        }
      }

      environment.codeGenerator.createNewFile(
        dependencies = Dependencies(false, file),
        packageName = current.asString(),
        fileName = "${file.fileName.removeSuffix(".kt")}SubProviders_" +
            file.filePath.hashCode().toString().filter { it.isLetterOrDigit() },
      ).write(code.toByteArray())
    }
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
