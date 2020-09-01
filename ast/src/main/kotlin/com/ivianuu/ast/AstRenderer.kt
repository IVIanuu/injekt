package com.ivianuu.ast

import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstCallableMemberDeclaration
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstImport
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstSimpleFunction
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.AstTypedDeclaration
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.isActual
import com.ivianuu.ast.declarations.isCompanion
import com.ivianuu.ast.declarations.isConst
import com.ivianuu.ast.declarations.isData
import com.ivianuu.ast.declarations.isExpect
import com.ivianuu.ast.declarations.isExternal
import com.ivianuu.ast.declarations.isInfix
import com.ivianuu.ast.declarations.isInline
import com.ivianuu.ast.declarations.isInner
import com.ivianuu.ast.declarations.isLateInit
import com.ivianuu.ast.declarations.isOperator
import com.ivianuu.ast.declarations.isOverride
import com.ivianuu.ast.declarations.isStatic
import com.ivianuu.ast.declarations.isSuspend
import com.ivianuu.ast.declarations.isTailRec
import com.ivianuu.ast.declarations.modality
import com.ivianuu.ast.declarations.visibility
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArrayOfCall
import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstAugmentedArraySetCall
import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstCheckNotNullCall
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstClassReferenceExpression
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstComponentCall
import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstExpressionWithSmartcast
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.AstLoopJump
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstQualifiedAccessExpression
import com.ivianuu.ast.expressions.AstResolvedQualifier
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.AstSafeCallExpression
import com.ivianuu.ast.expressions.AstSpreadArgumentExpression
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstStringConcatenationCall
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.expressions.AstTryExpression
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.expressions.AstVarargArgumentsExpression
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.AstWrappedDelegateExpression
import com.ivianuu.ast.expressions.argument
import com.ivianuu.ast.expressions.arguments
import com.ivianuu.ast.expressions.impl.AstElseIfTrueCondition
import com.ivianuu.ast.expressions.impl.AstExpressionStub
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.expressions.impl.AstStubStatement
import com.ivianuu.ast.expressions.impl.AstUnitExpression
import com.ivianuu.ast.references.AstBackingFieldReference
import com.ivianuu.ast.references.AstDelegateFieldReference
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstResolvedCallableReference
import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.references.AstSuperReference
import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.ConeClassLikeLookupTag
import com.ivianuu.ast.symbols.StandardClassIds
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstDynamicTypeRef
import com.ivianuu.ast.types.AstFunctionTypeRef
import com.ivianuu.ast.types.AstImplicitTypeRef
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.AstTypeRefWithNullability
import com.ivianuu.ast.types.AstUserTypeRef
import com.ivianuu.ast.types.ConeClassLikeType
import com.ivianuu.ast.types.ConeKotlinType
import com.ivianuu.ast.types.ConeLookupTagBasedType
import com.ivianuu.ast.types.dropExtensionFunctionAnnotation
import com.ivianuu.ast.types.isExtensionFunctionAnnotationCall
import com.ivianuu.ast.types.render
import com.ivianuu.ast.visitors.AstVisitorVoid
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun AstElement.renderWithType(mode: AstRenderer.RenderMode = AstRenderer.RenderMode.Normal): String =
    buildString {
        append(this@renderWithType)
        append(": ")
        this@renderWithType.accept(AstRenderer(this, mode))
    }

fun AstElement.render(mode: AstRenderer.RenderMode = AstRenderer.RenderMode.Normal): String =
    buildString { this@render.accept(AstRenderer(this, mode)) }

class AstRenderer(builder: StringBuilder, private val mode: RenderMode = RenderMode.Normal) :
    AstVisitorVoid() {
    companion object {
        private val visibilitiesToRenderEffectiveSet = setOf(
            Visibilities.Private, Visibilities.PrivateToThis, Visibilities.Internal,
            Visibilities.Protected, Visibilities.Public, Visibilities.Local
        )
    }

    abstract class RenderMode(
        val renderLambdaBodies: Boolean,
        val renderCallArguments: Boolean,
        val renderCallableFqNames: Boolean
    ) {
        object Normal : RenderMode(
            renderLambdaBodies = true,
            renderCallArguments = true,
            renderCallableFqNames = false
        )

        object WithFqNames : RenderMode(
            renderLambdaBodies = true,
            renderCallArguments = true,
            renderCallableFqNames = true
        )
    }

    private val printer = Printer(builder)

    private var lineBeginning = true

    private fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    private fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    private fun pushIndent() {
        printer.pushIndent()
    }

    private fun popIndent() {
        printer.popIndent()
    }

    fun newLine() {
        println()
    }

    override fun visitElement(element: AstElement) {
        element.acceptChildren(this)
    }

    override fun visitFile(file: AstFile) {
        println("FILE: ${file.name}")
        pushIndent()
        visitElement(file)
        popIndent()
    }

    private fun List<AstElement>.renderSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            element.accept(this@AstRenderer)
        }
    }

    private fun List<AstElement>.renderSeparatedWithNewlines() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(",")
                newLine()
            }
            element.accept(this@AstRenderer)
        }
    }

    private fun List<ConeKotlinType>.renderTypesSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            print(element.render())
        }
    }


    private fun List<AstValueParameter>.renderParameters() {
        print("(")
        renderSeparated()
        print(")")
    }

    private fun List<AstAnnotationCall>.renderAnnotations() {
        for (annotation in this) {
            visitAnnotationCall(annotation)
        }
    }

    private fun Variance.renderVariance() {
        label.let {
            print(it)
            if (it.isNotEmpty()) {
                print(" ")
            }
        }
    }

    override fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>) {
        if (callableDeclaration is AstMemberDeclaration) {
            visitMemberDeclaration(callableDeclaration)
        } else {
            callableDeclaration.annotations.renderAnnotations()
            visitTypedDeclaration(callableDeclaration)
        }
        val receiverType = callableDeclaration.receiverTypeRef
        print(" ")
        if (receiverType != null) {
            receiverType.accept(this)
            print(".")
        }
        when (callableDeclaration) {
            is AstSimpleFunction -> {
                if (!mode.renderCallableFqNames) {
                    print(callableDeclaration.name)
                } else {
                    print(callableDeclaration.symbol.callableId)
                }
            }
            is AstVariable<*> -> {
                if (!mode.renderCallableFqNames) {
                    print(callableDeclaration.name)
                } else {
                    print(callableDeclaration.symbol.callableId)
                }
            }
        }

        if (callableDeclaration is AstFunction<*>) {
            callableDeclaration.valueParameters.renderParameters()
        }
        print(": ")
        callableDeclaration.returnTypeRef.accept(this)
        callableDeclaration.renderContractDescription()
    }

    private fun AstDeclaration.renderContractDescription() {
        val contractDescription =
            (this as? AstContractDescriptionOwner)?.contractDescription ?: return
        pushIndent()
        contractDescription.accept(this@AstRenderer)
        popIndent()
    }

    private fun Visibility.asString() = when (this) {
        Visibilities.Unknown -> "public?"
        else -> toString()
    }

    private fun AstMemberDeclaration.modalityAsString(): String {
        return modality?.name?.toLowerCase() ?: run {
            if (this is AstCallableMemberDeclaration<*> && this.isOverride) {
                "open?"
            } else {
                "final?"
            }
        }
    }

    private fun List<AstTypeParameterRef>.renderTypeParameters() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated()
            print(">")
        }
    }

    private fun List<AstTypeProjection>.renderTypeArguments() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated()
            print(">")
        }
    }

    override fun visitTypeParameterRef(typeParameterRef: AstTypeParameterRef) {
        typeParameterRef.symbol.ast.accept(this)
    }

    override fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration) {
        memberDeclaration.annotations.renderAnnotations()
        if (memberDeclaration !is AstProperty || !memberDeclaration.isLocal) {
            // we can't access session.effectiveVisibilityResolver from here!
            // print(memberDeclaration.visibility.asString(memberDeclaration.getEffectiveVisibility(...)) + " ")
            print(memberDeclaration.visibility.asString() + " ")
            print(memberDeclaration.modalityAsString() + " ")
        }
        if (memberDeclaration.isExpect) {
            print("expect ")
        }
        if (memberDeclaration.isActual) {
            print("actual ")
        }
        if (memberDeclaration is AstCallableMemberDeclaration<*>) {
            if (memberDeclaration.isOverride) {
                print("override ")
            }
            if (memberDeclaration.isStatic) {
                print("static ")
            }
        }
        if (memberDeclaration is AstRegularClass) {
            if (memberDeclaration.isInner) {
                print("inner ")
            }
            if (memberDeclaration.isCompanion) {
                print("companion ")
            }
            if (memberDeclaration.isData) {
                print("data ")
            }
            if (memberDeclaration.isInline) {
                print("inline ")
            }
        } else if (memberDeclaration is AstSimpleFunction) {
            if (memberDeclaration.isOperator) {
                print("operator ")
            }
            if (memberDeclaration.isInfix) {
                print("infix ")
            }
            if (memberDeclaration.isInline) {
                print("inline ")
            }
            if (memberDeclaration.isTailRec) {
                print("tailrec ")
            }
            if (memberDeclaration.isExternal) {
                print("external ")
            }
            if (memberDeclaration.isSuspend) {
                print("suspend ")
            }
        } else if (memberDeclaration is AstProperty) {
            if (memberDeclaration.isConst) {
                print("const ")
            }
            if (memberDeclaration.isLateInit) {
                print("lateinit ")
            }
        }

        visitDeclaration(memberDeclaration)
        when (memberDeclaration) {
            is AstClassLikeDeclaration<*> -> {
                if (memberDeclaration is AstRegularClass) {
                    print(" " + memberDeclaration.name)
                }
                if (memberDeclaration is AstTypeAlias) {
                    print(" " + memberDeclaration.name)
                }
                memberDeclaration.typeParameters.renderTypeParameters()
            }
            is AstCallableDeclaration<*> -> {
                // Name is handled by visitCallableDeclaration
                if (memberDeclaration.typeParameters.isNotEmpty()) {
                    print(" ")
                    memberDeclaration.typeParameters.renderTypeParameters()
                }
            }
        }
    }

    override fun visitDeclaration(declaration: AstDeclaration) {
        print(
            when (declaration) {
                is AstRegularClass -> declaration.classKind.name.toLowerCase().replace("_", " ")
                is AstTypeAlias -> "typealias"
                is AstSimpleFunction -> "fun"
                is AstProperty -> {
                    val prefix = if (declaration.isLocal) "l" else ""
                    prefix + if (declaration.isVal) "val" else "var"
                }
                is AstField -> "field"
                is AstEnumEntry -> "enum entry"
                else -> "unknown"
            }
        )
    }

    private fun List<AstDeclaration>.renderDeclarations() {
        renderInBraces {
            for (declaration in this) {
                declaration.accept(this@AstRenderer)
                println()
            }
        }
    }

    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", f: () -> Unit) {
        println(" $leftBrace")
        pushIndent()
        f()
        popIndent()
        println(rightBrace)
    }

    fun renderSupertypes(regularClass: AstRegularClass) {
        if (regularClass.superTypeRefs.isNotEmpty()) {
            print(" : ")
            regularClass.superTypeRefs.renderSeparated()
        }
    }

    override fun visitRegularClass(regularClass: AstRegularClass) {
        visitMemberDeclaration(regularClass)
        renderSupertypes(regularClass)
        regularClass.declarations.renderDeclarations()
    }

    override fun visitEnumEntry(enumEntry: AstEnumEntry) {
        visitCallableDeclaration(enumEntry)
        enumEntry.initializer?.let {
            print(" = ")
            it.accept(this)
        }
    }

    override fun visitAnonymousObject(anonymousObject: AstAnonymousObject) {
        anonymousObject.annotations.renderAnnotations()
        print("object : ")
        anonymousObject.superTypeRefs.renderSeparated()
        anonymousObject.declarations.renderDeclarations()
    }

    override fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>) {
        visitCallableDeclaration(variable)
        variable.initializer?.let {
            print(" = ")
            it.accept(this)
        }
        variable.delegate?.let {
            print("by ")
            it.accept(this)
        }
    }

    override fun visitField(field: AstField) {
        visitVariable(field)
        println()
    }

    override fun visitProperty(property: AstProperty) {
        visitVariable(property)
        if (property.isLocal) return
        println()
        pushIndent()
        property.getter?.accept(this)
        if (property.getter?.body == null) {
            println()
        }
        if (property.isVar) {
            property.setter?.accept(this)
            if (property.setter?.body == null) {
                println()
            }
        }
        popIndent()
    }

    override fun visitSimpleFunction(simpleFunction: AstSimpleFunction) {
        visitCallableDeclaration(simpleFunction)
        simpleFunction.body?.renderBody()
        if (simpleFunction.body == null) {
            println()
        }
    }

    override fun visitConstructor(constructor: AstConstructor) {
        constructor.annotations.renderAnnotations()
        print(constructor.visibility.asString() + " ")
        if (constructor.isExpect) {
            print("expect ")
        }
        if (constructor.isActual) {
            print("actual ")
        }
        print("constructor")
        constructor.typeParameters.renderTypeParameters()
        constructor.valueParameters.renderParameters()
        print(": ")
        constructor.returnTypeRef.accept(this)
        val body = constructor.body
        val delegatedConstructor = constructor.delegatedConstructor
        if (body == null) {
            if (delegatedConstructor != null) {
                renderInBraces {
                    delegatedConstructor.accept(this)
                    println()
                }
            } else {
                println()
            }
        }
        body?.renderBody(listOfNotNull<AstStatement>(delegatedConstructor))
    }

    override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) {
        propertyAccessor.annotations.renderAnnotations()
        print(propertyAccessor.visibility.asString() + " ")
        print(if (propertyAccessor.isGetter) "get" else "set")
        propertyAccessor.valueParameters.renderParameters()
        print(": ")
        propertyAccessor.returnTypeRef.accept(this)
        propertyAccessor.renderContractDescription()
        propertyAccessor.body?.renderBody()
    }

    override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction) {
        anonymousFunction.annotations.renderAnnotations()
        val label = anonymousFunction.label
        if (label != null) {
            print("${label.name}@")
        }
        print("fun ")
        val receiverType = anonymousFunction.receiverTypeRef
        if (receiverType != null) {
            receiverType.accept(this)
            print(".")
        }
        print("<anonymous>")
        anonymousFunction.valueParameters.renderParameters()
        print(": ")
        anonymousFunction.returnTypeRef.accept(this)
        if (anonymousFunction.invocationKind != null) {
            print(" <kind=${anonymousFunction.invocationKind}> ")
        }
        if (mode.renderLambdaBodies) {
            anonymousFunction.body?.renderBody()
        }
    }

    override fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>) {
        function.valueParameters.renderParameters()
        visitDeclaration(function)
        function.body?.renderBody()
    }

    override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) {
        print("init")
        anonymousInitializer.body?.renderBody()
    }

    private fun AstBlock.renderBody(additionalStatements: List<AstStatement> = emptyList()) {
        renderInBraces {
            for (statement in additionalStatements + statements) {
                statement.accept(this@AstRenderer)
                println()
            }
        }
    }

    override fun visitBlock(block: AstBlock) {
        block.renderBody()
    }

    override fun visitTypeAlias(typeAlias: AstTypeAlias) {
        typeAlias.annotations.renderAnnotations()
        visitMemberDeclaration(typeAlias)
        print(" = ")
        typeAlias.expandedTypeRef.accept(this)
        println()
    }

    override fun visitTypeParameter(typeParameter: AstTypeParameter) {
        typeParameter.annotations.renderAnnotations()
        if (typeParameter.isReified) {
            print("reified ")
        }
        typeParameter.variance.renderVariance()
        print(typeParameter.name)

        val meaningfulBounds = typeParameter.bounds.filter {
            if (it !is AstResolvedTypeRef) return@filter true
            if (!it.type.isNullable) return@filter true
            val type = it.type as? ConeLookupTagBasedType ?: return@filter true
            type.lookupTag.safeAs<ConeClassLikeLookupTag>()?.classId != StandardClassIds.Any
        }

        if (meaningfulBounds.isNotEmpty()) {
            print(" : ")
            meaningfulBounds.renderSeparated()
        }
    }

    override fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression) {
        safeCallExpression.receiver.accept(this)
        print("?.{ ")
        safeCallExpression.regularQualifiedAccess.accept(this)
        print(" }")
    }

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject) {
        print("\$subj\$")
    }

    override fun visitTypedDeclaration(typedDeclaration: AstTypedDeclaration) {
        visitDeclaration(typedDeclaration)
    }

    override fun visitValueParameter(valueParameter: AstValueParameter) {
        valueParameter.annotations.renderAnnotations()
        if (valueParameter.isCrossinline) {
            print("crossinline ")
        }
        if (valueParameter.isNoinline) {
            print("noinline ")
        }
        if (valueParameter.isVararg) {
            print("vararg ")
        }
        if (valueParameter.name != SpecialNames.NO_NAME_PROVIDED) {
            print(valueParameter.name.toString() + ": ")
        }
        valueParameter.returnTypeRef.accept(this)
        valueParameter.defaultValue?.let {
            print(" = ")
            it.accept(this)
        }
    }

    override fun visitImport(import: AstImport) {
        visitElement(import)
    }

    override fun visitStatement(statement: AstStatement) {
        if (statement is AstStubStatement) {
            print("[StubStatement]")
        } else {
            visitElement(statement)
        }
    }

    override fun visitReturnExpression(returnExpression: AstReturnExpression) {
        returnExpression.annotations.renderAnnotations()
        print("^")
        val target = returnExpression.target
        val labeledElement = target.labeledElement
        if (labeledElement is AstSimpleFunction) {
            print("${labeledElement.name}")
        } else {
            val labelName = target.labelName
            if (labelName != null) {
                print("@$labelName")
            }
        }
        print(" ")
        returnExpression.result.accept(this)
    }

    override fun visitWhenBranch(whenBranch: AstWhenBranch) {
        val condition = whenBranch.condition
        if (condition is AstElseIfTrueCondition) {
            print("else")
        } else {
            condition.accept(this)
        }
        print(" -> ")
        whenBranch.result.accept(this)
    }

    override fun visitWhenExpression(whenExpression: AstWhenExpression) {
        whenExpression.annotations.renderAnnotations()
        print("when (")
        val subjectVariable = whenExpression.subjectVariable
        if (subjectVariable != null) {
            subjectVariable.accept(this)
        } else {
            whenExpression.subject?.accept(this)
        }
        println(") {")
        pushIndent()
        for (branch in whenExpression.branches) {
            branch.accept(this)
        }
        popIndent()
        println("}")
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression) {
        print("\$subj\$")
    }

    override fun visitTryExpression(tryExpression: AstTryExpression) {
        tryExpression.annotations.renderAnnotations()
        print("try")
        tryExpression.tryBlock.accept(this)
        for (catchClause in tryExpression.catches) {
            print("catch (")
            catchClause.parameter.accept(this)
            print(")")
            catchClause.block.accept(this)
        }
        val finallyBlock = tryExpression.finallyBlock ?: return
        print("finally")
        finallyBlock.accept(this)
    }

    override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop) {
        val label = doWhileLoop.label
        if (label != null) {
            print("${label.name}@")
        }
        print("do")
        doWhileLoop.block.accept(this)
        print("while(")
        doWhileLoop.condition.accept(this)
        print(")")
    }

    override fun visitWhileLoop(whileLoop: AstWhileLoop) {
        val label = whileLoop.label
        if (label != null) {
            print("${label.name}@")
        }
        print("while(")
        whileLoop.condition.accept(this)
        print(")")
        whileLoop.block.accept(this)
    }

    override fun visitLoopJump(loopJump: AstLoopJump) {
        val target = loopJump.target
        val labeledElement = target.labeledElement
        print("@@@[")
        labeledElement.condition.accept(this)
        print("] ")
    }

    override fun visitBreakExpression(breakExpression: AstBreakExpression) {
        breakExpression.annotations.renderAnnotations()
        print("break")
        visitLoopJump(breakExpression)
    }

    override fun visitContinueExpression(continueExpression: AstContinueExpression) {
        continueExpression.annotations.renderAnnotations()
        print("continue")
        visitLoopJump(continueExpression)
    }

    override fun visitExpression(expression: AstExpression) {
        expression.annotations.renderAnnotations()
        print(
            when (expression) {
                is AstExpressionStub -> "STUB"
                is AstUnitExpression -> "Unit"
                is AstElseIfTrueCondition -> "else"
                is AstNoReceiverExpression -> ""
                else -> "??? ${expression.javaClass}"
            }
        )
    }

    override fun <T> visitConstExpression(constExpression: AstConstExpression<T>) {
        constExpression.annotations.renderAnnotations()
        val kind = constExpression.kind
        val value = constExpression.value
        print("$kind(")
        if (value !is Char) {
            print(value.toString())
        } else {
            if (value.toInt() in 32..127) {
                print(value)
            } else {
                print(value.toInt())
            }
        }
        print(")")
    }

    override fun visitWrappedDelegateExpression(wrappedDelegateExpression: AstWrappedDelegateExpression) {
        wrappedDelegateExpression.expression.accept(this)
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression) {
        print(namedArgumentExpression.name)
        print(" = ")
        if (namedArgumentExpression.isSpread) {
            print("*")
        }
        namedArgumentExpression.expression.accept(this)
    }

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression) {
        if (spreadArgumentExpression.isSpread) {
            print("*")
        }
        spreadArgumentExpression.expression.accept(this)
    }

    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression) {
        print("<L> = ")
        lambdaArgumentExpression.expression.accept(this)
    }

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: AstVarargArgumentsExpression) {
        print("vararg(")
        varargArgumentsExpression.arguments.renderSeparated()
        print(")")
    }

    override fun visitCall(call: AstCall) {
        print("(")
        if (mode.renderCallArguments) {
            call.arguments.renderSeparated()
        } else {
            if (call.arguments.isNotEmpty()) {
                print("...")
            }
        }
        print(")")
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall) {
        print("<strcat>")
        visitCall(stringConcatenationCall)
    }

    override fun visitTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall) {
        print("(")
        typeOperatorCall.argument.accept(this)
        print(" ")
        print(typeOperatorCall.operation.operator)
        print(" ")
        typeOperatorCall.conversionTypeRef.accept(this)
        print(")")
    }

    override fun visitAnnotationCall(annotationCall: AstAnnotationCall) {
        print("@")
        annotationCall.useSiteTarget?.let {
            print(it.name)
            print(":")
        }
        annotationCall.annotationTypeRef.accept(this)
        visitCall(annotationCall)
        if (annotationCall.useSiteTarget == AnnotationUseSiteTarget.FILE) {
            println()
        } else {
            print(" ")
        }
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall) {
        val dispatchReceiver = delegatedConstructorCall.dispatchReceiver
        if (dispatchReceiver !is AstNoReceiverExpression) {
            dispatchReceiver.accept(this)
            print(".")
        }
        if (delegatedConstructorCall.isSuper) {
            print("super<")
        } else if (delegatedConstructorCall.isThis) {
            print("this<")
        }
        delegatedConstructorCall.constructedTypeRef.accept(this)
        print(">")
        visitCall(delegatedConstructorCall)
    }

    override fun visitTypeRef(typeRef: AstTypeRef) {
        typeRef.annotations.renderAnnotations()
        visitElement(typeRef)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: AstImplicitTypeRef) {
        print("<implicit>")
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: AstTypeRefWithNullability) {
        if (typeRefWithNullability.isMarkedNullable) {
            print("?")
        }
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: AstDynamicTypeRef) {
        dynamicTypeRef.annotations.renderAnnotations()
        print("<dynamic>")
        visitTypeRefWithNullability(dynamicTypeRef)
    }

    override fun visitFunctionTypeRef(functionTypeRef: AstFunctionTypeRef) {
        print("( ")
        functionTypeRef.receiverTypeRef?.let {
            it.accept(this)
            print(".")
        }
        functionTypeRef.valueParameters.renderParameters()
        print(" -> ")
        functionTypeRef.returnTypeRef.accept(this)
        print(" )")
        visitTypeRefWithNullability(functionTypeRef)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: AstResolvedTypeRef) {
        val kind = resolvedTypeRef.functionTypeKind
        val annotations = if (kind.withPrettyRender()) {
            resolvedTypeRef.annotations.dropExtensionFunctionAnnotation()
        } else {
            resolvedTypeRef.annotations
        }
        annotations.renderAnnotations()
        print("R|")
        val coneType = resolvedTypeRef.type
        print(coneType.renderFunctionType(kind, resolvedTypeRef.annotations.any {
            it.isExtensionFunctionAnnotationCall
        }))
        print("|")
    }

    private val AstResolvedTypeRef.functionTypeKind: FunctionClassKind?
        get() {
            val classId = (type as? ConeClassLikeType)?.lookupTag?.classId ?: return null
            return FunctionClassKind.getFunctionalClassKind(
                classId.shortClassName.asString(), classId.packageFqName
            )
        }

    override fun visitUserTypeRef(userTypeRef: AstUserTypeRef) {
        userTypeRef.annotations.renderAnnotations()
        for ((index, qualifier) in userTypeRef.qualifier.withIndex()) {
            if (index != 0) {
                print(".")
            }
            print(qualifier.name)
            if (qualifier.typeArgumentList.typeArguments.isNotEmpty()) {
                print("<")
                qualifier.typeArgumentList.typeArguments.renderSeparated()
                print(">")
            }
        }
        visitTypeRefWithNullability(userTypeRef)
    }

    override fun visitTypeProjection(typeProjection: AstTypeProjection) {
        visitElement(typeProjection)
    }

    override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance) {
        typeProjectionWithVariance.variance.renderVariance()
        typeProjectionWithVariance.typeRef.accept(this)
    }

    override fun visitStarProjection(starProjection: AstStarProjection) {
        print("*")
    }

    private fun AbstractAstBasedSymbol<*>.render(): String {
        return when (this) {
            is AstCallableSymbol<*> -> callableId.toString()
            is AstClassLikeSymbol<*> -> classId.toString()
            else -> "?"
        }
    }

    override fun visitNamedReference(namedReference: AstNamedReference) {
        val symbol = namedReference.candidateSymbol
        when {
            symbol != null -> {
                print("R?C|${symbol.render()}|")
            }
            namedReference is AstErrorNamedReference -> print("<${namedReference.diagnostic.reason}>#")
            else -> print("${namedReference.name}#")
        }
    }

    override fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference) {
        print("F|")
        print(backingFieldReference.resolvedSymbol.callableId)
        print("|")
    }

    override fun visitDelegateFieldReference(delegateFieldReference: AstDelegateFieldReference) {
        print("D|")
        print(delegateFieldReference.resolvedSymbol.callableId)
        print("|")
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: AstResolvedNamedReference) {
        print("R|")
        val symbol = resolvedNamedReference.resolvedSymbol
        val isFakeOverride = when (symbol) {
            is AstNamedFunctionSymbol -> symbol.isFakeOverride
            is AstPropertySymbol -> symbol.isFakeOverride
            else -> false
        }

        if (isFakeOverride) {
            print("FakeOverride<")
        }
        print(symbol.render())


        if (resolvedNamedReference is AstResolvedCallableReference) {
            if (resolvedNamedReference.inferredTypeArguments.isNotEmpty()) {
                print("<")

                resolvedNamedReference.inferredTypeArguments.renderTypesSeparated()

                print(">")
            }
        }

        if (isFakeOverride) {
            when (symbol) {
                is AstNamedFunctionSymbol -> {
                    print(": ")
                    symbol.ast.returnTypeRef.accept(this)
                }
                is AstPropertySymbol -> {
                    print(": ")
                    symbol.ast.returnTypeRef.accept(this)
                }
            }
            print(">")
        }
        print("|")
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: AstResolvedCallableReference) {
        visitResolvedNamedReference(resolvedCallableReference)
    }

    override fun visitThisReference(thisReference: AstThisReference) {
        print("this")
        val labelName = thisReference.labelName
        val symbol = thisReference.boundSymbol
        when {
            symbol != null -> print("@R|${symbol.render()}|")
            labelName != null -> print("@$labelName#")
            else -> print("#")
        }
    }

    override fun visitSuperReference(superReference: AstSuperReference) {
        print("super<")
        superReference.superTypeRef.accept(this)
        print(">")
        superReference.labelName?.let {
            print("@$it#")
        }
    }

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val dispatchReceiver = qualifiedAccess.dispatchReceiver
        val extensionReceiver = qualifiedAccess.extensionReceiver
        var hasSomeReceiver = true
        when {
            dispatchReceiver !is AstNoReceiverExpression && extensionReceiver !is AstNoReceiverExpression -> {
                print("(")
                dispatchReceiver.accept(this)
                print(", ")
                extensionReceiver.accept(this)
                print(")")
            }
            dispatchReceiver !is AstNoReceiverExpression -> {
                dispatchReceiver.accept(this)
            }
            extensionReceiver !is AstNoReceiverExpression -> {
                extensionReceiver.accept(this)
            }
            explicitReceiver != null -> {
                explicitReceiver.accept(this)
            }
            else -> {
                hasSomeReceiver = false
            }
        }
        if (hasSomeReceiver) {
            print(".")
        }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: AstCheckNotNullCall) {
        checkNotNullCall.argument.accept(this)
        print("!!")
    }

    override fun visitElvisExpression(elvisExpression: AstElvisExpression) {
        elvisExpression.lhs.accept(this)
        print(" ?: ")
        elvisExpression.rhs.accept(this)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess) {
        callableReferenceAccess.annotations.renderAnnotations()
        callableReferenceAccess.explicitReceiver?.accept(this)
        if (callableReferenceAccess.hasQuestionMarkAtLHS && callableReferenceAccess.explicitReceiver !is AstResolvedQualifier) {
            print("?")
        }
        print("::")
        callableReferenceAccess.calleeReference.accept(this)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: AstQualifiedAccessExpression) {
        qualifiedAccessExpression.annotations.renderAnnotations()
        visitQualifiedAccess(qualifiedAccessExpression)
        qualifiedAccessExpression.calleeReference.accept(this)
        qualifiedAccessExpression.typeArguments.renderTypeArguments()
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression) {
        visitQualifiedAccessExpression(thisReceiverExpression)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast) {
        visitQualifiedAccessExpression(expressionWithSmartcast)
    }

    private fun visitAssignment(operation: AstOperation, rValue: AstExpression) {
        print(operation.operator)
        print(" ")
        rValue.accept(this)
    }

    override fun visitVariableAssignment(variableAssignment: AstVariableAssignment) {
        variableAssignment.annotations.renderAnnotations()
        visitQualifiedAccess(variableAssignment)
        variableAssignment.lValue.accept(this)
        print(" ")
        visitAssignment(AstOperation.ASSIGN, variableAssignment.rValue)
    }

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: AstAugmentedArraySetCall) {
        augmentedArraySetCall.annotations.renderAnnotations()
        print("ArraySet:[")
        augmentedArraySetCall.assignCall.accept(this)
        print("]")
    }

    override fun visitFunctionCall(functionCall: AstFunctionCall) {
        functionCall.annotations.renderAnnotations()
        visitQualifiedAccess(functionCall)
        functionCall.calleeReference.accept(this)
        functionCall.typeArguments.renderTypeArguments()
        visitCall(functionCall)
    }

    override fun visitComparisonExpression(comparisonExpression: AstComparisonExpression) {
        print("CMP(${comparisonExpression.operation.operator}, ")
        comparisonExpression.compareToCall.accept(this)
        print(")")
    }

    override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement) {
        assignmentOperatorStatement.annotations.renderAnnotations()
        print(assignmentOperatorStatement.operation.operator)
        print("(")
        assignmentOperatorStatement.leftArgument.accept(this@AstRenderer)
        print(", ")
        assignmentOperatorStatement.rightArgument.accept(this@AstRenderer)
        print(")")
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall) {
        equalityOperatorCall.annotations.renderAnnotations()
        print(equalityOperatorCall.operation.operator)
        visitCall(equalityOperatorCall)
    }

    override fun visitComponentCall(componentCall: AstComponentCall) {
        visitFunctionCall(componentCall)
    }

    override fun visitGetClassCall(getClassCall: AstGetClassCall) {
        getClassCall.annotations.renderAnnotations()
        print("<getClass>")
        visitCall(getClassCall)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: AstClassReferenceExpression) {
        classReferenceExpression.annotations.renderAnnotations()
        print("<getClass>")
        print("(")
        classReferenceExpression.classTypeRef.accept(this)
        print(")")
    }

    override fun visitArrayOfCall(arrayOfCall: AstArrayOfCall) {
        arrayOfCall.annotations.renderAnnotations()
        print("<implicitArrayOf>")
        visitCall(arrayOfCall)
    }

    override fun visitThrowExpression(throwExpression: AstThrowExpression) {
        throwExpression.annotations.renderAnnotations()
        print("throw ")
        throwExpression.exception.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: AstResolvedQualifier) {
        print("Q|")
        val classId = resolvedQualifier.classId
        if (classId != null) {
            print(classId.asString())
        } else {
            print(resolvedQualifier.packageFqName.asString().replace(".", "/"))
        }
        if (resolvedQualifier.isNullableLHSForCallableReference) {
            print("?")
        }
        print("|")
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression) {
        binaryLogicExpression.leftOperand.accept(this)
        print(" ${binaryLogicExpression.kind.token} ")
        binaryLogicExpression.rightOperand.accept(this)
    }

}
