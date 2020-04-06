package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.psi.propertyRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.types.Variance
import java.io.File

class InjektAnalysisHandlerExtension(
    outputDir: String
) : AnalysisHandlerExtension {

    private var processingFinished = false

    private lateinit var container: ComponentProvider

    private val outputDir = File(outputDir).apply { mkdirs() }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        container = componentProvider
        return super.doAnalysis(
            project,
            module,
            projectContext,
            files,
            bindingTrace,
            componentProvider
        )
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (processingFinished) return null
        processingFinished = true

        var generatedFiles = false

        fun resolveFile(file: KtFile) {
            try {
                container.get<LazyTopDownAnalyzer>().apply {
                    analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(file))
                    analyzeDeclarations(TopDownAnalysisMode.LocalDeclarations, listOf(file))
                }
            } catch (t: Throwable) {
            }
        }

        files.forEach { file ->
            resolveFile(file)
            processSyntheticAnnotationProperties(
                file,

                bindingTrace
            ) { generatedFiles = true }
            processKeyOverloads(
                file = file,
                bindingTrace = bindingTrace
            ) { generatedFiles = true }
        }

        return if (generatedFiles) {
            AnalysisResult.RetryWithAdditionalRoots(
                bindingTrace.bindingContext,
                module,
                emptyList(),
                listOf(outputDir)
            )
        } else {
            null
        }
    }

    private fun processSyntheticAnnotationProperties(
        file: KtFile,
        bindingTrace: BindingTrace,
        onFileGenerated: () -> Unit
    ) {
        val syntheticAnnotationProperties = mutableListOf<PropertyDescriptor>()
        file.accept(
            propertyRecursiveVisitor {
                val descriptor = bindingTrace[BindingContext.VARIABLE, it] as? PropertyDescriptor
                    ?: return@propertyRecursiveVisitor

                if (descriptor.getAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)
                        .isNotEmpty() &&
                    descriptor.dispatchReceiverParameter == null &&
                    descriptor.extensionReceiverParameter == null &&
                    descriptor.module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(
                            file.packageFqName.child(Name.identifier("synthetic")).child(
                                Name.identifier(descriptor.name.asString().capitalize())
                            )
                        )
                    ) == null
                ) {
                    syntheticAnnotationProperties += descriptor
                }
            }
        )

        syntheticAnnotationProperties.forEach { property ->
            FileSpec.builder(
                    file.packageFqName.child(Name.identifier("synthetic")).asString(),
                    property.name.asString().capitalize()
                )
                .addType(
                    TypeSpec.annotationBuilder(property.name.asString().capitalize())
                        .addAnnotation(
                            property.module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektClassNames.SyntheticAnnotation)
                            )!!.asClassName()!!
                        )
                        .build()
                )
                .build()
                .writeTo(outputDir)
            onFileGenerated()
        }
    }

    // todo generate file per overload and only generate if it does not exist
    private fun processKeyOverloads(
        file: KtFile,
        bindingTrace: BindingTrace,
        onFileGenerated: () -> Unit
    ) {
        val functions = mutableListOf<FunctionDescriptor>()
        file.accept(
            namedFunctionRecursiveVisitor { function ->
                val descriptor = bindingTrace[BindingContext.FUNCTION, function]
                if (descriptor?.annotations?.hasAnnotation(InjektClassNames.KeyOverload) == true) {
                    functions += descriptor
                }
            }
        )

        if (functions.isNotEmpty()) {
            FileSpec.builder(
                    file.packageFqName.asString(),
                    "${file.name.removeSuffix(".kt")}Stubs.kt"
                )
                .apply {
                    functions.forEach { function ->
                        addFunction(keyOverloadStubFunction(function))
                    }
                }
                .build()
                .writeTo(outputDir)
            onFileGenerated()
        }
    }

    private fun keyOverloadStubFunction(function: FunctionDescriptor): FunSpec {
        return FunSpec.builder(function.name.asString())
            .addAnnotation(InjektClassNames.KeyOverloadStub.asClassName())
            .apply {
                if (function.isInline) {
                    addModifiers(KModifier.INLINE)
                }
                if (function.visibility == Visibilities.INTERNAL) {
                    addModifiers(KModifier.INTERNAL)
                }
                if (function.isOperator) {
                    addModifiers(KModifier.OPERATOR)
                }
            }
            .addTypeVariables(
                function.typeParameters
                    .map { typeParameter ->
                        TypeVariableName(
                            typeParameter.name.asString(),
                            *typeParameter.upperBounds
                                .map { it.asTypeName()!! }
                                .toTypedArray(),
                            variance = when (typeParameter.variance) {
                                Variance.INVARIANT -> null
                                Variance.IN_VARIANCE -> KModifier.IN
                                Variance.OUT_VARIANCE -> KModifier.OUT
                            }
                        )
                    }
            )
            .apply {
                function.extensionReceiverParameter?.let { extensionReceiver ->
                    receiver(extensionReceiver.type.asTypeName()!!)
                } ?: function.dispatchReceiverParameter?.let { dispatchReceiver ->
                    receiver(dispatchReceiver.type.asTypeName()!!)
                }
            }
            .addParameter(
                ParameterSpec.builder(
                        "qualifier",
                        InjektClassNames.Qualifier.asClassName()
                    )
                    .defaultValue("error(\"stub\")\n")
                    .build()
            )
            .addParameters(
                function.valueParameters
                    .drop(1)
                    .map { valueParameter ->
                        ParameterSpec.builder(
                                valueParameter.name.asString(),
                                valueParameter.type.asTypeName()!!
                            )
                            .apply {
                                if (valueParameter.declaresDefaultValue()) {
                                    if (valueParameter.type.isFunctionType &&
                                        function.isInline && !valueParameter.isNoinline
                                    ) {
                                        defaultValue("{ ${
                                        valueParameter.type.getValueParameterTypesFromFunctionType()
                                            .indices.joinToString(", ") { "_" }
                                        } error(\"stub\") }")
                                    } else {
                                        defaultValue("error(\"stub\")")
                                    }
                                }

                                if (valueParameter.isCrossinline) {
                                    addModifiers(KModifier.CROSSINLINE)
                                } else if (valueParameter.isNoinline) {
                                    addModifiers(KModifier.NOINLINE)
                                }
                            }
                            .build()
                    }
            )
            .apply { function.returnType?.let { returns(it.asTypeName()!!) } }
            .addCode("error(\"stub\")\n")
            .build()
    }
}