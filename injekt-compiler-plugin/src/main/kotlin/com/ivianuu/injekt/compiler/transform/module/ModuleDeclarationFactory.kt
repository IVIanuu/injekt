package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.ClassPath
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.Path
import com.ivianuu.injekt.compiler.PropertyPath
import com.ivianuu.injekt.compiler.TypeParameterPath
import com.ivianuu.injekt.compiler.ValueParameterPath
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ModuleDeclarationFactory(
    private val module: ModuleImplementation,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val nameProvider: NameProvider,
    private val declarationStore: InjektDeclarationStore,
    private val providerFactory: ModuleProviderFactory
) {

    private val moduleClass get() = module.clazz

    fun create(call: IrCall): List<ModuleDeclaration> {
        val callee = call.symbol.descriptor

        val declarations = mutableListOf<ModuleDeclaration>()

        when {
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.scope" -> {
                declarations += ScopeDeclaration(call.getTypeArgument(0)!!)
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.dependency" -> {
                val dependencyType = call.getTypeArgument(0)!!

                val property = InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                    .fieldBakedProperty(
                        moduleClass,
                        Name.identifier(nameProvider.allocate("dependency")),
                        dependencyType
                    )

                val path = PropertyPath(property)

                module.initializerBlocks += {
                    irSetField(
                        it(),
                        property.backingField!!,
                        call.getValueArgument(0)!!
                    )
                }

                declarations += DependencyDeclaration(dependencyType, path)
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.childFactory" -> {
                val factoryRef = call.getValueArgument(0)!! as IrFunctionReference
                val factoryModuleClass = declarationStore.getModuleClassOrNull(
                    declarationStore.getModuleFunctionForFactory(factoryRef.symbol.owner)
                )
                declarations += ChildFactoryDeclaration(factoryRef, factoryModuleClass)
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.alias" -> {
                declarations += AliasDeclaration(
                    call.getTypeArgument(0)!!,
                    call.getTypeArgument(1)!!
                )
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.transient" ||
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.scoped" ||
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.instance" -> {
                val bindingQualifiers =
                    pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
                val bindingType = call.getTypeArgument(0)!!
                    .withAnnotations(bindingQualifiers)

                val singleArgument = if (call.valueArgumentsCount != 0)
                    call.getValueArgument(0) else null

                val bindingPath: Path
                val parameters =
                    mutableListOf<InjektDeclarationIrBuilder.ProviderParameter>()

                fun addParametersFromProvider(provider: IrClass) {
                    val assisted = provider.functions
                        .single { it.name.asString() == "invoke" }
                        .valueParameters
                        .map { it.type }

                    val nonAssisted = provider.constructors
                        .single()
                        .valueParameters
                        .map { it.type.typeArguments.single() }

                    parameters += (assisted + nonAssisted).mapIndexed { index, type ->
                        InjektDeclarationIrBuilder.ProviderParameter(
                            name = "p$index",
                            type = type,
                            assisted = type in assisted
                        )
                    }
                }

                if (singleArgument == null) {
                    if (bindingType.toKotlinType().isTypeParameter()) {
                        bindingPath = TypeParameterPath(
                            module.function.typeParameters.single {
                                it.descriptor ==
                                        bindingType.toKotlinType().constructor.declarationDescriptor
                            }
                        )
                    } else {
                        val provider = providerFactory.providerForClass(
                            name = Name.identifier(nameProvider.allocate("Factory")),
                            clazz = bindingType.classOrFail
                                .ensureBound(pluginContext.irProviders).owner,
                            visibility = module.clazz.visibility
                        )
                        module.clazz.addChild(provider)
                        addParametersFromProvider(provider)
                        bindingPath = ClassPath(provider)
                    }
                } else {
                    if (callee.name.asString() == "instance") {
                        val property = InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                            .fieldBakedProperty(
                                moduleClass,
                                Name.identifier(nameProvider.allocate("instance")),
                                bindingType
                            )

                        module.initializerBlocks += {
                            irSetField(
                                it(),
                                property.backingField!!,
                                singleArgument
                            )
                        }

                        bindingPath = PropertyPath(property)
                    } else {
                        when (singleArgument) {
                            is IrFunctionExpression -> {
                                val provider = providerFactory.providerForDefinition(
                                    name = Name.identifier(nameProvider.allocate("Factory")),
                                    definition = singleArgument,
                                    visibility = module.clazz.visibility,
                                    moduleParametersMap = module.parameterMap,
                                    moduleFieldsByParameter = module.fieldsByParameters
                                )
                                module.clazz.addChild(provider)
                                addParametersFromProvider(provider)
                                bindingPath = ClassPath(provider)
                            }
                            is IrGetValue -> {
                                bindingPath = ValueParameterPath(
                                    module.function.valueParameters.single {
                                        it.symbol == singleArgument.symbol
                                    }
                                )
                            }
                            else -> {
                                // todo enforce in frontend
                                error("Unexpected definition ${singleArgument.dump()}")
                            }
                        }
                    }
                }

                declarations += BindingDeclaration(
                    bindingType = bindingType,
                    parameters = parameters,
                    scoped = callee.name.asString() == "scoped",
                    path = bindingPath
                )
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.map" -> {
                val mapQualifiers =
                    pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
                val mapKeyType = call.getTypeArgument(0)!!
                val mapValueType = call.getTypeArgument(1)!!

                val mapType = pluginContext.symbolTable.referenceClass(pluginContext.builtIns.map)
                    .ensureBound(pluginContext.irProviders)
                    .typeWith(mapKeyType, mapValueType)
                    .withAnnotations(mapQualifiers)

                declarations += MapDeclaration(mapType)

                val mapBlock = call.getValueArgument(0) as? IrFunctionExpression
                mapBlock?.function?.body?.transformChildrenVoid(object :
                    IrElementTransformerVoid() {
                    override fun visitCall(expression: IrCall): IrExpression {
                        if (expression.symbol == symbols.mapDsl.functions.single { it.descriptor.name.asString() == "put" }) {
                            declarations += MapEntryDeclaration(
                                mapType,
                                expression.getValueArgument(0)!!,
                                expression.getTypeArgument(0)!!
                            )
                        }

                        return super.visitCall(expression)
                    }
                })
            }
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.set" -> {
                val setQualifiers =
                    pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
                val setElementType = call.getTypeArgument(0)!!

                val setType = pluginContext.symbolTable.referenceClass(pluginContext.builtIns.set)
                    .ensureBound(pluginContext.irProviders)
                    .typeWith(setElementType)
                    .withAnnotations(setQualifiers)

                declarations += SetDeclaration(setType)

                val setBlock = call.getValueArgument(0) as? IrFunctionExpression
                setBlock?.function?.body?.transformChildrenVoid(object :
                    IrElementTransformerVoid() {
                    override fun visitCall(expression: IrCall): IrExpression {
                        if (expression.symbol == symbols.setDsl.functions.single { it.descriptor.name.asString() == "add" }) {
                            declarations += SetElementDeclaration(
                                setType,
                                expression.getTypeArgument(0)!!
                            )
                        }
                        return super.visitCall(expression)
                    }
                })
            }
            call.symbol.descriptor.annotations.hasAnnotation(InjektFqNames.Module) -> {
                val includedClass = declarationStore.getModuleClass(call.symbol.owner)
                val includedType = includedClass.typeWith((0 until call.typeArgumentsCount)
                    .map { call.getTypeArgument(it)!! })
                val property = InjektDeclarationIrBuilder(pluginContext, includedClass.symbol)
                    .fieldBakedProperty(
                        moduleClass,
                        Name.identifier(nameProvider.allocate("module")),
                        includedType
                    )

                module.initializerBlocks += {
                    irSetField(
                        it(),
                        property.backingField!!,
                        irCall(includedClass.constructors.single()).apply {
                            (0 until call.typeArgumentsCount)
                                .map { call.getTypeArgument(it)!! }
                                .forEachIndexed { index, type ->
                                    putTypeArgument(index, type)
                                }
                            copyValueArgumentsFrom(call, call.symbol.owner, symbol.owner)
                        }
                    )
                }

                declarations += IncludedModuleDeclaration(
                    includedType,
                    PropertyPath(property)
                )
            }

            /*expression.symbol.descriptor.annotations.hasAnnotation(InjektFqNames.Module) -> {
                /*checkNotInlined()

                if (!expression.symbol.descriptor.isInline) {
                    moduleCalls += expression
                } else {
                    val inlinedModuleClass = declarationStore.getModuleClass(expression.symbol.owner)
                    val inlinedModuleDescriptor = inlinedModuleClass.declarations.single {
                        it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
                    } as IrClass

                    val inlinedModuleFunctions = inlinedModuleDescriptor.functions.toList()

                    inlinedModuleFunctions.forEach { inlinedModuleFunction ->
                        when {
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstScope) -> {
                                inlinedScopeFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstAlias) -> {
                                inlinedAliasFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstSet) -> {
                                inlinedSetFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstSetElement) -> {
                                inlinedSetElementFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstMap) -> {
                                inlinedMapFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstMapEntry) -> {
                                inlinedMapEntryFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                            }
                            inlinedModuleFunction.hasAnnotation(InjektFqNames.AstBinding) -> {
                                inlinedBindingFunctions.getOrPut(expression) { mutableListOf() } += inlinedModuleFunction
                                when {
                                    inlinedModuleFunction.hasAnnotation(InjektFqNames.AstClassPath) -> {
                                        val provider = inlinedModuleFunction.getAnnotation(
                                            InjektFqNames.AstClassPath)
                                            ?.getValueArgument(0)
                                            ?.let { it as IrClassReferenceImpl }
                                            ?.classType
                                            ?.classOrFail
                                            ?.owner
                                            ?: inlinedModuleFunction.descriptor.annotations.findAnnotation(
                                                    InjektFqNames.AstClassPath)
                                                ?.allValueArguments
                                                ?.get(Name.identifier("clazz"))
                                                ?.let { it as KClassValue }
                                                ?.let { it.value as KClassValue.Value.NormalClass }
                                                ?.classId
                                                ?.shortClassName
                                                ?.asString()
                                                ?.substringAfter("\$")
                                                ?.let { name ->
                                                    inlinedModuleClass.declarations
                                                        .filterIsInstance<IrClass>()
                                                        .single { it.name.asString() == name }
                                                } ?: error("Couldn't get provider for ${inlinedModuleFunction.dump()}")

                                        pathByInlinedBindingFunctions[inlinedModuleFunction] = ClassPath(provider)
                                    }
                                    inlinedModuleFunction.hasAnnotation(InjektFqNames.AstTypeParameterPath) -> {
                                        val typeParameterName = inlinedModuleFunction.getAnnotation(
                                            InjektFqNames.AstTypeParameterPath)!!
                                            .getValueArgument(0).let { it as IrConst<String> }.value
                                        val typeParameter = inlinedModuleClass.typeParameters
                                            .single { it.name.asString() == typeParameterName }
                                        val typeArgument = expression.getTypeArgument(typeParameter.index)!!

                                        if (bindingType.toKotlinType().isTypeParameter()) {
                                            bindingPathsByCall[bindingCall] = TypeParameterPath(
                                                function.typeParameters.single {
                                                    it.descriptor ==
                                                            bindingType.toKotlinType().constructor.declarationDescriptor
                                                }
                                            )
                                        } else {
                                            providerForClass(
                                                providerIndex = index,
                                                clazz = bindingType.classOrFail
                                                    .ensureBound(irProviders).owner,
                                                visibility = visibility
                                            ).also { bindingPathsByCall[bindingCall] = ClassPath(it) }
                                        }

                                        providerForClass(
                                            providerIndex = providerIndex++,
                                            clazz = typeArgument.classOrFail
                                                .ensureBound(irProviders).owner,
                                            visibility = visibility
                                        ).also { bindingPathsByCall[bindingCall] = ClassPath(it) }
                                    }

                                    /*annotation class Class(val clazz: KClass<*>)
                                    annotation class Property(val name: String)
                                    annotation class TypeParameter(val name: String)
                                    annotation class ValueParameter(val name: String)*/
                                    else -> error("Couldn't get path for ${inlinedModuleFunction.dump()}")
                                }
                            }
                        }
                    }
                }*/*/
        }

        return declarations
    }

}
