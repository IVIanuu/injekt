package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotation
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class IrFileGenerator(
    private val srcDir: File,
    private val cacheDir: File,
    private val irFileStore: IrFileStore,
    private val lookupManager: LookupManager
) : AnalysisHandlerExtension {

    private val fileCache = KeyValueFileCache(
        cacheFile = cacheDir.resolve("file-cache"),
        fromString = { File(it) },
        toString = { it.absolutePath },
        onDelete = ::fileCacheOnDelete
    )

    private fun fileCacheOnDelete(file: File) {
        fileCache.deleteDependents(file)
        file.delete()
    }

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!generatedCode) {
            irFileStore.clear()
            files as ArrayList<KtFile>
            files.removeAll { it.text.contains("// injekt-generated") }
            files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
        }
        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode) {
            fileCache.flush()
            return null
        }
        generatedCode = true

        val rootContexts = mutableSetOf<FqName>()

        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    private val descriptorStack = mutableListOf<DeclarationDescriptor>()
                    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                        val descriptor =
                            bindingTrace[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                        if (descriptor != null) descriptorStack.push(descriptor)
                        super.visitNamedDeclaration(declaration)
                        if (descriptor != null) {
                            if (descriptor.hasAnnotation(InjektFqNames.Given) ||
                                descriptor.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                descriptor.hasAnnotation(InjektFqNames.GivenSetElements) ||
                                descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect, module)
                            ) {
                                generateIndexFileFor(
                                    declaration.containingKtFile,
                                    descriptor
                                )
                            }
                        }
                        if (descriptor != null) descriptorStack.pop()
                    }

                    override fun visitCallExpression(expression: KtCallExpression) {
                        super.visitCallExpression(expression)
                        val resolvedCall = expression.getResolvedCall(bindingTrace.bindingContext)
                        if (resolvedCall?.resultingDescriptor?.fqNameSafe?.asString() ==
                            "com.ivianuu.injekt.runReader" ||
                            resolvedCall?.resultingDescriptor?.fqNameSafe?.asString() ==
                            "com.ivianuu.injekt.rootContext"
                        ) {
                            if (resolvedCall.resultingDescriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.rootContext"
                            ) {
                                rootContexts += FqName(
                                    "${expression.containingKtFile.packageFqName}.${
                                        resolvedCall.typeArguments.values
                                            .single()
                                            .constructor
                                            .declarationDescriptor!!
                                            .name
                                    }${expression.startOffsetSkippingComments}FactoryImpl"
                                )
                            }
                            generateIndexFileFor(
                                expression.containingKtFile,
                                descriptorStack.last()
                            )
                        }
                    }
                }
            )
        }

        var initTrigger: KtDeclaration? = null
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor {
                    val descriptor = bindingTrace[BindingContext.DECLARATION_TO_DESCRIPTOR, it]
                    if (descriptor?.hasAnnotation(InjektFqNames.InitializeInjekt) == true) {
                        initTrigger = initTrigger ?: it
                    }
                }
            )
        }

        if (initTrigger != null) {
            rootContexts += module.getPackage(InjektFqNames.IndexPackage)
                .memberScope
                .getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .mapNotNull { index ->
                    index.annotations.findAnnotation(InjektFqNames.Index)
                        ?.takeIf {
                            it.allValueArguments[Name.identifier("type")]
                                .let { it as StringValue }
                                .value == "class"
                        }
                        ?.let { annotation ->
                            val fqName = annotation.allValueArguments[Name.identifier("fqName")]!!
                                .let { it as StringValue }
                                .value
                                .let { FqName(it) }
                            module.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                        }
                }
                .mapNotNull {
                    it.annotations.findAnnotation(InjektFqNames.RootContextFactory)
                        ?.allValueArguments
                        ?.values
                        ?.single()
                        ?.let { it as StringValue }
                        ?.value
                        ?.let { FqName(it) }
                }
            val descriptor = bindingTrace[BindingContext.DECLARATION_TO_DESCRIPTOR, initTrigger]!!
            rootContexts.forEach {
                generateRootFactoryFileFor(
                    it,
                    initTrigger!!.containingKtFile,
                    descriptor
                )
            }
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(srcDir), true
        )
    }

    private fun generateIndexFileFor(
        originatingFile: KtFile,
        originatingDescriptor: DeclarationDescriptor
    ) {
        val existingIndexFilePath = irFileStore.get(originatingFile.virtualFilePath)
        if (existingIndexFilePath != null) {
            lookupManager.recordLookup(existingIndexFilePath, originatingDescriptor)
            return
        }
        val indexFileName = getJoinedName(
            InjektFqNames.IndexPackage,
            FqName(
                "${
                    originatingFile.packageFqName.pathSegments()
                        .joinToString("_")
                }_${originatingFile.name.removeSuffix(".kt")}Indices"
            )
        )

        val code = buildString {
            appendLine("// injekt-generated")
            appendLine("package ${InjektFqNames.IndexPackage}")
        }

        val indexFile = srcDir.resolve(InjektFqNames.IndexPackage.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve("$indexFileName.kt")
            .also { it.createNewFile() }
        indexFile.writeText(code)
        fileCache.recordDependency(indexFile, File(originatingFile.virtualFilePath))
        irFileStore.put(originatingFile.virtualFilePath, indexFile.absolutePath)
    }

    private fun generateRootFactoryFileFor(
        fqName: FqName,
        originatingFile: KtFile,
        originatingDescriptor: DeclarationDescriptor
    ) {
        val code = buildString {
            appendLine("// injekt-generated")
            appendLine("package ${fqName.parent()}")
        }

        val factoryFile = srcDir.resolve(fqName.parent().asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve("${fqName.shortName()}.kt")
            .also { it.createNewFile() }
        factoryFile.writeText(code)
        fileCache.recordDependency(factoryFile, File(originatingFile.virtualFilePath))
        irFileStore.put(fqName.asString(), factoryFile.absolutePath)
        lookupManager.recordLookup(factoryFile.absolutePath, originatingDescriptor)
    }

}

class IrFileStore {
    val map = mutableMapOf<String, String>()
    fun put(key: String, value: String) {
        map[key] = value
    }

    fun get(key: String) = map[key]
    fun clear() {
        map.clear()
    }
}
