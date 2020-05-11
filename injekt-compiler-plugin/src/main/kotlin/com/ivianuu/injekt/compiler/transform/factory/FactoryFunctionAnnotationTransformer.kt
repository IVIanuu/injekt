package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.statements

class FactoryFunctionAnnotationTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (!declaration.hasAnnotation(InjektFqNames.ChildFactory) &&
            !declaration.hasAnnotation(InjektFqNames.Factory)
        ) return super.visitFunction(declaration)
        val createCall = (declaration.body!!.statements.last() as IrReturn).value as IrCall

        when {
            createCall.symbol.descriptor.name.asString() == "createImpl" -> {
                declaration.annotations += InjektDeclarationIrBuilder(
                    pluginContext,
                    declaration.symbol
                )
                    .noArgSingleConstructorCall(symbols.astImplFactory)
            }
            createCall.symbol.descriptor.name.asString() == "createInstance" -> {
                declaration.annotations += InjektDeclarationIrBuilder(
                    pluginContext,
                    declaration.symbol
                )
                    .noArgSingleConstructorCall(symbols.astInstanceFactory)
            }
            else -> {
                error("Unexpected factory body ${declaration.dump()}")
            }
        }
        return super.visitFunction(declaration)
    }

}