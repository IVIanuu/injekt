package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getGivenFunctionType
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor

@Given
class FunctionAliasGenerator : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val givenFunctions = mutableListOf<FunctionDescriptor>()
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<FunctionDescriptor>()
                        ?: return@namedFunctionRecursiveVisitor
                    if (descriptor.hasAnnotation(InjektFqNames.Given) ||
                        descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect)
                    ) {
                        givenFunctions += descriptor
                    }
                }
            )

            if (givenFunctions.isNotEmpty()) {
                generateFunctionAliases(file, givenFunctions)
            }
        }
    }

    private fun generateFunctionAliases(
        file: KtFile,
        givenFunctions: List<FunctionDescriptor>,
    ) {
        val fileName = "${file.name.removeSuffix(".kt")}FunctionAliases.kt"
        val code = buildCodeString {
            emitLine("package ${file.packageFqName}")
            emitLine("import ${InjektFqNames.FunctionAlias}")
            emitLine()
            givenFunctions.forEach { function ->
                val aliasType = function.getGivenFunctionType().toTypeRef()
                emitLine("typealias ${function.name} = ${aliasType.render()}")
            }
        }

        generateFile(
            packageFqName = file.packageFqName,
            fileName = fileName,
            code = code
        )
    }

}
