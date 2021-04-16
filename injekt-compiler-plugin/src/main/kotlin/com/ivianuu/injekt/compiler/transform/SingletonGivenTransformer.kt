package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.*

class SingletonGivenTransformer(
    private val context: InjektContext,
    private val trace: BindingTrace,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
    private val ignoredExpressions = mutableListOf<IrExpression>()
    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.descriptor.isSingletonGiven(context, trace)) {
            instanceFieldForDeclaration(declaration)
        }
        return super.visitClass(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        if (expression in ignoredExpressions) return super.visitConstructorCall(expression)
        if (expression.type.classifierOrNull?.descriptor?.isSingletonGiven(context, trace) == true)  {
            val module = expression.type.classOrNull!!.owner
            val instanceField = instanceFieldForDeclaration(module)
            return DeclarationIrBuilder(pluginContext, expression.symbol)
                .irGetField(null, instanceField)
        }
        return super.visitConstructorCall(expression)
    }

    private fun instanceFieldForDeclaration(module: IrClass): IrField {
        module.fields
            .singleOrNull { it.name.asString() == "INSTANCE" }
            ?.let { return it }
        return module.addField {
            name = "INSTANCE".asNameId()
            isStatic = true
            type = module.defaultType
            origin = if (module.descriptor.isExternalDeclaration()) IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            else IrDeclarationOrigin.DEFINED
        }.apply {
            parent = module
            if (!module.descriptor.original.isExternalDeclaration()) {
                initializer = DeclarationIrBuilder(pluginContext, symbol).run {
                    irExprBody(
                        irCall(module.constructors.single())
                            .also { ignoredExpressions += it }
                    )
                }
            }
        }
    }
}
