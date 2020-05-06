package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.Name

class FactoryImplementation(
    val parent: FactoryImplementation?,
    val irParent: IrDeclarationParent,
    val name: Name,
    val superType: IrType,
    val moduleClass: IrClass?,
    val context: IrPluginContext,
    val symbols: InjektSymbols,
    val factoryTransformer: TopLevelFactoryTransformer,
    val declarationStore: InjektDeclarationStore
) {

    val clazz = buildClass {
        this.name = this@FactoryImplementation.name
    }.apply {
        parent = irParent
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        superTypes += superType
    }

    val constructor = clazz.addConstructor {
        returnType = clazz.defaultType
        isPrimary = true
        visibility = Visibilities.PUBLIC
    }.apply {
        copyTypeParametersFrom(clazz)
    }
    val factoryImplementationNode =
        FactoryImplementationNode(
            key = clazz.defaultType.asKey(context),
            factoryImplementation = this,
            initializerAccessor = { it() }
        )

    val factoryMembers =
        FactoryMembers(
            factoryImplementationNode,
            this@FactoryImplementation.context
        )

    val factoryExpressions: FactoryExpressions =
        FactoryExpressions(
            context = this@FactoryImplementation.context,
            symbols = symbols,
            members = factoryMembers,
            parent = parent?.factoryExpressions,
            factoryImplementation = this
        )

    val parentField by lazy {
        if (parent != null) {
            clazz.addField(
                "parent",
                parent.clazz.defaultType
            )
        } else null
    }
    val parentConstructorValueParameter by lazy {
        if (parent != null) {
            constructor.addValueParameter(
                "parent",
                parent.clazz.defaultType
            )
        } else null
    }

    val moduleConstructorValueParameter = lazy {
        constructor.addValueParameter(
            "module",
            moduleClass!!.defaultType
        )
    }

    val graph: Graph = Graph(
        parent = parent?.graph,
        factoryImplementation = this,
        factoryTransformer = factoryTransformer,
        context = this@FactoryImplementation.context,
        factoryImplementationNode = factoryImplementationNode,
        factoryImplementationModule = moduleClass?.let {
            ModuleNode(
                key = moduleClass.defaultType.asKey(context),
                module = moduleClass,
                initializerAccessor = {
                    irGet(moduleConstructorValueParameter.value)
                }
            )
        },
        declarationStore = declarationStore,
        symbols = symbols,
        factoryMembers = factoryMembers
    ).also { factoryExpressions.graph = it }

    private val dependencyRequests = mutableMapOf<IrDeclaration, Key>()

    init {
        collectDependencyRequests()
        graph.validate(dependencyRequests.values.toList())

        DeclarationIrBuilder(context, clazz.symbol).run {
            implementDependencyRequests()
            writeConstructor()
        }
    }

    private fun IrBuilderWithScope.implementDependencyRequests(): Unit = clazz.run clazz@{
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
                            factoryExpressions.getBindingExpression(
                                    BindingRequest(
                                        binding.key,
                                        RequestType.Instance
                                    )
                                )
                                .invoke(this@implementDependencyRequests,
                                    FactoryExpressionContext(this@FactoryImplementation) {
                                        irGet(
                                            dispatchReceiverParameter!!
                                        )
                                    })
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
                            val bindingExpression = factoryExpressions.getBindingExpression(
                                BindingRequest(
                                    binding.key,
                                    RequestType.Instance
                                )
                            )

                            body = irExprBody(
                                bindingExpression
                                    .invoke(
                                        this@implementDependencyRequests,
                                        FactoryExpressionContext(this@FactoryImplementation) {
                                            irGet(
                                                dispatchReceiverParameter!!
                                            )
                                        })
                            )
                        }
                    }
                }
            }
        }
    }

    private fun collectDependencyRequests() {
        fun IrClass.collectDependencyRequests() {
            for (declaration in declarations) {
                when (declaration) {
                    is IrFunction -> {
                        if (declaration !is IrConstructor &&
                            declaration.dispatchReceiverParameter?.type != context.irBuiltIns.anyType &&
                            !declaration.isFakeOverride
                        )
                            dependencyRequests[declaration] = declaration.returnType
                                .asKey(context)
                    }
                    is IrProperty -> {
                        if (!declaration.isFakeOverride)
                            dependencyRequests[declaration] = declaration.getter!!.returnType
                                .asKey(context)
                    }
                }
            }

            superTypes
                .mapNotNull { it.classOrNull?.owner }
                .forEach { it.collectDependencyRequests() }
        }

        val superType = clazz.superTypes.single().classOrFail.owner
        superType.collectDependencyRequests()
    }

    private fun IrBuilderWithScope.writeConstructor() = constructor.apply {
        val superType = clazz.superTypes.single().classOrFail.owner
        body = irBlockBody {
            +IrDelegatingConstructorCallImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                context.irBuiltIns.unitType,
                if (superType.kind == ClassKind.CLASS)
                    superType.constructors.single { it.valueParameters.isEmpty() }
                        .symbol
                else context.irBuiltIns.anyClass.constructors.single()
            )
            +IrInstanceInitializerCallImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                clazz.symbol,
                context.irBuiltIns.unitType
            )

            if (this@FactoryImplementation.parentField != null) {
                +irSetField(
                    irGet(clazz.thisReceiver!!),
                    this@FactoryImplementation.parentField!!,
                    irGet(parentConstructorValueParameter!!)
                )
            }

            var lastRoundFields: Map<Key, FactoryField>? = null
            while (true) {
                val fieldsToInitialize = factoryMembers.fields
                    .filterKeys { it !in factoryMembers.initializedFields }
                if (fieldsToInitialize.isEmpty()) {
                    break
                } else if (lastRoundFields == fieldsToInitialize) {
                    error("Initializing error ${lastRoundFields.keys}")
                }
                lastRoundFields = fieldsToInitialize

                fieldsToInitialize.forEach { (key, field) ->
                    field.initializer(
                        this,
                        FactoryExpressionContext(
                            this@FactoryImplementation
                        ) {
                            irGet(clazz.thisReceiver!!)
                        }
                    )?.let { initExpr ->
                        +irSetField(
                            irGet(clazz.thisReceiver!!),
                            field.field,
                            initExpr
                        )
                        factoryMembers.initializedFields += key
                    }
                }
            }
        }
    }
}
