/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.subInjectablesLookupName
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BodyResolver
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
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

  private val logOutputFile = cacheDir.resolve("declaration-gen-log-${SimpleDateFormat("dd-MM-yyyy-HH-mm-ss-SSS").format(
    Date(System.currentTimeMillis())
  )}")
  private val logOutput = StringBuilder()

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

    logOutput.appendLine("files:")
    files.forEach {
      logOutput.appendLine("file: ${it.virtualFilePath}")
    }

    modifiedFiles?.let {
      logOutput.appendLine("modified files:")
      it.forEach {
        logOutput.appendLine("modified file: $it")
      }
    }

    removedFiles?.let {
      logOutput.appendLine("removed files:")
      it.forEach {
        logOutput.appendLine("removed file: $it")
      }
    }

    logOutput.appendLine("files map: $fileMap")

    fun File.backupFile() = File(backupDir, toRelativeString(srcDir))

    ((removedFiles ?: emptyList()) + (modifiedFiles ?: emptyList())).forEach { changedFile ->
      fileMap.remove(changedFile.absolutePath)?.forEach { outputFile ->
        logOutput.appendLine("delete file changed $changedFile output $outputFile")
        File(outputFile).run {
          delete()
          backupFile().delete()
        }
      }
    }

    val ctx = Context(module, bindingTrace)

    for (file in files) {
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
        logOutput.appendLine("process file ${file.virtualFilePath}")
        processFile(module, file, ctx, componentProvider.get(), componentProvider.get()).forEach { generatedFile ->
          fileMap.getOrPut(file.virtualFilePath) { mutableSetOf() } += generatedFile.absolutePath
          copy(generatedFile, generatedFile.backupFile(), true)
        }
      } else {
        logOutput.appendLine("skip and restore file ${file.virtualFilePath}")
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

    logOutputFile.parentFile.mkdirs()
    logOutputFile.createNewFile()
    logOutputFile.writeText(logOutput.toString())

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
    ctx: Context,
    resolveSession: ResolveSession,
    bodyResolver: BodyResolver
  ): List<File> {
    val injectables = mutableListOf<DeclarationDescriptor>()

    file.accept(
      declarationRecursiveVisitor { declaration ->
        fun KtAnnotated.isProvide() =
          annotationEntries.any { it.shortName == InjektFqNames.Provide.shortName() }

        when (declaration) {
          is KtClassOrObject -> {
            if (!declaration.isLocal && (declaration.isProvide()))
              injectables += resolveDeclaration(declaration, resolveSession, bodyResolver, ctx)!!
          }
          is KtConstructor<*> -> {
            if (!declaration.isLocal && (declaration.isProvide()))
              injectables += resolveDeclaration(declaration, resolveSession, bodyResolver, ctx)!!
          }
          is KtNamedFunction -> {
            if (!declaration.isLocal && declaration.isProvide())
              injectables += resolveDeclaration(declaration, resolveSession, bodyResolver, ctx)!!
          }
          is KtProperty -> {
            if (!declaration.isLocal && declaration.isProvide())
              injectables += resolveDeclaration(declaration, resolveSession, bodyResolver, ctx)!!
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
    }
  }

  private fun injectablesFile(
    file: KtFile,
    markerName: String,
    injectables: List<DeclarationDescriptor>,
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
        val key = injectable.uniqueKey(ctx)

        appendLine("// $key")

        appendLine("fun $injectablesLookupName(")
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
    injectables: List<DeclarationDescriptor>,
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
          val key = injectable.uniqueKey(ctx)

          appendLine("// $key")
          appendLine("fun $subInjectablesLookupName(")
          appendLine("  marker: ${file.packageFqName.child(markerName.asNameId())},")
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

      val injectablesKeyHash = injectables
        .joinToString { it.uniqueKey(ctx) }
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

  private fun resolveDeclaration(
    declaration: KtDeclaration,
    resolveSession: ResolveSession,
    bodyResolver: BodyResolver,
    ctx: Context
  ): DeclarationDescriptor? {
    return if (KtPsiUtil.isLocal(declaration)) {
      resolveDeclarationForLocal(declaration, resolveSession, bodyResolver, ctx)
      ctx.trace!!.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
    } else {
      resolveSession.resolveToDescriptor(declaration)
    }
  }

  private fun resolveDeclarationForLocal(
    localDeclaration: KtDeclaration,
    resolveSession: ResolveSession,
    bodyResolver: BodyResolver,
    ctx: Context
  ) {
    var declaration = KtStubbedPsiUtil.getContainingDeclaration(localDeclaration) ?: return
    while (KtPsiUtil.isLocal(declaration))
      declaration = KtStubbedPsiUtil.getContainingDeclaration(declaration)!!

    val containingFD = resolveSession.resolveToDescriptor(declaration).also {
      ForceResolveUtil.forceResolveAllContents(it)
    }

    if (declaration is KtNamedFunction) {
      val dataFlowInfo = DataFlowInfo.EMPTY
      val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
      bodyResolver.resolveFunctionBody(
        dataFlowInfo,
        ctx.trace!!,
        declaration,
        containingFD as FunctionDescriptor,
        scope,
        null
      )
    }
  }
}

