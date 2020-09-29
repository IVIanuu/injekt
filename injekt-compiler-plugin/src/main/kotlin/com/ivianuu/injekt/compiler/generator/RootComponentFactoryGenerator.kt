package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import org.jetbrains.kotlin.psi.KtFile

@Given
class RootComponentFactoryGenerator : Generator {
    override fun generate(files: List<KtFile>) {
        files.forEach { file ->

        }
    }
}
