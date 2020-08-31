package com.ivianuu.injekt.compiler.ast.string

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithExpectActual
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithModality
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithName
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.fqName
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThis
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.classOrFail
import com.ivianuu.injekt.compiler.ast.tree.type.classOrNull
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformerVoid
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitorVoid
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer

object Ast2StringTranslator {

    fun generate(element: AstElement): String {
        return buildString {
            element.accept(
                Writer(this)
            )
        }
            // replace tabs at beginning of line with white space
            .replace(Regex("\\n(%tab%)+", RegexOption.MULTILINE)) {
                val size = it.range.last - it.range.first - 1
                "\n" + (0..(size / 5)).joinToString("") { "    " }
            }
            // tabs that are inserted in the middle of lines should be replaced with empty strings
            .replace(Regex("%tab%", RegexOption.MULTILINE), "")
            // remove empty lines
            .replace(Regex("\\n(\\s)*$", RegexOption.MULTILINE), "")
            // brackets with comma on new line
            .replace(Regex("}\\n(\\s)*,", RegexOption.MULTILINE), "},")
    }

    private class Writer(out: Appendable) : AstVisitorVoid {

        private val printer = Printer(out, "%tab%")
        private fun emit(value: Any?) {
            printer.print(value)
        }

        private fun emitLine(value: Any?) {
            printer.println(value)
        }

        private fun emitLine() {
            printer.println()
        }

        private fun emitSpace() {
            emit(" ")
        }

        private fun AstElement.emit() {
            accept(this@Writer)
        }

        private inline fun indented(body: () -> Unit) {
            printer.pushIndent()
            body()
            printer.popIndent()
        }

        private inline fun bracedBlock(
            header: String = "",
            body: () -> Unit
        ) {
            emitLine("$header {")
            indented(body)
            emitLine()
            emitLine("}")
        }

        private var currentFile: AstFile? = null

        override fun visitElement(element: AstElement, data: Nothing?) {
            error("Unhandled $element")
        }

        override fun visitFile(file: AstFile, data: Nothing?) {
            val previousFile = currentFile
            currentFile = file
            if (file.packageFqName != FqName.ROOT)
                emitLine("package ${file.packageFqName}")

            val imports = mutableSetOf<FqName>()
            file.transform(
                object : AstTransformerVoid {
                    override fun visitType(type: AstType): AstTransformResult<AstType> {
                        type.classOrNull?.fqName?.let { imports += it }
                        return super.visitType(type)
                    }
                }
            )

            if (imports.isNotEmpty()) {
                emitLine()
                imports
                    .filterNot { it.parent().isRoot || it.parent() == file.packageFqName }
                    .forEach { emitLine("import $it") }
            }

            if (file.declarations.isNotEmpty()) emitLine()

            file.emitDeclarations()

            currentFile = previousFile
        }

        override fun visitClass(klass: AstClass, data: Nothing?) {
            klass.emitAnnotations()
            if (klass.kind != AstClass.Kind.ENUM_ENTRY) klass.emitVisibility()
            if (klass.kind != AstClass.Kind.ENUM_ENTRY) klass.emitExpectActual()
            if (klass.kind != AstClass.Kind.INTERFACE &&
                klass.kind != AstClass.Kind.ENUM_CLASS &&
                klass.kind != AstClass.Kind.ENUM_ENTRY
            ) klass.emitModality()
            if (klass.isFun) {
                emit("fun ")
            }
            if (klass.isData) {
                emit("data ")
            }
            if (klass.isInner) {
                emit("inner ")
            }
            if (klass.isExternal) {
                emit("external ")
            }
            when (klass.kind) {
                AstClass.Kind.CLASS -> emit("class ")
                AstClass.Kind.INTERFACE -> emit("interface ")
                AstClass.Kind.ENUM_CLASS -> emit("enum class ")
                AstClass.Kind.ENUM_ENTRY -> emit("")
                AstClass.Kind.ANNOTATION -> emit("annotation class ")
                AstClass.Kind.OBJECT -> {
                    if (klass.isCompanion) emit("companion ")
                    emit("object ")
                }
            }.let { }

            emit("${klass.name}")
            if (klass.typeParameters.isNotEmpty()) {
                klass.typeParameters.emitList()
                emitSpace()
                klass.typeParameters.emitWhere()
                emitSpace()
            }

            val primaryConstructor = klass.primaryConstructor

            if (primaryConstructor != null) {
                emit("(")
                primaryConstructor.valueParameters.forEachIndexed { index, valueParameter ->
                    valueParameter.emit()
                    if (index != primaryConstructor.valueParameters.lastIndex) emit(", ")
                }
                emit(") ")
            }

            if (klass.typeParameters.isEmpty() && primaryConstructor == null) {
                emitSpace()
            }

            val declarationsExceptPrimaryConstructor = klass.declarations
                .filter { it != primaryConstructor }

            if (declarationsExceptPrimaryConstructor.isNotEmpty()) {
                bracedBlock {
                    val (enumEntryDeclarations, otherDeclarations) = declarationsExceptPrimaryConstructor
                        .partition { it is AstClass && it.kind == AstClass.Kind.ENUM_ENTRY }

                    enumEntryDeclarations.forEachIndexed { index, declaration ->
                        declaration.emit()
                        if (index != enumEntryDeclarations.lastIndex) {
                            emitLine(",")
                        } else {
                            emitLine(";")
                        }
                    }

                    otherDeclarations
                        .forEach { declaration ->
                            declaration.emit()
                            emitLine()
                        }
                }
            }
            emitLine()
        }

        override fun visitFunction(
            function: AstFunction,
            data: Nothing?
        ) {
            function.emitAnnotations()
            function.emitVisibility()
            function.emitExpectActual()
            if (function.parent is AstClass) function.emitModality()
            if (function.overriddenDeclarations.isNotEmpty()) {
                emit("override ")
            }
            if (function.isInline) {
                emit("inline ")
            }
            if (function.isExternal) {
                emit("external ")
            }
            if (function.isInfix) {
                emit("infix ")
            }
            if (function.isOperator) {
                emit("operator ")
            }
            if (function.isTailrec) {
                emit("tailrec ")
            }
            if (function.isSuspend) {
                emit("suspend ")
            }
            emit("fun ")
            if (function.typeParameters.isNotEmpty()) {
                function.typeParameters.emitList()
                emitSpace()
            }
            when (function.kind) {
                AstFunction.Kind.SIMPLE_FUNCTION -> emit(function.name)
                AstFunction.Kind.PROPERTY_GETTER -> emit("get")
                AstFunction.Kind.PROPERTY_SETTER -> emit("set")
                AstFunction.Kind.CONSTRUCTOR -> emit("constructor")
            }.let {}
            emit("(")
            function.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.emit()
                if (index != function.valueParameters.lastIndex) emit(", ")
            }
            emit(")")
            if (function.kind != AstFunction.Kind.PROPERTY_SETTER &&
                function.kind != AstFunction.Kind.PROPERTY_GETTER &&
                function.kind != AstFunction.Kind.CONSTRUCTOR
            ) {
                emit(": ")
                function.returnType.emit()
                emitSpace()
                function.typeParameters.emitWhere()
            }
            emitSpace()

            function.body?.let { body ->
                bracedBlock {
                    body.emit()
                }
            } ?: emitLine()
        }

        override fun visitProperty(property: AstProperty, data: Nothing?) {
            property.emitAnnotations()
            property.emitVisibility()
            if (property.parent is AstClass) property.emitModality()
            if (property.overriddenDeclarations.isNotEmpty()) {
                emit("override ")
            }
            if (property.setter != null) {
                emit("var ")
            } else {
                emit("val ")
            }
            if (property.typeParameters.isNotEmpty()) {
                property.typeParameters.emitList()
                emitSpace()
            }
            emit("${property.name}")
            emit(": ")
            property.type.emit()
            if (property.typeParameters.isNotEmpty()) {
                emitSpace()
                property.typeParameters.emitWhere()
            }
            if (property.initializer != null) {
                emit(" = ")
                property.initializer!!.emit()
            }
            if (property.delegate != null) {
                emit(" by ")
                property.delegate!!.emit()
            }
            if (property.getter != null) {
                emitLine()
                emit(
                    indented {
                        property.getter!!.emit()
                    }
                )
            }
            if (property.setter != null) {
                emitLine()
                emit(
                    indented {
                        property.setter!!.emit()
                    }
                )
            }
            emitLine()
        }

        override fun visitAnonymousInitializer(
            anonymousInitializer: AstAnonymousInitializer,
            data: Nothing?
        ) {
            bracedBlock(header = "init") {
                anonymousInitializer.body.emit()
            }
        }

        override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?) {
            typeParameter.emit(null)
        }

        private fun List<AstTypeParameter>.emitList() {
            if (isNotEmpty()) {
                emit("<")
                forEachIndexed { index, typeParameter ->
                    typeParameter.emit()
                    if (index != lastIndex) emit(", ")
                }
                emit(">")
            }
        }

        private fun List<AstTypeParameter>.emitWhere() {
            if (isNotEmpty()) {
                emit("where ")
                val typeParametersWithSuperTypes = flatMap { typeParameter ->
                    typeParameter.superTypes
                        .map { typeParameter to it }
                }

                typeParametersWithSuperTypes.forEachIndexed { index, (typeParameter, superType) ->
                    typeParameter.emit(superType)
                    if (index != typeParametersWithSuperTypes.lastIndex) emit(", ")
                }
            }
        }

        private fun AstTypeParameter.emit(superTypeToRender: AstType?) {
            if (isReified) {
                emit("reified ")
            }
            emitAnnotations()
            emit("$name")
            if (superTypeToRender != null) {
                emit(" : ")
                superTypeToRender.emit()
            }
        }

        override fun visitValueParameter(
            valueParameter: AstValueParameter,
            data: Nothing?
        ) {
            valueParameter.emitAnnotations()
            if (valueParameter.isVarArg) {
                emit("vararg ")
            }
            valueParameter.inlineHint?.let {
                emit("${it.name.toLowerCase()} ")
            }
            emit("${valueParameter.name}: ")
            valueParameter.type.emit()
            if (valueParameter.defaultValue != null) {
                emit(" = ")
                valueParameter.defaultValue!!.emit()
            }
        }

        override fun visitTypeAlias(typeAlias: AstTypeAlias, data: Nothing?) {
            typeAlias.emitAnnotations()
            emit("typealias ")
            emit("${typeAlias.name}")
            typeAlias.typeParameters.emitList()
            emit(" = ")
            typeAlias.type.emit()
        }

        private fun AstDeclarationWithVisibility.emitVisibility(emitSpace: Boolean = true) {
            emit(visibility.name.toLowerCase())
            if (emitSpace) emitSpace()
        }

        private fun AstDeclarationWithExpectActual.emitExpectActual(emitSpace: Boolean = true) {
            if (expectActual != null) {
                emit(expectActual!!.name.toLowerCase())
                if (emitSpace) emitSpace()
            }
        }

        private fun AstDeclarationWithModality.emitModality(emitSpace: Boolean = true) {
            emit(modality.name.toLowerCase())
            if (emitSpace) emitSpace()
        }

        private fun AstDeclarationContainer.emitDeclarations() {
            declarations.forEachIndexed { index, declaration ->
                declaration.emit()
                if (index != declarations.lastIndex) emitLine()
            }
        }

        private fun AstAnnotationContainer.emitAnnotations() {
            annotations.forEachIndexed { index, annotation ->
                emit("@TODO")
                if (index != annotations.lastIndex) emitLine()
            }
        }

        override fun <T> visitConst(const: AstConst<T>, data: Nothing?) {
            emit(
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

        override fun visitBlock(block: AstBlock, data: Nothing?) {
            block.statements.forEachIndexed { index, statement ->
                statement.emit()
                if (index != block.statements.lastIndex) emitLine()
            }
        }

        override fun visitStringConcatenation(
            stringConcatenation: AstStringConcatenation,
            data: Nothing?
        ) {
            stringConcatenation.arguments.forEachIndexed { index, expression ->
                expression.emit()
                if (expression.type.classOrNull?.fqName != KotlinBuiltIns.FQ_NAMES.string.toSafe())
                    emit(".toString()")
                if (index != stringConcatenation.arguments.lastIndex)
                    emit(" + ")
            }
        }

        override fun visitQualifiedAccess(
            qualifiedAccess: AstQualifiedAccess,
            data: Nothing?
        ) {
            val explicitReceiver = if (qualifiedAccess.extensionReceiver != null)
                qualifiedAccess.extensionReceiver
            else qualifiedAccess.dispatchReceiver
                ?.takeIf { it !is AstThis }

            if (explicitReceiver != null) {
                explicitReceiver.emit()
                emit(".")
            }
            val callee = qualifiedAccess.callee
            if (callee is AstFunction && callee.kind == AstFunction.Kind.CONSTRUCTOR) {
                emit(callee.returnType.classOrFail.name)
            } else if (callee is AstDeclarationWithName) {
                emit("${callee.name}")
            }
            if (qualifiedAccess.typeArguments.isNotEmpty()) {
                emit("<")
                qualifiedAccess.typeArguments.forEachIndexed { index, typeArgument ->
                    typeArgument.emit()
                    if (index != qualifiedAccess.typeArguments.lastIndex) emit(", ")
                }
                emit(">")
            }
            if (callee is AstFunction) {
                emit("(")
                qualifiedAccess.valueArguments.forEachIndexed { index, valueArgument ->
                    if (valueArgument != null) {
                        emit("${callee.valueParameters[index].name} = ")
                        valueArgument.emit()
                        if (index != qualifiedAccess.valueArguments.lastIndex &&
                            qualifiedAccess.valueArguments[index + 1] != null
                        ) emit(", ")
                    }
                }
                emit(")")
            }
        }

        override fun visitTry(astTry: AstTry, data: Nothing?) {
            emitLine("try {")
            indented {
                astTry.tryResult.emit()
            }
            emitLine()
            if (astTry.catches.isNotEmpty()) {
                astTry.catches.forEach {
                    emit("} catch(")
                    it.catchParameter.emit()
                    emitLine(") {")
                    indented {
                        it.emit()
                    }
                    emitLine()
                }
            }
            astTry.finally?.let {
                emitLine("} finally {")
                indented {
                    it.emit()
                }
                emitLine()
            }
            emitLine("}")
        }

        override fun visitReturn(astReturn: AstReturn, data: Nothing?) {
            emit("return")
            /*astReturn.target?.let {
                emit("@$")
            } ?:*/ emitSpace()
            astReturn.expression.emit()
        }

        override fun visitThrow(astThrow: AstThrow, data: Nothing?) {
            emit("throw ")
            astThrow.expression.emit()
        }

        private fun AstType.emit() {
            emitAnnotations()

            when (val classifier = classifier) {
                is AstClass -> emit(classifier.fqName)
                is AstTypeParameter -> emit(classifier.name)
                else -> error("Unexpected classifier $classifier")
            }

            if (arguments.isNotEmpty()) {
                emit("<")
                arguments.forEachIndexed { index, typeArgument ->
                    typeArgument.emit()
                    if (index != arguments.lastIndex) emit(", ")
                }
                emit(">")
            }
        }

        private fun AstTypeArgument.emit() {
            when (this) {
                is AstStarProjection -> emit("*")
                is AstTypeProjection -> {
                    emit(variance?.let { "${it.name.toLowerCase()} " }
                        .orEmpty())
                    type.emit()
                }
                else -> error("Unexpected type argument $this")
            }
        }
    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
