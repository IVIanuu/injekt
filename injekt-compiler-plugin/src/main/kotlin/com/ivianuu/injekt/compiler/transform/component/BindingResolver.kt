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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.isAssistedProvider
import com.ivianuu.injekt.compiler.isNoArgProvider
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias BindingResolver = (Key) -> List<BindingNode>

class ChildComponentBindingResolver(
    private val parentFactory: ComponentImpl,
    descriptor: IrClass
) : BindingResolver {

    private val childComponents =
        mutableMapOf<Key, MutableList<Lazy<ChildComponentBindingNode>>>()

    init {
        /*descriptor
            .functions
            .filter { it.hasAnnotation(InjektFqNames.AstChildFactory) }
            .forEach { function ->
                val key = function.getFunctionType(parentFactory.pluginContext)
                    .withNoArgAnnotations(
                        parentFactory.pluginContext,
                        listOf(InjektFqNames.ChildFactory)
                    )
                    .asKey()

                childComponents.getOrPut(key) { mutableListOf() } += childFactoryBindingNode(
                    key, function
                )
            }*/
    }

    override fun invoke(requestedKey: Key): List<BindingNode> =
        childComponents[requestedKey]?.map { it.value } ?: emptyList()

    /*private fun childFactoryBindingNode(
        key: Key,
        function: IrFunction
    ) = lazy {
        val superType = function.returnType

        val moduleClass =
            function.getClassFromSingleValueAnnotation(
                InjektFqNames.AstClassPath,
                parentFactory.pluginContext
            )

        val fqName = FqName(
            function.getAnnotation(InjektFqNames.AstName)!!
                .getValueArgument(0)
                .let { it as IrConst<String> }
                .value
        )

        val childFactoryExpression = InjektDeclarationIrBuilder(
            parentFactory.pluginContext,
            moduleClass.symbol
        ).irLambda(key.type) { lambda ->
            val moduleAccessor = if (moduleClass.kind == ClassKind.OBJECT) {
                val expr: FactoryExpression = { irGetObject(moduleClass.symbol) }
                expr
            } else {
                val moduleFunction = parentFactory.declarationStore
                    .getModuleFunctionForClass(moduleClass)
                val moduleVariable = irTemporary(
                    irCall(moduleFunction).apply {
                        lambda.valueParameters.forEach {
                            putValueArgument(it.index, irGet(it))
                        }
                    }
                )
                val expr: FactoryExpression = { irGet(moduleVariable) }
                expr
            }
            val childFactoryImpl = FactoryImpl(
                factoryFunction = lambda,
                origin = fqName,
                parent = parentFactory,
                superType = superType,
                scope = function.getClassFromSingleValueAnnotationOrNull(
                    InjektFqNames.AstScope, parentFactory.pluginContext
                )
                    ?.defaultType
                    ?: function.returnType,
                factoryModuleAccessor = moduleAccessor,
                moduleClass = moduleClass,
                pluginContext = parentFactory.pluginContext,
                symbols = parentFactory.symbols,
                declarationStore = parentFactory.declarationStore
            )

            +irReturn(childFactoryImpl.getImplExpression())
        }

        return@lazy ChildComponentBindingNode(
            key = key,
            owner = parentFactory,
            origin = moduleClass.fqNameForIrSerialization,
            parent = parentFactory.clazz,
            childFactoryExpression = { childFactoryExpression }
        )
    }*/
}

class ProvideBindingResolver(
    private val pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val component: ComponentImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        return if (requestedKey.type.isAssistedProvider()) {
            /*val clazz = requestedKey.type.typeArguments.last().typeOrNull?.classOrNull
                ?.owner
                ?.let { component.declarationStore.readerTransformer.getTransformedClass(it) }
                ?: return emptyList()

            val constructor = clazz.getInjectConstructor()

            val scope = clazz.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Scoped, pluginContext
            )

            if (scope == null &&
                !clazz.hasAnnotation(InjektFqNames.Unscoped) &&
                constructor?.hasAnnotation(InjektFqNames.Unscoped) != true
            ) return emptyList()

            val parametersNameProvider = NameProvider()

            val constructorParameters = constructor?.valueParameters?.map { valueParameter ->
                InjektDeclarationIrBuilder.FactoryParameter(
                    name = parametersNameProvider.allocateForGroup(valueParameter.name).asString(),
                    type = valueParameter.type,
                    assisted = valueParameter.type.hasAnnotation(InjektFqNames.Assisted)
                )
            } ?: emptyList()

            val typeParametersMap = clazz
                .typeParameters
                .map { it.symbol }
                .associateWith { requestedKey.type.typeArguments[it.owner.index].typeOrFail }

            val dependencies = constructorParameters
                .filterNot { it.assisted }
                .map { parameter ->
                    BindingRequest(
                        key = parameter.type
                            .substituteAndKeepQualifiers(typeParametersMap)
                            .asKey(),
                        requestingKey = requestedKey,
                        requestOrigin = constructor?.valueParameters
                            ?.singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor
                            ?.fqNameSafe ?: clazz.properties
                            .singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor?.fqNameSafe
                    )
                }

            val assistedParameters = constructorParameters
                .filter { it.assisted }

            val factoryKey = pluginContext.tmpFunction(assistedParameters.size)
                .typeWith(assistedParameters.map { it.type } +
                        clazz.defaultType.typeWith(*typeParametersMap.values.toTypedArray()))
                .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                .asKey()

            if (factoryKey != requestedKey) return emptyList()

            val readerContext = clazz
                .getReaderConstructor()?.valueParameters?.last()?.type?.classOrNull?.owner

            listOf(
                AssistedProvisionBindingNode(
                    key = requestedKey,
                    context = readerContext,
                    dependencies = dependencies,
                    targetScope = scope?.defaultType,
                    scoped = scope != null,
                    module = null,
                    createExpression = newInstanceExpression(
                        clazz,
                        constructor,
                        constructorParameters
                    ),
                    parameters = constructorParameters,
                    owner = component,
                    origin = clazz.descriptor.fqNameSafe
                )
            )*/
            emptyList()
        } else {
            declarationGraph.bindings
                .filter { it.function.returnType.asKey() == requestedKey }
                .map { binding ->
                    val function = binding.function
                    val scope = function.getClassFromSingleValueAnnotationOrNull(
                        InjektFqNames.Scoped, pluginContext
                    )

                    val readerContext =
                        function.valueParameters.last().type.classOrNull!!.owner

                    val dependencies = mutableListOf<BindingRequest>()

                    dependencies += BindingRequest(
                        component.clazz.defaultType.asKey(), null, null
                    )

                    val processedSuperTypes = mutableSetOf<IrType>()

                    fun collectDependencies(
                        superClass: IrClass,
                        typeArguments: List<IrType>
                    ) {
                        if (superClass.defaultType in processedSuperTypes) return
                        processedSuperTypes += superClass.defaultType
                        for (declaration in superClass.declarations.toList()) {
                            if (declaration !is IrFunction) continue
                            if (declaration is IrConstructor) continue
                            if (declaration.isFakeOverride) continue
                            if (declaration.dispatchReceiverParameter?.type == component.factoryImpl.pluginContext.irBuiltIns.anyType) break
                            dependencies += BindingRequest(
                                key = declaration.returnType
                                    .substituteAndKeepQualifiers(
                                        superClass.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )
                                    .asKey(),
                                requestingKey = requestedKey,
                                requestOrigin = superClass.getAnnotation(InjektFqNames.Name)
                                    ?.getValueArgument(0)
                                    ?.let { it as IrConst<String> }
                                    ?.value
                                    ?.let { FqName(it) },
                            )
                        }

                        superClass.superTypes
                            .map { it to it.classOrNull?.owner }
                            .forEach { (superType, clazz) ->
                                if (clazz != null)
                                    collectDependencies(
                                        clazz,
                                        superType.typeArguments.map { it.typeOrFail }
                                    )
                            }
                    }

                    readerContext.superTypes.forEach { superType ->
                        collectDependencies(
                            superType.classOrNull!!.owner,
                            superType.typeArguments.map { it.typeOrFail }
                        )
                    }

                    val parameters = mutableListOf<InjektDeclarationIrBuilder.FactoryParameter>()

                    /*parameters += providerType
                        .getFunctionParameterTypes()
                        .dropLast(1)
                        .mapIndexed { index, type ->
                            InjektDeclarationIrBuilder.FactoryParameter(
                                name = "p$index",
                                type = type,
                                assisted = true
                            )
                        }*/

                    parameters += InjektDeclarationIrBuilder.FactoryParameter(
                        name = "_context",
                        type = readerContext.defaultType,
                        assisted = false
                    )

                    val assistedParameters = parameters.filter { it.assisted }

                    ProvisionBindingNode(
                        key = requestedKey,
                        context = readerContext,
                        dependencies = dependencies,
                        targetScope = scope?.defaultType,
                        scoped = scope != null,
                        createExpression = { parametersMap ->
                            irCall(function).apply {
                                if (function.dispatchReceiverParameter != null) {
                                    dispatchReceiver = irGetObject(
                                        function.dispatchReceiverParameter!!.type.classOrNull!!
                                    )
                                }

                                parametersMap.values.forEachIndexed { index, expression ->
                                    putValueArgument(
                                        index,
                                        expression()
                                    )
                                }
                            }
                        },
                        parameters = parameters,
                        owner = component,
                        origin = function.descriptor.fqNameSafe
                    )
                }
        }
    }

    private fun newInstanceExpression(
        clazz: IrClass,
        constructor: IrConstructor?,
        constructorParameters: List<InjektDeclarationIrBuilder.FactoryParameter>
    ): IrBuilderWithScope.(Map<InjektDeclarationIrBuilder.FactoryParameter, () -> IrExpression?>) -> IrExpression {
        return { parametersMap ->
            if (clazz.kind == ClassKind.OBJECT) {
                irGetObject(clazz.symbol)
            } else {
                irCall(constructor!!).apply {
                    constructorParameters
                        .forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                parametersMap.getValue(parameter)()
                            )
                        }
                }
            }
        }
    }
}

class NoArgProviderBindingResolver(
    private val component: ComponentImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        return when {
            requestedType.isNoArgProvider() ->
                listOf(
                    ProviderBindingNode(
                        requestedKey,
                        component,
                        null
                    )
                )
            else -> emptyList()
        }
    }
}

class ComponentImplBindingResolver(
    private val componentNode: ComponentRequirementNode
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != componentNode.key &&
            componentNode.component.factoryImpl.node.entryPoints.none {
                it.entryPoint.defaultType.asKey() == requestedKey
            }
        ) return emptyList()
        return listOf(
            ComponentImplBindingNode(
                componentNode
            )
        )
    }
}
