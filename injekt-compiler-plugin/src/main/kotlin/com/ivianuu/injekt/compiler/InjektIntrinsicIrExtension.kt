package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.toFactoryImplFqName
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.SimpleType

@Given
class InjektIntrinsicIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val rootFactoryStubByType = mutableMapOf<SimpleType, IrClass>()
        moduleFragment.transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    val result = super.visitCall(expression) as IrCall
                    return if (expression.symbol.descriptor.fqNameSafe ==
                        InjektFqNames.rootFactoryFun
                    ) {
                        val rootFactoryType = result.getTypeArgument(0)!!
                            .let { it as IrSimpleType }
                            .abbreviation!!.typeAlias.descriptor.defaultType
                        val rootFactoryStub = rootFactoryStubByType.getOrPut(rootFactoryType) {
                            val rootFactoryImplFqName =
                                rootFactoryType.constructor.declarationDescriptor!!
                                    .fqNameSafe.toFactoryImplFqName()
                            buildClass {
                                this.name = rootFactoryImplFqName.shortName()
                                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                                kind = ClassKind.OBJECT
                                visibility = Visibilities.INTERNAL
                            }.apply clazz@{
                                createImplicitParameterDeclarationWithWrappedDescriptor()
                                parent = IrExternalPackageFragmentImpl(
                                    IrExternalPackageFragmentSymbolImpl(
                                        EmptyPackageFragmentDescriptor(
                                            pluginContext.moduleDescriptor,
                                            rootFactoryImplFqName.parent()
                                        )
                                    ),
                                    rootFactoryImplFqName.parent()
                                )
                            }
                        }
                        DeclarationIrBuilder(pluginContext, result.symbol)
                            .irGetObject(rootFactoryStub.symbol)
                    } else if (expression.symbol.descriptor.fqNameSafe ==
                        InjektFqNames.mergeFactoryFun
                    ) {
                        val mergeFactoryType = result.getTypeArgument(0)!!
                            .let { it as IrSimpleType }
                            .abbreviation!!.typeAlias.descriptor.defaultType
                        val mergeFactoryStub = rootFactoryStubByType.getOrPut(mergeFactoryType) {
                            val mergeFactoryImplFqName =
                                mergeFactoryType.constructor.declarationDescriptor!!
                                    .fqNameSafe.toFactoryImplFqName()
                            buildClass {
                                this.name = mergeFactoryImplFqName.shortName()
                                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                                kind = ClassKind.OBJECT
                                visibility = Visibilities.INTERNAL
                            }.apply clazz@{
                                createImplicitParameterDeclarationWithWrappedDescriptor()
                                parent = IrExternalPackageFragmentImpl(
                                    IrExternalPackageFragmentSymbolImpl(
                                        EmptyPackageFragmentDescriptor(
                                            pluginContext.moduleDescriptor,
                                            mergeFactoryImplFqName.parent()
                                        )
                                    ),
                                    mergeFactoryImplFqName.parent()
                                )
                            }
                        }
                        DeclarationIrBuilder(pluginContext, result.symbol)
                            .irGetObject(mergeFactoryStub.symbol)
                    } else {
                        result
                    }
                }
            }
        )
    }
}
