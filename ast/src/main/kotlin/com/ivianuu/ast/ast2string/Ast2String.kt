package com.ivianuu.ast.ast2string

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationContainer
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.regularClassOrNull
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.visitors.AstVisitorVoid
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer

fun AstElement.toKotlinSourceString(): String {
    return buildString {
        try {
            accept(Ast2KotlinSourceWriter(this), null)
        } catch (e: Exception) {
            throw RuntimeException("${this.toString().format()}", e)
        }
    }.format()
}

private fun String.format() =
    // replace tabs at beginning of line with white space
    replace(Regex("\\n(%tab%)+", RegexOption.MULTILINE)) {
        val size = it.range.last - it.range.first - 1
        "\n" + (0..(size / 5)).joinToString("") { "    " }
    }
        // tabs that are inserted in the middle of lines should be replaced with empty strings
        .replace(Regex("%tab%", RegexOption.MULTILINE), "")
        // remove empty lines
        .replace(Regex("\\n(\\s)*$", RegexOption.MULTILINE), "")
        // brackets with comma on new line
        .replace(Regex("}\\n(\\s)*,", RegexOption.MULTILINE), "},")

private class Ast2KotlinSourceWriter(out: Appendable) : AstVisitorVoid() {

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
        accept(this@Ast2KotlinSourceWriter, null)
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private inline fun bracedBlock(
        header: String? = null,
        body: () -> Unit
    ) {
        emitLine("${header?.let { "$it " }.orEmpty()}{")
        indented(body)
        emitLine()
        emitLine("}")
    }

    /*private val uniqueNameByElement =
        mutableMapOf<AstElement, String>()
    private val existingNames = mutableSetOf<String>()
    private fun AstElement.uniqueName(): String {
        return uniqueNameByElement.getOrPut(this) {
            /*if (this is AstDeclarationWithName &&
                !name.isSpecial
            ) return@getOrPut name.asString()*/ //todo
            val finalBase = "uniqueName"
            var name = finalBase
            var differentiator = 2
            while (name in existingNames) {
                name = finalBase + differentiator
                differentiator++
            }
            existingNames += name
            return@getOrPut name
        }
    }*/

    override fun visitElement(element: AstElement) {
        error("Unhandled $element")
    }

    override fun visitFile(file: AstFile) = with(file) {
        if (packageFqName != FqName.ROOT) emitLine("package $packageFqName")

        val imports = mutableSetOf<FqName>()
        accept(
            object : AstVisitorVoid() {
                override fun visitElement(element: AstElement) {
                    element.acceptChildren(this)
                }

                override fun visitType(type: AstType) {
                    super.visitType(type)
                    type.regularClassOrNull?.let {
                        if (!it.owner.name.isSpecial) {
                            imports += it.classId.asSingleFqName()
                        }
                    }
                }
            },
            null
        )

        if (imports.isNotEmpty()) {
            emitLine()
            imports
                .filterNot { it.parent().isRoot || it.parent() == packageFqName }
                .forEach { emitLine("import $it") }
        }

        if (declarations.isNotEmpty()) emitLine()

        emitDeclarations()
    }

    override fun visitRegularClass(regularClass: AstRegularClass) = with(regularClass) {
        emitAnnotations()
        emitVisibility()
        emitPlatformStatus()
        if (classKind != ClassKind.ANNOTATION_CLASS) emitModality()
        if (isFun) {
            emit("fun ")
        }
        if (isData) {
            emit("data ")
        }
        if (isInner) {
            emit("inner ")
        }
        if (isExternal) {
            emit("external ")
        }
        when (classKind) {
            ClassKind.CLASS -> emit("class ")
            ClassKind.INTERFACE -> emit("interface ")
            ClassKind.ENUM_CLASS -> emit("enum class ")
            ClassKind.ENUM_ENTRY -> {} // enum entry is modelled a a separate element
            ClassKind.ANNOTATION_CLASS -> emit("annotation class ")
            ClassKind.OBJECT -> {
                if (isCompanion) emit("companion ")
                emit("object ")
            }
        }.let { }
        if (!name.isSpecial) emit("${name}")

        if (typeParameters.isNotEmpty()) {
            typeParameters.emitList()
            emitSpace()
        }

        val primaryConstructor = declarations
            .filterIsInstance<AstConstructor>()
            .singleOrNull { it.isPrimary }

        if (primaryConstructor != null) {
            emit("(")
            primaryConstructor.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.emit()
                if (index != primaryConstructor.valueParameters.lastIndex) emit(", ")
            }
            emit(") ")
        }

        typeParameters.emitWhere()

        val declarationsExceptPrimaryConstructor = declarations
            .filter { it != primaryConstructor }

        if (declarationsExceptPrimaryConstructor.isNotEmpty()) {
            emitSpace()

            bracedBlock {
                val (enumEntryDeclarations, otherDeclarations) = declarationsExceptPrimaryConstructor
                    .partition { it is AstEnumEntry }

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

    /*override fun visitFunction(function: AstFunction) {
        function.emitAnnotations()

        if (function.kind != AstFunction.Kind.ANONYMOUS_FUNCTION) {
            if (function.visibility != AstVisibility.LOCAL) function.emitVisibility()
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
        }

        if (function.isSuspend) {
            emit("suspend ")
        }

        if (function.kind != AstFunction.Kind.ANONYMOUS_FUNCTION) {
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
                AstFunction.Kind.ANONYMOUS_FUNCTION -> Unit
            }.let {}
        }

        if (function.kind == AstFunction.Kind.ANONYMOUS_FUNCTION) {
            emit("{ ")
        } else {
            emit("(")
        }
        function.valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != function.valueParameters.lastIndex) emit(", ")
        }
        if (function.kind == AstFunction.Kind.ANONYMOUS_FUNCTION) {
            emit(" ->")
        } else {
            emit(")")
        }

        if (function.kind != AstFunction.Kind.PROPERTY_SETTER &&
            function.kind != AstFunction.Kind.PROPERTY_GETTER &&
            function.kind != AstFunction.Kind.CONSTRUCTOR &&
            function.kind != AstFunction.Kind.ANONYMOUS_FUNCTION
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
    }*/

    private fun List<AstExpression?>.emit() {

    }

    override fun visitConstructor(constructor: AstConstructor) = with(constructor) {
        emitAnnotations()
        // todo emitVisibility()
        emit("constructor(")
        valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != valueParameters.lastIndex) emit(", ")
        }
        emit(")")
        if (delegatedConstructor != null) {
            emit(" : ${delegatedConstructor!!.kind.name.toLowerCase()}(")
            delegatedConstructor!!.emitValueArguments()
            emit(")")
        }
        body?.let { body ->
            emitSpace()
            bracedBlock {
                body.emit()
            }
        } ?: emitLine()
    }

   /* override fun visitProperty(property: AstProperty) {
        property.emitAnnotations()
        if (property.visibility != AstVisibility.LOCAL) property.emitVisibility()
        property.emitExpectActual()
        if (property.parent is AstClass) property.emitModality()
        if (property.overriddenDeclarations.isNotEmpty()) {
            emit("override ")
        }
        if (property.isInline) {
            emit("inline ")
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
        emit(property.uniqueName())
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
    }*/

    override fun visitTypeParameter(typeParameter: AstTypeParameter) {
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
                typeParameter.bounds
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

    /*override fun visitValueParameter(valueParameter: AstValueParameter) {
        valueParameter.emitAnnotations()
        if (valueParameter.isVararg) {
            emit("vararg ")
        }
        valueParameter.inlineHint?.let {
            emit("${it.name.toLowerCase()} ")
        }
        emit("${valueParameter.name}: ")
        if (valueParameter.isVararg) {
            valueParameter.type.arguments.single()
                .let { it as AstTypeProjectionImpl }
                .type
                .emit()
        } else {
            valueParameter.type.emit()
        }
        if (valueParameter.defaultValue != null) {
            emit(" = ")
            valueParameter.defaultValue!!.emit()
        }
    }

    override fun visitTypeAlias(typeAlias: AstTypeAlias) {
        typeAlias.emitAnnotations()
        emit("typealias ")
        emit("${typeAlias.name}")
        typeAlias.typeParameters.emitList()
        emit(" = ")
        typeAlias.type.emit()
    }

    override fun <T> visitConst(const: AstConst<T>) {
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

    override fun visitVararg(vararg: AstVararg) {
        vararg.elements.forEachIndexed { index, element ->
            element.emit()
            if (index != vararg.elements.lastIndex) emit(", ")
        }
    }

    override fun visitSpreadElement(spreadElement: AstSpreadElement) {
        emit("*")
        spreadElement.expression.emit()
    }

    override fun visitBlock(block: AstBlock) {
        block.statements.forEach { statement ->
            statement.emit()
            emitLine()
        }
    }

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) {
        val explicitReceiver = if (qualifiedAccess.extensionReceiver != null)
            qualifiedAccess.extensionReceiver
        else qualifiedAccess.dispatchReceiver
            ?.takeIf { it !is AstThis } // todo

        if (explicitReceiver != null) {
            explicitReceiver.emit()
            emit(".")
        }

        val callee = qualifiedAccess.callee
        if (callee is AstFunction && callee.kind == AstFunction.Kind.CONSTRUCTOR) {
            emit(callee.returnType.classOrFail.name)
        } else if (callee is AstDeclarationWithName) {
            if (callee is AstProperty && callee.visibility == AstVisibility.LOCAL) {
                emit(callee.uniqueName())
            } else if ((callee is AstProperty || callee is AstFunction)
                && explicitReceiver == null
            ) {
                emit(callee.fqName)
            } else {
                emit(callee.name)
            }
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
            val hasAllValueArguments = callee.valueParameters.size ==
                    qualifiedAccess.valueArguments.size &&
                    qualifiedAccess.valueArguments.none { it == null }
            qualifiedAccess.valueArguments.forEachIndexed { index, valueArgument ->
                if (valueArgument != null) {
                    if (!hasAllValueArguments)
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

    override fun visitAnonymousObjectExpression(
        expression: AstAnonymousObjectExpression,
        data: Nothing?
    ) {
        expression.anonymousObject.emit()
    }

    override fun visitComparisonOperation(
        comparisonOperation: AstComparisonOperation,
        data: Nothing?
    ) {
        comparisonOperation.left.emit()
        when (comparisonOperation.kind) {
            AstComparisonOperation.Kind.LESS_THAN -> emit(" < ")
            AstComparisonOperation.Kind.GREATER_THAN -> emit(" > ")
            AstComparisonOperation.Kind.LESS_THEN_EQUALS -> emit(" <= ")
            AstComparisonOperation.Kind.GREATER_THEN_EQUALS -> emit(" >= ")
        }.let {}
        comparisonOperation.right.emit()
    }

    override fun visitEqualityOperation(equalityOperation: AstEqualityOperation) {
        equalityOperation.left.emit()
        when (equalityOperation.kind) {
            AstEqualityOperation.Kind.EQUALS -> emit(" == ")
            AstEqualityOperation.Kind.NOT_EQUALS -> emit(" != ")
            AstEqualityOperation.Kind.IDENTITY -> emit(" === ")
            AstEqualityOperation.Kind.NOT_IDENTITY -> emit(" !== ")
        }.let {}
        equalityOperation.right.emit()
    }

    override fun visitLogicOperation(logicOperation: AstLogicOperation) {
        logicOperation.left.emit()
        when (logicOperation.kind) {
            AstLogicOperation.Kind.AND -> emit(" && ")
            AstLogicOperation.Kind.OR -> emit(" || ")
        }.let {}
        logicOperation.right.emit()
    }

    override fun visitWhen(astWhen: AstWhen) {
        emitLine("when {")
        indented {
            astWhen.branches.forEach { branch ->
                when (branch) {
                    is AstConditionBranch -> {
                        branch.condition.emit()
                        emitLine(" -> {")
                    }
                    is AstElseBranch -> {
                        emitLine("else -> {")
                    }
                    else -> error("Unexpected branch $branch")
                }.let {}
                indented {
                    branch.result.emit()
                }
                emitLine("}")
            }
        }
        emitLine("}")
    }

    override fun visitWhileLoop(whileLoop: AstWhileLoop) {
        when (whileLoop.kind) {
            AstWhileLoop.Kind.WHILE -> {
                emit("while (")
                whileLoop.condition.emit()
                emitLine(") {")
                indented {
                    whileLoop.body?.emit()
                }
                emitLine("}")
            }
            AstWhileLoop.Kind.DO_WHILE -> {
                emitLine("do {")
                indented {
                    whileLoop.body?.emit()
                }
                emit("} while (")
                whileLoop.condition.emit()
                emitLine(")")
            }
        }.let {}
    }

    override fun visitForLoop(forLoop: AstForLoop) {
        emit("for (")
        forLoop.loopParameter.emit()
        emit(" in ")
        forLoop.loopRange.emit()
        emitLine(") {")
        indented {
            forLoop.body?.emit()
        }
        emitLine("}")
    }

    override fun visitTry(astTry: AstTry) {
        emitLine("try {")
        indented {
            astTry.tryResult.emit()
        }
        emitLine()
        if (astTry.catches.isNotEmpty()) {
            astTry.catches.forEach {
                emit("} catch (")
                it.catchParameter.emit()
                emitLine(") {")
                indented {
                    it.result.emit()
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

    override fun visitReturn(astReturn: AstReturn) {
        emit("return")
        /*astReturn.target?.let {
            emit("@$")
        } ?:*/ emitSpace()
        astReturn.expression.emit()
    }

    override fun visitThrow(astThrow: AstThrow) {
        emit("throw ")
        astThrow.expression.emit()
    }
     */

    private fun AstMemberDeclaration.emitVisibility(emitSpace: Boolean = true) {
        emit(visibility.name.toLowerCase())
        if (emitSpace) emitSpace()
    }

    private fun AstMemberDeclaration.emitPlatformStatus(emitSpace: Boolean = true) {
        if (platformStatus != PlatformStatus.DEFAULT) {
            emit(platformStatus.name.toLowerCase())
            if (emitSpace) emitSpace()
        }
    }

    private fun AstMemberDeclaration.emitModality(emitSpace: Boolean = true) {
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
        annotations.forEach { annotation ->
            emit("@")
            annotation.emit()
            emitLine()
        }
    }

    private fun AstCall.emitValueArguments() {
        val useNamedArguments = valueArguments.any { it == null }
        valueArguments
            .mapIndexed { index, expression -> index to expression }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .forEach { (index, valueArgument) ->
                val valueParameter = callee.owner.valueParameters[index]
                if (useNamedArguments) {
                    emit("${valueParameter.name} = ")
                }
                valueArgument.emit()
                if (index != valueArguments.lastIndex) emit(", ")
            }
    }

    private fun AstType.emit() {
        emitAnnotations()
        check(this is AstSimpleType)
        when (val classifier = classifier) {
            is AstRegularClassSymbol -> emit(classifier.classId.asSingleFqName())
            is AstTypeParameterSymbol -> emit(classifier.owner.name)
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
        if (isMarkedNullable) emit("?")
    }

    private fun AstTypeProjection.emit() {
        when (this) {
            is AstStarProjection -> emit("*")
            is AstTypeProjectionWithVariance -> {
                if (variance != Variance.INVARIANT) emit("${variance.name.toLowerCase()} ")
                type.emit()
            }
            else -> error("Unexpected type argument $this")
        }
    }

}
