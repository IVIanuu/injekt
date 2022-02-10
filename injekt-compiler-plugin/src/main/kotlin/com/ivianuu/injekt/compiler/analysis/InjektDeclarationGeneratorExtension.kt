/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.*
import java.io.*
import java.nio.file.*
import java.util.*

class InjektDeclarationGeneratorExtension(
  private val srcDir: File,
  cacheDir: File,
  private val modifiedFiles: List<File>?,
  private val removedFiles: List<File>?,
  private val withCompilation: Boolean
) : AnalysisHandlerExtension {
  private val backupDir = cacheDir.resolve("backups")

  private val fileMapFile = cacheDir.resolve("file-map")

  private val fileMap = (if (fileMapFile.exists()) fileMapFile.readText() else "")
    .split("\n")
    .filter { it.isNotEmpty() }
    .map { entry ->
      val tmp = entry.split("=:=")
      tmp[0] to tmp[1]
    }
    .groupBy { it.first }
    .mapValues {
      it.value
        .map { it.second }
        .toMutableSet()
    }
    .toMutableMap()

  private var finished = false

  override fun doAnalysis(
    project: Project,
    module: ModuleDescriptor,
    projectContext: ProjectContext,
    files: Collection<KtFile>,
    bindingTrace: BindingTrace,
    componentProvider: ComponentProvider
  ): AnalysisResult? {
    if (finished) {
      if (!withCompilation)
        throw IllegalStateException("KSP is re-entered unexpectedly.")
      return null
    }

    finished = true

    fun File.backupFile() = File(backupDir, toRelativeString(srcDir))

    ((removedFiles ?: emptyList()) + (modifiedFiles ?: emptyList())).forEach { changedFile ->
      fileMap.remove(changedFile.absolutePath)?.forEach { outputFile ->
        File(outputFile).run {
          delete()
          backupFile().delete()
        }
      }
    }

    val ctx = Context(module, bindingTrace)

    files.forEach { file ->
      // Copy recursively, including last-modified-time of file and its parent dirs.
      //
      // `java.nio.file.Files.copy(path1, path2, options...)` keeps last-modified-time (if supported) according to
      // https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html
      fun copy(src: File, dst: File, overwrite: Boolean) {
        if (!dst.parentFile.exists())
          copy(src.parentFile, dst.parentFile, false)
        if (overwrite) {
          Files.copy(
            src.toPath(),
            dst.toPath(),
            StandardCopyOption.COPY_ATTRIBUTES,
            StandardCopyOption.REPLACE_EXISTING
          )
        } else {
          dst.delete()
          Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
      }

      if (modifiedFiles == null || file.virtualFilePath in modifiedFiles.map { it.absolutePath }) {
        processFile(module, file, ctx).forEach { generatedFile ->
          fileMap.getOrPut(file.virtualFilePath) { mutableSetOf() } += generatedFile.absolutePath
          copy(generatedFile, generatedFile.backupFile(), true)
        }
      } else {
        fileMap[file.virtualFilePath]?.forEach { outputFile ->
          copy(File(outputFile).backupFile(), File(outputFile), false)
        }
      }
    }

    if (fileMap.isNotEmpty()) {
      fileMapFile.parentFile.mkdirs()
      fileMapFile.createNewFile()
      fileMapFile.writeText(
        buildString {
          fileMap
            .forEach { (dependency, dependents) ->
              dependents.forEach { dependent ->
                appendLine("${dependency}=:=${dependent}")
              }
            }
        }
      )
    } else {
      fileMapFile.delete()
    }

    return if (!withCompilation) {
      AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
    } else {
      AnalysisResult.RetryWithAdditionalRoots(
        BindingContext.EMPTY,
        module,
        emptyList(),
        listOf(srcDir),
        emptyList()
      )
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun processFile(
    module: ModuleDescriptor,
    file: KtFile,
    ctx: Context
  ): List<File> {
    val injectables = mutableListOf<KtNamedDeclaration>()

    file.accept(
      namedDeclarationRecursiveVisitor { declaration ->
        if (declaration.fqName == null) return@namedDeclarationRecursiveVisitor

        when (declaration) {
          is KtClassOrObject -> {
            if (!declaration.isLocal &&
              (declaration.hasAnnotation(InjektFqNames.Provide) ||
                  declaration.primaryConstructor?.hasAnnotation(InjektFqNames.Provide) == true ||
                  declaration.secondaryConstructors.any { it.hasAnnotation(InjektFqNames.Provide) }))
                    injectables += declaration
          }
          is KtNamedFunction -> {
            if (!declaration.isLocal && declaration.hasAnnotation(InjektFqNames.Provide))
              injectables += declaration
          }
          is KtProperty -> {
            if (!declaration.isLocal && declaration.hasAnnotation(InjektFqNames.Provide))
              injectables += declaration
          }
        }
      }
    )

    if (injectables.isEmpty()) return emptyList()

    val markerName = "_${
      module.moduleName(ctx)
        .filter { it.isLetterOrDigit() }
    }_${
      file.name.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_ProvidersMarker"

    return buildList {
      this += injectablesFile(file, markerName, injectables, ctx)
      subInjectableFiles(file, markerName, injectables, ctx)
      this += indicesFile(file, markerName, injectables, ctx)
    }
  }

  private fun injectablesFile(
    file: KtFile,
    markerName: String,
    injectables: List<KtNamedDeclaration>,
    ctx: Context
  ): File {
    val injectablesCode = buildString {
      appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
      appendLine()

      if (!file.packageFqName.isRoot) {
        appendLine("package ${file.packageFqName}")
        appendLine()
      }

      appendLine("object $markerName")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        appendLine("fun $injectablesLookupName(")
        appendLine(" marker: $markerName,")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        val hash = injectable.hash()

        val finalHash = String(Base64.getEncoder().encode(hash.toByteArray()))

        finalHash
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

    val injectablesFile = srcDir.resolve(
      (if (!file.packageFqName.isRoot)
        "${file.packageFqName.pathSegments().joinToString("/")}/"
      else "") + "${file.name.removeSuffix(".kt")}Injectables.kt"
    )
    injectablesFile.parentFile.mkdirs()
    injectablesFile.createNewFile()
    injectablesFile.writeText(injectablesCode)
    return injectablesFile
  }

  private fun MutableList<File>.subInjectableFiles(
    file: KtFile,
    markerName: String,
    injectables: List<KtNamedDeclaration>,
    ctx: Context
  ) {
    if (file.packageFqName.isRoot) return
    var current = file.packageFqName
    while (!current.isRoot) {
      current = current.parent()

      val injectablesCode = buildString {
        appendLine("@file:Suppress(\"unused\", \"UNUSED_PARAMETER\")")
        appendLine()

        if (!current.isRoot) {
          appendLine("package $current")
          appendLine()
        }

        for ((i, injectable) in injectables.withIndex()) {
          appendLine("fun $subInjectablesLookupName(")
          appendLine("  marker: ${file.packageFqName.child(markerName.asNameId())},")
          repeat(i + 1) {
            appendLine("  index$it: Byte,")
          }

          val hash = injectable.hash()

          val finalHash = String(Base64.getEncoder().encode(hash.toByteArray()))

          finalHash
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

      val injectablesKeyHash = injectables
        .joinToString { it.hash() }
        .hashCode()
        .toString()
        .filter { it.isLetterOrDigit() }

      val subInjectablesFile = srcDir.resolve(
        (if (!current.isRoot)
          "${current.pathSegments().joinToString("/")}/"
        else "") + "${file.name.removeSuffix(".kt")}SubInjectables_${injectablesKeyHash}.kt"
      )
      subInjectablesFile.parentFile.mkdirs()
      subInjectablesFile.createNewFile()
      subInjectablesFile.writeText(injectablesCode)
      this += subInjectablesFile
    }
  }

  private fun indicesFile(
    file: KtFile,
    markerName: String,
    injectables: List<KtNamedDeclaration>,
    ctx: Context
  ): File {
    val indicesCode = buildString {
      appendLine("@file:Suppress(\"INVISIBLE_REFERENCE\", \"INVISIBLE_MEMBER\", \"unused\", \"UNUSED_PARAMETER\")")
      appendLine()

      appendLine("package ${InjektFqNames.IndicesPackage}")
      appendLine()

      appendLine("import ${InjektFqNames.Index}")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        append("@Index(fqName = ")
        appendLine("\"${
          if (injectable is KtConstructor<*>) injectable.getContainingClassOrObject().fqName!!
          else injectable.fqName!!
        }\")")
        appendLine("fun index(")
        appendLine("  marker: ${file.packageFqName.child(markerName.asNameId())},")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }
        appendLine(") {")
        appendLine("}")
        appendLine()
      }
    }

    val injectablesKeyHash = injectables
      .joinToString { it.hash() }
      .hashCode()
      .toString()
      .filter { it.isLetterOrDigit() }

    val indicesFile = srcDir.resolve(
      "${InjektFqNames.IndicesPackage.pathSegments().joinToString("/")}/" +
          (if (file.packageFqName.isRoot) "" else file.packageFqName.pathSegments().joinToString("_").plus("_")) +
          "${file.name.removeSuffix(".kt")}Indices_${injectablesKeyHash}.kt"
    )
    indicesFile.parentFile.mkdirs()
    indicesFile.createNewFile()
    indicesFile.writeText(indicesCode)

    return indicesFile
  }

  private fun KtDeclaration.hash(): String = when (this) {
    is KtClassOrObject ->
      "class" +
          name.orEmpty() +
          modifierList?.text.orEmpty() +
          annotationEntries.joinToString { it.text } +
          primaryConstructor
            ?.let {
              "primary constructor" +
                  it.valueParameters
                    .joinToString { it.text }
            } + secondaryConstructors
        .mapIndexed { index, it ->
          "secondary_constructor_$index" +
              it.valueParameters
                .joinToString(it.text)
        } + superTypeListEntries
        .joinToString { it.text }
    is KtFunction ->
      "function" +
          name.orEmpty() +
          modifierList?.text.orEmpty() +
          annotationEntries.joinToString { it.text } +
          receiverTypeReference?.text.orEmpty() +
          valueParameters.joinToString { it.text } +
          typeReference?.text.orEmpty()
    is KtProperty ->
      "property" +
          name.orEmpty() +
          modifierList?.text.orEmpty() +
          annotationEntries.joinToString { it.text } +
          receiverTypeReference?.text.orEmpty() +
          getter?.annotationEntries?.joinToString { it.text }.orEmpty() +
          typeReference?.text.orEmpty()
    else -> throw AssertionError()
  }
}
