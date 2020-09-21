package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@Given
class InjektKtGenerationExtension : AnalysisHandlerExtension {

    private val fileManager = given<KtFileManager>()

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
            files as ArrayList<KtFile>
            val copy = files.toList()
            files.clear()
            files += fileManager.onPreCompile(copy)
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
            fileManager.onPostCompile()
            return null
        }
        generatedCode = true

        files as List<KtFile>

        val context = childContext<KtGenerationContext>(
            bindingTrace,
            bindingTrace.bindingContext, module
        )

        context.runReader {
            given<GivenIndexingGenerator>().generate(files)
            given<EffectGenerator>().generate(files)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(given<SrcDir>()), true
        )
    }

    /**

    private fun generateIndexFileFor(
    originatingFile: KtFile,
    originatingDescriptor: DeclarationDescriptor
    ) {
    val existingIndexFilePath = fileStore.get(originatingFile.virtualFilePath)
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

    val indexFile =
    given<SrcDir>().resolve(InjektFqNames.IndexPackage.asString().replace(".", "/"))
    .also { it.mkdirs() }
    .resolve("$indexFileName.kt")
    .also { it.createNewFile() }
    indexFile.writeText(code)
    fileCache.recordDependency(indexFile, File(originatingFile.virtualFilePath))
    fileStore.put(originatingFile.virtualFilePath, indexFile.absolutePath)
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

    val factoryFile = given<SrcDir>().resolve(fqName.parent().asString().replace(".", "/"))
    .also { it.mkdirs() }
    .resolve("${fqName.shortName()}.kt")
    .also { it.createNewFile() }
    factoryFile.writeText(code)
    fileCache.recordDependency(factoryFile, File(originatingFile.virtualFilePath))
    fileStore.put(fqName.asString(), factoryFile.absolutePath)
    lookupManager.recordLookup(factoryFile.absolutePath, originatingDescriptor)
    }

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
    .let { memberScope ->
    (memberScope.getClassifierNames() ?: emptySet<Name>())
    .map {
    memberScope.getContributedClassifier(
    it,
    NoLookupLocation.FROM_BACKEND
    )
    }
    }
    .filterIsInstance<ClassDescriptor>()
    .mapNotNull { index ->
    index.annotations.findAnnotation(InjektFqNames.Index)
    ?.takeIf { annotation ->
    annotation.allValueArguments[Name.identifier("type")]
    .let { it as StringValue }
    .value == "class"
    }
    ?.let { annotation ->
    val fqName =
    annotation.allValueArguments.getValue(Name.identifier("fqName"))
    .let { it as StringValue }
    .value
    .let { FqName(it) }
    module.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
    }
    }
    .mapNotNull { index ->
    index.annotations.findAnnotation(InjektFqNames.RootContextFactory)
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
    }*/

}
