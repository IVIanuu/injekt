/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.readercontextimpl.Key
import com.ivianuu.injekt.compiler.backend.readercontextimpl.asKey
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation

@Given(IrContext::class)
class DeclarationGraph {

    private val indexer = given<Indexer>()

    val rootContextFactories: List<IrClass> by lazy {
        indexer.classIndices
            .filter { it.hasAnnotation(InjektFqNames.RootContextFactory) }
    }

    private val givensByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givens(key: Key) = givensByKey.getOrPut(key) {
        (indexer.functionIndices +
                indexer.classIndices
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices
                    .mapNotNull { it.getter }
                )
            .filter {
                (it.hasAnnotation(InjektFqNames.Given) || it.hasAnnotatedAnnotations(InjektFqNames.Effect)) ||
                        (it is IrSimpleFunction &&
                                (it.correspondingPropertySymbol?.owner?.hasAnnotation(InjektFqNames.Given) == true ||
                                        it.correspondingPropertySymbol?.owner?.hasAnnotatedAnnotations(
                                            InjektFqNames.Effect
                                        ) == true)) ||
                        (it is IrConstructor && (it.constructedClass.hasAnnotation(InjektFqNames.Given) ||
                                it.constructedClass.hasAnnotatedAnnotations(InjektFqNames.Effect)))
            }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .filter { function ->
                if (function.extensionReceiverParameter != null || function.valueParameters
                        .filter { it.name.asString() != "_context" }
                        .isNotEmpty()
                ) {
                    function.getFunctionType(pluginContext, skipContext = true).asKey() == key
                } else {
                    function.returnType.asKey() == key
                }
            }
            .distinct()
    }

    private val givenMapEntriesByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givenMapEntries(key: Key) = givenMapEntriesByKey.getOrPut(key) {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .filter { it.returnType.asKey() == key }
    }

    private val givenSetElementsByKey = mutableMapOf<Key, List<IrFunction>>()
    fun givenSetElements(key: Key) = givenSetElementsByKey.getOrPut(key) {
        (indexer.functionIndices +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .map { given<ReaderContextParamTransformer>().getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .filter { it.returnType.asKey() == key }
    }

    private val runReaderContexts = mutableMapOf<IrClass, List<IrClass>>()
    fun getRunReaderContexts(context: IrClass): List<IrClass> {
        return runReaderContexts.getOrPut(context) {
            indexer.classIndices
                .filter {
                    it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 0) == context
                }
                .mapNotNull { it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 1) }
        }
    }

}
