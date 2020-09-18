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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val readerContextParamTransformer: ReaderContextParamTransformer
) {

    val rootContextFactories: List<IrClass> by lazy {
        indexer.classIndices(listOf(ROOT_CONTEXT_FACTORY_PATH))
            .filter { it.hasAnnotation(InjektFqNames.RootContextFactory) }
    }

    private val givensByKey = mutableMapOf<String, List<IrFunction>>()
    fun givens(key: String) = givensByKey.getOrPut("") {
        (indexer.functionIndices(listOf(GIVEN_PATH, key)) +
                indexer.classIndices(listOf(GIVEN_PATH, key))
                    .flatMap { it.constructors.toList() } +
                indexer.propertyIndices(listOf(GIVEN_PATH, key))
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
            .map { readerContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
    }

    private val givenMapEntriesByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenMapEntries(key: String) = givenMapEntriesByKey.getOrPut("") {
        (indexer.functionIndices(listOf(MAP_ENTRIES_PATH, key)) +
                indexer.propertyIndices(listOf(MAP_ENTRIES_PATH, key)).mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenMapEntries) }
            .map { readerContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val givenSetElementsByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenSetElements(key: String) = givenSetElementsByKey.getOrPut("") {
        (indexer.functionIndices(listOf(SET_ELEMENTS_PATH, key)) +
                indexer.propertyIndices(listOf(SET_ELEMENTS_PATH, key)).mapNotNull { it.getter })
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElements) }
            .map { readerContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    fun getRunReaderContexts(context: IrClass): List<IrClass> {
        return indexer.classIndices(
            listOf(
                RUN_READER_CALL_PATH,
                context.descriptor.fqNameSafe.asString()
            )
        )
            .filter {
                it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 0) == context
            }
            .mapNotNull { it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 1) }
    }

    companion object {
        const val ROOT_CONTEXT_FACTORY_PATH = "root_context_factory"
        const val RUN_READER_CALL_PATH = "run_reader_call"
        const val GIVEN_PATH = "given"
        const val MAP_ENTRIES_PATH = "map_entries"
        const val SET_ELEMENTS_PATH = "set_elements"
    }

}
