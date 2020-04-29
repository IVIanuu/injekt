package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace

class RegisterModuleFunctionGenerator(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractInjektTransformer(context, symbolRemapper, bindingTrace) {

    override fun visitFile(declaration: IrFile): IrFile {
        val modules = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule() &&
                    declaration.descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope)
                ) {
                    modules += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        modules.forEach {
            declaration.addChild(
                DeclarationIrBuilder(context, declaration.symbol)
                    .registerModuleFunction(it)
            )
        }

        (declaration as IrFileImpl).metadata = MetadataSource.File(
            declaration.declarations
                .map { it.descriptor }
        )

        return super.visitFile(declaration)
    }

    private fun IrBuilderWithScope.registerModuleFunction(module: IrFunction): IrFunction {
        println("register module function $module")
        return buildFun {
            name = Name.identifier("register\$${module.name.asString()}")
            returnType = context.irBuiltIns.unitType
            origin = RegisterModuleOrigin
        }.apply {
            val registerModule = symbols.moduleRegistry
                .functions
                .single { it.descriptor.name.asString() == "register" }

            val scope = module.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                .single()
                .let {
                    symbolTable.referenceClass(it.type.constructor.declarationDescriptor as ClassDescriptor)
                }
                .ensureBound(this@RegisterModuleFunctionGenerator.context.irProviders)
                .owner

            body = irExprBody(
                irCall(registerModule).apply {
                    dispatchReceiver = irGetObject(symbols.moduleRegistry)
                    putValueArgument(
                        0, IrClassReferenceImpl(
                            startOffset,
                            endOffset,
                            this@RegisterModuleFunctionGenerator.context.irBuiltIns.kClassClass.typeWith(
                                scope.defaultType
                            ),
                            scope.symbol,
                            scope.defaultType
                        )
                    )
                    putValueArgument(
                        1,
                        IrFunctionReferenceImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            symbolTable.referenceClass(context.builtIns.getFunction(1))
                                .typeWith(
                                    symbols.componentDsl.defaultType,
                                    context.irBuiltIns.unitType
                                ),
                            module.symbol,
                            0,
                            null
                        ).apply {
                            dispatchReceiver = if (module.dispatchReceiverParameter != null) {
                                irGetObject(module.dispatchReceiverParameter!!.type.classOrNull!!)
                            } else null
                        }
                    )
                }
            )
        }
    }

}

object RegisterModuleOrigin : IrDeclarationOrigin
