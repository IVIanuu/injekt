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

import com.google.auto.service.AutoService
import com.ivianuu.injekt.BindingCreator
import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.processingx.steps.ProcessingStep
import com.ivianuu.processingx.steps.StepProcessor
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@AutoService(Processor::class)
class InjektProcessor : StepProcessor() {

    private val elements = mutableSetOf<Element>()

    override fun initSteps(): Set<ProcessingStep> =
        setOf(BindingFactoryProcessingStep())

    override fun postRound(roundEnv: RoundEnvironment) {
        super.postRound(roundEnv)
        elements.addAll(
            roundEnv.getElementsAnnotatedWith(Factory::class.java) +
                    roundEnv.getElementsAnnotatedWith(Single::class.java)
        )
        if (roundEnv.processingOver()) {
            generateFile()
        }
    }

    private fun generateFile() {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "generate file")

        val fileObject = processingEnv.filer.createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            "META-INF/services/${BindingCreator::class.java.name}"
        )

        val out = fileObject.openOutputStream()

        val writer = BufferedWriter(OutputStreamWriter(out, UTF_8))
        elements.forEach {
            writer.write(it.asType().toString() + "__Factory")
            writer.newLine()
        }
        writer.flush()
        out.close()

        processingEnv.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "wrote file types ${elements.map { it.asType().toString() }}"
        )
    }

}