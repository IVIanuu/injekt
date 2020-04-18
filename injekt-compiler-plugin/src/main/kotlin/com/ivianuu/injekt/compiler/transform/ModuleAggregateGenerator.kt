package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project

class ModuleAggregateGenerator(
    private val project: Project,
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    /*override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val modules = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Module) &&
                        declaration.descriptor.getAnnotatedAnnotations()) {
                    modules +=
                }
                return super.visitFunction(declaration)
            }
        })

        modules.forEach { componentCall ->
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
                        origin =
                            InjektDeclarationOrigin
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
                InjektFqNames.InjektModulesPackage
            )

        }

        return super.visitModuleFragment(declaration)
    }
*/
}