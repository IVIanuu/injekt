package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.propertyRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class SyntheticAnnotationProcessor : ElementProcessor {

    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val syntheticAnnotationProperties = mutableListOf<PropertyDescriptor>()
        file.accept(
            propertyRecursiveVisitor { property ->
                val descriptor =
                    bindingTrace[BindingContext.VARIABLE, property] as? PropertyDescriptor
                        ?: return@propertyRecursiveVisitor

                if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
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
                            if ((property as MemberDescriptor).visibility == Visibilities.INTERNAL) {
                                addModifiers(KModifier.INTERNAL)
                            }
                        }
                        .addKdoc("Annotation for [${property.fqNameSafe.asString()}]")
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