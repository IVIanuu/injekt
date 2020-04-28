package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ModuleAccessorGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        val modules = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope) &&
                    declaration.extensionReceiverParameter?.type == symbols.componentDsl.defaultType
                ) {
                    modules += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        modules.forEach { declaration.addChild(moduleAccessor(it)) }

        (declaration as IrFileImpl).metadata = MetadataSource.File(
            declaration.declarations
                .map { it.descriptor }
        )

        return super.visitFile(declaration)
    }

    private fun moduleAccessor(module: IrFunction): IrFunction {
        return buildFun {
            name = Name.identifier(module.name.asString() + "\$ModuleAccessor")
            returnType = module.returnType
            origin = ModuleAccessorOrigin(module)
        }.apply {
            extensionReceiverParameter = module.extensionReceiverParameter?.copyTo(this)
            body = DeclarationIrBuilder(context, symbol).irBlockBody {
                +irReturn(irCall(module).apply {
                    extensionReceiver = irGet(extensionReceiverParameter!!)
                })
            }
        }
    }

}

class ModuleAccessorOrigin(val module: IrFunction) : IrDeclarationOrigin
