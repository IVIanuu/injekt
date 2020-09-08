package com.ivianuu.ast.ast2string

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstIntrinsics
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstClass
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
import com.ivianuu.ast.expressions.AstBreak
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstContinue
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstForLoop
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.visitors.AstVisitorVoid
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
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
            if (this is AstNamedDeclaration && !name.isSpecial) name.asString()
            else allocateUniqueName()
        }
    }

    private val uniqueLabelNameByTarget = mutableMapOf<AstTargetElement, String>()
    private fun AstTargetElement.uniqueLabelName(): String {
        return if (this is AstNamedDeclaration) name.asString()
        else uniqueLabelNameByTarget.getOrPut(this) { allocateUniqueName() }
    }

    private fun allocateUniqueName(): String {
        val finalBase = "tmp"
        var name = finalBase
        var differentiator = 2
        while (name in existingNames) {
            name = finalBase + differentiator
            differentiator++
        }
        existingNames += name
        return name
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
                    type.regularClassOrNull
                        ?.takeUnless { it.owner.name.isSpecial }
                        ?.takeUnless { it.owner.visibility == Visibilities.Local }
                        ?.let { imports += it.fqName }
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
        if (visibility != Visibilities.Local) emitVisibility()
        emitPlatformStatus()
        if (classKind != ClassKind.ANNOTATION_CLASS &&
            (visibility != Visibilities.Local || modality == Modality.ABSTRACT)) emitModality()
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

        renderSuperTypes()
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
                    if (index != enumEntryDeclarations.lastIndex) emitLine(",")
                }

                if (classKind == ClassKind.ENUM_CLASS) emitLine(";")

                otherDeclarations
                    .filter { declaration ->
                        primaryConstructor?.valueParameters?.none {
                            it.correspondingProperty?.owner == declaration
                        } ?: true
                    }
                    .forEach { declaration ->
                        declaration.emit()
                        emitLine()
                    }
            }
        }
        emitLine()
    }

    private fun AstClass<*>.renderSuperTypes(appendSpace: Boolean = true) {
        val superTypesToRender = superTypes
            .filterNot {
                it == context.builtIns.anyType ||
                        it == context.builtIns.annotationType ||
                        it == context.builtIns.enumType
            }
        if (superTypesToRender.isEmpty()) return
        emit(": ")
        superTypesToRender
            .forEachIndexed { index, superType ->
                superType.emit()

                declarations
                    .filterIsInstance<AstConstructor>()
                    .singleOrNull { it.isPrimary }
                    ?.delegatedConstructor
                    ?.let {
                        emit("(")
                        it.emitValueArguments()
                        emit(")")
                    }

                delegateInitializers
                    .filter { it.delegatedSuperType == superType }
                    .singleOrNull()
                    ?.let {
                        emit(" by ")
                        it.expression.emit()
                    }

                if (index != superTypesToRender.lastIndex) emit(", ")
            }
        if (appendSpace) emitSpace()
    }

    override fun visitNamedFunction(namedFunction: AstNamedFunction): Unit = with(namedFunction) {
        emitAnnotations()
        if (visibility != Visibilities.Local) emitVisibility()
        emitPlatformStatus()
        if (dispatchReceiverType != null) emitModality()
        if (overriddenFunctions.isNotEmpty()) emit("override ")
        if (isInline) emit("inline ")
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

    override fun visitConstructor(constructor: AstConstructor): Unit = with(constructor) {
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
        if (dispatchReceiverType != null) emitModality()
        if (overriddenProperties.isNotEmpty()) emit("override ")
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
        if (extensionReceiverType != null) {
            extensionReceiverType!!.emit()
            emit(".")
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

    override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor): Unit = with(propertyAccessor) {
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
        bracedBlock(header = "init") { body.emit() }
    }

    override fun visitEnumEntry(enumEntry: AstEnumEntry) = with(enumEntry) {
        emitAnnotations()
        emit(name)
        bracedBlock {
            emitDeclarations()
        }
    }

    override fun visitTypeParameter(typeParameter: AstTypeParameter) {
        typeParameter.emit(null, false)
    }

    private fun List<AstTypeParameter>.emitList() {
        if (isNotEmpty()) {
            emit("<")
            forEachIndexed { index, typeParameter ->
                typeParameter.emit(
                    boundToRender = typeParameter.bounds.singleOrNull()
                        ?.takeIf {
                            !it.isMarkedNullable ||
                                    it.classifier != it.context.builtIns.anyType.classifier
                        },
                    forWhere = false
                )
                if (index != lastIndex) emit(", ")
            }
            emit(">")
        }
    }

    private fun List<AstTypeParameter>.emitWhere() {
        val typeParametersWithMultipleBounds = filter { it.bounds.size > 1 }
            .flatMap { typeParameter ->
                typeParameter.bounds
                    .map { typeParameter to it }
            }
        if (typeParametersWithMultipleBounds.isNotEmpty()) {
            emit("where ")
            typeParametersWithMultipleBounds.forEachIndexed { index, (typeParameter, superType) ->
                typeParameter.emit(superType, true)
                if (index != typeParametersWithMultipleBounds.lastIndex) emit(", ")
            }
        }
    }

    private fun AstTypeParameter.emit(
        boundToRender: AstType?,
        forWhere: Boolean
    ) {
        if (!forWhere) {
            if (isReified) { emit("reified ") }
            emitAnnotations()
        }
        emit("$name")
        if (boundToRender != null) {
            emit(" : ")
            boundToRender.emit()
        }
    }

    override fun visitValueParameter(valueParameter: AstValueParameter) = with(valueParameter) {
        emitAnnotations()
        if (correspondingProperty != null) {
            correspondingProperty!!.owner.emitVisibility()
            if (correspondingProperty!!.owner.isVar) {
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
        /*if (isVararg) {
            (returnType as AstSimpleType).arguments.single()
                .let { it as AstTypeProjectionWithVariance }
                .type
                .emit()
        } else {*/
        returnType.emit()
        //}
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

    private fun runBlock(
        block: () -> Unit
    ) {
        // we wrap the block in a run call to allow them to declare local variables and classes
        bracedBlock("run ${allocateUniqueName()}@", block)
    }

    override fun visitBlock(block: AstBlock) = with(block) {
        runBlock {
            statements.forEach { statement ->
                statement.emit()
                emitLine()
            }
            if (block.type == context.builtIns.unitType) {
                emitLine("Unit")
            }
        }
    }

    override fun visitFunctionCall(functionCall: AstFunctionCall) = with(functionCall) {
        val callee = callee.owner

        fun emitBinaryExpression(operatorToken: String) {
            emit("(")
            valueArguments[0]!!.emit()
            emit(" $operatorToken ")
            valueArguments[1]!!.emit()
            emit(")")
        }

        when (callee.symbol.fqName) {
            AstIntrinsics.LessThan -> {
                emitBinaryExpression("<")
                return@with
            }
            AstIntrinsics.GreaterThan -> {
                emitBinaryExpression(">")
                return@with
            }
            AstIntrinsics.LessThanEqual -> {
                emitBinaryExpression("<=")
                return@with
            }
            AstIntrinsics.GreaterThanEqual -> {
                emitBinaryExpression(">=")
                return@with
            }
            AstIntrinsics.StructuralEqual -> {
                emitBinaryExpression("==")
                return@with
            }
            AstIntrinsics.StructuralNotEqual -> {
                emitBinaryExpression("!=")
                return@with
            }
            AstIntrinsics.IdentityEqual -> {
                emitBinaryExpression("===")
                return@with
            }
            AstIntrinsics.IdentityNotEqual -> {
                emitBinaryExpression("!==")
                return@with
            }
            AstIntrinsics.LazyAnd -> {
                emitBinaryExpression("&&")
                return@with
            }
            AstIntrinsics.LazyOr -> {
                emitBinaryExpression("||")
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
                if (callee.visibility == Visibilities.Local) {
                    emit(callee.returnType.regularClassOrFail.owner.name)
                } else {
                    emit(callee.returnType.regularClassOrFail.owner.symbol.fqName)
                }
                emitArguments()
            }
            is AstNamedFunction -> {
                withReceivers(
                    dispatchReceiverType = callee.dispatchReceiverType,
                    extensionReceiverType = callee.extensionReceiverType,
                    dispatchReceiverArgument = dispatchReceiver,
                    extensionReceiverArgument = extensionReceiver,
                    callToken = "."
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
            extensionReceiverArgument = extensionReceiver,
            callToken = "."
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
            extensionReceiverArgument = extensionReceiver,
            callToken = "."
        ) {
            callee.emitCallableName()
        }
        emit(" = ")
        value.emit()
    }

    override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction) = with(anonymousFunction) {
        emit("${uniqueLabelName()}@ { ")
        anonymousFunction.valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != anonymousFunction.valueParameters.lastIndex) emit(", ")
        }
        emitLine(" ->")
        indented { anonymousFunction.body!!.emit() }
        emitLine()
        emitLine("}")
    }

    override fun visitAnonymousObject(anonymousObject: AstAnonymousObject) = with(anonymousObject) {
        emit("object ")
        renderSuperTypes()
        bracedBlock {
            emitDeclarations()
        }
    }

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

    override fun visitWhileLoop(whileLoop: AstWhileLoop) = with(whileLoop) {
        runBlock {
            emit("${uniqueLabelName()}@ while (")
            condition.emit()
            emitLine(") {")
            indented { body.emit() }
            emitLine("}")
        }
    }

    override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop) = with(doWhileLoop) {
        runBlock {
            emitLine("${uniqueLabelName()}@ do {")
            indented { body.emit() }
            emit("} while (")
            condition.emit()
            emitLine(")")
        }
    }

    override fun visitForLoop(forLoop: AstForLoop) = with(forLoop) {
        emit("${uniqueLabelName()}@ for (")
        loopParameter.emit()
        emit(" in ")
        loopRange.emit()
        emitLine(") {")
        indented { body.emit() }
        emitLine("}")
    }

    override fun visitTry(tryExpression: AstTry) = with(tryExpression) {
        emitLine("try {")
        indented { tryBody.emit() }
        emitLine()
        if (catches.isNotEmpty()) {
            catches.forEach {
                emit("} catch (")
                it.parameter.emit()
                emitLine(") {")
                indented { it.body.emit() }
                emitLine()
            }
        }
        finallyBody?.let {
            emitLine("} finally {")
            indented { it.emit() }
            emitLine()
        }
        emitLine("}")
    }

    override fun visitBreak(breakExpression: AstBreak) = with(breakExpression) {
        emit("break@${target.labeledElement.uniqueLabelName()}")
    }

    override fun visitContinue(continueExpression: AstContinue) = with(continueExpression) {
        emit("continue@${target.labeledElement.uniqueLabelName()}")
    }

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

    override fun visitSuperReference(superReference: AstSuperReference) {
        emit("super")
        if (superReference.superType != null) {
            emit("<")
            superReference.superType!!.emit()
            emit(">")
        }
    }

    override fun visitClassReference(classReference: AstClassReference) = with(classReference) {
        classifier.emit()
        emit("::class")
    }

    override fun visitCallableReference(callableReference: AstCallableReference) = with(callableReference) {
        val callee = callee.owner
        withReceivers(
            dispatchReceiverType = callee.dispatchReceiverType,
            extensionReceiverType = callee.extensionReceiverType,
            dispatchReceiverArgument = dispatchReceiver,
            extensionReceiverArgument = extensionReceiver,
            callToken = "::"
        ) {
            (callee as AstNamedDeclaration).emitCallableName()
        }
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

    private fun AstDeclarationContainer.emitDeclarations(
        delimiter: String = "\n"
    ) {
        declarations
            .filter { it !is AstConstructor || !it.isPrimary }
            .forEachIndexed { index, declaration ->
                declaration.emit()
                if (index != declarations.lastIndex) emit(delimiter)
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

    private fun AstClassifierSymbol<*>.emit() {
        when (this) {
            is AstRegularClassSymbol -> emit(fqName)
            is AstTypeParameterSymbol -> emit(owner.name)
            else -> error("Unexpected classifier $this")
        }
    }

    private fun AstType.emit() {
        emitAnnotations()
        classifier.emit()
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
                if (variance != Variance.INVARIANT) emit("${variance.label} ")
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
        callToken: String,
        block: () -> Unit
    ) {
        when {
            dispatchReceiverType == null && extensionReceiverType == null -> block()
            dispatchReceiverType != null && extensionReceiverType == null-> {
                dispatchReceiverArgument!!.emit()
                emit(callToken)
                block()
            }
            dispatchReceiverType == null && extensionReceiverType != null -> {
                emit("(")
                extensionReceiverArgument!!.emit()
                emit(" as ")
                extensionReceiverType.emit()
                emit(")")
                emit(callToken)
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
                emit(callToken)
                block()
                emit(" })")
            }
        }
    }

    private fun AstNamedDeclaration.emitCallableName() {
        when {
            this is AstProperty && isLocal -> emit(uniqueName())
            this is AstCallableDeclaration<*> &&
                    this !is AstValueParameter &&
                    dispatchReceiverType == null &&
                    extensionReceiverType == null &&
                    (this !is AstNamedFunction ||
                            this.visibility != Visibilities.Local) -> emit(symbol.fqName)
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
