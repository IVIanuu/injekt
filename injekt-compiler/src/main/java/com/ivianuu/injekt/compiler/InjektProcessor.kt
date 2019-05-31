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
import com.ivianuu.processingx.steps.ProcessingStep
import com.ivianuu.processingx.steps.StepProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class InjektProcessor : StepProcessor() {

    private val kindCollector = KindCollector()
    private val creatorStep = CreatorStep(kindCollector)
    private val multiCreatorStep = MultiCreatorStep(kindCollector)

    override fun initSteps(): Set<ProcessingStep> =
        setOf(kindCollector, creatorStep, multiCreatorStep)

    override fun process(elements: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        creatorStep.roundEnv = roundEnv
        multiCreatorStep.roundEnv = roundEnv
        return super.process(elements, roundEnv)
    }

}