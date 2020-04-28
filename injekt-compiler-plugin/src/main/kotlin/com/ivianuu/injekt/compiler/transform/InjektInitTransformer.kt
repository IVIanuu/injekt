package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.ensureBound
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class InjektInitTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.ensureBound(context.irProviders).owner.name.asString() != "initializeEndpoint") return expression

        val jitBindings = mutableListOf<IrFunction>()
        val modules = mutableListOf<IrFunction>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                val origin = declaration.origin
                if (origin is RegisterModuleOrigin) {
                    modules += declaration
                } else if (origin is RegisterBindingOrigin) {
                    jitBindings += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        symbols.aggregatePackage
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .forEach {
                val fqName = FqName(it.name.asString().replace("_", "."))
                val name = Name.identifier(fqName.shortName().asString())

                try {
                    modules += symbols.getPackage(fqName.parent())
                        .memberScope
                        .findSingleFunction(name)
                        .let { context.symbolTable.referenceFunction(it) }
                        .ensureBound(context.irProviders)
                        .owner
                } catch (e: Exception) {
                    try {
                        jitBindings += symbols.getPackage(fqName.parent())
                            .memberScope
                            .getContributedDescriptors()
                            .single { it.name == name }
                            .let { it as FunctionDescriptor }
                            .let { context.symbolTable.referenceFunction(it) }
                            .ensureBound(context.irProviders)
                            .owner
                    } catch (e: Exception) {
                    }
                }
            }

        return DeclarationIrBuilder(context, expression.symbol).run {
            irBlock {
                jitBindings.forEach { jitBinding -> +irCall(jitBinding) }
                modules.forEach { module -> +irCall(module) }
            }
        }
    }
}
