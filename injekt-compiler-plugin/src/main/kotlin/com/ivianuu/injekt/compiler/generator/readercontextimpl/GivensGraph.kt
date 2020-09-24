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

package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.CallableRef
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.GivenSetDescriptor
import com.ivianuu.injekt.compiler.generator.ReaderContextDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Given
class GivensGraph(private val owner: ContextImpl) {

    private val parent = owner.factoryImpl.parent?.graph

    private val statements = owner.statements
    private val inputs = owner.inputTypes

    private val declarationStore = given<DeclarationStore>()

    private val contextId = owner.contextId

    private val instanceNodes = inputs
        .mapIndexed { index, inputType -> InputGivenNode(inputType, "p$index", owner) }
        .groupBy { it.type }

    private val givenSetGivens = mutableMapOf<TypeRef, MutableList<GivenNode>>()
    private val givenSetMapEntries = mutableMapOf<TypeRef, MutableList<CallableRef>>()
    private val givenSetSetElements = mutableMapOf<TypeRef, MutableList<CallableRef>>()

    val resolvedGivens = mutableMapOf<TypeRef, GivenNode>()

    sealed class ChainElement {
        class Given(val type: TypeRef) : ChainElement() {
            override fun toString() = type.render()
        }

        class Call(val fqName: FqName?) : ChainElement() {
            override fun toString() = fqName.orUnknown()
        }
    }

    private val chain = mutableListOf<ChainElement>()
    private val checkedTypes = mutableSetOf<TypeRef>()
    private val checkedGivens = mutableSetOf<GivenNode>()

    init {
        val givenSets = inputs
            .filter { it.isGivenSet }
            .map { declarationStore.getGivenSetForType(it) }

        fun GivenSetDescriptor.collectGivens(
            parentAccessStatement: ContextStatement?,
            parentCallable: CallableRef?
        ) {
            val thisAccessStatement: ContextStatement = {
                if (parentAccessStatement == null) {
                    emit("p${inputs.indexOf(type)}")
                } else {
                    parentAccessStatement()
                    emit(".")
                    emit("${parentCallable!!.name}")
                    if (!parentCallable.isPropertyAccessor) {
                        emit("()")
                    }
                }
            }

            for (callable in callables) {
                when (callable.givenKind) {
                    CallableRef.GivenKind.GIVEN -> {
                        givenSetGivens.getOrPut(callable.type) { mutableListOf() } += CallableGivenNode(
                            type = callable.type,
                            owner = owner,
                            contexts = listOf(declarationStore.getReaderContextForCallable(callable)!!),
                            origin = callable.fqName,
                            external = callable.isExternal,
                            targetContext = callable.targetContext,
                            givenSetAccessStatement = thisAccessStatement,
                            callable = callable
                        )
                    }
                    CallableRef.GivenKind.GIVEN_MAP_ENTRIES -> {
                        givenSetMapEntries.getOrPut(callable.type) { mutableListOf() } += callable
                    }
                    CallableRef.GivenKind.GIVEN_SET_ELEMENTS -> {
                        givenSetSetElements.getOrPut(callable.type) { mutableListOf() } += callable
                    }
                    CallableRef.GivenKind.GIVEN_SET -> {
                        declarationStore.getGivenSetForType(callable.type)
                            .collectGivens(thisAccessStatement, callable)
                    }
                }.let {}
            }
        }

        givenSets.forEach { it.collectGivens(null, null) }
    }

    fun checkEntryPoints(entryPoints: List<ReaderContextDescriptor>) {
        entryPoints.forEach { check(it) }
    }

    private fun check(given: GivenNode) {
        if (given in checkedGivens) return
        chain.push(ChainElement.Given(given.type))
        checkedGivens += given
        given
            .contexts
            .forEach {
                check(it)
                getGiven(it.type)
            }
        chain.pop()
    }

    private fun check(context: ReaderContextDescriptor) {
        if (context.type in checkedTypes) return
        checkedTypes += context.type
        chain.push(ChainElement.Call(context.origin))
        val substitutionMap = context.type.classifier.typeParameters
            .zip(context.type.typeArguments)
            .toMap()
        context.givenTypes
            .map { it.substitute(substitutionMap) }
            .forEach { givenType ->
                println("check type ${givenType.render()} subs $substitutionMap")
                val existingFunction = owner.members.singleOrNull {
                    it is ContextFunction && it.name == givenType.uniqueTypeName()
                } as? ContextFunction
                if (existingFunction != null) {
                    existingFunction.isOverride = true
                    return@forEach
                }
                check(getGiven(givenType))
            }
        chain.pop()
    }

    fun getGiven(type: TypeRef): GivenNode {
        var given = getGivenOrNull(type)
        if (given != null) return given

        if (type.isMarkedNullable) {
            given = NullGivenNode(type, owner)
            resolvedGivens[type] = given
            return given
        }

        error(
            buildString {
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine("No given found for '${type.render()}' in '${contextId.render()}':")

                chain.forEachIndexed { index, element ->
                    if (index == 0) {
                        appendLine("${indendation}runReader call '${element}'")
                    } else {
                        when (element) {
                            is ChainElement.Call -> {
                                val lastElement = chain.getOrNull(index - 1)
                                if (lastElement is ChainElement.Given) {
                                    appendLine("${indendation}given by '${element}'")
                                } else {
                                    appendLine("${indendation}calls reader '${element}'")
                                }
                            }
                            is ChainElement.Given -> appendLine("${indendation}requires given '${element}'")
                        }

                    }
                    indent()
                }
            }
        )
    }

    private fun getGivenOrNull(type: TypeRef): GivenNode? {
        var given = resolvedGivens[type]
        if (given != null) return given

        fun GivenNode.check(): GivenNode? {
            if (targetContext != null && targetContext != contextId) {
                if (parent == null) {
                    error(
                        "Context mismatch, given '${type.render()}' " +
                                "is scoped to '${targetContext!!.render()}' but actual context is " +
                                contextId.render()
                    )
                } else {
                    return null
                }
            }

            return this
        }

        val allGivens = givensForKey(type)

        val instanceAndGivenSetGivens = allGivens
            .filter { it.type == type }
            .filter { it is InputGivenNode || it.givenSetAccessStatement != null }

        if (instanceAndGivenSetGivens.size > 1) {
            error(
                "Multiple givens found in inputs for '${type.render()}' at:\n${
                    instanceAndGivenSetGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = instanceAndGivenSetGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            return it
        }

        val (internalGlobalGivens, externalGlobalGivens) = allGivens
            .filterNot { it is InputGivenNode }
            .filter { it.givenSetAccessStatement == null }
            .filter { it.type == type }
            .partition { !it.external }

        if (internalGlobalGivens.size > 1) {
            error(
                "Multiple internal givens found for '${type.render()}' at:\n${
                    internalGlobalGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = internalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            (it as? ChildContextGivenNode)?.childFactoryImpl?.initialize()
            (it as? CalleeContextGivenNode)?.calleeContextStatement
            return it
        }

        if (externalGlobalGivens.size > 1) {
            error(
                "Multiple external givens found for '${type.render()}' at:\n${
                    externalGlobalGivens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                }.\nPlease specify a given for the requested type in this project."
            )
        }

        given = externalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[type] = it
            check(it)
            return it
        }

        parent?.getGivenOrNull(type)?.let {
            resolvedGivens[type] = it
            return it
        }

        return null
    }

    private fun givensForKey(type: TypeRef): List<GivenNode> = buildList<GivenNode> {
        instanceNodes[type]?.let { this += it }

        if (type == contextId) {
            this += SelfGivenNode(
                type = type,
                context = owner
            )
        }

        if (type.isContext) {
            val contexts = mutableListOf<ReaderContextDescriptor>()
            this += CalleeContextGivenNode(
                type = type,
                owner = owner,
                origin = null,
                lazyContexts = { contexts },
                lazyCalleeContextStatement = {
                    if (type.typeArguments.isNotEmpty()) {
                        class GivenTypeWithStatement(
                            val type: TypeRef,
                            val name: Name,
                            val statement: ContextStatement
                        )

                        val givenTypesWithStatements = declarationStore
                            .getReaderContextByFqName(type.classifier.fqName)!!
                            .givenTypes
                            .map {
                                it to it.substitute(
                                    type.classifier
                                        .typeParameters
                                        .zip(type.typeArguments)
                                        .toMap()
                                )
                            }
                            .map { (originalType, substitutedType) ->
                                val statement = statements.getGivenStatement(
                                    getGiven(substitutedType),
                                    false
                                )
                                GivenTypeWithStatement(
                                    substitutedType,
                                    if (originalType == substitutedType) substitutedType.uniqueTypeName()
                                    else originalType.uniqueTypeName(),
                                    statement
                                )
                            }

                        return@CalleeContextGivenNode {
                            emit("object : ${type.render()} ")
                            braced {
                                givenTypesWithStatements.forEach { typeWithStatement ->
                                    emit("override fun ${typeWithStatement.name}(): ${typeWithStatement.type.render()} ")
                                    braced {
                                        emit("return ")
                                        typeWithStatement.statement(this)
                                    }
                                }
                            }
                        }
                    } else {
                        owner.superTypes += type
                        declarationStore.getReaderContextByFqName(type.classifier.fqName)!!
                            .givenTypes
                            .forEach { statements.getGivenStatement(getGiven(it), true) }
                        return@CalleeContextGivenNode {
                            emit("this@${owner.name}")
                        }
                    }
                }
            )
        }

        if (type.isChildContextFactory) {
            val existingFactories = mutableSetOf<TypeRef>()
            var currentContext: ContextImpl? = owner
            while (currentContext != null) {
                existingFactories += currentContext.contextId
                currentContext = currentContext.factoryImpl.parent
            }
            if (type !in existingFactories) {
                val factoryDescriptor = given<DeclarationStore>()
                    .getContextFactoryForFqName(type.classifier.fqName)
                val factoryImpl = ContextFactoryImpl(
                    name = owner.factoryImpl.contextTreeNameProvider("F").asNameId(),
                    factoryType = type,
                    inputTypes = factoryDescriptor.inputTypes,
                    contextType = factoryDescriptor.contextType,
                    parent = owner
                )
                this += ChildContextGivenNode(
                    type = type,
                    owner = owner,
                    origin = null,
                    childFactoryImpl = factoryImpl
                )
            }
        }

        givenSetGivens[type]?.let { this += it }
        this += declarationStore.givens(type)
            .filter { it.targetContext == null || it.targetContext == contextId }
            .map { callable ->
                CallableGivenNode(
                    type = type,
                    owner = owner,
                    contexts = listOf(declarationStore.getReaderContextForCallable(callable)!!),
                    external = callable.isExternal,
                    origin = callable.fqName,
                    callable = callable,
                    targetContext = callable.targetContext,
                    givenSetAccessStatement = null
                )
            }

        ((givenSetMapEntries[type] ?: emptyList()) + declarationStore.givenMapEntries(type))
            .filter { it.targetContext == null || it.targetContext == contextId }
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                MapGivenNode(
                    type = type,
                    owner = owner,
                    contexts = entries.map {
                        declarationStore
                            .getReaderContextForCallable(it)!!
                    },
                    givenSetAccessStatement = null,
                    entries = entries
                )
            }
            ?.let { this += it }

        ((givenSetSetElements[type] ?: emptyList()) + declarationStore.givenSetElements(type))
            .filter { it.targetContext == null || it.targetContext == contextId }
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                SetGivenNode(
                    type = type,
                    owner = owner,
                    contexts = elements.map {
                        declarationStore
                            .getReaderContextForCallable(it)!!
                    },
                    givenSetAccessStatement = null,
                    elements = elements
                )
            }
            ?.let { this += it }
    }

}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"
