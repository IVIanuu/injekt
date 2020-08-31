package com.ivianuu.injekt.compiler.ast.psi

/*
fun generateForLoop(ktFor: KtForExpression): IrExpression {
    val ktLoopParameter = ktFor.loopParameter
    val ktLoopDestructuringDeclaration = ktFor.destructuringDeclaration
    if (ktLoopParameter == null && ktLoopDestructuringDeclaration == null) {
        throw AssertionError("Either loopParameter or destructuringParameter should be present:\n${ktFor.text}")
    }

    val ktLoopRange = ktFor.loopRange!!
    val ktForBody = ktFor.body
    val iteratorResolvedCall = getOrFail(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, ktLoopRange)
    val hasNextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, ktLoopRange)
    val nextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, ktLoopRange)

    val callGenerator = CallGenerator(statementGenerator)

    val startOffset = ktFor.startOffsetSkippingComments
    val endOffset = ktFor.endOffset

    val irForBlock = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP)

    val iteratorCall = statementGenerator.pregenerateCall(iteratorResolvedCall)
    val irIteratorCall = callGenerator.generateCall(ktLoopRange, iteratorCall, IrStatementOrigin.FOR_LOOP_ITERATOR)
    val irIterator = scope.createTemporaryVariable(irIteratorCall, "iterator", origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR)
    val iteratorValue = VariableLValue(context, irIterator)
    irForBlock.statements.add(irIterator)

    val irInnerWhile = IrWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
    irInnerWhile.label = getLoopLabel(ktFor)
    statementGenerator.bodyGenerator.putLoop(ktFor, irInnerWhile)
    irForBlock.statements.add(irInnerWhile)

    val hasNextCall = statementGenerator.pregenerateCall(hasNextResolvedCall)
    hasNextCall.setExplicitReceiverValue(iteratorValue)
    val irHasNextCall = callGenerator.generateCall(ktLoopRange, hasNextCall, IrStatementOrigin.FOR_LOOP_HAS_NEXT)
    irInnerWhile.condition = irHasNextCall

    val irInnerBody = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
    irInnerWhile.body = irInnerBody

    val nextCall = statementGenerator.pregenerateCall(nextResolvedCall)
    nextCall.setExplicitReceiverValue(iteratorValue)
    val irNextCall = callGenerator.generateCall(ktLoopRange, nextCall, IrStatementOrigin.FOR_LOOP_NEXT)
    val irLoopParameter =
        if (ktLoopParameter != null && ktLoopDestructuringDeclaration == null) {
            val loopParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktLoopParameter)
            context.symbolTable.declareVariable(
                ktLoopParameter.startOffsetSkippingComments, ktLoopParameter.endOffset, IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                loopParameter, loopParameter.type.toIrType(),
                irNextCall
            )
        } else {
            scope.createTemporaryVariable(irNextCall, "loop_parameter")
        }
    irInnerBody.statements.add(irLoopParameter)

    if (ktLoopDestructuringDeclaration != null) {
        statementGenerator.declareComponentVariablesInBlock(
            ktLoopDestructuringDeclaration,
            irInnerBody,
            VariableLValue(context, irLoopParameter)
        )
    }

    if (ktForBody != null) {
        irInnerBody.statements.add(ktForBody.genExpr())
    }

    return irForBlock
}
*/