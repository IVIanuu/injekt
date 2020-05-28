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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.ClassKey
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.IntKey
import com.ivianuu.injekt.compiler.LongKey
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.StringKey
import com.ivianuu.injekt.compiler.findPropertyGetter
import com.ivianuu.injekt.compiler.getIrClass
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.StringValue

class Graph(
    val parent: Graph?,
    val factory: AbstractFactory,
    context: IrPluginContext,
    factoryModule: ModuleNode?,
    declarationStore: InjektDeclarationStore,
    val symbols: InjektSymbols
) {

    private val scopes = mutableSetOf<FqName>()

    private val explicitBindingResolvers = mutableListOf<BindingResolver>()
    private val implicitBindingResolvers = mutableListOf<BindingResolver>()
    private val mapBindingResolver: MapBindingResolver =
        MapBindingResolver(
            context,
            factory,
            parent?.mapBindingResolver
        )
    private val setBindingResolver: SetBindingResolver =
        SetBindingResolver(
            context,
            factory,
            parent?.setBindingResolver
        )
    private val resolvedBindings = mutableMapOf<Key, BindingNode>()

    private val chain = mutableSetOf<Key>()

    init {
        if (factoryModule != null) addModule(factoryModule)
        implicitBindingResolvers += LazyOrProviderBindingResolver(
            factory
        )
        implicitBindingResolvers += mapBindingResolver
        implicitBindingResolvers += setBindingResolver
        implicitBindingResolvers += MembersInjectorBindingResolver(
            symbols,
            declarationStore,
            factory
        )
        if (factory is ImplFactory) {
            implicitBindingResolvers += FactoryImplementationBindingResolver(
                factory.factoryImplementationNode
            )
        }
        implicitBindingResolvers += AnnotatedClassBindingResolver(
            context,
            declarationStore,
            factory
        )
    }

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val explicitBindings = explicitBindingResolvers.flatMap { it(request.key) }
        if (explicitBindings.size > 1) {
            error(
                "Multiple bindings found for '${request.key}' at:\n${explicitBindings.joinToString(
                    "\n"
                ) {
                    "'${it.origin.orUnknown()}'"
                }} in '${factory.origin}'"
            )
        }

        binding = explicitBindings.singleOrNull()

        if (binding == null) {
            val implicitBindings = implicitBindingResolvers.flatMap { it(request.key) }
            binding = implicitBindings.singleOrNull()
            if (binding?.targetScope != null && binding.targetScope !in scopes) {
                if (parent == null) {
                    error(
                        "Scope mismatch binding '${binding.key}' " +
                                "with scope '${binding.targetScope}' is not compatible with this component '${factory.origin}' scopes [$scopes]"
                    )
                } else {
                    binding = null
                }
            }
        }

        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        parent?.getBinding(request)?.let { return it }

        if (request.key.type.isMarkedNullable()) {
            binding = NullBindingNode(request.key, factory)
            resolvedBindings[request.key] = binding
            return binding
        }

        error(
            "No binding found for '${request.key}' " +
                    "required at '${request.requestOrigin.orUnknown()}' " +
                    "in '${factory.origin}'"
        )
    }

    fun validate(request: BindingRequest) {
        check(request.key !in chain) {
            "Circular dependency for '${request.key}' in '${factory.origin}'"
        }
        chain += request.key
        val binding = getBinding(request)
        binding.dependencies.forEach { validate(it) }
        chain -= request.key
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it.hasAnnotation(InjektFqNames.AstModule)
        } as IrClass

        val functions = descriptor.functions.toList()

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.getClass()!!.fqNameForIrSerialization) }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { function ->
                val dependencyName = function.getAnnotation(InjektFqNames.AstPropertyPath)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value
                val dependencyType = function.returnType
                    .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                addExplicitBindingResolver(
                    DependencyBindingResolver(
                        moduleNode = moduleNode,
                        dependencyNode = DependencyNode(
                            dependency = function.returnType.getClass()!!,
                            key = dependencyType.asKey(),
                            accessor = {
                                irCall(moduleNode.module.findPropertyGetter(dependencyName)).apply {
                                    dispatchReceiver = moduleNode.accessor(this@DependencyNode)
                                }
                            }
                        ),
                        factory = factory
                    )
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMap) }
            .forEach { function ->
                addMap(
                    key = function
                        .returnType
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    origin = module.fqNameForIrSerialization
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMapEntry) }
            .forEach { function ->
                val entryKey = function.valueParameters[1].let { entry ->
                    val entryDescriptor = entry.descriptor
                    when {
                        entryDescriptor.hasAnnotation(InjektFqNames.AstMapClassKey) -> {
                            ClassKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapClassKey)
                                !!.allValueArguments.values.single())
                                    .let { it as KClassValue }
                                    .getIrClass(factory.pluginContext)
                                    .defaultType
                            )
                        }
                        entryDescriptor.hasAnnotation(InjektFqNames.AstMapTypeParameterClassKey) -> {
                            ClassKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapTypeParameterClassKey)
                                !!.allValueArguments.values.single())
                                    .let { it as StringValue }
                                    .value
                                    .let { typeParameterName ->
                                        moduleNode.typeParametersMap.toList()
                                            .single { it.first.descriptor.name.asString() == typeParameterName }
                                            .second
                                    }
                            )
                        }
                        entryDescriptor.hasAnnotation(InjektFqNames.AstMapIntKey) -> {
                            IntKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapIntKey)
                                !!.allValueArguments.values.single())
                                    .let { it as IntValue }
                                    .value
                            )
                        }
                        entryDescriptor.hasAnnotation(InjektFqNames.AstMapLongKey) -> {
                            LongKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapLongKey)
                                !!.allValueArguments.values.single())
                                    .let { it as LongValue }
                                    .value
                            )
                        }
                        entryDescriptor.hasAnnotation(InjektFqNames.AstMapStringKey) -> {
                            StringKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapStringKey)
                                !!.allValueArguments.values.single())
                                    .let { it as StringValue }
                                    .value
                            )
                        }
                        else -> error("Corrupt map binding ${function.dump()}")
                    }
                }
                putMapEntry(
                    mapKey = function.valueParameters[0].type
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    entryKey = entryKey,
                    entryValue = function.valueParameters[1].type
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    origin = module.fqNameForIrSerialization
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSet) }
            .forEach { function ->
                addSet(
                    key = function.returnType
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    origin = module.fqNameForIrSerialization
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSetElement) }
            .forEach { function ->
                addSetElement(
                    setKey = function.valueParameters[0].type
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    elementKey = function.valueParameters[1].type
                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                        .asKey(),
                    origin = module.fqNameForIrSerialization
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstModule) }
            .forEach { function ->
                val includedModule = function.returnType.classOrNull!!.owner

                val key = function.returnType
                    .substitute(moduleNode.descriptorTypeParametersMap)
                    .asKey()

                val includedModuleAccessor = if (includedModule.kind == ClassKind.OBJECT) {
                    val expr: FactoryExpression = { irGetObject(includedModule.symbol) }
                    expr
                } else {
                    val moduleName = function.getAnnotation(InjektFqNames.AstPropertyPath)!!
                        .getValueArgument(0)
                        .let { it as IrConst<String> }
                        .value
                    val propertyGetter = moduleNode.module.findPropertyGetter(moduleName)
                    val expr: FactoryExpression = expr@{
                        irCall(propertyGetter).apply {
                            dispatchReceiver = moduleNode.accessor(this@expr)
                        }
                    }
                    expr
                }

                val typeParametersMap = includedModule.typeParameters
                    .map {
                        it.symbol to function.returnType.typeArguments[it.index]
                            .typeOrFail
                            .substitute(moduleNode.descriptorTypeParametersMap)
                    }
                    .toMap()
                    .mapValues { it.value.substituteAndKeepQualifiers(moduleNode.typeParametersMap) }

                addModule(
                    ModuleNode(
                        module = includedModule,
                        key = key,
                        accessor = includedModuleAccessor,
                        typeParametersMap = typeParametersMap
                    )
                )
            }

        addExplicitBindingResolver(
            ModuleBindingResolver(
                moduleNode,
                descriptor,
                factory
            )
        )

        if (factory is ImplFactory) {
            addExplicitBindingResolver(
                ChildFactoryBindingResolver(
                    factory,
                    descriptor
                )
            )
        }
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addExplicitBindingResolver(bindingResolver: BindingResolver) {
        explicitBindingResolvers += bindingResolver
    }

    private fun addMap(key: Key, origin: FqName) {
        mapBindingResolver.addMap(key, origin)
    }

    private fun putMapEntry(
        mapKey: Key,
        entryKey: MapKey,
        entryValue: Key,
        origin: FqName
    ) {
        mapBindingResolver.putMapEntry(
            mapKey, entryKey,
            BindingRequest(entryValue, origin)
        )
    }

    private fun addSet(key: Key, origin: FqName) {
        setBindingResolver.addSet(key, origin)
    }

    private fun addSetElement(
        setKey: Key,
        elementKey: Key,
        origin: FqName
    ) {
        setBindingResolver.addSetElement(
            setKey,
            BindingRequest(elementKey, origin)
        )
    }
}
