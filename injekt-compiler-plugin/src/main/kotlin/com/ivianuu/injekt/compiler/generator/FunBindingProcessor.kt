/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

@Binding
class FunBindingProcessor(
    private val errorCollector: ErrorCollector,
    private val fileManager: FileManager
) : ElementProcessor {
    override fun process(files: List<KtFile>): List<KtFile> {
        return files.flatMap { file ->
            val files = mutableListOf<KtFile>()
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    if (declaration.hasAnnotation(InjektFqNames.FunBinding)) {
                        runExitCatching {
                            files += generateFunBinding(declaration)
                        }
                    }
                }
            )
            files
        }
    }

    private fun generateFunBinding(declaration: KtNamedFunction): KtFile {
        val file = declaration.containingKtFile
        val packageFqName = file.packageFqName
        val fileName = joinedNameOf(
            packageFqName,
            declaration.fqName!!
        ).asString() + "FunBinding.kt"

        val isSuspend = declaration.hasModifier(KtTokens.SUSPEND_KEYWORD)
        val isComposable = declaration.hasAnnotation(InjektFqNames.Composable)

        val funApiReceiverType = declaration.receiverTypeReference
            ?.takeIf { it.hasAnnotation(InjektFqNames.FunApi) }
            ?.typeElement
            ?.text
        val funApiValueParametersTypes = declaration.valueParameters
            .filter { it.hasAnnotation(InjektFqNames.FunApi) }
            .map { it.typeReference!!.text }
        val returnType = declaration.typeReference?.text
            ?: if (declaration.hasBlockBody()) "Unit" else errorCollector.add(
                "@FunBinding function must have explicit return type ${declaration.text} ${file.virtualFilePath}"
            )

        val code = buildCodeString {
            emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
            emitLine("package $packageFqName")

            val imports = mutableSetOf(
                "import ${InjektFqNames.Binding.asString()}",
                "import ${InjektFqNames.FunBinding.asString()}"
            )
            imports += file
                .importDirectives
                .map { it.text }

            imports.forEach {
                emitLine(it)
            }

            emitLine()

            emit("@${InjektFqNames.FunApiParams}([")
            val funApiParametersNames = buildList<String> {
                if (funApiReceiverType != null) this += "_extensionReceiver"
                this += declaration.valueParameters
                    .filter { it.hasAnnotation(InjektFqNames.FunApi) }
                    .map { it.name!! }
            }
            funApiParametersNames.forEachIndexed { index, parameterName ->
                emit("\"${parameterName}\"")
                if (index != funApiValueParametersTypes.lastIndex) emit(", ")
            }
            emitLine("])")

            if (declaration.visibilityModifier()?.text == "internal") {
                emit("internal ")
            }

            emit("typealias ${declaration.name}")

            if (declaration.typeParameters.isNotEmpty()) {
                emit("<")
                declaration.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != declaration.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emit(" = ")

            if (isComposable) emit("@${InjektFqNames.Composable} ")
            if (isSuspend) emit("suspend ")
            if (funApiReceiverType != null) {
                emit("$funApiReceiverType.")
            }
            emit("(")
            funApiValueParametersTypes.forEachIndexed { index, param ->
                emit(param)
                if (index != funApiValueParametersTypes.lastIndex) emit(", ")
            }
            emitLine(") -> $returnType")

            emitLine()

            if (isComposable) emitLine("@${InjektFqNames.Composable}")

            if (declaration.visibilityModifier()?.text == "internal") {
                emit("internal ")
            }

            if (isSuspend) emit("suspend ")

            emit("fun ")
            declaration.typeParameterList?.text?.let {
                emit(it)
                emitSpace()
            }

            emit(declaration.name)
            if (declaration.typeParameters.isNotEmpty()) {
                emit("<")
                declaration.typeParameters.forEachIndexed { index, typeParameter ->
                    emit(typeParameter.name)
                    if (index != declaration.typeParameters.lastIndex) emit(", ")
                }
                emit(">")
            }

            val invokeFunctionName = "invoke${declaration.name!!.capitalize()}".asNameId()

            emit(".$invokeFunctionName(")
            val funApiValueParameters = declaration.valueParameters
                .filter { it.hasAnnotation(InjektFqNames.FunApi) }
            if (funApiReceiverType != null) {
                emit("_receiver: $funApiReceiverType")
                if (funApiValueParameters.isNotEmpty()) emit(", ")
            }
            funApiValueParameters.forEachIndexed { index, valueParameter ->
                emit("${valueParameter.name}: ${valueParameter.typeReference!!.text}")
                if (valueParameter.defaultValue != null) {
                    emit(" = ${valueParameter.defaultValue!!.text}")
                }
                if (index != funApiValueParametersTypes.lastIndex) emit(", ")
            }
            emit("): $returnType ")
            declaration.typeConstraintList?.let {
                emit("where ")
                emit(it.text)
                emitSpace()
            }
            braced {
                emit("return invoke(")
                if (funApiReceiverType != null) {
                    emit("_receiver")
                    if (funApiValueParameters.isNotEmpty()) emit(", ")
                }
                funApiValueParameters.forEachIndexed { index, valueParameter ->
                    emit(valueParameter.name)
                    if (index != funApiValueParametersTypes.lastIndex) emit(", ")

                }
                emitLine(")")
            }
        }

        return fileManager.generateFile(
            originatingFile = file,
            packageFqName = packageFqName,
            fileName = fileName,
            code = code,
            forAdditionalSource = true
        )!!
    }
}
