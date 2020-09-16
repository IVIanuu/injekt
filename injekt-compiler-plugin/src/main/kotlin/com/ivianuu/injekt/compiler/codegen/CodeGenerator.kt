package com.ivianuu.injekt.compiler.codegen

import org.jetbrains.kotlin.psi.KtFile

interface CodeGenerator {
    fun generate(files: List<KtFile>)
}
