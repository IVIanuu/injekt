package com.ivianuu.injekt.compiler.ast.string

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithExpectActual
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithModality
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithName
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPropertyAccessor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.fqName
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.isClassType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName

object Ast2StringTranslator {

    fun generate(element: AstElement): String = element.accept(Writer(), null)

    private class Writer : AstVisitor<String, Nothing?> {

        private var indent = ""

        private fun indented(value: Any?): String {
            indent += "    "
            val result = "$indent$value"
            indent = indent.dropLast(4)
            return result
        }

        private fun braced(value: String, header: String? = null): String = buildString {
            appendLine("$indent${header?.let { "$it " }.orEmpty()}{")
            appendLine(indented(value))
            appendLine("$indent}")
        }

        private fun StringBuilder.appendSpace() {
            append(" ")
        }

        override fun visitElement(element: AstElement, data: Nothing?): String {
            error("Unhandled $element")
        }

        override fun visitFile(file: AstFile, data: Nothing?): String = buildString {
            if (file.packageFqName != FqName.ROOT)
                append("package ${file.packageFqName}")
            if (file.declarations.isNotEmpty()) appendLine()
            append(file.renderDeclarations())
        }

        override fun visitClass(klass: AstClass, data: Nothing?): String = buildString {
            append(klass.renderAnnotations())
            append(klass.renderVisibility())
            append(klass.renderExpectActual())
            if (klass.kind != AstClass.Kind.INTERFACE) append(klass.renderModality())
            if (klass.isFun) {
                append("fun ")
            }
            if (klass.isData) {
                append("data ")
            }
            if (klass.isInner) {
                append("inner ")
            }
            if (klass.isExternal) {
                append("external ")
            }
            when (klass.kind) {
                AstClass.Kind.CLASS -> append("class ")
                AstClass.Kind.INTERFACE -> append("interface ")
                AstClass.Kind.ENUM_CLASS -> append("enum class ")
                AstClass.Kind.ENUM_ENTRY -> TODO()
                AstClass.Kind.ANNOTATION -> append("annotation class ")
                AstClass.Kind.OBJECT -> {
                    if (klass.isCompanion) append("companion ")
                    append("object ")
                }
            }.let { }

            append("${klass.name}")
            if (klass.typeParameters.isNotEmpty()) {
                append(klass.typeParameters.renderList())
                appendSpace()
                append(klass.typeParameters.renderWhere())
                appendSpace()
            }

            val primaryConstructor = klass
                .declarations
                .filterIsInstance<AstConstructor>()
                .singleOrNull { it.isPrimary }

            if (primaryConstructor != null) {
                append("(")
                primaryConstructor.valueParameters.forEachIndexed { index, valueParameter ->
                    append(valueParameter.accept(this@Writer, null))
                    if (index != primaryConstructor.valueParameters.lastIndex) append(", ")
                }
                append(") ")
            }

            if (klass.typeParameters.isEmpty() && primaryConstructor == null) {
                appendSpace()
            }

            val declarationsExpectPrimaryConstructor = klass.declarations
                .filter { it !is AstConstructor || !it.isPrimary }

            if (declarationsExpectPrimaryConstructor.isNotEmpty()) {
                append(
                    braced(
                        buildString {
                            appendLine()
                            declarationsExpectPrimaryConstructor
                                .forEach { declaration ->
                                    append(declaration.accept(this@Writer, null))
                                }
                        }
                    )
                )
            }
            appendLine()
        }

        override fun visitSimpleFunction(
            simpleFunction: AstSimpleFunction,
            data: Nothing?
        ): String = buildString {
            append(simpleFunction.renderAnnotations())
            append(simpleFunction.renderVisibility())
            append(simpleFunction.renderExpectActual())
            if (simpleFunction.parent is AstClass) append(simpleFunction.renderModality())
            if (simpleFunction.overriddenFunctions.isNotEmpty()) {
                append("override ")
            }
            if (simpleFunction.isInline) {
                append("inline ")
            }
            if (simpleFunction.isExternal) {
                append("external ")
            }
            if (simpleFunction.isInfix) {
                append("infix ")
            }
            if (simpleFunction.isOperator) {
                append("operator ")
            }
            if (simpleFunction.isTailrec) {
                append("tailrec ")
            }
            if (simpleFunction.isSuspend) {
                append("suspend ")
            }
            append("fun ")
            if (simpleFunction.typeParameters.isNotEmpty()) {
                append(simpleFunction.typeParameters.renderList())
                appendSpace()
            }
            append("${simpleFunction.name}")
            append("(")
            simpleFunction.valueParameters.forEachIndexed { index, valueParameter ->
                append(valueParameter.accept(this@Writer, null))
                if (index != simpleFunction.valueParameters.lastIndex) append(", ")
            }
            append(")")
            append(": ")
            append(simpleFunction.returnType.render())
            appendSpace()
            if (simpleFunction.typeParameters.isNotEmpty()) {
                append(simpleFunction.typeParameters.renderWhere())
                appendSpace()
            }
            simpleFunction.body?.let { body ->
                append(braced(body.accept(this@Writer, null)))
            } ?: appendLine()
        }

        override fun visitConstructor(constructor: AstConstructor, data: Nothing?): String =
            buildString {
                append(constructor.renderAnnotations())
                append(constructor.renderVisibility())
                append(constructor.renderExpectActual())
                append("constructor")
                append("(")
                constructor.valueParameters.forEachIndexed { index, valueParameter ->
                    append(valueParameter.accept(this@Writer, null))
                    if (index != constructor.valueParameters.lastIndex) append(", ")
                }
                append(")")
                append(": ${constructor.returnType.render()} ")
                constructor.body?.let { body ->
                    append(braced(body.accept(this@Writer, null)))
                } ?: if (!constructor.isPrimary) appendLine()
            }

        override fun visitPropertyAccessor(
            propertyAccessor: AstPropertyAccessor,
            data: Nothing?
        ): String = buildString {
            append(propertyAccessor.renderAnnotations())
            if (propertyAccessor.isSetter) {
                append("set")
            } else {
                append("get")
            }
            append("(")
            propertyAccessor.valueParameters.forEachIndexed { index, valueParameter ->
                append(valueParameter.accept(this@Writer, null))
                if (index != propertyAccessor.valueParameters.lastIndex) append(", ")
            }
            append(")")
            propertyAccessor.body?.let { body ->
                append(
                    braced(body.accept(this@Writer, null))
                )
            } ?: appendLine()
        }

        override fun visitProperty(property: AstProperty, data: Nothing?): String = buildString {
            append(property.renderAnnotations())
            append(property.renderVisibility())
            if (property.parent is AstClass) append(property.renderModality())
            if (property.overriddenProperties.isNotEmpty()) {
                append("override ")
            }
            if (property.setter != null) {
                append("var ")
            } else {
                append("val ")
            }
            if (property.typeParameters.isNotEmpty()) {
                append(property.typeParameters.renderList())
                appendSpace()
            }
            append("${property.name}")
            append(": ")
            append(property.type.render())
            if (property.typeParameters.isNotEmpty()) {
                appendSpace()
                append(property.typeParameters.renderWhere())
            }
            if (property.initializer != null) {
                append(" = ")
                append(property.initializer!!.accept(this@Writer, null))
            }
            if (property.delegate != null) {
                append(" by ")
                append(property.delegate!!.accept(this@Writer, null))
            }
            if (property.getter != null) {
                appendLine()
                indented {
                    property.getter!!.accept(this@Writer, null)
                }
            }
            if (property.setter != null) {
                appendLine()
                indented {
                    property.setter!!.accept(this@Writer, null)
                }
            }
            appendLine()
        }

        override fun visitAnonymousInitializer(
            anonymousInitializer: AstAnonymousInitializer,
            data: Nothing?
        ): String = braced(
            header = "init",
            value = anonymousInitializer.body.accept(this@Writer, null)
        )

        override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?): String =
            typeParameter.render(null)

        private fun List<AstTypeParameter>.renderList(): String = buildString {
            if (this@renderList.isNotEmpty()) {
                append("<")
                this@renderList.forEachIndexed { index, typeParameter ->
                    append(typeParameter.accept(this@Writer, null))
                    if (index != this@renderList.lastIndex) append(", ")
                }
                append(">")
            }
        }

        private fun List<AstTypeParameter>.renderWhere(): String = buildString {
            if (this@renderWhere.isNotEmpty()) {
                append("where ")
                val typeParametersWithSuperTypes = this@renderWhere.flatMap { typeParameter ->
                    typeParameter.superTypes
                        .map { typeParameter to it }
                }

                typeParametersWithSuperTypes.forEachIndexed { index, (typeParameter, superType) ->
                    append(typeParameter.render(superType))
                    if (index != typeParametersWithSuperTypes.lastIndex) append(", ")
                }
            }
        }

        private fun AstTypeParameter.render(superTypeToRender: AstType?): String = buildString {
            if (isReified) {
                append("reified ")
            }
            append(renderAnnotations())
            append("$name")
            if (superTypeToRender != null) {
                append(" : ")
                append(superTypeToRender.render())
            }
        }

        override fun visitValueParameter(
            valueParameter: AstValueParameter,
            data: Nothing?
        ): String = buildString {
            append(valueParameter.renderAnnotations())
            if (valueParameter.isVarArg) {
                append("vararg ")
            }
            valueParameter.inlineHint?.let {
                append("${it.name.toLowerCase()} ")
            }
            append("${valueParameter.name}: ")
            append(valueParameter.type.render())
            if (valueParameter.defaultValue != null) {
                append(" = ")
                append(valueParameter.defaultValue!!.accept(this@Writer, null))
            }
        }

        override fun visitTypeAlias(typeAlias: AstTypeAlias, data: Nothing?): String = buildString {
            append(typeAlias.renderAnnotations())
            append("typealias ")
            append("${typeAlias.name}")
            if (typeAlias.typeParameters.isNotEmpty()) {
                append(typeAlias.typeParameters.renderList())
            }
            append(" = ")
            append(typeAlias.type.render())
        }

        private fun AstDeclarationWithVisibility.renderVisibility(appendSpace: Boolean = true): String {
            return buildString {
                append(visibility.name.toLowerCase())
                if (appendSpace) appendSpace()
            }
        }

        private fun AstDeclarationWithExpectActual.renderExpectActual(appendSpace: Boolean = true) =
            buildString {
                if (expectActual != null) {
                    append(expectActual!!.name.toLowerCase())
                    if (appendSpace) appendSpace()
                }
            }

        private fun AstDeclarationWithModality.renderModality(appendSpace: Boolean = true): String =
            buildString {
                append(modality.name.toLowerCase())
                if (appendSpace) appendSpace()
            }

        private fun AstDeclarationContainer.renderDeclarations(): String = buildString {
            declarations.forEachIndexed { index, declaration ->
                append(declaration.accept(this@Writer, null))
                if (index != declarations.lastIndex) appendLine()
            }
        }

        private fun AstAnnotationContainer.renderAnnotations(): String = buildString {
            annotations.forEachIndexed { index, annotation ->
                append("@TODO")
                if (index != annotations.lastIndex) appendLine()
            }
        }

        override fun <T> visitConst(const: AstConst<T>, data: Nothing?): String =
            when (const.kind) {
                AstConst.Kind.Null -> "null"
                AstConst.Kind.Boolean -> const.value.toString()
                AstConst.Kind.Char -> "'${const.value}'"
                AstConst.Kind.Byte -> const.value.toString()
                AstConst.Kind.Short -> const.value.toString()
                AstConst.Kind.Int -> const.value.toString()
                AstConst.Kind.Long -> "${const.value}L"
                AstConst.Kind.String -> "\"${const.value}\""
                AstConst.Kind.Float -> "${const.value}f"
                AstConst.Kind.Double -> const.value.toString()
            }

        override fun visitBlock(block: AstBlock, data: Nothing?): String = buildString {
            block.statements.forEachIndexed { index, statement ->
                append(statement.accept(this@Writer, null))
                if (index != block.statements.lastIndex) appendLine()
            }
        }

        override fun visitStringConcatenation(
            stringConcatenation: AstStringConcatenation,
            data: Nothing?
        ): String = buildString {
            append("\"")
            stringConcatenation.arguments.forEach {
                if (it.type.isClassType(KotlinBuiltIns.FQ_NAMES.string.toSafe())) append("\${")
                it.accept(this@Writer, null)
                append("}")
            }
            append("\"")
        }

        override fun visitQualifiedAccess(
            qualifiedAccess: AstQualifiedAccess,
            data: Nothing?
        ): String = buildString {
            if (qualifiedAccess.receiver != null) {
                qualifiedAccess.receiver!!.accept(this@Writer, null)
                append(".")
            }
            val callee = qualifiedAccess.callee
            if (callee is AstConstructor) {
                append(callee.constructedClass.name)
            } else if (callee is AstDeclarationWithName) {
                append("${callee.name}")
            }
            if (qualifiedAccess.typeArguments.isNotEmpty()) {
                append("<")
                qualifiedAccess.typeArguments.forEachIndexed { index, typeArgument ->
                    typeArgument.render()
                    if (index != qualifiedAccess.typeArguments.lastIndex) append(", ")
                }
                append(">")
            }
            if (callee is AstFunction) {
                append("(")
                qualifiedAccess.valueArguments.forEachIndexed { index, valueArgument ->
                    if (valueArgument != null) {
                        append("${callee.valueParameters[index].name} = ")
                        append(valueArgument.accept(this@Writer, null))
                        if (index != qualifiedAccess.valueArguments.lastIndex &&
                            qualifiedAccess.valueArguments[index + 1] != null
                        ) append(", ")
                    }
                }
                append(")")
            }
        }

        override fun visitReturn(astReturn: AstReturn, data: Nothing?): String = buildString {
            append("return")
            astReturn.target.label?.let {
                append("@$it ")
            } ?: appendSpace()
            append(astReturn.expression.accept(this@Writer, null))
        }

        private fun AstType.render(): String = buildString {
            append(renderAnnotations())

            when (val classifier = classifier) {
                is AstClass -> append(classifier.fqName)
                is AstTypeParameter -> append(classifier.name)
                else -> error("Unexpected classifier $classifier")
            }

            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, typeArgument ->
                    append(typeArgument.renderTypeArgument())
                    if (index != arguments.lastIndex) append(", ")
                }
                append(">")
            }
        }

        private fun AstTypeArgument.renderTypeArgument(): String = when (this) {
            is AstStarProjection -> "*"
            is AstTypeProjection -> {
                variance?.let { "${it.name.toLowerCase()} " }
                    .orEmpty() + type.render()
            }
            else -> error("Unexpected type argument $this")
        }
    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
