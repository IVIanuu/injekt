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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.renderToString
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.shouldPersistInfo
import com.ivianuu.injekt.compiler.subInjectablesLookupName
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.toVisibility
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

class InjektDeclarationGeneratorExtension(
  private val srcDir: File,
  cacheDir: File,
  private val modifiedFiles: List<File>?,
  private val removedFiles: List<File>?,
  private val withCompilation: Boolean,
  @Inject private val injektFqNames: InjektFqNames
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

    @Provide val ctx = Context(module, injektFqNames, bindingTrace)
    @Provide val analyzer = componentProvider.get<LazyTopDownAnalyzer>()

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
          Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
      }

      if (modifiedFiles == null || file.virtualFilePath in modifiedFiles.map { it.absolutePath }) {
        processFile(module, file).forEach { generatedFile ->
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

    return if (finished && !withCompilation) {
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
    @Inject ctx: Context,
    lazyTopDownAnalyzer: LazyTopDownAnalyzer
  ): List<File> {
    val injectables = mutableListOf<DeclarationDescriptor>()

    file.accept(
      namedDeclarationRecursiveVisitor { declaration ->
        if (declaration.fqName == null) return@namedDeclarationRecursiveVisitor

        fun addInjectable() {
          lazyTopDownAnalyzer.analyzeDeclarations(
            TopDownAnalysisMode.TopLevelDeclarations,
            listOf(declaration)
          )
          injectables += declaration.descriptor<DeclarationDescriptor>()!!
        }

        when (declaration) {
          is KtClassOrObject -> {
            if (!declaration.isLocal &&
              declaration.visibilityModifierTypeOrDefault().toVisibility().shouldPersistInfo() &&
              (declaration.hasAnnotation(injektFqNames.provide) ||
                  declaration.primaryConstructor?.hasAnnotation(injektFqNames.provide) == true ||
                  declaration.secondaryConstructors.any { it.hasAnnotation(injektFqNames.provide) } ||
                  declaration.hasAnnotation(injektFqNames.component) ||
                  declaration.hasAnnotation(injektFqNames.entryPoint)))
              addInjectable()
          }
          is KtNamedFunction -> {
            if (!declaration.isLocal &&
              declaration.visibilityModifierTypeOrDefault().toVisibility().shouldPersistInfo() &&
              (declaration.hasAnnotation(injektFqNames.provide) ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.component) == true ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.entryPoint) == true))
              addInjectable()
          }
          is KtProperty -> {
            if (!declaration.isLocal &&
              declaration.visibilityModifierTypeOrDefault().toVisibility().shouldPersistInfo() &&
              (declaration.hasAnnotation(injektFqNames.provide) ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.component) == true ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.entryPoint) == true))
              addInjectable()
          }
        }
      }
    )

    if (injectables.isEmpty()) return emptyList()

    val markerName = "_${
      module.moduleName()
        .filter { it.isLetterOrDigit() }
    }_${
      file.name.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_ProvidersMarker"

    return buildList {
      this += injectablesFile(file, markerName, injectables)
      subInjectableFiles(file, markerName, injectables)
      this += indicesFile(file, markerName, injectables)
    }
  }

  private fun injectablesFile(
    file: KtFile,
    markerName: String,
    injectables: List<DeclarationDescriptor>,
    @Inject ctx: Context
  ): File {
    val injectablesCode = buildString {
      if (!file.packageFqName.isRoot) {
        appendLine("package ${file.packageFqName}")
        appendLine()
      }

      appendLine("object $markerName")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        appendLine("@Suppress(\"unused\") fun $injectablesLookupName(")
        appendLine(" @Suppress(\"UNUSED_PARAMETER\") marker: $markerName,")
        repeat(i + 1) {
          appendLine("  @Suppress(\"UNUSED_PARAMETER\") index$it: Byte,")
        }

        fun DeclarationDescriptor.hash(): String = when (this) {
          is ClassDescriptor ->
            uniqueKey() +
                visibility +
                annotations.joinToString { it.type.toTypeRef().renderToString() } +
                constructors.joinToString { it.hash() }
          is CallableDescriptor ->
            uniqueKey() +
                visibility +
                callContext() +
                annotations.joinToString { it.type.toTypeRef().renderToString() } +
                safeAs<PropertyDescriptor>()
                  ?.let { it.getter?.hash().orEmpty() + it.setter?.hash().orEmpty() }
          else -> throw AssertionError()
        }

        val hash = injectable.hash()

        val finalHash = String(Base64.getEncoder().encode(hash.toByteArray()))

        finalHash
          .filter { it.isLetterOrDigit() }
          .chunked(256)
          .forEachIndexed { index, value ->
            appendLine("  @Suppress(\"UNUSED_PARAMETER\") hash_${index}_$value: Int,")
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
    @Inject ctx: Context
  ) {
    if (file.packageFqName.isRoot) return
    var current = file.packageFqName
    while (!current.isRoot) {
      current = current.parent()

      val injectablesCode = buildString {
        if (!current.isRoot) {
          appendLine("package $current")
          appendLine()
        }

        for ((i, injectable) in injectables.withIndex()) {
          appendLine("@Suppress(\"unused\") fun $subInjectablesLookupName(")
          appendLine("  @Suppress(\"UNUSED_PARAMETER\") marker: ${file.packageFqName.child(markerName.asNameId())},")
          repeat(i + 1) {
            appendLine("  @Suppress(\"UNUSED_PARAMETER\") index$it: Byte,")
          }

          fun DeclarationDescriptor.hash(): String = when (this) {
            is ClassDescriptor ->
              uniqueKey() +
                  visibility +
                  annotations.joinToString { it.type.toTypeRef().renderToString() } +
                  constructors.joinToString { it.hash() }
            is CallableDescriptor ->
              uniqueKey() +
                  visibility +
                  callContext() +
                  annotations.joinToString { it.type.toTypeRef().renderToString() } +
                  safeAs<PropertyDescriptor>()
                    ?.let { it.getter?.hash().orEmpty() + it.setter?.hash().orEmpty() }
            else -> throw AssertionError()
          }

          val hash = injectable.hash()

          val finalHash = String(Base64.getEncoder().encode(hash.toByteArray()))

          finalHash
            .filter { it.isLetterOrDigit() }
            .chunked(256)
            .forEachIndexed { index, value ->
              appendLine("  @Suppress(\"UNUSED_PARAMETER\") hash_${index}_$value: Int,")
            }

          appendLine(") {")
          appendLine("}")
          appendLine()
        }
      }

      val injectablesKeyHash = injectables
        .joinToString { it.uniqueKey() }
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
    injectables: List<DeclarationDescriptor>,
    @Inject ctx: Context
  ): File {
    val indicesCode = buildString {
      appendLine("package ${injektFqNames.indicesPackage}")
      appendLine()

      appendLine("import ${injektFqNames.index}")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        appendLine("@Index(")
        appendLine("  fqName = \"${
          if (injectable is ConstructorDescriptor) injectable.constructedClass.fqNameSafe
          else injectable.fqNameSafe
        }\",")
        appendLine("  uniqueKey = \"${injectable.uniqueKey()}\"")
        appendLine(")")
        appendLine("@Suppress(\"unused\") fun index(")
        appendLine("  @Suppress(\"UNUSED_PARAMETER\") marker: ${file.packageFqName.child(markerName.asNameId())},")
        repeat(i + 1) {
          appendLine("  @Suppress(\"UNUSED_PARAMETER\") index$it: Byte,")
        }
        appendLine(") {")
        appendLine("}")
        appendLine()
      }
    }

    val injectablesKeyHash = injectables
      .joinToString { it.uniqueKey() }
      .hashCode()
      .toString()
      .filter { it.isLetterOrDigit() }

    val indicesFile = srcDir.resolve(
      "${injektFqNames.indicesPackage.pathSegments().joinToString("/")}/" +
          (if (file.packageFqName.isRoot) "" else file.packageFqName.pathSegments().joinToString("_").plus("_")) +
          "${file.name.removeSuffix(".kt")}Indices_${injectablesKeyHash}.kt"
    )
    indicesFile.parentFile.mkdirs()
    indicesFile.createNewFile()
    indicesFile.writeText(indicesCode)

    return indicesFile
  }
}
