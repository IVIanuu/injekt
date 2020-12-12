package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.DumpDir
import com.ivianuu.injekt.compiler.FileManager
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

class InjektIrDumper(
    private val cacheDir: CacheDir,
    private val dumpDir: DumpDir,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val fileManager = FileManager(dumpDir, cacheDir)
        moduleFragment.files.forEach {
            val file = File(it.fileEntry.name)
            fileManager.generateFile(
                it.fqName,
                file.name.removeSuffix(".kt"),
                file.absolutePath,
                it.dumpSrc()
            )
        }
        fileManager.postGenerate()
    }
}