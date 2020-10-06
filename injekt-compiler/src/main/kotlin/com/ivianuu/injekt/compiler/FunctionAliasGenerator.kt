package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import com.ivianuu.injekt.Binding

@Binding
class FunctionAliasGenerator(
    private val codeGenerator: CodeGenerator,
    private val injektTypes: InjektTypes
) : Generator {

    override fun generate(files: List<KSFile>) {
        files.forEach { file ->
            val funBindings = mutableListOf<KSFunctionDeclaration>()
            file.accept(
                object : KSTopDownVisitor<Nothing?, Unit>() {
                    override fun defaultHandler(node: KSNode, data: Nothing?) {
                    }
                    override fun visitFunctionDeclaration(
                        function: KSFunctionDeclaration,
                        data: Nothing?
                    ) {
                        super.visitFunctionDeclaration(function, data)
                        if (function.parentDeclaration !is KSClassDeclaration &&
                            function.hasAnnotation(injektTypes.funBinding)) {
                            funBindings += function
                        }
                    }
                },
                null
            )

            if (funBindings.isNotEmpty()) {
                generateFunctionAliases(file, funBindings)
            }
        }
    }

    private fun generateFunctionAliases(
        file: KSFile,
        funBindings: List<KSFunctionDeclaration>,
    ) {
        val fileName = "${file.fileName.removeSuffix(".kt")}FunctionAliases.kt"
        val code = buildCodeString {
            emitLine("package ${file.packageName.asString()}")
            emitLine()
            funBindings.forEach { function ->
                val isSuspend = Modifier.SUSPEND in function.modifiers
                val isComposable = function.hasAnnotation(injektTypes.composable)
                val assistedReceiver = function.extensionReceiver
                    ?.takeIf { it.hasAnnotation(injektTypes.assisted) }
                    ?.resolve()
                    ?.toTypeRef(injektTypes)
                val assistedValueParameters = function.parameters
                    .filter { it.type!!.resolve().hasAnnotation(injektTypes.assisted) }
                    .map { it.type!!.resolve().toTypeRef(injektTypes) }
                val returnType = function.returnType!!.resolve().toTypeRef(injektTypes)
                emitLine("@com.ivianuu.injekt.internal.FunctionAlias")
                emit("typealias ${function.simpleName.asString()}")
                function.typeParameters
                    .map { it.name.asString() }
                    .takeIf { it.isNotEmpty() }
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
                    emit("${assistedReceiver.render()}.")
                }
                emit("(")
                assistedValueParameters.forEachIndexed { index, param ->
                    emit(param.render())
                    if (index != assistedValueParameters.lastIndex) emit(", ")
                }
                emitLine(") -> ${returnType.render()}")
            }
        }

        codeGenerator.generateFile(file.packageName.asString(), fileName, code)
    }
}
