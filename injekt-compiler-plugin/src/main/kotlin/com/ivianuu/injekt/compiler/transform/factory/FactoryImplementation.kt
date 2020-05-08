package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
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
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class FactoryImplementation(
    val name: Name,
    val superType: IrType,
    val parent: FactoryImplementation?,
    val factoryFunction: IrFunction?,
    irParent: IrDeclarationContainer,
    moduleClass: IrClass,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols,
    factoryTransformer: TopLevelFactoryTransformer,
    declarationStore: InjektDeclarationStore
) : AbstractFactoryProduct(
    moduleClass,
    pluginContext,
    symbols,
    factoryTransformer,
    declarationStore
) {

    val clazz = buildClass {
        this.name = this@FactoryImplementation.name
        visibility = Visibilities.PRIVATE
    }.apply {
        this.parent = irParent
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
            key = clazz.defaultType.asKey(pluginContext),
            factoryImplementation = this,
            initializerAccessor = { it() }
        )

    override val factoryMembers = ClassFactoryMembers(
        pluginContext,
        clazz,
        factoryImplementationNode.factoryImplementation
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
            moduleClass.defaultType
        )
    }

    init {
        val dependencyRequests = collectDependencyRequests()
        init(
            parent,
            dependencyRequests.values.toList()
        ) {
            irGet(moduleConstructorValueParameter.value)
        }

        DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            implementDependencyRequests(dependencyRequests)
            writeConstructor()
        }

        if (factoryFunction != null) {
            val moduleCall = factoryFunction.body!!.statements[0] as IrCall
            factoryFunction.file.addChild(clazz)
            factoryFunction.body = DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
                irExprBody(
                    irCall(constructor).apply {
                        if (constructor.valueParameters.isNotEmpty()) {
                            putValueArgument(
                                0,
                                irCall(moduleClass.constructors.single()).apply {
                                    copyTypeArgumentsFrom(moduleCall)
                                    (0 until moduleCall.valueArgumentsCount).forEach {
                                        putValueArgument(it, moduleCall.getValueArgument(it))
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.implementDependencyRequests(
        dependencyRequests: Map<IrDeclaration, BindingRequest>
    ): Unit = clazz.run clazz@{
        dependencyRequests.forEach { (declaration, request) ->
            val binding = graph.getBinding(request)
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
                                        request.requestOrigin,
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
                                    request.requestOrigin,
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

    private fun collectDependencyRequests(): Map<IrDeclaration, BindingRequest> {
        val dependencyRequests = mutableMapOf<IrDeclaration, BindingRequest>()
        fun IrClass.collectDependencyRequests(sub: IrClass?) {
            for (declaration in declarations) {
                fun reqisterRequest(type: IrType) {
                    dependencyRequests[declaration] = BindingRequest(
                        type
                            .substitute(
                                typeParameters,
                                sub?.superTypes
                                    ?.single { it.classOrNull?.owner == this }
                                    ?.typeArguments ?: emptyList()
                            )
                            .asKey(pluginContext),
                        declaration.descriptor.fqNameSafe
                    )
                }

                when (declaration) {
                    is IrFunction -> {
                        if (declaration !is IrConstructor &&
                            declaration.dispatchReceiverParameter?.type != pluginContext.irBuiltIns.anyType &&
                            !declaration.isFakeOverride
                        ) reqisterRequest(declaration.returnType)
                    }
                    is IrProperty -> {
                        if (!declaration.isFakeOverride)
                            reqisterRequest(declaration.getter!!.returnType)
                    }
                }
            }

            superTypes
                .mapNotNull { it.classOrNull?.owner }
                .forEach { it.collectDependencyRequests(this) }
        }

        val superType = clazz.superTypes.single().classOrFail.owner
        superType.collectDependencyRequests(clazz)
        return dependencyRequests
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
                            field.backingField!!,
                            initExpr
                        )
                        factoryMembers.initializedFields += key
                    }
                }
            }
        }
    }
}
