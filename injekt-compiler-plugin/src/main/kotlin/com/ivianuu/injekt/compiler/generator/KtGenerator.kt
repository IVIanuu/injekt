package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Context
import org.jetbrains.kotlin.psi.KtFile

interface KtGenerator {
    fun generate(files: List<KtFile>)
}

@Context
interface GenerationContext
