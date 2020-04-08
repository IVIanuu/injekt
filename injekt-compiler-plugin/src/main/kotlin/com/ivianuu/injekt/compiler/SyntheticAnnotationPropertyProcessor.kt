package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.propertyRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class SyntheticAnnotationPropertyProcessor : ElementProcessor {

    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
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
                        .apply {
                            if (property.visibility == Visibilities.INTERNAL) {
                                addModifiers(KModifier.INTERNAL)
                            }
                        }
                        .addAnnotation(
                            property.module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektClassNames.SyntheticAnnotation)
                            )!!.asClassName()!!
                        )
                        .build()
                )
                .build()
                .let(generateFile)
        }
    }

}