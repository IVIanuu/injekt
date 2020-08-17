package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.lambdaContext
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RunReaderCallTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    private data class NewIndexBuilder(
        val originatingDeclaration: IrDeclarationWithName,
        val name: Name,
        val classBuilder: IrClass.() -> Unit
    )

    override fun lower() {
        val newIndexBuilders = mutableListOf<NewIndexBuilder>()

        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.runReader")
                    return result

                val contextExpression = result.extensionReceiver!!
                val lambdaExpression = result.getValueArgument(0)!!

                newIndexBuilders += NewIndexBuilder(
                    contextExpression.type.classOrNull!!.owner,
                    ((currentScope!!.irElement as IrDeclarationWithName)
                        .descriptor.fqNameSafe.asString().hashCode() + expression.startOffset)
                        .toString()
                        .asNameId()
                ) {
                    addMetadataIfNotLocal()
                    annotations += DeclarationIrBuilder(injektContext, symbol).run {
                        irCall(injektContext.injektSymbols.runReaderCall.constructors.single()).apply {
                            putValueArgument(
                                0,
                                irClassReference(lambdaExpression.type.lambdaContext!!)
                            )
                        }
                    }
                }

                return DeclarationIrBuilder(injektContext, result.symbol).run {
                    irCall(
                        injektContext.referenceFunctions(
                            FqName("com.ivianuu.injekt.internal.runReaderDummy")
                        ).single()
                    ).apply {
                        putTypeArgument(0, contextExpression.type)
                        putTypeArgument(1, lambdaExpression.type.typeArguments.last().typeOrFail)
                        putValueArgument(0, contextExpression)
                        putValueArgument(1, lambdaExpression)
                    }
                }
            }
        })

        newIndexBuilders.forEach {
            indexer.index(
                it.originatingDeclaration,
                it.name,
                listOf(
                    DeclarationGraph.RUN_READER_CALL_PATH,
                    it.originatingDeclaration.descriptor.fqNameSafe.asString()
                ),
                it.classBuilder
            )
        }
    }

}
