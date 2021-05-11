package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.lang.reflect.*

class InfoAnnotationPatcher(private val context: InjektContext) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> patchClassIfNeeded(descriptor, context.trace)
            is SimpleFunctionDescriptor -> patchCallableIfNeeded(descriptor, context.trace)
            is PropertyDescriptor -> patchCallableIfNeeded(descriptor, context.trace)
        }
    }

    private fun patchClassIfNeeded(descriptor: ClassDescriptor, trace: BindingTrace) {
        val classifierRef = descriptor.toClassifierRef(context, trace)
        val defaultType = classifierRef.defaultType
        if (descriptor.hasAnnotation(InjektFqNames.Given) ||
            classifierRef.typeParameters.any { it.isForTypeKey || it.isGivenConstraint } ||
            defaultType
                .collectGivens(context, trace)
                .isNotEmpty() ||
            defaultType
                .anySuperType {
                    it.classifier.isQualifier ||
                            (it.classifier.isTypeAlias &&
                                    it.fullyExpandedType.isSuspendFunctionType)
                } || defaultType.anyType {
                it.classifier.isQualifier ||
                        (it.classifier.isTypeAlias &&
                                it.fullyExpandedType.isSuspendFunctionType)
            }) {
            val info = descriptor.toClassifierRef(context, null)
                .toPersistedClassifierInfo(context, trace)
            val serializedValue = info.encode()
            descriptor.addAnnotation(
                AnnotationDescriptorImpl(
                    context.module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(
                            InjektFqNames.ClassifierInfo
                        ))!!.defaultType,
                    mapOf("value".asNameId() to StringValue(serializedValue)),
                    SourceElement.NO_SOURCE
                )
            )
        }
    }

    private fun patchCallableIfNeeded(descriptor: CallableDescriptor, trace: BindingTrace) {
        var needsInfo = descriptor.hasAnnotation(InjektFqNames.Given) ||
                (descriptor is IrConstructor &&
                        descriptor.constructedClass.hasAnnotation(InjektFqNames.Given))
        if (!needsInfo) {
            needsInfo = descriptor
                .safeAs<IrFunction>()
                ?.valueParameters
                ?.any { it.descriptor.isGiven(context, trace) } == true
        }
        val callableRef = descriptor.toCallableRef(context, trace)
        if (!needsInfo) {
            needsInfo = callableRef.type.anyType {
                it.classifier.isQualifier ||
                        (it.classifier.isTypeAlias &&
                                it.fullyExpandedType.isSuspendFunctionType)
            } || callableRef.parameterTypes.any { (_, parameterType) ->
                parameterType.anyType {
                    it.classifier.isQualifier ||
                            (it.classifier.isTypeAlias &&
                                    it.fullyExpandedType.isSuspendFunctionType)
                }
            }
        }
        if (!needsInfo) return
        val info = callableRef.toPersistedCallableInfo(context, trace)
        val serializedValue = info.encode()
        descriptor.addAnnotation(
            AnnotationDescriptorImpl(
                context.module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(
                        InjektFqNames.CallableInfo
                    ))!!.defaultType,
                mapOf("value".asNameId() to StringValue(serializedValue)),
                SourceElement.NO_SOURCE
            )
        )
    }

    private fun Annotated.addAnnotation(annotation: AnnotationDescriptor) {
        when (this) {
            is AnnotatedImpl -> {
                val field = AnnotatedImpl::class.java.declaredFields
                    .single { it.name == "annotations" }
                field.isAccessible = true
                val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                field.set(
                    this,
                    Annotations.create(
                        annotations.toList() + annotation
                    )
                )
            }
            is LazyClassDescriptor -> {
                val field = LazyClassDescriptor::class.java.declaredFields
                    .single { it.name == "annotations" }
                field.isAccessible = true
                val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                field.set(
                    this,
                    Annotations.create(
                        annotations.toList() + annotation
                    )
                )
            }
            else -> throw AssertionError("Cannot add annotation to $this")
        }
    }
}
