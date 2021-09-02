package com.ivianuu.injekt.compiler.incremental

import java.io.File
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

class InjektIncrementalDeclarationGenerationExtension(
  private val outputDir: File
) : AnalysisHandlerExtension {
  override fun doAnalysis(
    project: Project,
    module: ModuleDescriptor,
    projectContext: ProjectContext,
    files: Collection<KtFile>,
    bindingTrace: BindingTrace,
    componentProvider: ComponentProvider
  ): AnalysisResult {
    error("Hello world")
    return AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
  }
}

/*
class InjektSymbolProcessor(
  private val codeGenerator: CodeGenerator
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val injectablesByFile = resolver.getSymbolsWithAnnotation(
      InjektFqNames.Provide.asString(), false
    )
      .filterIsInstance<KSDeclaration>()
      .filter { it.containingFile != null }
      .filter {
        val visibility = it.getVisibility()
        visibility == Visibility.PUBLIC ||
            visibility == Visibility.INTERNAL ||
            visibility == Visibility.PROTECTED
      }
      .groupBy { it.containingFile }
      .filterKeys { it != null }
      .mapKeys { it.key!! }
      .filterValues { it.isNotEmpty() }

    for ((file, injectables) in injectablesByFile) {
      val code = buildString {
        appendLine("package ${file.packageName.asString()}")
        appendLine()

        val markerName = "_${
          "moduleName"
        }_${
          file.fileName.removeSuffix(".kt")
            .substringAfterLast(".")
            .substringAfterLast("/")
        }_ProvidersMarker"

        appendLine("object $markerName")

        appendLine()

        for ((i, injectable) in injectables.withIndex()) {
          val functionName = injectablesLookupName(
            FqName(injectable.qualifiedName!!.asString()),
            FqName(file.packageName.asString())
          )

          appendLine("fun $functionName(")
          appendLine("  marker: $markerName,")
          repeat(i) {
            appendLine("  index$it: Byte,")
          }

          val stringToHash: String = when (injectable) {
            is KSClassDeclarationImpl -> {
              if (injectable.ktClassOrObject is KtObjectDeclaration) ""
              else injectable.ktClassOrObject.annotationEntries.joinToString { it.text } +
                  injectable.ktClassOrObject.primaryConstructor
                    ?.let {
                      it.valueParameters
                        .joinToString { it.text }
                    } + injectable.ktClassOrObject.secondaryConstructors
                .map {
                  it.valueParameters
                    .joinToString(it.text)
                }
            }
            is KSFunctionDeclarationImpl ->
              injectable.ktFunction.receiverTypeReference?.text.orEmpty() +
                  injectable.ktFunction.valueParameters
                    .joinToString { it.text } +
                  injectable.ktFunction.typeReference?.text.orEmpty()
            is KSPropertyDeclarationImpl -> injectable.ktProperty.receiverTypeReference?.text.orEmpty() +
                injectable.ktProperty.typeReference?.text.orEmpty()
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

      codeGenerator.createNewFile(
        dependencies = Dependencies(true, file),
        packageName = file.packageName.asString(),
        fileName = "${file.fileName.removeSuffix(".kt")}_ProviderMarkers"
      ).write(code.toByteArray())
    }

    return emptyList()
  }
}*/