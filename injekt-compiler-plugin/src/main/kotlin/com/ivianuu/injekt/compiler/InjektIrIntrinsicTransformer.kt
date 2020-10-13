package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.generator.toComponentImplFqName
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektIrIntrinsicTransformer : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (result.symbol.descriptor.fqNameSafe.asString() ==
                        "com.ivianuu.injekt.component") {
                    val componentClass = result.getTypeArgument(0)!!.classOrNull!!.owner
                    val componentImplFqName =
                        componentClass.descriptor.fqNameSafe.toComponentImplFqName()
                    val componentImplClassStub = buildClass {
                        this.name = componentImplFqName.shortName()
                        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                        kind = ClassKind.OBJECT
                    }.apply clazz@{
                        createImplicitParameterDeclarationWithWrappedDescriptor()
                        parent = IrExternalPackageFragmentImpl(
                            IrExternalPackageFragmentSymbolImpl(
                                EmptyPackageFragmentDescriptor(
                                    pluginContext.moduleDescriptor,
                                    componentImplFqName.parent()
                                )
                            ),
                            componentImplFqName.parent()
                        )
                    }

                    val componentConstructor = componentClass.constructors.single()

                    val componentImplConstructorStub = componentImplClassStub.addConstructor {
                        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                    }.apply {
                        parent = componentImplClassStub
                        componentConstructor.valueParameters.forEach {
                            addValueParameter(it.name.asString(), it.type)
                        }
                    }

                    return DeclarationIrBuilder(pluginContext, result.symbol)
                        .irCall(componentImplConstructorStub).apply {
                            val arguments = (expression.getValueArgument(0) as? IrVarargImpl)
                                ?.elements ?: emptyList()

                            arguments
                                .map { it as IrExpression }
                                .forEachIndexed { index, argument ->
                                    putValueArgument(index, argument)
                                }
                        }
                }
                return result
            }
        })
    }
}
