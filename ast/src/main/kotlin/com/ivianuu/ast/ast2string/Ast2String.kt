package com.ivianuu.ast.ast2string

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstIntrinsics
import com.ivianuu.ast.AstLoopTarget
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
import com.ivianuu.ast.declarations.fqName
import com.ivianuu.ast.declarations.regularClassOrFail
import com.ivianuu.ast.declarations.regularClassOrNull
import com.ivianuu.ast.deepcopy.deepCopy
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
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.buildConstBoolean
import com.ivianuu.ast.expressions.buildTemporaryVariable
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildBreak
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildWhen
import com.ivianuu.ast.expressions.builder.buildWhenBranch
import com.ivianuu.ast.printing.AstPrintingVisitor
import com.ivianuu.ast.printing.formatPrintedString
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.visitors.AstTransformerVoid
import com.ivianuu.ast.visitors.AstVisitorVoid
import com.ivianuu.ast.visitors.CompositeTransformResult
import com.ivianuu.ast.visitors.compose
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance

fun AstElement.toKotlinSourceString(): String {
    val finalElement = /*accept(TransformAstBlockExpressionsTransformer(), null).single
        .accept(InlineBlocksTransformer(), null).single*/ this
    return buildString {
        try {
            finalElement.accept(Ast2KotlinSourceWriter(this), null)
        } catch (e: Throwable) {
            throw RuntimeException(toString().formatPrintedString(), e)
        }
    }.formatPrintedString()
}

private class Ast2KotlinSourceWriter(out: Appendable) : AstPrintingVisitor(out) {

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

    private var inStatementContainer = false
    inline fun <R> withInStatementContainer(
        canDeclareStatements: Boolean,
        block: () -> R
    ): R {
        val prev = canDeclareStatements
        this.inStatementContainer = canDeclareStatements
        val result = block()
        this.inStatementContainer = prev
        return result
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
        emit(uniqueName())

        if (typeParameters.isNotEmpty()) {
            typeParameters.emitList()
            emitSpace()
        }

        val primaryConstructor = declarations
            .filterIsInstance<AstConstructor>()
            .singleOrNull { it.isPrimary }

        primaryConstructor?.valueParameters?.emit()

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
    }

    private fun AstClass<*>.renderSuperTypes(appendSpace: Boolean = true) {
        val superTypesToRender = superTypes
            .filterNot {
                it == context.builtIns.anyType ||
                        it == context.builtIns.annotationType ||
                        it.classifier == context.builtIns.enumType.classifier
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
                    ?.takeIf {
                        it.callee.owner.returnType.classifier == superType.classifier
                    }
                    ?.emitValueArguments()

                delegateInitializers.singleOrNull {
                    it.delegatedSuperType == superType
                }
                    ?.let {
                        emit(" by ")
                        it.expression.emitWithInStatementContainer(false)
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

        emit(uniqueName())

        valueParameters.emit()

        emit(": ")
        returnType.emit()
        emitSpace()
        if (typeParameters.isNotEmpty()) {
            typeParameters.emitWhere()
            emitSpace()
        }

        body?.let { body ->
            bracedBlock {
                body.emitWithInStatementContainer(true)
            }
        }
    }

    override fun visitConstructor(constructor: AstConstructor): Unit = with(constructor) {
        emitAnnotations()
        emit(visibility.name.toLowerCase())
        emitSpace()
        emit("constructor")
        valueParameters.emit()
        if (delegatedConstructor != null) {
            emit(" : ${delegatedConstructor!!.kind.name.toLowerCase()}")
            delegatedConstructor!!.emitValueArguments()
        }
        body?.let { body ->
            emitSpace()
            bracedBlock {
                body.emitWithInStatementContainer(true)
            }
        }
    }

    override fun visitProperty(property: AstProperty) = with(property) {
       emit(skipValVar = false)
    }

    private fun AstProperty.emit(skipValVar: Boolean) {
        emitAnnotations()
        if (!isLocal) emitVisibility()
        emitPlatformStatus()
        if (dispatchReceiverType != null) emitModality()
        if (overriddenProperties.isNotEmpty()) emit("override ")
        if (isInline) {
            emit("inline ")
        }
        if (!skipValVar) {
            if (isVar) {
                emit("var ")
            } else {
                emit("val ")
            }
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
            initializer!!.emitWithInStatementContainer(false)
        }
        if (delegate != null) {
            emit(" by ")
            delegate!!.emitWithInStatementContainer(false)
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
    }

    override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor): Unit = with(propertyAccessor) {
        emitVisibility()
        if (propertyAccessor.isSetter) {
            emit("set")
        } else {
            emit("get")
        }
        valueParameters.emit(skipBracesIfEmpty = body == null)
        body?.let { body ->
            emitSpace()
            bracedBlock {
                body.emitWithInStatementContainer(true)
            }
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) = with(anonymousInitializer) {
        bracedBlock(header = "init") {
            body.emitWithInStatementContainer(true)
        }
    }

    override fun visitEnumEntry(enumEntry: AstEnumEntry) = with(enumEntry) {
        emitAnnotations()
        emit(name)
        enumEntry.initializer?.emitValueArguments()
        emitSpace()
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
        if (isVararg) {
            emit("vararg ")
        }
        if (correspondingProperty != null) {
            correspondingProperty!!.owner.emitVisibility()
            if (correspondingProperty!!.owner.isVar) {
                emit("var ")
            } else {
                emit("val ")
            }
        }
        if (isNoinline) {
            emit("noinline ")
        }
        if (isCrossinline) {
            emit("crossinline ")
        }
        emit("${name}: ")
        (if (isVararg) varargElementType ?: returnType else returnType).emit()
        if (defaultValue != null) {
            emit(" = ")
            defaultValue!!.emitWithInStatementContainer(false)
        }
    }

    private fun List<AstValueParameter>.emit(skipBracesIfEmpty: Boolean = false) {
        if (isNotEmpty() || !skipBracesIfEmpty) emit("(")
        forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != lastIndex) emit(", ")
        }
        if (isNotEmpty() || !skipBracesIfEmpty) emit(")")
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
            element.emitWithInStatementContainer(false)
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
        bracedBlock("run ${allocateUniqueName()}@") {
            withInStatementContainer(true, block)
        }
    }

    override fun visitBlock(block: AstBlock) = with(block) {
        fun emitStatements() {
            statements.forEach { statement ->
                statement.emit()
                emitLine()
            }
        }
        if (inStatementContainer || statements.size < 2) {
            emitStatements()
        } else {
            runBlock {
                emitStatements()
                if (type == type.context.builtIns.unitType) {
                    emitLine("Unit")
                }
            }
        }
    }

    override fun visitFunctionCall(functionCall: AstFunctionCall) = with(functionCall) {
        val callee = callee.owner

        fun emitBinaryExpression(operatorToken: String) {
            emit("(")
            valueArguments[0]!!.emitWithInStatementContainer(false)
            emit(" $operatorToken ")
            valueArguments[1]!!.emitWithInStatementContainer(false)
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
            emitValueArguments()
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
        value.emitWithInStatementContainer(false)
    }

    override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction) = with(anonymousFunction) {
        emit("${uniqueLabelName()}@ { ")
        anonymousFunction.valueParameters.forEachIndexed { index, valueParameter ->
            valueParameter.emit()
            if (index != anonymousFunction.valueParameters.lastIndex) emit(", ")
        }
        emitLine(" ->")
        indented { anonymousFunction.body!!.emitWithInStatementContainer(true) }
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
        argument.emitWithInStatementContainer(false)
        emit(" ${operator.keyword} ")
        typeOperand.emit()
        emit(")")
    }

    override fun visitWhen(whenExpression: AstWhen) = with(whenExpression) {
        branches.forEachIndexed { index, branch ->
            val condition = branch.condition
            if (index == branches.lastIndex && condition is AstConst<*> && condition.value == true) {
                emitLine("else {")
            } else {
                if (index != 0) emit("else ")
                emit("if (")
                condition.emitWithInStatementContainer(false)
                emitLine(") {")
            }
            indented {
                branch.result.emitWithInStatementContainer(true)
                emitLine()
            }
            emit("}")
            if (index != branches.lastIndex) emitSpace()
        }
    }

    override fun visitWhileLoop(whileLoop: AstWhileLoop) = with(whileLoop) {
        emit("${uniqueLabelName()}@ while (")
        condition.emitWithInStatementContainer(false)
        emitLine(") {")
        indented { body.emitWithInStatementContainer(true) }
        emitLine("}")
    }

    override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop) = with(doWhileLoop) {
        emitLine("${uniqueLabelName()}@ do {")
        indented { body.emitWithInStatementContainer(true) }
        emit("} while (")
        condition.emitWithInStatementContainer(false)
        emitLine(")")
    }

    override fun visitForLoop(forLoop: AstForLoop) = with(forLoop) {
        emit("${uniqueLabelName()}@ for (")
        loopParameter.emit(skipValVar = true)
        emit(" in ")
        loopRange.emitWithInStatementContainer(false)
        emitLine(") {")
        indented { body.emitWithInStatementContainer(true) }
        emitLine("}")
    }

    override fun visitTry(tryExpression: AstTry) = with(tryExpression) {
        emitLine("try {")
        indented { tryBody.emitWithInStatementContainer(true) }
        emitLine()
        if (catches.isNotEmpty()) {
            catches.forEach {
                emit("} catch (")
                it.parameter.emit(skipValVar = true)
                emitLine(") {")
                indented { it.body.emitWithInStatementContainer(true) }
                emitLine()
            }
        }
        finallyBody?.let {
            emitLine("} finally {")
            indented { it.emitWithInStatementContainer(true) }
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
        emit("<")
        superReference.superType.emit()
        emit(">")
    }

    override fun visitClassReference(classReference: AstClassReference) = with(classReference) {
        classifier.emit()
        emit("::class")
    }

    override fun visitCallableReference(callableReference: AstCallableReference) = with(callableReference) {
        val callee = callee.owner
        withReceivers(
            dispatchReceiverType = callee.dispatchReceiverType,
            extensionReceiverType = callee.extensionReceiverType?.takeIf { extensionReceiver != null },
            dispatchReceiverArgument = dispatchReceiver ?: callee.dispatchReceiverType,
            extensionReceiverArgument = extensionReceiver ?: callee.extensionReceiverType,
            callToken = "::"
        ) {
            when (callee) {
                is AstNamedDeclaration -> callee.emitCallableName()
                is AstConstructor -> callee.returnType.regularClassOrFail.fqName
            }
        }
    }

    override fun visitType(type: AstType) = with(type) { emit() }

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

    override fun visitTypeProjection(typeProjection: AstTypeProjection) = with(typeProjection) {
        emit()
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
        emit("(")
        val useNamedArguments = valueArguments.any { it == null }
        valueArguments
            .mapIndexed { index, expression -> index to expression }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .forEach { (index, valueArgument) ->
                if (index == valueArguments.lastIndex &&
                        valueArgument is AstAnonymousFunction) return@forEach
                val valueParameter = callee.owner.valueParameters[index]
                if (useNamedArguments) {
                    emit("${valueParameter.name} = ")
                }
                valueArgument.emitWithInStatementContainer(false)
                if (index != valueArguments.lastIndex) emit(", ")
            }
        emit(")")
        (valueArguments.lastOrNull() as? AstAnonymousFunction)
            ?.let {
                emitSpace()
                it.emit()
            }
    }

    private fun AstClassifierSymbol<*>.emit() {
        when (this) {
            is AstRegularClassSymbol -> if (owner.visibility == Visibilities.Local)
                emit(owner.name) else emit(owner.fqName)
            is AstTypeParameterSymbol -> emit(owner.name)
            else -> error("Unexpected classifier $this")
        }
    }

    private fun withReceivers(
        dispatchReceiverType: AstType?,
        extensionReceiverType: AstType?,
        dispatchReceiverArgument: AstElement?,
        extensionReceiverArgument: AstElement?,
        callToken: String,
        block: () -> Unit
    ) {
        when {
            dispatchReceiverType == null && extensionReceiverType == null -> block()
            dispatchReceiverType != null && extensionReceiverType == null-> {
                dispatchReceiverArgument!!.emitWithInStatementContainer(false)
                emit(callToken)
                block()
            }
            dispatchReceiverType == null && extensionReceiverType != null -> {
                emit("(")
                extensionReceiverArgument!!.emitWithInStatementContainer(false)
                emit(" as ")
                extensionReceiverType.emit()
                emit(")")
                emit(callToken)
                block()
            }
            dispatchReceiverType != null && extensionReceiverType != null -> {
                emit("(with(")
                dispatchReceiverArgument!!.emitWithInStatementContainer(false)
                emit(") { ")
                emit("(")
                extensionReceiverArgument!!.emitWithInStatementContainer(false)
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
            else -> emit(uniqueName())
        }
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

    private fun AstElement.emitWithInStatementContainer(inStatementContainer: Boolean) {
        withInStatementContainer(inStatementContainer) {
            emit()
        }
    }

}
