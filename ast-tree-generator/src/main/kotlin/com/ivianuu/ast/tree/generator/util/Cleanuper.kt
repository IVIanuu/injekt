package com.ivianuu.ast.tree.generator.util

import com.ivianuu.ast.tree.generator.printer.GENERATED_MESSAGE
import java.io.File

fun removePreviousGeneratedFiles(generationPath: File) {
    generationPath.walkTopDown().forEach {
        if (it.isFile && it.readText().contains(GENERATED_MESSAGE)) {
            it.delete()
        }
    }
}