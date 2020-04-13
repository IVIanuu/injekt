package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.Variance

class SyntheticAnnotationProcessor(
    private val module: ModuleDescriptor
) : ElementProcessor {

    private val keyOf = module.findClassAcrossModuleDependencies(
        ClassId.topLevel(InjektClassNames.KeyOf)
    )!!

    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val syntheticAnnotationDeclarations = mutableListOf<DeclarationDescriptor>()
        file.accept(
            declarationRecursiveVisitor { declaration ->
                when (declaration) {
                    is KtClassOrObject -> {
                        val descriptor =
                            bindingTrace[BindingContext.CLASS, declaration]
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
                            syntheticAnnotationDeclarations += descriptor
                        }
                    }
                    is KtFunction -> {
                        val descriptor =
                            bindingTrace[BindingContext.FUNCTION, declaration] as? FunctionDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
                            syntheticAnnotationDeclarations += descriptor
                        }
                    }
                    is KtProperty -> {
                        val descriptor =
                            bindingTrace[BindingContext.VARIABLE, declaration] as? PropertyDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
                            syntheticAnnotationDeclarations += descriptor
                        }
                    }
                }
            }
        )

        syntheticAnnotationDeclarations.forEach { declaration ->
            FileSpec.builder(
                    file.packageFqName.child(Name.identifier("synthetic")).asString(),
                    declaration.name.asString().capitalize()
                )
                .addType(syntheticAnnotation(declaration))
                .build()
                .let(generateFile)
        }
    }

    private fun syntheticAnnotation(
        declaration: DeclarationDescriptor
    ) = TypeSpec.annotationBuilder(declaration.name.asString().capitalize())
        .apply {
            if ((declaration as MemberDescriptor).visibility == Visibilities.INTERNAL) {
                addModifiers(KModifier.INTERNAL)
            }
        }
        .addKdoc("Annotation for [${declaration.fqNameSafe.asString()}]")
        .addAnnotation(
            declaration.module.findClassAcrossModuleDependencies(
                ClassId.topLevel(InjektClassNames.SyntheticAnnotation)
            )!!.asClassName()!!
        )
        .apply {
            val function = when (declaration) {
                is FunctionDescriptor -> declaration
                is ClassDescriptor -> declaration.unsubstitutedPrimaryConstructor!!
                else -> null
            }
            if (function != null) {
                addTypeVariables(
                    function.typeParameters.map { typeParameter ->
                        TypeVariableName(
                            typeParameter.name.asString(),
                            typeParameter.upperBounds
                                .map { it.asTypeName()!! }
                        )
                    }
                )

                addProperties(
                    function.valueParameters.map { valueParameter ->
                        val type = valueParameter.type
                        if (type.typeEquals(InjektClassNames.Key)) {
                            PropertySpec.builder(
                                    valueParameter.name.asString(),
                                    keyOf.defaultType.asTypeName()!!
                                )
                                .initializer(valueParameter.name.asString())
                                .build()
                        } else if (KotlinBuiltIns.isArray(type) &&
                            type.arguments.single().type.typeEquals(InjektClassNames.Key)
                        ) {
                            PropertySpec.builder(
                                    valueParameter.name.asString(),
                                    valueParameter.builtIns.getArrayType(
                                        Variance.INVARIANT,
                                        keyOf.defaultType
                                    ).asTypeName()!!
                                )
                                .initializer(valueParameter.name.asString())
                                .build()
                        } else {
                            PropertySpec.builder(
                                    valueParameter.name.asString(),
                                    valueParameter.type.asTypeName()!!
                                )
                                .initializer(valueParameter.name.asString())
                                .build()
                        }
                    }
                )
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameters(
                            function.valueParameters.map { valueParameter ->
                                val type = valueParameter.type
                                if (type.typeEquals(InjektClassNames.Key)) {
                                    ParameterSpec.builder(
                                            valueParameter.name.asString(),
                                            keyOf.defaultType.asTypeName()!!
                                        )
                                        .build()
                                } else if (KotlinBuiltIns.isArray(type) &&
                                    type.arguments.single().type.typeEquals(InjektClassNames.Key)
                                ) {
                                    ParameterSpec.builder(
                                        valueParameter.name.asString(),
                                        valueParameter.builtIns.getArrayType(
                                            Variance.INVARIANT,
                                            keyOf.defaultType
                                        ).asTypeName()!!
                                    ).build()
                                } else {
                                    ParameterSpec.builder(
                                            valueParameter.name.asString(),
                                            valueParameter.type.asTypeName()!!
                                        )
                                        .apply {
                                            if (valueParameter.declaresDefaultValue()) {
                                                defaultValue((valueParameter.findPsi() as KtParameter).defaultValue!!.text)
                                            }
                                        }
                                        .build()
                                }
                            }
                        )
                        .build()
                )
            }
        }
        .build()
}