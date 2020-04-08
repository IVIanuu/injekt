package com.ivianuu.injekt.processor

import com.google.auto.service.AutoService
import com.ivianuu.injekt.compiler.InjektClassNames
import com.squareup.kotlinpoet.FileSpec
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
class InjektProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(
            InjektClassNames.KeyOverload.asString(),
            InjektClassNames.SyntheticAnnotationMarker.asString()
        )

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    private lateinit var processingEnv: ProcessingEnvironment

    private val keyOverload by lazy {
        processingEnv.elementUtils.getTypeElement(InjektClassNames.KeyOverload.asString())
    }
    private val syntheticAnnotationMarker by lazy {
        processingEnv.elementUtils.getTypeElement(InjektClassNames.SyntheticAnnotationMarker.asString())
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        this.processingEnv = processingEnv
    }

    override fun process(
        elements: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        roundEnv.getElementsAnnotatedWith(keyOverload)
            .filterIsInstance<ExecutableElement>()
            .forEach {
                FileSpec.builder(it.p)
            }

        roundEnv.getElementsAnnotatedWith(syntheticAnnotationMarker)
            .filterIsInstance<TypeElement>()
            .forEach { syntheticAnnotation ->
                val metaData =
                    syntheticAnnotation.kotlinMetadata as? KotlinClassMetadata ?: return@forEach

                FileSpec.builder()


            }

        return false
    }

}
