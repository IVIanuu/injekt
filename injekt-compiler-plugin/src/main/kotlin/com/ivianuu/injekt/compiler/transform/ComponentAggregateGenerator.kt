package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.getComponentFqName
import com.ivianuu.injekt.compiler.getConstant
import com.ivianuu.injekt.compiler.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentAggregateGenerator(
    private val project: Project,
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val componentCalls = mutableListOf<IrCall>()
        val fileByCall = mutableMapOf<IrCall, IrFile>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val fileStack = mutableListOf<IrFile>()
            override fun visitFile(declaration: IrFile): IrFile {
                fileStack.push(declaration)
                return super.visitFile(declaration)
                    .also { fileStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                if (expression.symbol.descriptor.fqNameSafe
                        .asString() == "com.ivianuu.injekt.Component"
                ) {
                    componentCalls += expression
                    fileByCall[expression] = fileStack.last()
                }
                return expression
            }
        })

        componentCalls.forEach { componentCall ->
            val key = componentCall.getValueArgument(0)!!.getConstant<String>()

            val componentFqName = getComponentFqName(
                componentCall,
                fileByCall[componentCall]!!
            )
            val aggregateName = Name.identifier(
                "${key}\$${componentFqName.asString().replace(".", "_")}"
            )

            pluginContext.irTrace.record(
                InjektWritableSlices.COMPONENT_FQ_NAME,
                componentCall, componentFqName
            )

            declaration.addEmptyClass(
                pluginContext,
                project,
                aggregateName,
                InjektFqNames.InjektComponentsPackage
            )

        }

        return super.visitModuleFragment(declaration)
    }

}