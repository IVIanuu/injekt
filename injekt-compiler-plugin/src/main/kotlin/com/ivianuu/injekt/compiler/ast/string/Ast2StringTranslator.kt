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
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitorVoid
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName

object Ast2StringTranslator {

    fun generate(element: AstElement): String {
        val builder = StringBuilder()
        element.accept(Writer(builder))
        return builder.toString()
    }

    // todo use AstVisitor<String>
    private class Writer(private val builder: StringBuilder) : AstVisitorVoid {

        private var indent = ""

        private fun append(value: Any) {
            builder.append("$value")
        }

        private fun appendLine() {
            builder.appendLine()
        }

        private fun appendLine(value: Any) {
            builder.appendLine("$value")
        }

        private fun appendIndented(value: Any) {
            append(indent)
            append(value)
        }

        private fun appendIndentedLine(value: Any) {
            appendLine("$indent$value")
        }

        private fun appendIndent() {
            append(indent)
        }

        private fun appendSpace() = append(" ")

        private inline fun <T> indented(block: () -> T): T {
            indent += "    "
            val result = block()
            indent = indent.dropLast(4)
            return result
        }

        private inline fun <T> appendBraced(block: () -> T): T = run {
            appendLine("{")
            val result = indented(block)
            appendIndentedLine("}")
            result
        }

        override fun visitElement(element: AstElement) {
            error("Unhandled $element")
        }

        override fun visitFile(file: AstFile) {
            if (file.packageFqName != FqName.ROOT)
                appendIndentedLine("package ${file.packageFqName}")
            if (file.declarations.isNotEmpty()) appendLine()
            file.renderDeclarations()
        }

        override fun visitClass(klass: AstClass) {
            klass.renderAnnotations()
            appendIndent()
            klass.renderVisibility()
            klass.renderExpectActual()
            if (klass.kind != AstClass.Kind.INTERFACE) klass.renderModality()
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
                klass.typeParameters.renderList()
                appendSpace()
                klass.typeParameters.renderWhere()
                appendSpace()
            }

            val primaryConstructor = klass
                .declarations
                .filterIsInstance<AstConstructor>()
                .singleOrNull { it.isPrimary }

            if (primaryConstructor != null) {
                append("(")
                primaryConstructor.valueParameters.forEachIndexed { index, valueParameter ->
                    valueParameter.accept(this)
                    if (index != primaryConstructor.valueParameters.lastIndex) append(", ")
                }
                append(") ")
            }

            if (klass.typeParameters.isEmpty() && primaryConstructor == null) {
                appendSpace()
            }

            if (klass.declarations.isNotEmpty()) {
                appendBraced {
                    klass.declarations
                        .filter { it !is AstConstructor || !it.isPrimary }
                        .forEach { it.accept(this) }
                }
            } else {
                appendLine()
            }
        }

        override fun visitSimpleFunction(simpleFunction: AstSimpleFunction) {
            simpleFunction.renderAnnotations()
            appendIndent()
            simpleFunction.renderVisibility()
            simpleFunction.renderExpectActual()
            if (simpleFunction.parent is AstClass) simpleFunction.renderModality()
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
                simpleFunction.typeParameters.renderList()
                append(" ")
            }
            append("${simpleFunction.name}")
            append("(")
            simpleFunction.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.accept(this)
                if (index != simpleFunction.valueParameters.lastIndex) append(", ")
            }
            append(")")
            append(": ")
            simpleFunction.returnType.render()
            appendSpace()
            if (simpleFunction.typeParameters.isNotEmpty()) {
                simpleFunction.typeParameters.renderWhere()
                appendSpace()
            }
            simpleFunction.body?.let { body ->
                appendBraced { body.accept(this) }
            } ?: appendLine()
        }

        override fun visitConstructor(constructor: AstConstructor) {
            constructor.renderAnnotations()
            appendIndent()
            constructor.renderVisibility()
            constructor.renderExpectActual()
            append("constructor")
            append("(")
            constructor.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.accept(this)
                if (index != constructor.valueParameters.lastIndex) append(", ")
            }
            append(")")
            append(": ${constructor.returnType.render()} ")
            constructor.body?.let { body ->
                appendBraced { body.accept(this) }
            } ?: if (!constructor.isPrimary) appendLine()
        }

        override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) {
            propertyAccessor.renderAnnotations()
            appendIndent()
            if (propertyAccessor.isSetter) {
                append("set")
            } else {
                append("get")
            }
            append("(")
            propertyAccessor.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.accept(this)
                if (index != propertyAccessor.valueParameters.lastIndex) append(", ")
            }
            append(")")
            propertyAccessor.body?.let { body ->
                appendBraced { body.accept(this) }
            } ?: appendLine()
        }

        override fun visitProperty(property: AstProperty) {
            property.renderAnnotations()
            appendIndent()
            property.renderVisibility()
            if (property.parent is AstClass) property.renderModality()
            if (property.overriddenProperties.isNotEmpty()) {
                append("override ")
            }
            if (property.setter != null) {
                append("var ")
            } else {
                append("val ")
            }
            if (property.typeParameters.isNotEmpty()) {
                property.typeParameters.renderList()
                append(" ")
            }
            append("${property.name}")
            append(": ")
            property.type.render()
            if (property.typeParameters.isNotEmpty()) {
                appendSpace()
                property.typeParameters.renderWhere()
            }
            if (property.initializer != null) {
                append(" = ")
                property.initializer!!.accept(this)
            }
            if (property.delegate != null) {
                append(" by ")
                property.delegate!!.accept(this)
            }
            if (property.getter != null) {
                appendLine()
                indented {
                    property.getter!!.accept(this)
                }
            }
            if (property.setter != null) {
                appendLine()
                indented {
                    property.setter!!.accept(this)
                }
            }
            appendLine()
        }

        override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) {
            appendIndent()
            append("init ")
            anonymousInitializer.body?.let { body ->
                appendBraced { body.accept(this) }
            } ?: appendLine()
        }

        override fun visitTypeParameter(typeParameter: AstTypeParameter) {
            typeParameter.render(null)
        }

        private fun List<AstTypeParameter>.renderList() {
            if (isNotEmpty()) {
                append("<")
                forEachIndexed { index, typeParameter ->
                    typeParameter.accept(this@Writer)
                    if (index != lastIndex) append(", ")
                }
                append(">")
            }
        }

        private fun List<AstTypeParameter>.renderWhere() {
            if (isNotEmpty()) {
                append("where ")
                val typeParametersWithSuperTypes = flatMap { typeParameter ->
                    typeParameter.superTypes
                        .map { typeParameter to it }
                }

                typeParametersWithSuperTypes.forEachIndexed { index, (typeParameter, superType) ->
                    typeParameter.render(superType)
                    if (index != typeParametersWithSuperTypes.lastIndex) append(", ")
                }
            }
        }

        private fun AstTypeParameter.render(superTypeToRender: AstType?) {
            if (isReified) {
                append("reified ")
            }
            renderAnnotations()
            append("$name")
            if (superTypeToRender != null) {
                append(" : ")
                superTypeToRender.render()
            }
        }

        override fun visitValueParameter(valueParameter: AstValueParameter) {
            valueParameter.renderAnnotations()
            if (valueParameter.isVarArg) {
                append("vararg ")
            }
            valueParameter.inlineHint?.let {
                append("${it.name.toLowerCase()} ")
            }
            append("${valueParameter.name}: ")
            valueParameter.type.render()
            if (valueParameter.defaultValue != null) {
                append(" = ")
                valueParameter.defaultValue!!.accept(this)
            }
        }

        override fun visitTypeAlias(typeAlias: AstTypeAlias) {
            typeAlias.renderAnnotations()
            append("typealias ")
            append("${typeAlias.name}")
            if (typeAlias.typeParameters.isNotEmpty()) {
                typeAlias.typeParameters.renderList()

            }
            append(" = ")
            typeAlias.type.render()
        }

        private fun AstDeclarationWithVisibility.renderVisibility(appendSpace: Boolean = true) {
            append(visibility.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstDeclarationWithExpectActual.renderExpectActual(appendSpace: Boolean = true) {
            if (expectActual != null) {
                append(expectActual!!.name.toLowerCase())
                if (appendSpace) appendSpace()
            }
        }

        private fun AstDeclarationWithModality.renderModality(appendSpace: Boolean = true) {
            append(modality.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstDeclarationContainer.renderDeclarations() {
            declarations.forEachIndexed { index, declaration ->
                declaration.accept(this@Writer)
                if (index != declarations.lastIndex) appendLine()
            }
        }

        private fun AstAnnotationContainer.renderAnnotations() {
            annotations.forEachIndexed { index, annotation ->
                appendIndentedLine("@TODO")
                if (index != annotations.lastIndex) appendLine()
            }
        }

        override fun <T> visitConst(const: AstConst<T>) {
            append(
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
            )
        }

        override fun visitBlock(block: AstBlock) {
            block.acceptChildren(this)
        }

        override fun visitStringConcatenation(stringConcatenation: AstStringConcatenation) {
            append("\"")
            stringConcatenation.arguments.forEach {
                if (it.type.isClassType(KotlinBuiltIns.FQ_NAMES.string.toSafe())) append("\${")
                it.accept(this)
                append("}")
            }
            append("\"")
        }

        override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) {
            if (qualifiedAccess.receiver != null) {
                qualifiedAccess.receiver!!.accept(this)
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
                        valueArgument.accept(this)
                        if (index != qualifiedAccess.valueArguments.lastIndex &&
                            qualifiedAccess.valueArguments[index + 1] != null
                        ) append(", ")
                    }
                }
                append(")")
            }
        }

        override fun visitReturn(astReturn: AstReturn) {
            appendIndent()
            append("return")
            astReturn.target.label?.let {
                append("@$it ")
            } ?: append(" ")
            astReturn.expression.accept(this)
            appendLine()
        }

        private fun AstType.render() {
            renderAnnotations()

            when (val classifier = classifier) {
                is AstClass -> append(classifier.fqName)
                is AstTypeParameter -> append(classifier.name)
                else -> error("Unexpected classifier $classifier")
            }

            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, typeArgument ->
                    typeArgument.renderTypeArgument()
                    if (index != arguments.lastIndex) append(", ")
                }
                append(">")
            }
        }

        private fun AstTypeArgument.renderTypeArgument() {
            when (this) {
                is AstStarProjection -> append("*")
                is AstTypeProjection -> {
                    variance?.let { append("${it.name.toLowerCase()} ") }
                    type.render()
                }
                else -> error("Unexpected type argument $this")
            }
        }
    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
