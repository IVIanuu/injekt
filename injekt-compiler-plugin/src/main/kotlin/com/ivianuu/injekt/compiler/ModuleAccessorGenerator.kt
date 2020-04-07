package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ModuleAccessorGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        val moduleProperties = mutableListOf<IrProperty>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.annotations.forEach {
                    if (!it.symbol.isBound) pluginContext.irProvider.getDeclaration(it.symbol)
                }
                if (declaration.annotations.hasAnnotation(InjektClassNames.ModuleMarker)) {
                    moduleProperties += declaration
                }
                return super.visitProperty(declaration)
            }
        })

        moduleProperties.forEach { declaration.addChild(moduleAccessor(it)) }

        (declaration as IrFileImpl).metadata = MetadataSource.File(
            declaration.declarations
                .map { it.descriptor }
        )

        return super.visitFile(declaration)
    }

    private fun moduleAccessor(moduleProperty: IrProperty): IrFunction {
        val descriptor = object : SimpleFunctionDescriptorImpl(
            moduleProperty.descriptor.containingDeclaration,
            null,
            Annotations.EMPTY,
            Name.identifier(moduleProperty.name.asString() + "\$ModuleAccessor"),
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE
        ) {
            init {
                initialize(
                    null,
                    null,
                    emptyList(),
                    emptyList(),
                    moduleProperty.descriptor.type,
                    Modality.FINAL,
                    Visibilities.PUBLIC
                )
            }
        }

        return symbolTable.declareSimpleFunction(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            descriptor
        ) {
            IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                InjektOrigin,
                it,
                moduleProperty.descriptor.type.toIrType()
            ).apply {
                createParameterDeclarations(descriptor)
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irReturn(irCall(moduleProperty.getter!!))
                }
            }
        }
    }

}
