package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getConstant
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.resolve.ComponentNode
import com.ivianuu.injekt.compiler.resolve.Graph
import com.ivianuu.injekt.compiler.resolve.Key
import com.ivianuu.injekt.compiler.resolve.ModuleNode
import com.ivianuu.injekt.compiler.resolve.StatefulBinding
import com.ivianuu.injekt.compiler.resolve.TreeElement
import com.ivianuu.injekt.compiler.resolve.UserBinding
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ComponentTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val moduleFragment: IrModuleFragment
) : AbstractInjektTransformer(context) {

    private val componentCalls = mutableListOf<IrCall>()
    private val fileByCall = mutableMapOf<IrCall, IrFile>()
    private val valueParametersByCapturesByComponent =
        mutableMapOf<IrClass, Map<IrGetValue, IrValueParameter>>()

    private var computedComponentCalls = false
    private val processedComponentsByKey = mutableMapOf<String, IrClass>()
    private val processedComponentsByCall = mutableMapOf<IrCall, IrClass>()
    private val processingComponents = mutableSetOf<String>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        computeComponentCallsIfNeeded()

        componentCalls.forEach { getProcessedComponent(it) }

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                return processedComponentsByCall[expression]
                    ?.let {
                        DeclarationIrBuilder(context, expression.symbol).run {
                            val constructor = it.constructors.single()
                            irCall(constructor).apply {
                                valueParametersByCapturesByComponent[it]?.forEach { (capture, valueParameter) ->
                                    putValueArgument(valueParameter.index, capture)
                                }
                            }
                        }
                    } ?: expression
            }
        })

        return moduleFragment
    }

    fun getProcessedComponent(key: String): IrClass? {
        computeComponentCallsIfNeeded()

        processedComponentsByKey[key]?.let { return it }

        val call = componentCalls.firstOrNull {
            it.getValueArgument(0)!!.getConstant<String>() == key
        } ?: return null

        return getProcessedComponent(call)
    }

    fun getProcessedComponent(call: IrCall): IrClass? {
        computeComponentCallsIfNeeded()

        return DeclarationIrBuilder(context, call.symbol).run {
            val key = call.getValueArgument(0)!!.getConstant<String>()

            check(key !in processingComponents) {
                "Circular dependency for component $key"
            }
            processingComponents += key

            val componentDefinition = call.getValueArgument(1) as? IrFunctionExpression
            val file = fileByCall[call]!!
            val component = componentClass(
                componentDefinition,
                this@ComponentTransformer.context.irTrace[InjektWritableSlices.COMPONENT_FQ_NAME, call]!!
                    .shortName(),
                key
            )
            file.addChild(component)
            processedComponentsByCall[call] = component
            processedComponentsByKey[key] = component
            processingComponents -= key
            component
        }
    }

    private fun computeComponentCallsIfNeeded() {
        if (computedComponentCalls) return
        computedComponentCalls = true

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val fileStack = mutableListOf<IrFile>()
            override fun visitFile(declaration: IrFile): IrFile {
                fileStack.push(declaration)
                return super.visitFile(declaration)
                    .also { fileStack.pop() }
            }
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                if (expression.symbol.descriptor.fqNameSafe
                        .asString() == "com.ivianuu.injekt.Component"
                ) {
                    componentCalls += expression
                    fileByCall[expression] = fileStack.last()
                }
                return expression
            }
        })
    }

    private fun IrBuilderWithScope.componentClass(
        componentDefinition: IrFunctionExpression?,
        name: Name,
        key: String
    ) = buildClass {
        this.name = name
    }.apply clazz@{
        superTypes += symbols.component.defaultType

        createImplicitParameterDeclarationWithWrappedDescriptor()

        val valueParametersByCapture = mutableMapOf<IrGetValue, IrValueParameter>()
        valueParametersByCapturesByComponent[this] = valueParametersByCapture

        val moduleCall = componentDefinition?.function?.body
            .safeAs<IrExpressionBody>()?.expression as? IrCall

        val moduleFqName = moduleCall?.symbol?.owner?.fqNameForIrSerialization
            ?.parent()
            ?.child(Name.identifier("${moduleCall.symbol.owner.name}\$Impl"))

        val module = if (moduleFqName != null) declarationStore.getModule(moduleFqName)
        else null

        val captures = mutableListOf<IrGetValue>()
        componentDefinition?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (expression.symbol != componentDefinition.function.extensionReceiverParameter?.symbol) {
                    captures += expression
                }
                return super.visitGetValue(expression)
            }
        })

        val moduleField = if (module != null) addField(
            "module",
            module.defaultType,
            Visibilities.PUBLIC
        ) else null

        val componentNode = ComponentNode(
            key = key,
            component = this,
            treeElement = TreeElement("") { it }
        )

        val graph = Graph(
            context = this@ComponentTransformer.context,
            symbols = symbols,
            thisComponent = componentNode,
            thisComponentModule = module?.let {
                ModuleNode(
                    module = module,
                    componentNode = componentNode,
                    typeParametersMap = module.typeParameters.map {
                        it.symbol to moduleCall!!.getTypeArgument(it.index)!!
                    }.toMap(),
                    treeElement = componentNode.treeElement!!.childField(
                        moduleField!!.name.asString()
                    )
                )
            },
            declarationStore = declarationStore
        )

        val bindings = graph.allBindings

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            captures.forEachIndexed { index, capture ->
                valueParametersByCapture[capture] = addValueParameter(
                    "p$index",
                    capture.type
                )
            }

            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)

                if (moduleField != null) {
                    +irSetField(
                        irGet(thisReceiver!!),
                        moduleField,
                        irCall(module!!.constructors.single()).apply {
                            copyValueArgumentsFrom(
                                moduleCall!!,
                                moduleCall.symbol.owner,
                                symbol.owner
                            )
                        }
                    )
                }

                val fieldsToInitialize = graph.allBindings.values
                    .filterIsInstance<StatefulBinding>()
                    .filter { it.field in fields }

                val initializedKeys = mutableSetOf<Key>()
                val processedFields = mutableSetOf<IrField>()

                while (true) {
                    val fieldsToProcess = fieldsToInitialize
                        .filter { it.field !in processedFields }
                    if (fieldsToProcess.isEmpty()) break

                    fieldsToProcess
                        .filter {
                            it.dependencies.all {
                                val binding = graph.allBindings.getValue(it)
                                binding !is StatefulBinding || it in initializedKeys
                            }
                        }
                        .forEach {
                            initializedKeys += it.key
                            processedFields += it.field
                            +irSetField(
                                irGet(thisReceiver!!),
                                it.field,
                                it.providerInstance(this, irGet(thisReceiver!!))
                            )
                        }
                }
            }
        }.apply {
            transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return valueParametersByCapture[expression]
                        ?.let { irGet(it) }
                        ?: super.visitGetValue(expression)
                }
            })
        }

        addFunction {
            this.name = Name.identifier("get")
            returnType = this@ComponentTransformer.context.irBuiltIns.anyNType
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this)

            overriddenSymbols += symbolTable.referenceSimpleFunction(
                symbols.component.descriptor.unsubstitutedMemberScope.findSingleFunction(
                    Name.identifier(
                        "get"
                    )
                )
            )

            addValueParameter(
                "key",
                this@ComponentTransformer.context.irBuiltIns.intType
            )

            body = DeclarationIrBuilder(this@ComponentTransformer.context, symbol).run {
                irExprBody(
                    irReturn(
                        irWhen(
                            type = returnType,
                            branches = bindings
                                .map { (key, binding) ->
                                    irBranch(
                                        condition = irEquals(
                                            irInt(key.hashCode()),
                                            irGet(valueParameters.single())
                                        ),
                                        result = irCall(binding.getFunction(this)).apply {
                                            dispatchReceiver =
                                                irGet(dispatchReceiverParameter!!)
                                        }
                                    )
                                } + irElseBranch(
                                irCall(
                                    symbolTable.referenceFunction(
                                        symbols.component.descriptor.unsubstitutedMemberScope
                                            .findSingleFunction(Name.identifier("get"))
                                    )
                                        .ensureBound(this@ComponentTransformer.context.irProviders).owner,
                                    InjektStatementOrigin,
                                    symbols.component
                                ).apply {
                                    dispatchReceiver =
                                        irGet(dispatchReceiverParameter!!)
                                    putValueArgument(
                                        0,
                                        irGet(valueParameters.single())
                                    )
                                }
                            )
                        )
                    )
                )
            }
        }

        annotations += componentMetadata(
            graph.thisScopes,
            graph.thisParents,
            graph.thisModules,
            graph.thisBindings.values.map { it as UserBinding }
        )
    }

    private fun IrBuilderWithScope.componentMetadata(
        scopes: Set<FqName>,
        parents: List<ComponentNode>,
        modules: List<ModuleNode>,
        bindings: List<UserBinding>
    ): IrConstructorCall {
        return irCallConstructor(
            symbols.componentMetadata.constructors.single(),
            emptyList()
        ).apply {
            // scopes
            putValueArgument(
                0,
                irStringArray(scopes.map { irString(it.asString()) })
            )

            // parents
            putValueArgument(
                1,
                irStringArray(
                    parents.map {
                        if (it.treeElement != null) {
                            irString("${it.key}=:=/${it.treeElement.path}")
                        } else {
                            irString(it.key)
                        }
                    }
                )
            )

            // modules
            putValueArgument(
                2,
                irStringArray(modules.map { irString("/${it.treeElement!!.path}") })
            )

            // bindings
            putValueArgument(
                3,
                irStringArray(
                    bindings.map {
                        if (it is StatefulBinding) {
                            irString("/${it.treeElement.path}")
                        } else {
                            irString(it.provider.fqNameForIrSerialization.asString())
                        }
                    }
                )
            )
        }
    }
}
