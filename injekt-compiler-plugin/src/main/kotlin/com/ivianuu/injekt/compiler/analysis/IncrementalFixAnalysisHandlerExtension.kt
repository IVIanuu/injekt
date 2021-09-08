package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.updatePrivateFinalField
import java.util.Base64
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.openapi.editor.impl.DocumentImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.toVisibility
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

class IncrementalFixAnalysisHandlerExtension : AnalysisHandlerExtension {
  private var appliedFix = false

  override fun doAnalysis(
    project: Project,
    module: ModuleDescriptor,
    projectContext: ProjectContext,
    files: Collection<KtFile>,
    bindingTrace: BindingTrace,
    componentProvider: ComponentProvider
  ): AnalysisResult? {
    if (!appliedFix) {
      appliedFix = true

      val newFiles = files.map { processFile(project, module, it) }
      files as MutableList<KtFile>
      files.clear()
      files += newFiles

      return AnalysisResult.RetryWithAdditionalRoots(
        BindingContext.EMPTY,
        module,
        emptyList(),
        emptyList(), emptyList(),
        true
      )
    }

    return null
  }

  private fun processFile(
    project: Project,
    module: ModuleDescriptor,
    file: KtFile
  ): KtFile {
    if (file.text.contains(FILE_MARKER_COMMENT))
      return file

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
              if (!declaration.isLocal && declaration.hasAnnotation(InjektFqNames.Provide) ||
                declaration.primaryConstructor?.hasAnnotation(InjektFqNames.Provide) == true ||
                declaration.secondaryConstructors.any {
                  it.hasAnnotation(InjektFqNames.Provide)
                })
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
      }
    )

    if (injectables.isEmpty()) return file

    val code = buildString {
      appendLine(file.text)

      appendLine()
      appendLine(FILE_MARKER_COMMENT)
      appendLine()

      val markerName = "_${
        module.name
          .asString()
          .filter { it.isLetterOrDigit() }
      }_${
        file.name.removeSuffix(".kt")
          .substringAfterLast(".")
          .substringAfterLast("/")
      }_ProvidersMarker"

      appendLine("object $markerName")

      appendLine()

      for ((i, injectable) in injectables.withIndex()) {
        val functionName = injectablesLookupName(
          injectable.fqName
            ?: error("Wtf ${injectable.text}"),
          file.packageFqName
        )

        appendLine("fun $functionName(")
        appendLine("  marker: $markerName,")
        repeat(i + 1) {
          appendLine("  index$it: Byte,")
        }

        val stringToHash: String = when (injectable) {
          is KtClassOrObject ->
            injectable.annotationEntries.joinToString { it.text } +
                injectable.primaryConstructor
                  ?.let {
                    it.valueParameters
                      .joinToString { it.text }
                  } + injectable.secondaryConstructors
              .map {
                it.valueParameters
                  .joinToString(it.text)
              }
          is KtFunction ->
            injectable.receiverTypeReference?.text.orEmpty() +
                injectable.valueParameters
                  .joinToString { it.text } +
                injectable.typeReference?.text.orEmpty()
          is KtProperty -> injectable.receiverTypeReference?.text.orEmpty() +
              injectable.typeReference?.text.orEmpty()
          else -> throw AssertionError()
        }

        val finalHash = String(Base64.getEncoder().encode(stringToHash.toByteArray()))

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

    return KtFile(
      viewProvider = object : SingleRootFileViewProvider(
        PsiManager.getInstance(project),
        file.virtualFile
      ) {
        override fun getDocument(): Document? = super.getDocument()
          ?.also {
            if (!it.text.contains(FILE_MARKER_COMMENT)) {
              it.updatePrivateFinalField<Boolean>(
                DocumentImpl::class,
                "myAssertThreading"
              ) { false }
              it.setText(code)
            }
          }
      }.also { it.document },
      isCompiled = false
    )
  }
}

private const val FILE_MARKER_COMMENT =
  "// injekt incremental " + "supporting declarations"
