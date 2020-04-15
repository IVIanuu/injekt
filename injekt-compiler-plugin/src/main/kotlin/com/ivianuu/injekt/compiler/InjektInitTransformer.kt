package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class InjektInitTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private val aggregatePackage =
        pluginContext.moduleDescriptor.getPackage(FqName("com.ivianuu.injekt.aggregate"))
    private val module = getClass(InjektClassNames.ModuleImpl)
    private val injekt = getClass(InjektClassNames.Injekt)

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.ensureBound().owner.name.asString() != "initializeEndpoint") return expression

        val thisModules = mutableListOf<IrFunctionSymbol>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.name.asString().endsWith("\$ModuleAccessor")) {
                    thisModules += declaration.symbol
                }
                return super.visitFunction(declaration)
            }
        })

        val allModules = aggregatePackage
            .memberScope
            .getClassifierNames()!!
            .map { FqName(it.asString().replace("_", ".")) }
            .map {
                val name = Name.identifier(it.shortName().asString() + "\$ModuleAccessor")
                pluginContext.moduleDescriptor.getPackage(it.parent())
                    .memberScope
                    .findSingleFunction(name)
            }
            .map { symbolTable.referenceFunction(it) } + thisModules

        val addModules = injekt.unsubstitutedMemberScope
            .findFirstFunction("modules") {
                it.valueParameters.firstOrNull()?.isVararg == true
            }

        return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(
            callee = symbolTable.referenceSimpleFunction(addModules),
            type = pluginContext.irBuiltIns.unitType
        ).apply {
            dispatchReceiver = expression.dispatchReceiver

            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(module.defaultType.toIrType()),
                    module.defaultType.toIrType(),
                    allModules.map {
                        DeclarationIrBuilder(pluginContext, it)
                            .irCall(it, module.defaultType.toIrType())
                    }
                )
            )
        }
    }
}
