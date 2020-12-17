package com.ivianuu.injekt.ide

import com.ivianuu.injekt.compiler.generator.Generator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor

class TestGenerator : Generator {

    override fun generate(context: Generator.Context, files: List<KtFile>) {
        files.forEach { file ->
            val functions = mutableListOf<KtNamedFunction>()
            file.accept(
                namedFunctionRecursiveVisitor { namedFunction ->
                    if (namedFunction.isTopLevel) {
                        functions += namedFunction
                    }
                }
            )

            if (functions.isNotEmpty()) {
                context.generateFile(
                    file.packageFqName,
                    "${file.name.removeSuffix(".kt")}Aliases.kt",
                    file,
                    buildString {
                        appendLine("package ${file.packageFqName}")
                        functions.forEach { function ->
                            appendLine("typealias ${function.name} = Unit")
                        }
                    }
                )
            }
        }
    }

}