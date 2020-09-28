package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Context
import org.jetbrains.kotlin.psi.KtFile

interface Generator {
    fun generate(files: List<KtFile>)
}

interface GenerationContext : Context
