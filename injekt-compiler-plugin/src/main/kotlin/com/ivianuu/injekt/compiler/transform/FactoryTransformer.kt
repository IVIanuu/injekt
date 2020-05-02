package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.graph.ComponentNode
import com.ivianuu.injekt.compiler.transform.graph.Graph
import com.ivianuu.injekt.compiler.transform.graph.Key
import com.ivianuu.injekt.compiler.transform.graph.ModuleNode
import com.ivianuu.injekt.compiler.transform.graph.RequestType
import com.ivianuu.injekt.compiler.transform.graph.child
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace

class FactoryTransformer(
    context: IrPluginContext,
    bindingTrace: BindingTrace,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context, bindingTrace) {

    private val factoryFunctions = mutableListOf<IrFunction>()
    private val transformedFactories = mutableMapOf<IrFunction, IrClass>()
    private val transformingFactories = mutableSetOf<FqName>()
    private var computedFactoryFunctions = false

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        computeFactoryFunctionsIfNeeded()

        factoryFunctions.forEach { function ->
            DeclarationIrBuilder(context, function.symbol).run {
                getImplementationClassForFactory(function)
            }
        }

        return super.visitModuleFragment(declaration)
    }

    // todo simplify implementation if we don't need it
    fun getImplementationClassForFactory(fqName: FqName): IrClass? {
        transformedFactories.values.firstOrNull {
            it.fqNameForIrSerialization == fqName
        }?.let { return it }

        val function = factoryFunctions.firstOrNull {
            val packageName = it.fqNameForIrSerialization.parent()
            packageName.child(
                InjektNameConventions.getModuleNameForModuleFunction(it.name)
            ) == fqName
        } ?: return null

        return getImplementationClassForFactory(function)
    }

    fun getImplementationClassForFactory(function: IrFunction): IrClass? {
        computeFactoryFunctionsIfNeeded()
        check(function in factoryFunctions) {
            "Unknown function $function"
        }
        transformedFactories[function]?.let { return it }
        return DeclarationIrBuilder(context, function.symbol).run {
            val packageName = function.fqNameForIrSerialization.parent()
            val implementationName =
                InjektNameConventions.getImplementationNameForFactoryFunction(function.name)
            val implementationFqName = packageName.child(implementationName)
            check(implementationFqName !in transformingFactories) {
                "Circular dependency for factory $implementationFqName"
            }
            transformingFactories += implementationFqName

            val moduleCall = function.body?.statements?.single()
                ?.let { (it as? IrReturn)?.value }
                ?.let { (it as? IrCall)?.getValueArgument(0) as? IrFunctionExpression }
                ?.function
                ?.body
                ?.statements
                ?.single() as? IrCall

            val moduleFqName = moduleCall?.symbol?.owner?.fqNameForIrSerialization
                ?.parent()
                ?.child(InjektNameConventions.getModuleNameForModuleFunction(moduleCall.symbol.owner.name))

            val moduleClass = if (moduleFqName != null) declarationStore.getModule(moduleFqName)
            else null


            val implementationClass = implementationClass(function, moduleClass)
            println(implementationClass.dump())

            function.file.addChild(implementationClass)
            (function.file as IrFileImpl).metadata =
                MetadataSource.File(function.file.declarations.map { it.descriptor })

            function.body = irExprBody(
                irCall(implementationClass.constructors.single()).apply {
                    if (moduleCall != null) {
                        putValueArgument(
                            0,
                            irCall(moduleClass!!.constructors.single()).apply {
                                copyTypeArgumentsFrom(moduleCall)
                                (0 until moduleCall.valueArgumentsCount).forEach {
                                    putValueArgument(it, moduleCall.getValueArgument(it))
                                }
                            }
                        )
                    }
                }
            )

            transformedFactories[function] = implementationClass
            transformingFactories -= implementationFqName
            implementationClass
        }
    }

    private fun computeFactoryFunctionsIfNeeded() {
        if (computedFactoryFunctions) return
        computedFactoryFunctions = true
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Factory)) {
                    factoryFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })
    }

    private fun IrBuilderWithScope.implementationClass(
        function: IrFunction,
        moduleClass: IrClass?
    ) = buildClass {
        // todo make kind = OBJECT if this component has no state
        name = InjektNameConventions.getImplementationNameForFactoryFunction(function.name)
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        superTypes += function.returnType

        val moduleField: Lazy<IrField>? = if (moduleClass != null) lazy {
            addField("module", moduleClass.defaultType)
        } else null

        val componentNode =
            ComponentNode(
                context = this@FactoryTransformer.context,
                component = this,
                treeElement = { it },
                symbols = symbols
            )

        val graph = Graph(
            context = this@FactoryTransformer.context,
            symbols = symbols,
            componentNode = componentNode,
            componentModule = moduleClass?.let {
                ModuleNode(
                    module = moduleClass,
                    treeElement = componentNode.treeElement.child(moduleField!!.value)
                )
            },
            declarationStore = declarationStore
        )

        val dependencyRequests = mutableMapOf<IrDeclaration, Key>()

        fun IrClass.collectDependencyRequests() {
            for (declaration in declarations) {
                when (declaration) {
                    is IrFunction -> {
                        if (declaration !is IrConstructor &&
                            declaration.dispatchReceiverParameter?.type != irBuiltIns.anyType &&
                            !declaration.isFakeOverride
                        )
                            dependencyRequests[declaration] =
                                Key(
                                    declaration.returnType
                                )
                    }
                    is IrProperty -> {
                        if (!declaration.isFakeOverride)
                            dependencyRequests[declaration] =
                                Key(
                                    declaration.getter!!.returnType
                                )
                    }
                }
            }

            superTypes
                .mapNotNull { it.classOrNull?.owner }
                .forEach { it.collectDependencyRequests() }
        }

        superTypes.single().classOrNull?.owner?.collectDependencyRequests()

        dependencyRequests.forEach { (declaration, key) ->
            val binding = graph.getBinding(key)
            when (declaration) {
                is IrFunction -> {
                    addFunction {
                        name = declaration.name
                        returnType = declaration.returnType
                        visibility = declaration.visibility
                    }.apply {
                        overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                        dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                        body = irExprBody(
                            graph.factoryExpressions.getBindingExpression(
                                    binding,
                                    RequestType.Instance
                                )
                                .invoke(
                                    this@implementationClass,
                                    irGet(dispatchReceiverParameter!!)
                                )
                        )
                    }
                }
                is IrProperty -> {
                    addProperty {
                        name = declaration.name
                    }.apply {
                        addGetter {
                            returnType = declaration.getter!!.returnType
                        }.apply {
                            overriddenSymbols += declaration.getter!!.symbol
                            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                            body = irExprBody(
                                graph.factoryExpressions.getBindingExpression(
                                        binding,
                                        RequestType.Instance
                                    )
                                    .invoke(
                                        this@implementationClass,
                                        irGet(dispatchReceiverParameter!!)
                                    )
                            )
                        }
                    }
                }
            }
        }

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            copyTypeParametersFrom(this@clazz)

            if (moduleField?.isInitialized() == true) {
                addValueParameter(
                    "module",
                    moduleField.value.type
                )
            }

            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)

                if (moduleField?.isInitialized() == true) {
                    +irSetField(
                        irGet(thisReceiver!!),
                        moduleField.value,
                        irGet(valueParameters.single())
                    )
                }

                var lastRoundFields: Map<Key, ComponentNode.ComponentField>? = null
                while (true) {
                    val fieldsToInitialize = componentNode.componentFields
                        .filterKeys { it !in componentNode.initializedFields }
                    if (fieldsToInitialize.isEmpty()) {
                        break
                    } else if (lastRoundFields == fieldsToInitialize) {
                        error("Initializing error ${lastRoundFields.keys}")
                    }
                    lastRoundFields = fieldsToInitialize

                    fieldsToInitialize.forEach { (key, field) ->
                        field.initializer(this, irGet(thisReceiver!!))?.let { initExpr ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                field.field,
                                initExpr
                            )
                            componentNode.initializedFields += key
                        }
                    }
                }
            }
        }
    }

}
