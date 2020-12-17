package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

interface Generator {
    fun generate(context: Context, files: List<KtFile>)

    interface Context {
        fun generateFile(
            packageFqName: FqName,
            fileName: String,
            originatingFile: KtFile,
            code: String,
        )
    }
}
