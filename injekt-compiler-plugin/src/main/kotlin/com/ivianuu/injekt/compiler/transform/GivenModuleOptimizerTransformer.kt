package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isOptimizableModule
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.BindingTrace

class GivenModuleOptimizerTransformer(
    private val context: InjektContext,
    private val trace: BindingTrace,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
    private val ignoredExpressions = mutableListOf<IrExpression>()
    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.descriptor.isOptimizableModule(context, trace)) {
            instanceFieldForModule(declaration)
        }
        return super.visitClass(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        if (expression in ignoredExpressions) return super.visitConstructorCall(expression)
        if (expression.type.classifierOrNull?.descriptor?.isOptimizableModule(context, trace) == true)  {
            val module = expression.type.classOrNull!!.owner
            val instanceField = instanceFieldForModule(module)
            return DeclarationIrBuilder(pluginContext, expression.symbol)
                .irGetField(null, instanceField)
        }
        return super.visitConstructorCall(expression)
    }

    private fun instanceFieldForModule(module: IrClass): IrField {
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
