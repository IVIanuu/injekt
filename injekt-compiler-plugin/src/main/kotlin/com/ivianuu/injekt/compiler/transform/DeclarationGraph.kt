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
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    val rootContextFactories: List<IrClass> by lazy {
        indexer.classIndices(listOf(ROOT_CONTEXT_FACTORY_PATH))
    }

    val genericContexts: List<IrClass> by lazy {
        indexer.classIndices(listOf(GENERIC_CONTEXT_PATH))
    }

    private val givensByKey = mutableMapOf<String, List<IrFunction>>()
    fun givens(key: String) = givensByKey.getOrPut(key) {
        (indexer.functionIndices(listOf(GIVEN_PATH, key)) +
                indexer.classIndices(listOf(GIVEN_PATH, key))
                    .flatMapFix { it.constructors.toList() } +
                indexer.propertyIndices(listOf(GIVEN_PATH, key))
                    .mapNotNull { it.getter }
                )
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
    }

    private val givenMapEntriesByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenMapEntries(key: String) = givenMapEntriesByKey.getOrPut(key) {
        indexer.functionIndices(listOf(MAP_ENTRIES_PATH, key))
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val givenSetElementsByKey = mutableMapOf<String, List<IrFunction>>()
    fun givenSetElements(key: String) = givenSetElementsByKey.getOrPut(key) {
        indexer.functionIndices(listOf(SET_ELEMENTS_PATH, key))
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    fun getRunReaderContexts(context: IrClass): List<IrClass> {
        return indexer.classIndices(
            listOf(
                RUN_READER_CALL_PATH,
                context.descriptor.fqNameSafe.asString()
            )
        )
            .map { it.getClassFromAnnotation(InjektFqNames.RunReaderCall, 0)!! }
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
                    READER_IMPL_SUPER_TO_SUB_PATH,
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
                    READER_CALL_CALLER_TO_CALLEE_PATH,
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
        const val READER_CALL_CALLEE_TO_CALLER_PATH = "reader_invocation_callee_to_caller"
        const val READER_CALL_CALLER_TO_CALLEE_PATH = "reader_invocation_caller_to_callee"
        const val READER_IMPL_SUPER_TO_SUB_PATH = "reader_impl_super_to_sub"
        const val READER_IMPL_SUB_TO_SUPER_PATH = "reader_impl_sub_to_super"
        const val ROOT_CONTEXT_FACTORY_PATH = "root_context_factory"
        const val RUN_READER_CALL_PATH = "run_reader_call"
        const val GIVEN_PATH = "given"
        const val GIVEN_CONTEXTS_PATH = "given_contexts"
        const val GENERIC_CONTEXT_PATH = "generic_context"
        const val MAP_ENTRIES_PATH = "map_entries"
        const val SET_ELEMENTS_PATH = "set_elements"
        const val SIGNATURE_PATH = "signature"
    }

}
