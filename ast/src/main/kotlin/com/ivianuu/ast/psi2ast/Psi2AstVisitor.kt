package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.builder.buildConstructor
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.declarations.builder.buildNamedFunction
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.declarations.builder.buildPropertyAccessor
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.declarations.builder.buildTypeAlias
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildDelegatedConstructorCall
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildReturn
import com.ivianuu.ast.expressions.builder.buildSuperReference
import com.ivianuu.ast.expressions.builder.buildThisReference
import com.ivianuu.ast.expressions.builder.buildVariableAssignment
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

class Psi2AstVisitor(
    override val context: Psi2AstGeneratorContext
) : KtVisitor<AstElement, Nothing?>(), Generator {

    /*

    override fun visitObjectLiteralExpression(
        expression: KtObjectLiteralExpression,
        data: Nothing?
    ): AstElement {
        // important to compute the ast declaration before asking for it's type
        val anonymousObject = expression.objectDeclaration.accept<AstClass>(mode)
        return AstAnonymousObjectExpression(
            expression.getTypeInferredByFrontendOrFail().toAstType(),
            anonymousObject
        )
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): AstElement {
        return AstAnonymousFunctionExpression(
            type = expression.getTypeInferredByFrontendOrFail().toAstType(),
            anonymousFunction = visitFunction(
                function = expression.functionLiteral,
                mode = mode,
                body = expression.bodyExpression
            )
        )
    }*/

    /*
    override fun visitDestructuringDeclaration(
        multiDeclaration: KtDestructuringDeclaration,
        data: Nothing?
    ): AstElement {
        return AstBlock(context.builtIns.unitType).apply {
            val ktInitializer = multiDeclaration.initializer!!
            val containerProperty = AstProperty(
                name = Name.special("<destructuring container>"),
                type = ktInitializer.getTypeInferredByFrontendOrFail().toAstType(),
                visibility = AstVisibility.LOCAL
            ).apply {
                applyParentFromStack()
                initializer = ktInitializer.accept(mode)
            }

            statements += containerProperty

            statements += multiDeclaration.entries
                .mapNotNull { ktEntry ->
                    val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)
                    // componentN for '_' SHOULD NOT be evaluated
                    if (componentVariable.name.isSpecial) return@mapNotNull null
                    val componentResolvedCall =
                        getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)
                    componentResolvedCall.getReturnType()

                    AstProperty(
                        name = componentVariable.name,
                        type = componentVariable.type.toAstType(),
                        visibility = AstVisibility.LOCAL
                    ).apply {
                        applyParentFromStack()
                        initializer = AstQualifiedAccess(
                            callee = context.provider.get(componentResolvedCall.resultingDescriptor),
                            type = componentVariable.type.toAstType()
                        ).apply {
                            dispatchReceiver = AstQualifiedAccess(
                                callee = containerProperty,
                                type = containerProperty.type
                            )
                        }
                    }
                }
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        val referenceTarget =
            getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        TODO("What $referenceTarget")
    }*/

    /*override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): AstElement {
        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        val astSubject = when {
            subjectVariable != null -> subjectVariable.accept(mode)
            subjectExpression != null -> {
                val astSubjectExpression = subjectExpression.accept<AstExpression>(mode)
                AstProperty(
                    name = Name.special("<when subject>"),
                    type = astSubjectExpression.type,
                    visibility = AstVisibility.LOCAL
                ).apply {
                    applyParentFromStack()
                    initializer = astSubjectExpression
                }
            }
            else -> null
        }
        val astWhen = AstWhen(expression.getTypeInferredByFrontendOrFail().toAstType())
        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                val astElseResult = ktEntry.expression!!.accept<AstExpression>(mode)
                astWhen.branches.add(AstElseBranch(astElseResult))
                break
            }
            var astBranchCondition: AstExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val astCondition = if (astSubject != null)
                    generateWhenConditionWithSubject(ktCondition, astSubject)
                else
                    generateWhenConditionNoSubject(ktCondition)
                astBranchCondition =
                    astBranchCondition?.let { context.whenComma(it, astCondition) } ?: astCondition
            }
            val astBranchResult = ktEntry.expression!!.accept<AstExpression>(mode)
            astWhen.branches += AstConditionBranch(astBranchCondition!!, astBranchResult)
        }
        addElseBranchForExhaustiveWhenIfNeeded(irWhen, expression)
        if (irSubject == null) {
            if (irWhen.branches.isEmpty())
                IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
            else
                irWhen
        } else {
            if (irWhen.branches.isEmpty()) {
                val irBlock = IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
                irBlock.statements.add(irSubject)
                irBlock
            } else {
                val irBlock = IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    irWhen.type,
                    IrStatementOrigin.WHEN
                )
                irBlock.statements.add(irSubject)
                irBlock.statements.add(irWhen)
                irBlock
            }
        }
    }
    private fun addElseBranchForExhaustiveWhenIfNeeded(
        irWhen: IrWhen,
        whenExpression: KtWhenExpression
    ) {
        if (irWhen.branches.filterIsInstance<IrElseBranch>().isEmpty()) {
            //TODO: check condition: seems it's safe to always generate exception
            val isExhaustive = whenExpression.isExhaustiveWhen()
            if (isExhaustive) {
                val call = IrCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.nothingType,
                    context.irBuiltIns.noWhenBranchMatchedExceptionSymbol
                )
                irWhen.branches.add(elseBranch(call))
            }
        }
    }
    private fun KtWhenExpression.isExhaustiveWhen(): Boolean =
        elseExpression != null // TODO front-end should provide correct exhaustiveness information
                || true == get(BindingContext.EXHAUSTIVE_WHEN, this)
                || true == get(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, this)
    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
        (ktCondition as KtWhenConditionWithExpression).expression!!.genExpr()
    private fun generateWhenConditionWithSubject(
        ktCondition: KtWhenCondition,
        irSubject: IrVariable
    ): IrExpression {
        return when (ktCondition) {
            is KtWhenConditionWithExpression ->
                generateEqualsCondition(irSubject, ktCondition)
            is KtWhenConditionInRange ->
                generateInRangeCondition(irSubject, ktCondition)
            is KtWhenConditionIsPattern ->
                generateIsPatternCondition(irSubject, ktCondition)
            else ->
                error("Unexpected 'when' condition: ${ktCondition.text}")
        }
    }
    private fun generateIsPatternCondition(irSubject: IrVariable, ktCondition: KtWhenConditionIsPattern): IrExpression {
        val typeOperand = getOrFail(BindingContext.TYPE, ktCondition.typeReference)
        val irTypeOperand = typeOperand.toAstType()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        val irInstanceOf = IrTypeOperatorCallImpl(
            startOffset, endOffset,
            context.builtIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            irTypeOperand,
            irSubject.loadAt(startOffset, startOffset)
        )
        return if (ktCondition.isNegated)
            primitiveOp1(
                ktCondition.startOffsetSkippingComments, ktCondition.endOffset,
                context.irBuiltIns.booleanNotSymbol,
                context.irBuiltIns.booleanType,
                IrStatementOrigin.EXCL,
                irInstanceOf
            )
        else
            irInstanceOf
    }
    private fun generateInRangeCondition(irSubject: IrVariable, ktCondition: KtWhenConditionInRange): IrExpression {
        val inCall = statementGenerator.pregenerateCall(getResolvedCall(ktCondition.operationReference)!!)
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        inCall.irValueArgumentsByIndex[0] = irSubject.loadAt(startOffset, startOffset)
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = CallGenerator(statementGenerator).generateCall(ktCondition, inCall, inOperator)
        return when (inOperator) {
            IrStatementOrigin.IN ->
                irInCall
            IrStatementOrigin.NOT_IN ->
                primitiveOp1(
                    startOffset, endOffset,
                    context.irBuiltIns.booleanNotSymbol,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCL,
                    irInCall
                )
            else -> error("Expected 'in' or '!in', got $inOperator")
        }
    }
    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrExpression {
        val ktExpression = ktCondition.expression
        val irExpression = ktExpression!!.genExpr()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        return OperatorExpressionGenerator(statementGenerator).generateEquality(
            startOffset, endOffset, IrStatementOrigin.EQEQ,
            irSubject.loadAt(startOffset, startOffset), irExpression,
            context.bindingContext[BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, ktExpression]
        )
    }

     /*
    override fun visitDotQualifiedExpression(
        expression: KtDotQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = false
        }
    }

    override fun visitSafeQualifiedExpression(
        expression: KtSafeQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = true
        }
    }*/
*/
}