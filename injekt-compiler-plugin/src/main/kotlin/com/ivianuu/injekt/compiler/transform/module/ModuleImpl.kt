package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ModuleImpl(
    val function: IrFunction,
    val pluginContext: IrPluginContext,
    val symbols: InjektSymbols,
    val declarationStore: InjektDeclarationStore
) {

    val nameProvider = NameProvider()

    private val providerFactory = ModuleProviderFactory(this, pluginContext)
    private val declarationFactory = ModuleDeclarationFactory(
        this, pluginContext,
        symbols, nameProvider, declarationStore, providerFactory
    )
    private val moduleDescriptor = ModuleDescriptor(
        this@ModuleImpl,
        pluginContext,
        symbols
    )

    val fieldsByParameters = mutableMapOf<IrValueDeclaration, IrField>()

    val clazz: IrClass = buildClass {
        name = InjektNameConventions.getModuleClassNameForModuleFunction(function)
        visibility = function.visibility
    }

    init {
        clazz.apply clazz@{
            parent = function.parent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
            copyTypeParametersFrom(function)

            val declarations = mutableListOf<ModuleDeclaration>()

            val ignoreGetValue = mutableSetOf<IrGetValue>()

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                copyTypeParametersFrom(this@clazz)

                function.allParameters
                    .filter {
                        !it.type.isFunction() ||
                                (it.type.typeArguments.firstOrNull()?.classOrNull != symbols.providerDsl &&
                                        !it.type.hasAnnotation(InjektFqNames.Module))
                    }
                    .forEachIndexed { index, p ->
                        val newValueParameter = addValueParameter(
                            "p$index",
                            p.type
                        )
                        addField(
                            newValueParameter.name,
                            p.type
                        ).also {
                            fieldsByParameters[p] = it
                            fieldsByParameters[newValueParameter] = it
                        }
                    }

                body = InjektDeclarationIrBuilder(pluginContext, symbol).run {
                    builder.irBlockBody {
                        initializeClassWithAnySuperClass(this@clazz.symbol)

                        fieldsByParameters
                            .filter { it.key.parent == this@apply }
                            .forEach { (parameter, field) ->
                                +irSetField(
                                    irGet(thisReceiver!!),
                                    field,
                                    irGet(parameter)
                                        .also { ignoreGetValue += it }
                                )
                            }

                        function.body!!.statements.forEach { moduleStatement ->
                            when (moduleStatement) {
                                is IrCall -> {
                                    declarations += declarationFactory.create(moduleStatement)
                                        .onEach {
                                            it.statement?.invoke(builder) { irGet(thisReceiver!!) }
                                                ?.let { +it }
                                        }
                                }
                                is IrVariable -> {
                                    val field = addField(
                                        moduleStatement.name.asString(),
                                        moduleStatement.type
                                    )
                                    fieldsByParameters[moduleStatement] = field
                                    if (moduleStatement.initializer != null) {
                                        +irSetField(
                                            irGet(thisReceiver!!),
                                            field,
                                            moduleStatement.initializer!!
                                        )
                                    }
                                }
                                is IrSetVariable -> {
                                    fieldsByParameters[moduleStatement.symbol.owner]?.let {
                                        +irSetField(
                                            irGet(thisReceiver!!),
                                            it,
                                            moduleStatement.value
                                        )
                                    }
                                }
                                else -> +moduleStatement
                            }
                        }
                    }
                }
            }

            moduleDescriptor.addDeclarations(declarations)
            addChild(moduleDescriptor.clazz)

            transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (expression in ignoreGetValue ||
                        fieldsByParameters.keys.none { it.symbol == expression.symbol }
                    ) {
                        super.visitGetValue(expression)
                    } else {
                        val field = fieldsByParameters[expression.symbol.owner]!!
                        return DeclarationIrBuilder(pluginContext, symbol).run {
                            irGetField(
                                irGet(thisReceiver!!),
                                field
                            )
                        }
                    }
                }
            })
        }
    }

}
