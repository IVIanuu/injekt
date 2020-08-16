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
import com.ivianuu.injekt.compiler.transform.readercontext.ReaderContextImplTransformer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    val components: List<IrClass> by lazy {
        indexer.classIndices(listOf(COMPONENT_CONTEXT_PATH))
    }

    val genericContexts: List<IrClass> by lazy {
        indexer.classIndices(listOf(GENERIC_CONTEXT_PATH))
    }

    private val bindingsByKey = mutableMapOf<String, List<IrFunction>>()
    fun bindings(key: String) = bindingsByKey.getOrPut(key) {
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

    private val mapEntriesByKey = mutableMapOf<String, List<IrFunction>>()
    fun mapEntries(key: String) = mapEntriesByKey.getOrPut(key) {
        indexer.functionIndices(listOf(MAP_ENTRIES_PATH, key))
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    private val setElementsByKey = mutableMapOf<String, List<IrFunction>>()
    fun setElements(key: String) = setElementsByKey.getOrPut(key) {
        indexer.functionIndices(listOf(SET_ELEMENTS_PATH, key))
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
    }

    lateinit var readerContextImplTransformer: ReaderContextImplTransformer

    sealed class ParentRunReaderContext {
        data class Known(val clazz: IrClass) : ParentRunReaderContext() {
            override fun toString(): String {
                return "Known(context=${clazz.descriptor.fqNameSafe})"
            }
        }

        data class Unknown(
            val origin: IrFunction
        ) : ParentRunReaderContext() {
            override fun toString(): String {
                return "Unknown(origin=${origin.descriptor.fqNameSafe})"
            }
        }
    }

    fun getParentRunReaderContexts(context: IrClass): List<ParentRunReaderContext> {
        val parents = mutableListOf<ParentRunReaderContext>()

        val processedClasses = mutableSetOf<IrClass>()

        val invokingContexts = getCallingContexts(context)

        fun collectParents(invokingContext: IrClass) {
            if (invokingContext in processedClasses) return
            processedClasses += invokingContext

            if (isRunReaderContext(invokingContext)) {
                parents += ParentRunReaderContext.Known(invokingContext)
                return
            }

            parents += getGivenDeclarationsForContext(invokingContext)
                .flatMapFix { declaration ->
                    val generatedContextsWithInvokerSuperType = readerContextImplTransformer
                        .generatedContexts
                        .values
                        .flatten()
                        .filter { invokingContext.defaultType in it.superTypes }
                        .map { it.superTypes.first().classOrNull!!.owner }

                    if (generatedContextsWithInvokerSuperType.isNotEmpty()) {
                        generatedContextsWithInvokerSuperType
                            .map { ParentRunReaderContext.Known(it) }
                    } else {
                        listOf(ParentRunReaderContext.Unknown(declaration))
                    }
                }

            getCallingContexts(invokingContext)
                .forEach { collectParents(it) }
        }

        invokingContexts.forEach { collectParents(it) }

        return parents
    }

    fun getNonGenericParentContext(context: IrClass): List<IrClass> {
        val parents = mutableListOf<IrClass>()

        val processedClasses = mutableSetOf<IrClass>()

        val invokingContexts = getCallingContexts(context)

        fun collectParents(invokingContext: IrClass) {
            if (invokingContext in processedClasses) return
            processedClasses += invokingContext

            if (invokingContext.typeParameters.isEmpty()) {
                parents += invokingContext
                return
            }

            getCallingContexts(invokingContext)
                .forEach { collectParents(it) }
        }

        invokingContexts.forEach { collectParents(it) }

        return parents
    }

    private fun isRunReaderContext(context: IrClass): Boolean {
        return components
            .map { it.superTypes.first() }
            .any { it == context.defaultType }
    }

    private fun getGivenDeclarationsForContext(context: IrClass): List<IrFunction> {
        return indexer.classIndices(
            listOf(GIVEN_CONTEXTS_PATH, context.descriptor.fqNameSafe.asString())
        )
            .flatMapFix {
                val key =
                    it.getConstantFromAnnotationOrNull<String>(InjektFqNames.GivenContext, 0)!!
                bindings(key) + mapEntries(key) + setElements(key)
            }
    }

    private fun getCallingContexts(context: IrClass): Set<IrClass> {
        val allContexts = listOf(context) + getAllSuperContexts(context)

        val invokerIfRunChildReader = components
            .singleOrNull { it.superTypes[0] == context.defaultType }
            ?.superTypes
            ?.getOrNull(1)
            ?.classOrNull
            ?.owner

        return setOfNotNull(
            *allContexts
                .flatMapFix {
                    indexer.classIndices(
                        listOf(
                            READER_CALL_CALLEE_TO_CALLER_PATH,
                            it.descriptor.fqNameSafe.asString()
                        )
                    )
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderCall,
                        0
                    )!!
                }
                .filter { it != context }
                .toTypedArray(),
            invokerIfRunChildReader
        )
    }

    private val superContextsByContext = mutableMapOf<IrClass, Set<IrClass>>()
    private fun getAllSuperContexts(
        context: IrClass
    ): Set<IrClass> = superContextsByContext.getOrPut(context) {
        indexer.classIndices(
            listOf(
                READER_IMPL_SUB_TO_SUPER_PATH, context.descriptor.fqNameSafe.asString()
            )
        )
            .map {
                it.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    0
                )!!
            }
            .toSet()
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
        const val READER_CALL_CALLEE_TO_CALLER_PATH = "readerinvocationcalleetocaller"
        const val READER_CALL_CALLER_TO_CALLEE_PATH = "readerinvocationcallertocallee"
        const val READER_IMPL_SUPER_TO_SUB_PATH = "readerimplsupertosub"
        const val READER_IMPL_SUB_TO_SUPER_PATH = "readerimplsubtosuper"
        const val COMPONENT_CONTEXT_PATH = "component"
        const val RUN_READER_CALL_PATH = "runreadercall"
        const val GIVEN_PATH = "given"
        const val GIVEN_CONTEXTS_PATH = "givencontexts"
        const val GENERIC_CONTEXT_PATH = "genericcontext"
        const val MAP_ENTRIES_PATH = "mapentries"
        const val SET_ELEMENTS_PATH = "setelements"
        const val SIGNATURE_PATH = "signature"
    }

}
