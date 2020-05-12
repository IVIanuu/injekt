package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl

class FileMetadataPatcher(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        (declaration as IrFileImpl).metadata =
            MetadataSource.File(declaration.declarations.map { it.descriptor })
        return super.visitFile(declaration)
    }

}
