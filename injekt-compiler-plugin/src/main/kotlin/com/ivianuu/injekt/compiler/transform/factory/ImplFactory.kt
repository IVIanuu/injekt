package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplFactory(
    val name: Name,
    superType: IrType,
    val parent: ImplFactory?,
    typeParameterMap: Map<IrTypeParameterSymbol, IrType>,
    irDeclarationParent: IrDeclarationParent,
    moduleClass: IrClass,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols,
    declarationStore: InjektDeclarationStore
) : AbstractFactory(
    moduleClass,
    typeParameterMap,
    pluginContext,
    symbols,
    declarationStore
) {
    val clazz = buildClass {
        this.name = this@ImplFactory.name
        visibility = Visibilities.PRIVATE
    }.apply {
        this.parent = irDeclarationParent
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        superTypes += superType.substituteWithFactoryTypeArguments()
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
            key = clazz.defaultType.asKey(),
            implFactory = this,
            initializerAccessor = { it() }
        )

    override val factoryMembers = ClassFactoryMembers(
        pluginContext,
        clazz,
        factoryImplementationNode.implFactory
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

    val dependencyRequests =
        mutableMapOf<IrDeclaration, BindingRequest>()
    val implementedRequests = mutableMapOf<IrDeclaration, IrDeclaration>()

    init {
        collectDependencyRequests()
        init(parent, dependencyRequests.values.toList()) {
            irGet(moduleConstructorValueParameter.value)
        }

        DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            implementDependencyRequests()
            writeConstructor()
        }
    }

    fun getInitExpression(
        valueArguments: List<IrExpression>
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irCall(constructor).apply {
                if (constructor.valueParameters.isNotEmpty()) {
                    putValueArgument(0, getModuleInitExpression(valueArguments))
                }
            }
        }
    }

    private fun IrBuilderWithScope.implementDependencyRequests(): Unit = clazz.run clazz@{
        dependencyRequests.forEach { factoryExpressions.getBindingExpression(it.value) }

        dependencyRequests
            .filter { it.key !in implementedRequests }
            .forEach { (declaration, request) ->
                val binding = graph.getBinding(request)
                implementedRequests[declaration] =
                    factoryMembers.addDependencyRequestImplementation(declaration) { function ->
                        val bindingExpression = factoryExpressions.getBindingExpression(
                            BindingRequest(
                                binding.key,
                                request.requestOrigin,
                                RequestType.Instance
                            )
                        )

                        bindingExpression
                            .invoke(
                                this@implementDependencyRequests,
                                FactoryExpressionContext(this@ImplFactory) {
                                    irGet(function.dispatchReceiverParameter!!)
                                }
                            )
                    }
        }
    }

    private fun collectDependencyRequests() {
        fun IrClass.collectDependencyRequests(
            typeArguments: List<IrType>
        ) {
            for (declaration in declarations) {
                fun reqisterRequest(type: IrType) {
                    dependencyRequests[declaration] = BindingRequest(
                        type
                            .substitute(
                                typeParameters.map { it.symbol }.associateWith {
                                    typeArguments[it.owner.index]
                                }
                            )
                            .asKey(),
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
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    clazz?.collectDependencyRequests(
                        superType.typeArguments
                    )
                }
        }

        val superType = clazz.superTypes.single()
        val superTypeClass = superType.getClass()!!
        superTypeClass.collectDependencyRequests(superType.typeArguments)
    }

    private fun IrBuilderWithScope.writeConstructor() = constructor.apply {
        val superType = clazz.superTypes.single().getClass()!!
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

            if (this@ImplFactory.parentField != null) {
                +irSetField(
                    irGet(clazz.thisReceiver!!),
                    this@ImplFactory.parentField!!,
                    irGet(parentConstructorValueParameter!!)
                )
            }

            factoryMembers.fields.values.forEach { (field, initializer) ->
                +irSetField(
                    irGet(clazz.thisReceiver!!),
                    field,
                    initializer(this, FactoryExpressionContext(this@ImplFactory) {
                        irGet(clazz.thisReceiver!!)
                    })
                )
            }
        }
    }
}
