package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.propertyRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

class GenerateDslBuilderProcessor : ElementProcessor {
    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val behaviors = mutableListOf<PropertyDescriptor>()

        file.accept(
            propertyRecursiveVisitor {
                val descriptor = bindingTrace[BindingContext.VARIABLE, it] as? PropertyDescriptor
                    ?: return@propertyRecursiveVisitor
                if (descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDslBuilder)) {
                    behaviors += descriptor
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

    private fun keyOverloadStub(property: PropertyDescriptor) = baseDslBuilder(
        property = property,
        annotation = InjektClassNames.KeyOverloadStub,
        firstParameter = ParameterSpec.builder(
                "qualifier",
                InjektClassNames.Qualifier.asClassName()
            )
            .defaultValue("error(\"stub\")")
            .build(),
        code = "error(\"stub\")"
    )

    private fun keyOverload(property: PropertyDescriptor) = baseDslBuilder(
        property = property,
        annotation = InjektClassNames.KeyOverload,
        firstParameter = ParameterSpec.builder(
            "key",
            InjektClassNames.Key.asClassName()
                .parameterizedBy(TypeVariableName("T"))
        ).build(),
        code = "bind(key = key,\n" +
                "behavior = ${property.name.asString()} + behavior,\n" +
                "duplicateStrategy = duplicateStrategy,\n" +
                "provider = provider\n)"
    )

    private fun baseDslBuilder(
        property: PropertyDescriptor,
        annotation: FqName,
        firstParameter: ParameterSpec,
        code: String
    ) = FunSpec.builder(property.name.asString().decapitalize())
        .addKdoc("Dsl builder for the [${property.name}] behavior")
        .addAnnotation(annotation.asClassName())
        .addTypeVariable(TypeVariableName("T"))
        .receiver(InjektClassNames.ComponentBuilder.asClassName())
        .addParameter(firstParameter)
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
                        .parameterizedBy(TypeVariableName("T"))
                )
                .build()
        )
        .addCode(code)
        .build()
}
