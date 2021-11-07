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
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
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
import org.jetbrains.kotlin.psi.psiUtil.toVisibility
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
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

  private fun processFile(module: ModuleDescriptor, file: KtFile): List<File> {
    val injectables = mutableListOf<KtNamedDeclaration>()

    file.accept(
      namedDeclarationRecursiveVisitor { declaration ->
        if (declaration.fqName == null) return@namedDeclarationRecursiveVisitor

        val visibility =
          declaration.visibilityModifierTypeOrDefault().toVisibility()

        if (visibility == DescriptorVisibilities.PUBLIC ||
            visibility == DescriptorVisibilities.INTERNAL ||
            visibility == DescriptorVisibilities.PROTECTED) {
          when (declaration) {
            is KtClassOrObject -> {
              if (!declaration.isLocal && declaration.hasAnnotation(injektFqNames.provide) ||
                declaration.primaryConstructor?.hasAnnotation(injektFqNames.provide) == true ||
                declaration.secondaryConstructors.any { it.hasAnnotation(injektFqNames.provide) } ||
                declaration.hasAnnotation(injektFqNames.component) ||
                declaration.hasAnnotation(injektFqNames.entryPoint))
                  injectables += declaration
            }
            is KtNamedFunction -> {
              if (!declaration.isLocal && (declaration.hasAnnotation(injektFqNames.provide) ||
                    declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.component) == true ||
                    declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.entryPoint) == true))
                injectables += declaration
            }
            is KtProperty -> {
              if (!declaration.isLocal && (declaration.hasAnnotation(injektFqNames.provide) ||
                    declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.component) == true ||
                    declaration.getParentOfType<KtClass>(false)?.hasAnnotation(injektFqNames.entryPoint) == true))
                injectables += declaration
            }
          }
        }
      }
    )

    if (injectables.isEmpty()) return emptyList()

    val code = buildString {
      appendLine("package ${file.packageFqName}")

      appendLine()

      @Provide val ctx = Context(module, injektFqNames, null)
      val markerName = "_${
        module.moduleName()
          .filter { it.isLetterOrDigit() }
      }_${
        file.name.removeSuffix(".kt")
          .substringAfterLast(".")
          .substringAfterLast("/")
      }_ProvidersMarker"

      appendLine("object $markerName")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        val functionName = injectablesLookupName

        appendLine("fun $functionName(")
        appendLine("  marker: $markerName,")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        val hash = when (injectable) {
          is KtClassOrObject ->
            "class" +
                injectable.name.orEmpty() +
                injectable.visibilityModifier()?.text.orEmpty() +
                injectable.annotationEntries.joinToString { it.text } +
                injectable.primaryConstructor
                  ?.let {
                    "primary constructor" +
                        it.valueParameters
                          .joinToString { it.text }
                  } + injectable.secondaryConstructors
              .mapIndexed { index, it ->
                "secondary_constructor_$index" +
                    it.valueParameters
                      .joinToString(it.text)
              } + injectable.superTypeListEntries
              .joinToString { it.text }
          is KtFunction ->
            "function" +
                injectable.name.orEmpty() +
                injectable.visibilityModifier()?.text.orEmpty() +
                injectable.hasModifier(KtTokens.SUSPEND_KEYWORD).toString() +
                injectable.modifierList +
                injectable.annotationEntries.joinToString { it.text } +
                injectable.receiverTypeReference?.text.orEmpty() +
                injectable.valueParameters
                  .joinToString { it.text } +
                injectable.typeReference?.text.orEmpty()
          is KtProperty ->
            "property" +
                injectable.name.orEmpty() +
                injectable.visibilityModifier()?.text.orEmpty() +
                injectable.annotationEntries.joinToString { it.text } +
                injectable.getter?.annotationEntries?.joinToString { it.text }.orEmpty() +
                injectable.receiverTypeReference?.text.orEmpty() +
                injectable.typeReference?.text.orEmpty()
          else -> throw AssertionError()
        }

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

    val injectableHashFile = srcDir.resolve(
      (file.packageFqName.pathSegments().joinToString("/") +
          "/${file.name.removeSuffix(".kt")}Hashes.kt")
    )
    injectableHashFile.parentFile.mkdirs()
    injectableHashFile.createNewFile()
    injectableHashFile.writeText(code)

    return listOf(injectableHashFile)
  }
}
