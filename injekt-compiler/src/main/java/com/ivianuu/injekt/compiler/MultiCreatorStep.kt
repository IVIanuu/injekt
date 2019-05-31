/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import com.google.common.collect.SetMultimap
import com.ivianuu.injekt.Bind
import com.ivianuu.injekt.MultiCreator
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

class MultiCreatorStep(
    private val kindCollector: KindCollector
) : ProcessingStep() {

    override fun annotations() = setOf(Bind::class) + kindAnnotations

    private val creatorNames = mutableSetOf<ClassName>()

    lateinit var roundEnv: RoundEnvironment

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        val dynamicKinds = kindCollector.kinds.flatMap {
            roundEnv.getElementsAnnotatedWith(it)
        }
        val staticKinds = annotations()
            .flatMap { elementsByAnnotation[it] }

        (dynamicKinds + staticKinds)
            .map { ClassName.bestGuess(it.asType().toString() + "__Creator") }
            .let { creatorNames.addAll(it) }

        return emptySet()
    }

    override fun postRound(processingOver: Boolean) {
        super.postRound(processingOver)
        if (!processingOver) return
        if (creatorNames.isEmpty()) return

        val multiCreatorName = creatorNames
            .map { it.canonicalName }
            .joinToString()
            .let { hashString64Bit(it) }
            .absoluteValue
            .toString()
            .let { "MultiCreator_$it" }
            .let { ClassName("", it) }

        val descriptor = MultiCreatorDescriptor(multiCreatorName, creatorNames)

        val generator = MultiCreatorGenerator(descriptor)

        generator.generate().writeTo(processingEnv.filer)

        generateFile(multiCreatorName)
    }

    private fun generateFile(name: ClassName) {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "generate file")

        val fileObject = processingEnv.filer.createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            "META-INF/services/${MultiCreator::class.java.name}"
        )

        val out = fileObject.openOutputStream()

        val writer = BufferedWriter(OutputStreamWriter(out, UTF_8))
        writer.write(name.canonicalName)
        writer.flush()
        out.close()
    }

    private fun hashString64Bit(str: String): Long {
        var result = -0x340d631b7bdddcdbL
        val len = str.length
        for (i in 0 until len) {
            result = result xor str[i].toLong()
            result *= 0x100000001b3L
        }
        return result
    }

}