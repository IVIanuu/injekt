package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class GenerateDslBuilderProcessor : ElementProcessor {
    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val behaviors = mutableListOf<DeclarationDescriptor>()

        file.accept(
            declarationRecursiveVisitor { declaration ->
                when (declaration) {
                    is KtFunction -> {
                        val descriptor =
                            bindingTrace[BindingContext.FUNCTION, declaration] as? FunctionDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDslBuilder)) {
                            behaviors += descriptor
                        }
                    }
                    is KtProperty -> {
                        val descriptor =
                            bindingTrace[BindingContext.VARIABLE, declaration] as? PropertyDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDslBuilder)) {
                            behaviors += descriptor
                        }
                    }
                }
            }
        )

        if (behaviors.isNotEmpty()) {
            FileSpec.builder(
                    file.packageFqName.asString(),
                    "${file.name.removeSuffix(".kt")}DslBuilders"
                )
                .apply {
                    behaviors.forEach {
                        addFunction(keyOverloadStub(it))
                        addFunction(keyOverload(it))
                    }
                }
                .build()
                .let(generateFile)
        }
    }

    private fun keyOverloadStub(declarationDescriptor: DeclarationDescriptor) = baseDslBuilder(
        declaration = declarationDescriptor,
        annotation = InjektClassNames.KeyOverloadStub,
        firstParameter = ParameterSpec.builder(
                "qualifier",
                InjektClassNames.Qualifier.asClassName()
            )
            .defaultValue("error(\"stub\")")
            .build(),
        code = "error(\"stub\")"
    )

    private fun keyOverload(declarationDescriptor: DeclarationDescriptor) = baseDslBuilder(
        declaration = declarationDescriptor,
        annotation = InjektClassNames.KeyOverload,
        firstParameter = ParameterSpec.builder(
            "key",
            InjektClassNames.Key.asClassName()
                .parameterizedBy(TypeVariableName("BindingType"))
        ).build(),
        code = "bind(key = key,\n" +
                "behavior = ${
                declarationDescriptor.name.asString() + declarationDescriptor.safeAs<FunctionDescriptor>()
                    ?.let { function ->
                        function.typeParameters
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(",") { it.name.asString() }
                            ?.let { "<$it>" }
                            .orEmpty() + "(" + function.valueParameters.joinToString("") {
                            "${it.name} = ${it.name},\n"
                        } + ")"
                    }.orEmpty()
                } + behavior,\n" +
                "duplicateStrategy = duplicateStrategy,\n" +
                "provider = provider\n)"
    )

    private fun baseDslBuilder(
        declaration: DeclarationDescriptor,
        annotation: FqName,
        firstParameter: ParameterSpec,
        code: String
    ) = FunSpec.builder(declaration.name.asString().decapitalize())
        .addKdoc("Dsl builder for the [${declaration.name}] behavior")
        .addAnnotation(annotation.asClassName())
        .addTypeVariable(TypeVariableName("BindingType"))
        .apply {
            if (declaration is FunctionDescriptor) {
                addTypeVariables(
                    declaration.typeParameters
                        .map { typeParameter ->
                            TypeVariableName(
                                typeParameter.name.asString(),
                                typeParameter.upperBounds
                                    .map { it.asTypeName()!! }
                            )
                        }
                )
            }
        }
        .receiver(InjektClassNames.ComponentBuilder.asClassName())
        .addParameter(firstParameter)
        .apply {
            if (declaration is FunctionDescriptor) {
                declaration.valueParameters.forEach { valueParameter ->
                    addParameter(
                        ParameterSpec.builder(
                                valueParameter.name.asString(),
                                valueParameter.type.asTypeName()!!
                            )
                            .apply {
                                if (valueParameter.declaresDefaultValue()) {
                                    defaultValue(
                                        valueParameter.findPsi()
                                            .cast<KtParameter>().defaultValue!!.text
                                    )
                                }
                            }
                            .build()
                    )
                }
            }
        }
        .addParameter(
            ParameterSpec.builder(
                    "behavior",
                    InjektClassNames.Behavior.asClassName()
                )
                .defaultValue("Behavior.None")
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                    "duplicateStrategy",
                    InjektClassNames.DuplicateStrategy.asClassName()
                )
                .defaultValue("DuplicateStrategy.Fail")
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                    "provider",
                    InjektClassNames.BindingProvider.asClassName()
                        .parameterizedBy(TypeVariableName("BindingType"))
                )
                .build()
        )
        .addCode(code)
        .build()
}
