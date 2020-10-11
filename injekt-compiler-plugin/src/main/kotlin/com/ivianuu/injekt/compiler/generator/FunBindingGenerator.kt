package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding
class FunBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val typeTranslator: TypeTranslator
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val funBindings = mutableListOf<KtNamedFunction>()
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    if (declaration.isTopLevel && declaration.annotationEntries.any {
                            it.text.contains("FunBinding")
                        }) {
                        funBindings += declaration
                    }
                }
            )

            if (funBindings.isNotEmpty()) {
                generateFunctionAliases(file, funBindings)
            }
        }
    }

    private fun generateFunctionAliases(
        file: KtFile,
        funBindings: List<KtNamedFunction>,
    ) {
        val fileName = "${file.name.removeSuffix(".kt")}FunctionAliases.kt"
        val code = buildCodeString {
            emitLine("package ${file.packageFqName}")
            file.importDirectives.forEach {
                emitLine(it.text)
            }
            emitLine()
            funBindings.forEach { function ->
                val descriptor = function.descriptor<FunctionDescriptor>(bindingContext)!!
                val isSuspend = function.hasModifier(KtTokens.SUSPEND_KEYWORD)
                val isComposable = function.annotationEntries.any {
                    it.text.contains("Composable")
                }
                val assistedReceiver = function.receiverTypeReference
                    ?.takeIf {
                        it.annotationEntries
                            .any { it.text.contains("Assisted") }
                    }?.text
                    ?.replace("@${InjektFqNames.Assisted.asString()} ", "")
                    ?.replace("@Assisted ", "")
                val assistedValueParameters = function.valueParameters
                    .filter {
                        it.typeReference?.annotationEntries
                            ?.any { it.text.contains("Assisted") } == true
                    }
                    .map { it.typeReference!!.text }
                    .map {
                        it.replace("@${InjektFqNames.Assisted.asString()} ", "")
                            .replace("@Assisted ", "")
                    }
                val returnType = function.typeReference?.text
                    ?.replace("@${InjektFqNames.Assisted.asString()} ", "")
                    ?.replace("@Assisted ", "")
                    ?: if (function.hasBlockBody()) "Unit" else error(
                        "@FunBinding function must have explicit return type ${function.text}"
                    )

                emitLine("@com.ivianuu.injekt.internal.FunctionAlias")
                emit("typealias ${function.name}")
                declarationStore.addGeneratedClassifier(
                    ClassifierRef(
                        fqName = descriptor.fqNameSafe,
                        typeParameters = descriptor.typeParameters.map {
                            typeTranslator.toClassifierRef(it)
                        },
                        isFunctionAlias = true
                    )
                )
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
                if (isComposable) emit("@${InjektFqNames.Composable} ")
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

        fileManager.generateFile(file.packageFqName, fileName, code)
    }
}
