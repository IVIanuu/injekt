package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor

@Binding
class FunctionAliasGenerator(
    @Assisted private val generateFile: (FqName, String, String) -> Unit
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val bindingFunctions = mutableListOf<KtNamedFunction>()
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    if (declaration.isTopLevel && declaration.annotationEntries.any {
                        it.text.contains("Binding")
                    }
                    ) {
                        bindingFunctions += declaration
                    }
                }
            )

            if (bindingFunctions.isNotEmpty()) {
                generateFunctionAliases(file, bindingFunctions)
            }
        }
    }

    private fun generateFunctionAliases(
        file: KtFile,
        bindingFunctions: List<KtNamedFunction>,
    ) {
        val fileName = "${file.name.removeSuffix(".kt")}FunctionAliases.kt"
        val code = buildCodeString {
            emitLine("package ${file.packageFqName}")
            file.importDirectives.forEach {
                emitLine(it.text)
            }
            emitLine()
            bindingFunctions.forEach { function ->
                val isSuspend = function.hasModifier(KtTokens.SUSPEND_KEYWORD)
                val isComposable = function.annotationEntries.any {
                    it.text.contains("Composable")
                }
                val assistedReceiver = function.receiverTypeReference
                    ?.takeIf {
                        it.annotationEntries
                            .any { it.text.contains("Assisted") }
                    }?.text
                val assistedValueParameters = function.valueParameters
                    .filter {
                        it.annotationEntries
                            .any { it.text.contains("Assisted") }
                    }
                    .map { it.typeReference!!.text }
                val returnType = function.typeReference?.text
                    ?: if (function.hasBlockBody()) "Unit" else error(
                        "@Binding function must have explicit return type ${function.text}"
                    )

                emitLine("@com.ivianuu.injekt.internal.FunctionAlias")
                emit("typealias ${function.name}")
                function.typeParameterList?.parameters
                    ?.mapNotNull { it.name }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { typeParameters ->
                        emit("<")
                        typeParameters.forEachIndexed { index, name ->
                            emit(name)
                            if (index != typeParameters.lastIndex) emit(", ")
                        }
                        emit(">")
                    }
                emit(" = ")
                if (isComposable) emit("@androidx.compose.runtime.Composable ")
                if (isSuspend) emit("suspend ")
                if (assistedReceiver != null) {
                    emit("$assistedReceiver.")
                }
                emit("(")
                assistedValueParameters.forEachIndexed { index, param ->
                    emit(param)
                    if (index != assistedValueParameters.lastIndex) emit(", ")
                }
                emitLine(") -> $returnType")
            }
        }

        generateFile(file.packageFqName, fileName, code)
    }
}
