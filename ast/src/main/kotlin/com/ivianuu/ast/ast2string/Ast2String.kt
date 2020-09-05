package com.ivianuu.ast.ast2string

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstIntrinsics
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstCallableDeclaration
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
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhen
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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
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

    private val uniqueNameByElement = mutableMapOf<AstElement, String>()
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
        emitAnnotations(AnnotationUseSiteTarget.FILE)

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
        emit("$name")

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

        if (extensionReceiverType != null) {
            extensionReceiverType!!.emit()
            emit(".")
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
        emit(visibility.name.toLowerCase())
        emitSpace()
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

    override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) = with(anonymousInitializer) {
        bracedBlock(header = "init") {
            body!!.emit()
        }
    }

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

    override fun visitValueParameter(valueParameter: AstValueParameter) {
        valueParameter.emit()
    }

    private fun AstValueParameter.emit(property: AstProperty? = null) {
        emitAnnotations()
        if (property != null) {
            property.emitVisibility()
            if (property.isVar) {
                emit("var ")
            } else {
                emit("val ")
            }
        }
        if (isVararg) {
            emit("vararg ")
        }
        if (isNoinline) {
            emit("noinline ")
        }
        if (isCrossinline) {
            emit("crossinline ")
        }
        emit("${name}: ")
        if (isVararg) {
            (returnType as AstSimpleType).arguments.single()
                .let { it as AstTypeProjectionWithVariance }
                .type
                .emit()
        } else {
            returnType.emit()
        }
        if (defaultValue != null) {
            emit(" = ")
            defaultValue!!.emit()
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
                AstConstKind.UByte -> "${value}u"
                AstConstKind.UShort -> "${value}u"
                AstConstKind.UInt -> "${value}u"
                AstConstKind.ULong -> "${value}uL"
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
                emit("(")
                valueArguments[0]!!.emit()
                emit(" < ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.GreaterThan -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" > ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.LessThanEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" <= ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.GreaterThanEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" >= ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.StructuralEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" == ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.StructuralNotEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" != ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.IdentityEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" === ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.IdentityNotEqual -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" !== ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.LazyAnd -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" && ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
            AstIntrinsics.LazyOr -> {
                emit("(")
                valueArguments[0]!!.emit()
                emit(" || ")
                valueArguments[1]!!.emit()
                emit(")")
                return@with
            }
        }

        fun emitArguments() {
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

        when (callee) {
            is AstConstructor -> {
                emit(callee.returnType.regularClassOrFail.owner.name)
                emitArguments()
            }
            is AstNamedFunction -> {
                withReceivers(
                    dispatchReceiverType = callee.dispatchReceiverType,
                    extensionReceiverType = callee.extensionReceiverType,
                    dispatchReceiverArgument = dispatchReceiver,
                    extensionReceiverArgument = extensionReceiver
                ) {
                    callee.emitCallableName()
                    emitArguments()
                }
            }
            else -> error("Unexpected callee $callee")
        }
    }

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) = with(qualifiedAccess) {
        val callee = callee.owner
        callee as AstNamedDeclaration

        val (dispatchReceiverType, extensionReceiverType) = when (callee) {
            is AstCallableDeclaration<*> -> callee.dispatchReceiverType to callee.extensionReceiverType
            else -> null to null
        }

        withReceivers(
            dispatchReceiverType = dispatchReceiverType,
            extensionReceiverType = extensionReceiverType,
            dispatchReceiverArgument = dispatchReceiver,
            extensionReceiverArgument = extensionReceiver
        ) {
            callee.emitCallableName()
        }
    }

    override fun visitVariableAssignment(variableAssignment: AstVariableAssignment) = with(variableAssignment) {
        val callee = callee.owner

        withReceivers(
            dispatchReceiverType = callee.dispatchReceiverType,
            extensionReceiverType = callee.extensionReceiverType,
            dispatchReceiverArgument = dispatchReceiver,
            extensionReceiverArgument = extensionReceiver
        ) {
            callee.emitCallableName()
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
    }*/

    override fun visitTypeOperation(typeOperation: AstTypeOperation) = with(typeOperation) {
        emit("(")
        argument.emit()
        emit(" ${operator.keyword} ")
        typeOperand.emit()
        emit(")")
    }

    override fun visitWhen(whenExpression: AstWhen) = with(whenExpression) {
        emitLine("when {")
        indented {
            branches.forEach { branch ->
                val condition = branch.condition
                if (condition is AstConst<*> && condition.value == true) {
                    emit("else")
                } else {
                    condition.emit()
                }
                emitLine(" -> {")
                indented {
                    branch.result.emit()
                    emitLine()
                }
                emitLine("}")
            }
        }
        emitLine("}")
    }

    /*
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
        emit("return")
        val targetLabel = returnExpression.target.labelName
        if (targetLabel != null)
            emit("@$targetLabel")
        else emitSpace()
        result.emit()
    }

    override fun visitThrow(throwExpression: AstThrow) = with(throwExpression) {
        emit("throw ")
        exception.emit()
    }

    override fun visitThisReference(thisReference: AstThisReference) = with(thisReference) {
        emit("this")
        if (labelName != null) emit("@$labelName")
    }

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

    private fun AstAnnotationContainer.emitAnnotations(
        target: AnnotationUseSiteTarget? = null
    ) {
        annotations.forEach { annotation ->
            emit("@${target?.renderName?.let { "$it:" }.orEmpty()}")
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

    private fun withReceivers(
        dispatchReceiverType: AstType?,
        extensionReceiverType: AstType?,
        dispatchReceiverArgument: AstExpression?,
        extensionReceiverArgument: AstExpression?,
        block: () -> Unit
    ) {
        when {
            dispatchReceiverType == null && extensionReceiverType == null -> block()
            dispatchReceiverType != null && extensionReceiverType == null-> {
                dispatchReceiverArgument!!.emit()
                block()
            }
            dispatchReceiverType == null && extensionReceiverType != null -> {
                emit("(")
                extensionReceiverArgument!!.emit()
                emit(" as ")
                extensionReceiverType.emit()
                emit(")")
                emit(".")
                block()
            }
            dispatchReceiverType != null && extensionReceiverType != null -> {
                emit("(with(")
                dispatchReceiverArgument!!.emit()
                emit(") { ")
                emit("(")
                extensionReceiverArgument!!.emit()
                emit(" as ")
                extensionReceiverType.emit()
                emit(")")
                emit(".")
                block()
                emit(" })")
            }
        }
    }

    private fun AstNamedDeclaration.emitCallableName() {
        when {
            this is AstProperty && isLocal -> emit(uniqueName())
            this is AstCallableDeclaration<*> &&
                    dispatchReceiverType == null &&
                    extensionReceiverType == null -> emit(symbol.callableId.fqName)
            else -> emit(name)
        }
    }

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

}
