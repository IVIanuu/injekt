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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

@Binding
class SpecialBindingProcessor(
    private val errorCollector: ErrorCollector,
    private val fileManager: FileManager
) : ElementProcessor {
    override fun process(files: List<KtFile>): List<KtFile> {
        return files.mapNotNull { file ->
            val specialBindings = mutableListOf<KtCallableDeclaration>()
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    if (declaration.hasAnnotation(InjektFqNames.FunBinding) ||
                            declaration.hasAnnotation(InjektFqNames.TypeBinding)) {
                                specialBindings += declaration as KtCallableDeclaration
                    }
                }
            )
            if (specialBindings.isEmpty()) null
            else generate(file, specialBindings)
        }
    }

    private fun generate(file: KtFile, declaration: List<KtCallableDeclaration>): KtFile {
        val packageFqName = file.packageFqName
        val fileName = "${file.name.removeSuffix(".kt")}SpecialBindings.kt"
        val code = buildCodeString {
            emitLine("package $packageFqName")

            val imports = file
                .importDirectives
                .map { it.text }

            imports.forEach { emitLine(it) }

            emitLine()

            declaration.forEach {
                if (it.hasAnnotation(InjektFqNames.FunBinding)) {
                    generateFunBinding(it as KtNamedFunction)
                } else {
                    generateTypeBinding(it)
                }
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

    private fun CodeBuilder.generateFunBinding(declaration: KtNamedFunction) {
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
                "@FunBinding function must have explicit return type ${declaration.text} " +
                        declaration.containingKtFile.virtualFilePath
            )

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
        declaration.typeParameterList?.text
            ?.replace("reified ", "")
            ?.let {
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

    private fun CodeBuilder.generateTypeBinding(declaration: KtCallableDeclaration) {
        val returnType = declaration.typeReference?.text
            ?: if (declaration is KtNamedFunction &&
                declaration.hasBlockBody()) "Unit" else errorCollector.add(
                "@TypeBinding declarations must have explicit return type ${declaration.text} " +
                        declaration.containingKtFile.virtualFilePath
            )

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
        emitLine(" = $returnType")
    }
}
