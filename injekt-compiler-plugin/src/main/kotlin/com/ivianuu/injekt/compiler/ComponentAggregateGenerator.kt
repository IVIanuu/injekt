package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ComponentAggregateGenerator(
    private val project: Project,
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
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

            val componentFqName = getComponentFqName(componentCall, fileByCall[componentCall]!!)
            val aggregateName = Name.identifier(
                "${key}\$${componentFqName.asString().replace(".", "_")}"
            )

            pluginContext.irTrace.record(
                InjektWritableSlices.COMPONENT_FQ_NAME,
                componentCall, componentFqName
            )

            val existingComponent = try {
                declarationStore.getComponent(key)
            } catch (e: Exception) {
                null
            }

            if (existingComponent != null) {
                error("Already declared a component for key $key")
            }

            declaration.addClass(
                pluginContext.psiSourceManager.cast(),
                project,
                buildClass {
                    name = aggregateName
                }.apply clazz@{
                    createImplicitParameterDeclarationWithWrappedDescriptor()

                    addConstructor {
                        origin = InjektDeclarationOrigin
                        isPrimary = true
                        visibility = Visibilities.PUBLIC
                    }.apply {
                        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                            +IrDelegatingConstructorCallImpl(
                                startOffset, endOffset,
                                context.irBuiltIns.unitType,
                                pluginContext.symbolTable.referenceConstructor(
                                    context.builtIns.any.unsubstitutedPrimaryConstructor!!
                                )
                            )
                            +IrInstanceInitializerCallImpl(
                                startOffset,
                                endOffset,
                                this@clazz.symbol,
                                context.irBuiltIns.unitType
                            )
                        }
                    }
                },
                InjektClassNames.InjektComponentsPackage
            )

        }

        return super.visitModuleFragment(declaration)
    }

}