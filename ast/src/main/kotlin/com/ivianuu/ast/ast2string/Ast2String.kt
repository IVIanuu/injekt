package com.ivianuu.ast.ast2string

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstIntrinsics
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationContainer
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstNamedDeclaration
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.regularClassOrFail
import com.ivianuu.ast.declarations.regularClassOrNull
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.symbols.fqName
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
        check(value !is Unit)
        printer.print(value)
    }

    private fun emitLine(value: Any?) {
        check(value !is Unit)
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

    private val uniqueNameByElement =
        mutableMapOf<AstElement, String>()
    private val existingNames = mutableSetOf<String>()
    private fun AstElement.uniqueName(): String {
        return uniqueNameByElement.getOrPut(this) {
            if (this is AstNamedDeclaration && !name.isSpecial) return@getOrPut name.asString()
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
    }

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

    override fun visitNamedFunction(namedFunction: AstNamedFunction) = with(namedFunction) {
        emitAnnotations()
        if (visibility != Visibilities.Local) emitVisibility()
        emitPlatformStatus()
        if (symbol.callableId.className != null) emitModality()
        // todo if (overriddenDeclarations.isNotEmpty()) { emit("override ") }
        if (isInline) {
            emit("inline ")
        }
        if (isExternal) {
            emit("external ")
        }
        if (isInfix) {
            emit("infix ")
        }
        if (isOperator) {
            emit("operator ")
        }
        if (isTailrec) {
            emit("tailrec ")
        }

        if (isSuspend) {
            emit("suspend ")
        }

        emit("fun ")
        if (typeParameters.isNotEmpty()) {
            typeParameters.emitList()
            emitSpace()
        }
        emit(name)

        emit("(")
        valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != valueParameters.lastIndex) emit(", ")
        }
        emit(")")

        emit(": ")
        returnType.emit()
        emitSpace()
        if (typeParameters.isNotEmpty()) {
            typeParameters.emitWhere()
            emitSpace()
        }

        body?.let { body ->
            bracedBlock {
                body.emit()
            }
        } ?: emitLine()
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

    override fun visitProperty(property: AstProperty) = with(property) {
        emitAnnotations()
        if (!isLocal) emitVisibility()
        emitPlatformStatus()
        if (symbol.callableId.className != null) emitModality()
        // todo if (overriddenDeclarations.isNotEmpty()) { emit("override ") }
        if (isInline) {
            emit("inline ")
        }
        if (isVar) {
            emit("var ")
        } else {
            emit("val ")
        }
        if (typeParameters.isNotEmpty()) {
            typeParameters.emitList()
            emitSpace()
        }
        emit(uniqueName())
        emit(": ")
        returnType.emit()
        if (typeParameters.isNotEmpty()) {
            emitSpace()
            typeParameters.emitWhere()
        }
        if (initializer != null) {
            emit(" = ")
            initializer!!.emit()
        }
        if (delegate != null) {
            emit(" by ")
            delegate!!.emit()
        }
        if (getter != null) {
            emitLine()
            indented {
                getter!!.emit()
            }
        }
        if (setter != null) {
            emitLine()
            indented {
                setter!!.emit()
            }
        }
        emitLine()
    }

    override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) = with(propertyAccessor) {
        propertyAccessor.emitVisibility()
        if (propertyAccessor.isSetter) {
            emit("set")
        } else {
            emit("get")
        }
        emit("(")
        propertyAccessor.valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != valueParameters.lastIndex) emit(", ")
        }
        emit(")")
        body?.let { body ->
            emitSpace()
            bracedBlock {
                body.emit()
            }
        } ?: emitLine()
    }

    /*
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

    override fun visitValueParameter(valueParameter: AstValueParameter) = with(valueParameter) {
        emitAnnotations()
        if (valueParameter.isVararg) {
            emit("vararg ")
        }
        if (valueParameter.isNoinline) {
            emit("noinline ")
        }
        if (valueParameter.isCrossinline) {
            emit("crossinline ")
        }
        emit("${valueParameter.name}: ")
        if (valueParameter.isVararg) {
            (returnType as AstSimpleType).arguments.single()
                .let { it as AstTypeProjectionWithVariance }
                .type
                .emit()
        } else {
            valueParameter.returnType.emit()
        }
        if (valueParameter.defaultValue != null) {
            emit(" = ")
            valueParameter.defaultValue!!.emit()
        }
    }

    override fun visitTypeAlias(typeAlias: AstTypeAlias) = with(typeAlias) {
        emitAnnotations()
        emit("typealias ")
        emit(name)
        typeParameters.emitList()
        emit(" = ")
        expandedType.emit()
    }

    override fun <T> visitConst(const: AstConst<T>) = with(const) {
        emit(
            when (kind) {
                AstConstKind.Null -> "null"
                AstConstKind.Boolean -> value.toString()
                AstConstKind.Char -> "'${value}'"
                AstConstKind.Byte -> value.toString()
                AstConstKind.Short -> value.toString()
                AstConstKind.Int -> value.toString()
                AstConstKind.Long -> "${value}L"
                AstConstKind.String -> "\"${value}\""
                AstConstKind.Float -> "${value}f"
                AstConstKind.Double -> value.toString()
                AstConstKind.UnsignedByte -> "${value}u"
                AstConstKind.UnsignedShort -> "${value}u"
                AstConstKind.UnsignedInt -> "${value}u"
                AstConstKind.UnsignedLong -> "${value}uL"
                AstConstKind.IntegerLiteral -> TODO()
                AstConstKind.UnsignedIntegerLiteral -> TODO()
            }
        )
    }

    override fun visitVararg(vararg: AstVararg) = with(vararg) {
        elements.forEachIndexed { index, element ->
            element.emit()
            if (index != elements.lastIndex) emit(", ")
        }
    }

    override fun visitSpreadElement(spreadElement: AstSpreadElement) = with(spreadElement) {
        emit("*")
        expression.emit()
    }

    override fun visitBlock(block: AstBlock) = with(block) {
        statements.forEach { statement ->
            statement.emit()
            emitLine()
        }
    }

    override fun visitFunctionCall(functionCall: AstFunctionCall) = with(functionCall) {
        val callee = callee.owner

        when (callee.symbol.callableId) {
            AstIntrinsics.LessThan -> {
                valueArguments[0]!!.emit()
                emit(" < ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.GreaterThan -> {
                valueArguments[0]!!.emit()
                emit(" > ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.LessThanEqual -> {
                valueArguments[0]!!.emit()
                emit(" <= ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.GreaterThanEqual -> {
                valueArguments[0]!!.emit()
                emit(" >= ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.StructuralEqual -> {
                valueArguments[0]!!.emit()
                emit(" == ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.StructuralNotEqual -> {
                valueArguments[0]!!.emit()
                emit(" != ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.IdentityEqual -> {
                valueArguments[0]!!.emit()
                emit(" === ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.IdentityNotEqual -> {
                valueArguments[0]!!.emit()
                emit(" !== ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.LazyAnd -> {
                valueArguments[0]!!.emit()
                emit(" && ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.LazyOr -> {
                valueArguments[0]!!.emit()
                emit(" || ")
                valueArguments[1]!!.emit()
                return@with
            }
            AstIntrinsics.IsType -> {
                valueArguments.single()!!.emit()
                emit(" is ")
                type.emit()
                return@with
            }
            AstIntrinsics.IsNotType -> {
                valueArguments.single()!!.emit()
                emit(" !is ")
                type.emit()
                return@with
            }
            AstIntrinsics.AsType -> {
                valueArguments.single()!!.emit()
                emit(" as ")
                type.emit()
                return@with
            }
            AstIntrinsics.SafeAsType -> {
                valueArguments.single()!!.emit()
                emit(" as? ")
                type.emit()
                return@with
            }
        }


        val explicitReceiver = getExplicitReceiver()

        if (explicitReceiver != null) {
            explicitReceiver.emit()
            emit(".")
        }

        if (callee is AstConstructor) {
            emit(callee.returnType.regularClassOrFail.owner.name)
        } else if (callee is AstNamedFunction && callee.dispatchReceiverType == null) {
            emit(callee.symbol.callableId.fqName)
        } else if (callee is AstNamedFunction) {
            emit(callee.name)
        } else {
            error("Wtf $callee")
        }
        if (typeArguments.isNotEmpty()) {
            emit("<")
            typeArguments.forEachIndexed { index, typeArgument ->
                typeArgument.emit()
                if (index != typeArguments.lastIndex) emit(", ")
            }
            emit(">")
        }
        emit("(")
        emitValueArguments()
        emit(")")
    }

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) = with(qualifiedAccess) {
        val explicitReceiver = getExplicitReceiver()

        if (explicitReceiver != null) {
            explicitReceiver.emit()
            emit(".")
        }

        val callee = callee.owner
        if (callee is AstNamedDeclaration) {
            if (callee is AstProperty && callee.visibility == Visibilities.Local) {
                emit(callee.uniqueName())
            } else if (callee is AstProperty && callee.dispatchReceiverType == null) {
                emit(callee.symbol.callableId.fqName)
            } else {
                emit(callee.name)
            }
        } else {
            error("Wtf $callee")
        }
    }

    override fun visitVariableAssignment(variableAssignment: AstVariableAssignment) = with(variableAssignment) {
        val explicitReceiver = getExplicitReceiver()

        if (explicitReceiver != null) {
            explicitReceiver.emit()
            emit(".")
        }

        val callee = callee.owner
        if (callee is AstProperty && callee.visibility == Visibilities.Local) {
            emit(callee.uniqueName())
        } else if (callee is AstProperty && callee.dispatchReceiverType == null) {
            emit(callee.symbol.callableId.fqName)
        } else {
            emit(callee.name)
        }

        emit(" = ")
        value.emit()
    }

    /*

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
    }*/

    override fun visitReturn(returnExpression: AstReturn) = with(returnExpression) {
        emit("return ")
        // todo label
        result.emit()
    }

    /*

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

    private fun AstBaseQualifiedAccess.getExplicitReceiver(): AstExpression? =
        if (extensionReceiver != null)
            extensionReceiver
        else dispatchReceiver
            ?.takeIf { it !is AstThisReference } // todo

}
