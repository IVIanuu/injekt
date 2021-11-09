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
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
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

    removedFiles?.forEach {
      fileMap.remove(it.absolutePath)?.forEach { outputFile ->
        File(outputFile).backupFile().delete()
      }
    }

    @Provide val ctx = Context(module, injektFqNames, bindingTrace)
    @Provide val analyzer = componentProvider.get<LazyTopDownAnalyzer>()

    files.forEach { file ->
      // Copy recursively, including last-modified-time of file and its parent dirs.
      //
      // `java.nio.file.Files.copy(path1, path2, options...)` keeps last-modified-time (if supported) according to
      // https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html
      fun copy(src: File, dst: File) {
        if (!dst.parentFile.exists())
          copy(src.parentFile, dst.parentFile)
        Files.copy(
          src.toPath(),
          dst.toPath(),
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING
        )
      }

      if (modifiedFiles == null || file.virtualFilePath in modifiedFiles.map { it.absolutePath }) {
        processFile(module, file).forEach { generatedFile ->
          fileMap.getOrPut(file.virtualFilePath) { mutableSetOf() } += generatedFile.absolutePath
          copy(generatedFile, generatedFile.backupFile())
        }
      } else {
        fileMap[file.virtualFilePath]?.forEach { outputFile ->
          copy(File(outputFile).backupFile(), File(outputFile))
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
            if (!declaration.isLocal && declaration.hasAnnotation(injektFqNames.provide) ||
              declaration.primaryConstructor?.hasAnnotation(injektFqNames.provide) == true ||
              declaration.secondaryConstructors.any { it.hasAnnotation(injektFqNames.provide) } ||
              declaration.hasAnnotation(injektFqNames.component) ||
              declaration.hasAnnotation(injektFqNames.entryPoint))
              addInjectable()
          }
          is KtNamedFunction -> {
            if (!declaration.isLocal && (declaration.hasAnnotation(injektFqNames.provide) ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.component) == true ||
                  declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.entryPoint) == true))
              addInjectable()
          }
          is KtProperty -> {
            if (!declaration.isLocal && (declaration.hasAnnotation(injektFqNames.provide) ||
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
    }_Marker"

    val markerCode = buildString {
      appendLine("package ${file.packageFqName}")

      appendLine()

      appendLine("object $markerName")
    }

    val markerFile = srcDir.resolve(
      (file.packageFqName.pathSegments().joinToString("/") +
          "/${file.name.removeSuffix(".kt")}Marker.kt")
    )
    markerFile.parentFile.mkdirs()
    markerFile.createNewFile()
    markerFile.writeText(markerCode)

    val indicesCode = buildString {
      appendLine("package ${injektFqNames.indexPackage}")

      for ((i, injectable) in injectables.withIndex()) {
        val functionName = injectablesLookupName(file.packageFqName)

        appendLine()
        appendLine("fun $functionName(")
        appendLine("  marker: ${file.packageFqName.child(markerName.asNameId())},")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        fun DeclarationDescriptor.hash(): String = when (this) {
          is ClassDescriptor ->
            "class" +
                fqNameSafe +
                visibility +
                annotations.joinToString { it.type.toTypeRef().renderToString() } +
                uniqueKey() +
                constructors.joinToString { it.hash() }
          is CallableDescriptor ->
            if (this is FunctionDescriptor) "function" else "property" +
                fqNameSafe +
                visibility +
                callContext() +
                annotations.joinToString { it.type.toTypeRef().renderToString() } +
                safeAs<PropertyGetterDescriptor>()?.annotations
                  ?.joinToString { it.type.toTypeRef().renderToString() }.orEmpty() +
                uniqueKey()
          else -> throw AssertionError()
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

    val indicesFile = srcDir.resolve(
      (file.packageFqName.pathSegments().joinToString("/") +
          "/${file.name.removeSuffix(".kt")}Indices.kt")
    )
    indicesFile.parentFile.mkdirs()
    indicesFile.createNewFile()
    indicesFile.writeText(indicesCode)

    return listOf(markerFile, indicesFile)
  }
}
