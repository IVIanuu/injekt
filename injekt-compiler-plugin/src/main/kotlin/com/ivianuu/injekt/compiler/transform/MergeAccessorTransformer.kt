package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.asNameId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MergeAccessorTransformer(private val context: IrPluginContext) : IrElementTransformerVoid() {

    private var scope: IrDeclarationWithName? = null
    private inline fun <R> inScope(scope: IrDeclarationWithName, block: () -> R): R {
        val prevScope = scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement =
        inScope(declaration) { super.visitClass(declaration) }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        return if (declaration.name.isSpecial) super.visitSimpleFunction(declaration)
        else inScope(declaration) { super.visitSimpleFunction(declaration) }
    }

    override fun visitProperty(declaration: IrProperty): IrStatement =
        inScope(declaration) { super.visitProperty(declaration) }

    override fun visitCall(expression: IrCall): IrExpression {
        return if (expression.symbol.descriptor.fqNameSafe == InjektFqNames.get) {
            val accessorName = scope!!.file.fqName
                .child("${
                    scope!!.descriptor.fqNameSafe.pathSegments().joinToString("_")
                }_${expression.startOffset}".asNameId())
            val accessorClass = context.referenceClass(accessorName)?.owner
                ?: error("Nothing found for $accessorName")
            val accessorFunction = accessorClass
                .declarations
                .filterIsInstance<IrProperty>()
                .single()
                .getter!!

            IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                accessorFunction.symbol,
                null,
                null
            ).apply {
                dispatchReceiver = DeclarationIrBuilder(context, expression.symbol)
                    .irAs(expression.extensionReceiver!!, accessorClass.defaultType)
            }
        } else super.visitCall(expression)
    }

}