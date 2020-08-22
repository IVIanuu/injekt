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
import com.ivianuu.injekt.compiler.classifierOrTypeAliasFqName
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.reader.ReaderContextParamTransformer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    private val injektContext: InjektContext,
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val readerContextParamTransformer: ReaderContextParamTransformer
) {

    val rootContextFactories: List<IrClass> by lazy {
        indexer.classIndices(listOf(ROOT_CONTEXT_FACTORY_PATH))
    }

    private val givensByType = mutableMapOf<FqName, List<IrFunction>>()
    fun givens(type: IrType): List<IrFunction> {
        val fqName = type.classifierOrTypeAliasFqName
        return givensByType.getOrPut(fqName) {
            (indexer.functionIndices(listOf(GIVEN_PATH)) +
                    indexer.classIndices(listOf(GIVEN_PATH))
                        .flatMap { it.constructors.toList() } +
                    indexer.propertyIndices(listOf(GIVEN_PATH))
                        .mapNotNull { it.getter }
                    )
                .filter { function ->
                    val explicitParameters = function.valueParameters
                        .filter { it != function.getContextValueParameter() }
                    val realType =
                        if (explicitParameters.isEmpty()) function.returnType
                        else injektContext.tmpFunction(explicitParameters.size)
                            .typeWith(explicitParameters.map { it.type } + function.returnType)
                    realType.isTypeParameter() ||
                            realType.classifierOrTypeAliasFqName == fqName
                }
                .map { readerContextParamTransformer.getTransformedFunction(it) }
                .filter { it.getContext() != null }
                .distinct()
        }
    }

    private val givenMapEntriesByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenMapEntries(key: String) = givenMapEntriesByKey.getOrPut(key) {
        (indexer.functionIndices(listOf(MAP_ENTRIES_PATH, key)) +
                indexer.propertyIndices(listOf(MAP_ENTRIES_PATH, key)).mapNotNull { it.getter })
            .map { readerContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val givenSetElementsByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenSetElements(key: String) = givenSetElementsByKey.getOrPut(key) {
        (indexer.functionIndices(listOf(SET_ELEMENTS_PATH, key)) +
                indexer.propertyIndices(listOf(SET_ELEMENTS_PATH, key)).mapNotNull { it.getter })
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
            .mapNotNull { it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 0) }
    }

    private val implementationsByContext = mutableMapOf<IrClass, Set<IrClass>>()
    fun getAllContextImplementations(
        context: IrClass
    ): Set<IrClass> = implementationsByContext.getOrPut(context) {
        val contexts = mutableSetOf<IrClass>()

        contexts += context

        val processedClasses = mutableSetOf<IrClass>()

        fun collectImplementations(context: IrClass) {
            if (context in processedClasses) return
            processedClasses += context

            indexer.classIndices(
                listOf(
                    READER_IMPL_PATH,
                    context.descriptor.fqNameSafe.asString()
                )
            )
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        0
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            indexer.classIndices(
                listOf(
                    READER_CALL_PATH,
                    context.descriptor.fqNameSafe.asString()
                )
            )
                .filter {
                    it.getConstantFromAnnotationOrNull<Boolean>(
                        InjektFqNames.ReaderCall,
                        1
                    )!!
                }
                .map { it.getClassFromAnnotation(InjektFqNames.ReaderCall, 0)!! }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            context.superTypes
                .map { it.classOrNull!!.owner }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }
        }

        collectImplementations(context)

        contexts
    }

    companion object {
        const val READER_CALL_PATH = "reader_call"
        const val READER_IMPL_PATH = "reader_impl"
        const val ROOT_CONTEXT_FACTORY_PATH = "root_context_factory"
        const val RUN_READER_CALL_PATH = "run_reader_call"
        const val GIVEN_PATH = "given"
        const val MAP_ENTRIES_PATH = "map_entries"
        const val SET_ELEMENTS_PATH = "set_elements"
        const val SIGNATURE_PATH = "signature"
    }

}
