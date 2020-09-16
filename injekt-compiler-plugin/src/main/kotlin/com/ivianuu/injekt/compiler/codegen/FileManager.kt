package com.ivianuu.injekt.compiler.codegen

import org.jetbrains.kotlin.name.FqName
import java.io.File

class FileManager(
    private val srcDir: File
) {

    fun writeFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
        originatingFiles: List<String>
    ) {
        val file = srcDir.resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { it.createNewFile() }
        file.writeText(code)
    }

}
