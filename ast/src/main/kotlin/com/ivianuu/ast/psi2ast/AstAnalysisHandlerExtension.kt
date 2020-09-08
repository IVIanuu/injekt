package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstBuiltIns
import com.ivianuu.ast.ast2string.toKotlinSourceString
import com.ivianuu.ast.extension.AstGenerationExtension
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.util.slicedMap.SlicedMapImpl
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier

var astEnabled = false

class AstAnalysisHandlerExtension(
    private val languageVersionSettings: LanguageVersionSettings,
    private val outputDir: String
) : AnalysisHandlerExtension {

    private var generatedCode = false
    private var firstAnalysisCompleted = false
    private val userCodeDiagnostics = mutableListOf<Diagnostic>()

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!astEnabled) return null
        if (generatedCode) return null

        val extensions = AstGenerationExtension.getInstances(project)
        //if (extensions.isEmpty()) return null

        files as ArrayList<KtFile>

        forceAlwaysAllowRewrites(bindingTrace)
        forceResolveContents(componentProvider, files)

        // do not do anything if the project does contain errors
        if (bindingTrace.bindingContext.diagnostics.any {
                it.severity == Severity.ERROR
            }) return null

        val originalFiles = files.toList()
        val newFiles = transformModule(project, bindingTrace, module, extensions, originalFiles)
        files.clear()
        files += newFiles

        files.forEach { println("transformed file ${it.name}:\n${it.text}") }

        saveUserCodeDiagnostics(bindingTrace)
        resetBindingTrace(bindingTrace)

        generatedCode = true

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext,
            module,
            emptyList(),
            emptyList()
        )
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode && firstAnalysisCompleted &&
                bindingTrace.bindingContext.diagnostics.none { it.severity == Severity.ERROR }) {
            // we clear the diagnostics here because our ast2kt generated code might cause warnings
            // all user code warnings should have been signaled in the first round
            (bindingTrace.bindingContext.diagnostics as MutableDiagnosticsWithSuppression)
                .clear()
            userCodeDiagnostics.forEach { bindingTrace.report(it) }
        }
        if (!firstAnalysisCompleted) firstAnalysisCompleted = true
        return super.analysisCompleted(project, module, bindingTrace, files)
    }

    private fun transformModule(
        project: Project,
        bindingTrace: BindingTrace,
        module: ModuleDescriptor,
        extensions: List<AstGenerationExtension>,
        files: List<KtFile>
    ): List<KtFile> {
        val symbolTable = DescriptorSymbolTable()
        val typeConverter = TypeConverter(module.builtIns, languageVersionSettings, symbolTable)
        val constantValueGenerator = ConstantValueGenerator(module, symbolTable, typeConverter)
        typeConverter.constantValueGenerator = constantValueGenerator
        val stubGenerator = DeclarationStubGenerator(constantValueGenerator, symbolTable, typeConverter)
        val context = Psi2AstGeneratorContext(
            module,
            bindingTrace.bindingContext,
            module.builtIns,
            typeConverter,
            symbolTable,
            constantValueGenerator,
            stubGenerator
        )
        constantValueGenerator.builder = context
        stubGenerator.context = context
        typeConverter.builder = context
        val builtIns = AstBuiltIns(module.builtIns, typeConverter, symbolTable, context)
        val builder = Psi2AstBuilder(context)

        files.forEach {
            println("${it.text}")
        }

        val moduleFragment = builder.buildModule(files)

        println("generated module $moduleFragment for ${files.map { it.name }}")

        extensions.forEach { it.generate(moduleFragment, context) }

        return moduleFragment.files
            .map { file ->
                val src = file.toKotlinSourceString()
                val dir = file.packageFqName.pathSegments().fold(File(outputDir)) { dir, segment ->
                    dir.resolve(segment.asString())
                }.also { it.mkdirs() }
                val virtualFile = CoreLocalVirtualFile(
                    CoreLocalFileSystem(),
                    dir.resolve(file.name)
                        .also { if (!it.exists()) it.createNewFile() }
                        .also { it.writeText(src) }
                )
                KtFile(
                    viewProvider = MetaFileViewProvider(
                        PsiManager.getInstance(project),
                        virtualFile
                    ) {
                        it?.also {
                            it.setText(src)
                        }
                    },
                    isCompiled = false
                )
            }
    }

    private fun forceResolveContents(
        componentProvider: ComponentProvider,
        files: List<KtFile>
    ) {
        componentProvider.get<LazyTopDownAnalyzer>().apply {
            files.forEach {
                try {
                    analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(it))
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                try {
                    analyzeDeclarations(TopDownAnalysisMode.LocalDeclarations, listOf(it))
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun forceAlwaysAllowRewrites(bindingTrace: BindingTrace) {
        val map = BindingTraceContext::class.java
            .declaredFields
            .single { it.name == "map" }
            .also { it.isAccessible = true }
            .get(bindingTrace)
            .let { it as SlicedMapImpl }
        SlicedMapImpl::class.java
            .declaredFields
            .single { it.name == "alwaysAllowRewrite" }
            .also { it.isAccessible = true }
            .also { field ->
                Field::class.java.getDeclaredField("modifiers")
                    .also { it.isAccessible = true }
                    .also {
                        it.set(field, field.modifiers and Modifier.FINAL.inv())
                    }
            }
            .set(map, true)
    }

    private fun saveUserCodeDiagnostics(bindingTrace: BindingTrace) {
        userCodeDiagnostics += bindingTrace.bindingContext.diagnostics.all()
        (bindingTrace.bindingContext.diagnostics as MutableDiagnosticsWithSuppression)
            .clear()
    }

    private fun resetBindingTrace(bindingTrace: BindingTrace) {
        val map = BindingTraceContext::class.java
            .declaredFields
            .single { it.name == "map" }
            .also { it.isAccessible = true }
            .get(bindingTrace)
            .let { it as SlicedMapImpl }
        map.clear()
    }

}

class MetaFileViewProvider(
    psiManager: PsiManager,
    virtualFile: VirtualFile,
    val transformation: (Document?) -> Document?
) : SingleRootFileViewProvider(psiManager, virtualFile) {
    override fun getDocument(): Document? = transformation(super.getDocument())
}
