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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class GivenFunGenerator : Generator {

    override fun generate(context: Generator.Context, files: List<KtFile>) {
        files.forEach { file ->
            val givenFuns = mutableListOf<KtCallableDeclaration>()
            file.accept(
                namedDeclarationRecursiveVisitor { declaration ->
                    if (declaration.hasAnnotation(InjektFqNames.GivenFun)) {
                        givenFuns += declaration as KtCallableDeclaration
                    }
                }
            )
            if (givenFuns.isNotEmpty())
                generate(file, givenFuns, context)
        }
    }

    private fun generate(
        file: KtFile,
        declaration: List<KtCallableDeclaration>,
        context: Generator.Context
    ) {
        val packageFqName = file.packageFqName
        val fileName = "${file.name.removeSuffix(".kt")}GivenFunAliases.kt"
        val code = buildString {
            appendLine("// injekt_${file.virtualFilePath}")
            appendLine("package $packageFqName")

            val imports = file
                .importDirectives
                .map { it.text }

            imports.forEach { appendLine(it) }

            appendLine()

            declaration.forEach { generateFunBinding(it as KtNamedFunction) }
        }

        context.generateFile(
            originatingFile = file,
            packageFqName = packageFqName,
            fileName = fileName,
            code = code
        )
    }

    private fun StringBuilder.generateFunBinding(
        declaration: KtNamedFunction
    ) {
        val isSuspend = declaration.hasModifier(KtTokens.SUSPEND_KEYWORD)
        val isComposable = declaration.hasAnnotation(InjektFqNames.Composable)

        val nonGivenReceiverType = declaration.receiverTypeReference
            ?.takeUnless { it.hasAnnotation(InjektFqNames.Given) }
            ?.typeElement
            ?.text
        val nonGivenValueParameterTypes = declaration.valueParameters
            .filterNot { it.hasAnnotation(InjektFqNames.Given) }
            .map { it.typeReference!!.text }
        val returnType = declaration.typeReference?.text
            ?: if (declaration.hasBlockBody()) "Unit" else {
                // we show a user to the error
                return
            }

        if (declaration.visibilityModifier()?.text == "internal") {
            append("internal ")
        }

        appendLine("@${InjektFqNames.GivenFunAlias}")
        append("typealias ${declaration.name}")

        if (declaration.typeParameters.isNotEmpty()) {
            append("<")
            declaration.typeParameters
                .forEachIndexed { index, typeParameter ->
                    append(typeParameter.name)
                    if (index != declaration.typeParameters.lastIndex) append(", ")
                }
            append(">")
        }
        append(" = ")

        if (isComposable) append("@${InjektFqNames.Composable} ")
        if (isSuspend) append("suspend ")
        if (nonGivenReceiverType != null) {
            append("$nonGivenReceiverType.")
        }
        append("(")
        nonGivenValueParameterTypes.forEachIndexed { index, param ->
            append(param)
            if (index != nonGivenValueParameterTypes.lastIndex) append(", ")
        }
        appendLine(") -> $returnType")

        appendLine()

        if (isComposable) appendLine("@${InjektFqNames.Composable}")

        if (declaration.visibilityModifier()?.text == "internal") {
            append("internal ")
        }

        if (isSuspend) append("suspend ")

        append("fun ")
        declaration.typeParameterList?.text
            ?.replace("reified ", "")
            ?.let {
                append(it)
                append(" ")
            }

        append(declaration.name)
        if (declaration.typeParameters.isNotEmpty()) {
            append("<")
            declaration.typeParameters.forEachIndexed { index, typeParameter ->
                append(typeParameter.name)
                if (index != declaration.typeParameters.lastIndex) append(", ")
            }
            append(">")
        }

        val invokeFunctionName = "invoke${declaration.name!!.capitalize()}".asNameId()

        append(".$invokeFunctionName(")
        val nonGivenValueParameters = declaration.valueParameters
            .filterNot { it.hasAnnotation(InjektFqNames.Given) }
        if (nonGivenReceiverType != null) {
            append("_receiver: $nonGivenReceiverType")
            if (nonGivenValueParameters.isNotEmpty()) append(", ")
        }
        nonGivenValueParameters.forEachIndexed { index, valueParameter ->
            append("${valueParameter.name}: ${valueParameter.typeReference!!.text}")
            if (valueParameter.defaultValue != null) {
                append(" = ${valueParameter.defaultValue!!.text}")
            }
            if (index != nonGivenValueParameterTypes.lastIndex) append(", ")
        }
        append("): $returnType ")
        declaration.typeConstraintList?.let {
            append("where ")
            append(it.text)
            append(" ")
        }
        appendLine("{")
        append("    return invoke(")
        if (nonGivenReceiverType != null) {
            append("_receiver")
            if (nonGivenValueParameters.isNotEmpty()) append(", ")
        }
        nonGivenValueParameters.forEachIndexed { index, valueParameter ->
            append(valueParameter.name)
            if (index != nonGivenValueParameterTypes.lastIndex) append(", ")

        }
        appendLine(")")
        appendLine("}")
    }
}
