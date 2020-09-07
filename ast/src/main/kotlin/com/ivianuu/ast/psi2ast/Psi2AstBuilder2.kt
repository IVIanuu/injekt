package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstElement
import org.jetbrains.kotlin.psi.KtVisitor

class Psi2AstBuilder2(override val context: Psi2AstGeneratorContext) : Generator, KtVisitor<AstElement, Nothing?>() {

    /**

    override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): AstElement {
        val ktSubjectExpression = expression.subjectExpression
        val subjectExpression = when (ktSubjectExpression) {
            is KtVariableDeclaration -> ktSubjectExpression.initializer
            else -> ktSubjectExpression
        }!!.convert<AstExpression>()
        val subjectVariable = when (ktSubjectExpression) {
            is KtVariableDeclaration -> {
                val name = ktSubjectExpression.nameAsSafeName
                buildProperty {
                    returnType = ktSubjectExpression.typeReference.toAstOrImplicitType()
                    receiverTypeRef = null
                    this.name = name
                    initializer = subjectExpression
                    delegate = null
                    isVar = false
                    symbol = AstPropertySymbol(name)
                    isLocal = true
                    status = AstDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                }
            }
            else -> null
        }
        val hasSubject = subjectExpression != null

        @OptIn(AstContractViolation::class)
        val ref = AstElementRef<AstWhen>()
        return buildWhenExpression {
            this.subject = subjectExpression
            this.subjectVariable = subjectVariable

            for (entry in expression.entries) {
                val branchBody = entry.expression.toAstBlock()
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        buildWhenBranch {
                            condition = entry.conditions.toAstWhenCondition(
                                ref,
                                { toAstExpression(it) },
                                { toAstType() },
                            )
                            result = branchBody
                        }
                    } else {
                        val ktCondition = entry.conditions.first() as? KtWhenConditionWithExpression
                        buildWhenBranch {
                            condition = ktCondition?.expression.toAstExpression("No expression in condition with expression")
                            result = branchBody
                        }
                    }
                } else {
                    buildWhenBranch {
                        condition = buildElseIfTrueCondition()
                        result = branchBody
                    }
                }
            }
        }.also {
            if (hasSubject) {
                ref.bind(it)
            }
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {

        when (operationToken) {
            ELVIS ->
                return leftArgument.generateNotNullOrOther(rightArgument)
            ANDAND, OROR ->
                return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND)
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgument.generateContainsOperation(
                    leftArgument, operationToken == NOT_IN
                )
            in OperatorConventions.COMPARISON_OPERATIONS ->
                return leftArgument.generateComparisonExpression(
                    rightArgument, operationToken
                )
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            buildFunctionCall {
                calleeReference = buildSimpleNamedReference {
                    name = conventionCallName ?: expression.operationReference.getReferencedNameAsName()
                }
                explicitReceiver = leftArgument
                argumentList = buildUnaryArgumentList(rightArgument)
            }
        } else {
            val astOperation = operationToken.toAstOperation()
            if (astOperation in AstOperation.ASSIGNMENTS) {
                return expression.left.generateAssignment(expression.right, rightArgument, astOperation) {
                    (this as KtExpression).toAstExpression("Incorrect expression in assignment: ${expression.text}")
                }
            } else {
                buildEqualityOperatorCall {
                    operation = astOperation
                    argumentList = buildBinaryArgumentList(leftArgument, rightArgument)
                }
            }
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Nothing?): AstElement {
        val operationToken = expression.operationToken
        val argument = expression.baseExpression
        val conventionCallName = operationToken.toUnaryName()
        return when {
            operationToken == EXCLEXCL -> {
                buildCheckNotNullCall {
                    argumentList = buildUnaryArgumentList(argument.toAstExpression("No operand"))
                }
            }
            conventionCallName != null -> {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        expression, expression.operationReference, argument,
                        callName = conventionCallName,
                        prefix = expression is KtPrefixExpression,
                    ) { (this as KtExpression).toAstExpression("Incorrect expression inside inc/dec") }
                }
                buildFunctionCall {
                    calleeReference = buildSimpleNamedReference {
                        name = conventionCallName
                    }
                    explicitReceiver = argument.toAstExpression("No operand")
                }
            }
            else -> throw IllegalStateException("Unexpected expression: ${expression.text}")
        }
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Nothing?): AstElement {
        val arrayExpression = expression.arrayExpression
        return buildFunctionCall {
            val source: AstPsiSourceElement<*>
            val getArgument = arraySetArgument.remove(expression)
            if (getArgument != null) {
                calleeReference = buildSimpleNamedReference {
                    name = OperatorNameConventions.SET
                }
            } else {
                calleeReference = buildSimpleNamedReference {
                    name = OperatorNameConventions.GET
                }
            }
            explicitReceiver = arrayExpression.toAstExpression("No array expression")
            argumentList = buildArgumentList {
                for (indexExpression in expression.indexExpressions) {
                    arguments += indexExpression.toAstExpression("Incorrect index expression")
                }
                if (getArgument != null) {
                    arguments += getArgument
                }
            }
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        return buildThisReceiverExpression {
            calleeReference = buildExplicitThisReference {
                labelName = expression.getLabelName()
            }
        }
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Nothing?): AstElement {
        return buildArrayOfCall {
            argumentList = buildArgumentList {
                for (innerExpression in expression.getInnerExpressions()) {
                    arguments += innerExpression.toAstExpression("Incorrect collection literal argument")
                }
            }
        }
    }

    fun generateIncrementOrDecrementBlock(
        baseExpression: PsiElement,
        operationReference: PsiElement?,
        argument: PsiElement,
        callName: Name,
        prefix: Boolean,
        convert: PsiElement.() -> AstExpression
    ): AstExpression {
        return buildBlock {
            val tmpName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(
                tmpName,
                argument.convert()
            )
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = buildFunctionCall {
                calleeReference = buildSimpleNamedReference {
                    name = callName
                }
                explicitReceiver = generateResolvedAccessExpression(temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(resultName, resultInitializer)
            val assignment = argument.generateAssignment(
                argument,
                if (prefix && argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION)
                    generateResolvedAccessExpression(resultVar)
                else
                    resultInitializer,
                AstOperation.ASSIGN,
                convert
            )

            fun appendAssignment() {
                if (assignment is AstBlock) {
                    statements += assignment.statements
                } else {
                    statements += assignment
                }
            }

            if (prefix) {
                if (argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION) {
                    statements += resultVar
                    appendAssignment()
                    statements += generateResolvedAccessExpression(resultVar)
                } else {
                    appendAssignment()
                    statements += generateAccessExpression(argument.getReferencedNameAsName())
                }
            } else {
                appendAssignment()
                statements += generateResolvedAccessExpression(temporaryVariable)
            }
        }
    }

    fun PsiElement?.generateAssignment(
        rhs: PsiElement?,
        value: AstExpression, // value is AST for rhs
        operation: AstOperation,
        convert: PsiElement.() -> AstExpression
    ): AstStatement {
        val tokenType = this?.elementType
        if (tokenType == org.jetbrains.kotlin.KtNodeTypes.PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(baseSource, rhs, value, operation, convert)
        }
        if (tokenType == org.jetbrains.kotlin.KtNodeTypes.ARRAY_ACCESS_EXPRESSION) {
            require(this != null)
            if (operation == AstOperation.ASSIGN) {
                arraySetArgument[this] = value
            }
            return if (operation == AstOperation.ASSIGN) {
                this.convert()
            } else {
                generateAugmentedArraySetCall(baseSource, operation, rhs, convert)
            }
        }

        if (operation in AstOperation.ASSIGNMENTS && operation != AstOperation.ASSIGN) {
            return buildAssignmentOperatorStatement {
                this.operation = operation
                // TODO: take good psi
                leftArgument = this@generateAssignment?.convert() ?: buildErrorExpression {
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.ExpressionRequired
                    )
                }
                rightArgument = value
            }
        }
        require(operation == AstOperation.ASSIGN)

        if (this?.elementType == org.jetbrains.kotlin.KtNodeTypes.SAFE_ACCESS_EXPRESSION && this != null) {
            val safeCallNonAssignment = convert() as? AstSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, value)
            }
        }

        return buildVariableAssignment {
            source = baseSource
            rValue = value
            calleeReference = initializeLValue(this@generateAssignment) { convert() as? AstQualifiedAccess }
        }
    }

    // gets a?.{ $subj.x } and turns it to a?.{ $subj.x = v }
    private fun putAssignmentToSafeCall(
        safeCallNonAssignment: AstSafeCallExpression,
        value: AstExpression
    ): AstSafeCallExpression {
        val nestedAccess = safeCallNonAssignment.regularQualifiedAccess

        val assignment = buildVariableAssignment {
            rValue = value
            calleeReference = nestedAccess.calleeReference
            explicitReceiver = safeCallNonAssignment.checkedSubjectRef.value
        }

        safeCallNonAssignment.replaceRegularQualifiedAccess(
            assignment
        )

        return safeCallNonAssignment
    }

    private fun PsiElement.generateAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstStatement {
        return buildAugmentedArraySetCall {
            this.operation = operation
            assignCall = generateAugmentedCallForAugmentedArraySetCall(operation, rhs, convert)
            setGetBlock = generateSetGetBlockForAugmentedArraySetCall(operation, rhs, convert)
        }
    }

    private fun PsiElement.generateAugmentedCallForAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstFunctionCall {
        /*
         * Desugarings of a[x, y] += z to
         * a.get(x, y).plusAssign(z)
         */
        return buildFunctionCall {
            calleeReference = buildSimpleNamedReference {
                name = AstOperationNameConventions.ASSIGNMENTS.getValue(operation)
            }
            explicitReceiver = convert()
            argumentList = buildArgumentList {
                arguments += rhs?.convert() ?: buildErrorExpression(
                    null,
                    ConeSimpleDiagnostic("No value for array set", DiagnosticKind.Syntax)
                )
            }
        }
    }


    private fun PsiElement.generateSetGetBlockForAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstBlock {
        /*
         * Desugarings of a[x, y] += z to
         * {
         *     val tmp_a = a
         *     val tmp_x = x
         *     val tmp_y = y
         *     tmp_a.set(tmp_x, tmp_a.get(tmp_x, tmp_y).plus(z))
         * }
         */
        return buildBlock {
            val baseCall = convert() as AstFunctionCall

            val arrayVariable = generateTemporaryVariable(
                baseSession,
                "<array>",
                baseCall.explicitReceiver!!
            )
            statements += arrayVariable
            val indexVariables = baseCall.arguments.mapIndexed { i, index ->
                generateTemporaryVariable(baseSession, "<index_$i>", index)
            }
            statements += indexVariables
            statements += buildFunctionCall {
                explicitReceiver = arrayVariable.toQualifiedAccess()
                calleeReference = buildSimpleNamedReference {
                    name = org.jetbrains.kotlin.util.OperatorNameConventions.SET
                }
                argumentList = buildArgumentList {
                    for (indexVariable in indexVariables) {
                        arguments += indexVariable.toQualifiedAccess()
                    }

                    val getCall = buildFunctionCall {
                        explicitReceiver = arrayVariable.toQualifiedAccess()
                        calleeReference = buildSimpleNamedReference {
                            name = org.jetbrains.kotlin.util.OperatorNameConventions.GET
                        }
                        argumentList = buildArgumentList {
                            for (indexVariable in indexVariables) {
                                arguments += indexVariable.toQualifiedAccess()
                            }
                        }
                    }

                    val operatorCall = buildFunctionCall {
                        calleeReference = buildSimpleNamedReference {
                            name = AstOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operation)
                        }
                        explicitReceiver = getCall
                        argumentList = buildArgumentList {
                            arguments += rhs?.convert() ?: buildErrorExpression(
                                null,
                                ConeSimpleDiagnostic(
                                    "No value for array set",
                                    DiagnosticKind.Syntax
                                )
                            )
                        }
                    }
                    arguments += operatorCall
                }
            }
        }
    }

    private fun AstQualifiedAccess.wrapWithSafeCall(receiver: AstExpression): AstSafeCallExpression {
        val checkedSafeCallSubject = buildCheckedSafeCallSubject {
            @OptIn(AstContractViolation::class)
            this.originalReceiverRef = AstElementRef<AstExpression>().apply {
                bind(receiver)
            }
        }

        replaceExplicitReceiver(checkedSafeCallSubject)
        return buildSafeCallExpression {
            this.receiver = receiver
            @OptIn(AstContractViolation::class)
            this.checkedSubjectRef = AstElementRef<AstCheckedSafeCallSubject>().apply {
                bind(checkedSafeCallSubject)
            }
            this.regularQualifiedAccess = this@wrapWithSafeCall
        }
    }

    private fun KtWhenCondition.toAstWhenCondition(
        whenRefWithSubject: AstElementRef<AstWhen>,
        convert: KtExpression?.(String) -> AstExpression,
        toAstOrErrorTypeRef: KtTypeReference?.() -> AstType,
    ): AstExpression {
        val astSubjectExpression = buildWhenSubjectExpression {
            whenRef = whenRefWithSubject
        }
        return when (this) {
            is KtWhenConditionWithExpression -> {
                buildEqualityOperatorCall {
                    operation = AstOperation.EQ
                    argumentList = buildBinaryArgumentList(
                        astSubjectExpression, expression.convert("No expression in condition with expression")
                    )
                }
            }
            is KtWhenConditionInRange -> {
                val astRange = rangeExpression.convert("No range in condition with range")
                astRange.generateContainsOperation(
                    astSubjectExpression,
                    isNegated
                )
            }
            is KtWhenConditionIsPattern -> {
                buildTypeOperatorCall {
                    operation = if (isNegated) AstOperation.NOT_IS else AstOperation.IS
                    conversionTypeRef = typeReference.toAstOrErrorTypeRef()
                    argumentList = buildUnaryArgumentList(astSubjectExpression)
                }
            }
            else -> error("Unsupported when condition $this")
        }
    }

    private fun Array<KtWhenCondition>.toAstWhenCondition(
        subject: AstElementRef<AstWhen>,
        convert: KtExpression?.(String) -> AstExpression,
        toAstOrErrorTypeRef: KtTypeReference?.() -> AstType,
    ): AstExpression {
        var astCondition: AstExpression? = null
        for (condition in this) {
            val astConditionElement = condition.toAstWhenCondition(subject, convert, toAstOrErrorTypeRef)
            astCondition = when (astCondition) {
                null -> astConditionElement
                else -> astCondition.generateLazyLogicalOperation(astConditionElement, false,)
            }
        }
        return astCondition!!
    }
     */

}
