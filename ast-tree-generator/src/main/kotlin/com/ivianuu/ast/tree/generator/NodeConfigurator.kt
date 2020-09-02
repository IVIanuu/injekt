/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.FieldSets.annotations
import com.ivianuu.ast.tree.generator.FieldSets.valueArguments
import com.ivianuu.ast.tree.generator.FieldSets.body
import com.ivianuu.ast.tree.generator.FieldSets.classKind
import com.ivianuu.ast.tree.generator.FieldSets.isCompanion
import com.ivianuu.ast.tree.generator.FieldSets.isConst
import com.ivianuu.ast.tree.generator.FieldSets.isData
import com.ivianuu.ast.tree.generator.FieldSets.declarations
import com.ivianuu.ast.tree.generator.FieldSets.expectActual
import com.ivianuu.ast.tree.generator.FieldSets.isExternal
import com.ivianuu.ast.tree.generator.FieldSets.files
import com.ivianuu.ast.tree.generator.FieldSets.isFun
import com.ivianuu.ast.tree.generator.FieldSets.isInfix
import com.ivianuu.ast.tree.generator.FieldSets.initializer
import com.ivianuu.ast.tree.generator.FieldSets.isInline
import com.ivianuu.ast.tree.generator.FieldSets.isInner
import com.ivianuu.ast.tree.generator.FieldSets.isLateinit
import com.ivianuu.ast.tree.generator.FieldSets.modality
import com.ivianuu.ast.tree.generator.FieldSets.name
import com.ivianuu.ast.tree.generator.FieldSets.isOperator
import com.ivianuu.ast.tree.generator.FieldSets.receivers
import com.ivianuu.ast.tree.generator.FieldSets.superTypes
import com.ivianuu.ast.tree.generator.FieldSets.isSuspend
import com.ivianuu.ast.tree.generator.FieldSets.symbol
import com.ivianuu.ast.tree.generator.FieldSets.symbolWithPackage
import com.ivianuu.ast.tree.generator.FieldSets.isTailrec
import com.ivianuu.ast.tree.generator.FieldSets.typeArguments
import com.ivianuu.ast.tree.generator.FieldSets.typeParameters
import com.ivianuu.ast.tree.generator.FieldSets.typeField
import com.ivianuu.ast.tree.generator.FieldSets.visibility
import com.ivianuu.ast.tree.generator.context.AbstractFieldConfigurator
import com.ivianuu.ast.tree.generator.model.AbstractElement
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.ElementWithArguments
import com.ivianuu.ast.tree.generator.model.SimpleTypeArgument
import com.ivianuu.ast.tree.generator.model.booleanField
import com.ivianuu.ast.tree.generator.model.field
import com.ivianuu.ast.tree.generator.model.fieldList
import com.ivianuu.ast.tree.generator.model.stringField
import com.ivianuu.ast.tree.generator.model.withTransform

object NodeConfigurator : AbstractFieldConfigurator<AstTreeBuilder>(AstTreeBuilder) {
    fun configureFields() = configure {
        annotationContainer.configure {
            +annotations
        }

        symbolOwner.configure {
            withArg("E", symbolOwner, declaration)
            +symbolWithPackage("ast.symbols", "AbstractAstSymbol", "E")
        }

        typeParameter.configure {
            +symbol(typeParameterSymbolType.type)
        }

        typeParametersOwner.configure {
            +typeParameters.withTransform()
        }

        declaration.configure {
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
        }

        typedDeclaration.configure {
            +field("returnType", type, withReplace = true).withTransform()
        }

        callableDeclaration.configure {
            withArg("F", "AstCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("receiverType", type, nullable = true, withReplace = true).withTransform()
            +symbol("AstCallableSymbol", "F")
        }

        callableMemberDeclaration.configure {
            withArg("F", "AstCallableMemberDeclaration<F>")
            parentArg(callableDeclaration, "F", "F")
        }

        function.configure {
            withArg("F", "AstFunction<F>")
            parentArg(callableDeclaration, "F", "F")
            +symbol("AstFunctionSymbol", "F")
            +fieldList(valueParameter, withReplace = true).withTransform()
            +body(nullable = true).withTransform()
        }

        expression.configure {
            +typeField
            +annotations
        }

        call.configure {
            +valueArguments
        }

        block.configure {
            +fieldList(statement).withTransform()
            +typeField
            needTransformOtherChildren()
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression).withTransform()
            +field("rightOperand", expression).withTransform()
            +field("kind", operationKindType)
            needTransformOtherChildren()
        }

        jump.configure {
            withArg("E", targetElement)
            +field("target", jumpTargetType.withArgs("E"))
        }

        loopJump.configure {
            parentArg(jump, "E", loop)
        }

        returnExpression.configure {
            parentArg(jump, "E", function.withArgs("F" to "*"))
            +field("result", expression).withTransform()
            needTransformOtherChildren()
        }

        label.configure {
            +stringField("name")
        }

        loop.configure {
            +field(block).withTransform()
            +field("condition", expression).withTransform()
            +field(label, nullable = true)
            needTransformOtherChildren()
        }

        whileLoop.configure {
            +field("condition", expression).withTransform()
            +field(block).withTransform()
        }

        catchClause.configure {
            +field("parameter", valueParameter).withTransform()
            +field(block).withTransform()
            needTransformOtherChildren()
        }

        tryExpression.configure {
            +field("tryBlock", block).withTransform()
            +fieldList("catches", catchClause).withTransform()
            +field("finallyBlock", block, nullable = true).withTransform()
            needTransformOtherChildren()
        }

        elvisExpression.configure {
            +field("lhs", expression).withTransform()
            +field("rhs", expression).withTransform()
        }

        qualifiedAccess.configure {
            +typeArguments.withTransform()
            +receivers
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"), withReplace = true)
            +field("value", "T", null)
        }

        functionCall.configure {
            +field("callee", functionSymbolType)
        }

        comparisonExpression.configure {
            +field("operation", operationType)
            +field("compareToCall", functionCall)
        }

        typeOperatorCall.configure {
            +field("operation", operationType)
            +field("conversionType", type).withTransform()
            needTransformOtherChildren()
        }

        assignmentOperatorStatement.configure {
            +field("operation", operationType)
            +field("leftArgument", expression).withTransform()
            +field("rightArgument", expression).withTransform()
        }

        equalityOperatorCall.configure {
            +field("operation", operationType)
        }

        whenBranch.configure {
            +field("condition", expression).withTransform()
            +field("result", block).withTransform()
            needTransformOtherChildren()
        }

        classLikeDeclaration.configure {
            withArg("F", "AstClassLikeDeclaration<F>")
            parentArg(symbolOwner, "F", "F")
            +symbol("AstClassLikeSymbol", "F")
        }

        klass.configure {
            withArg("F", "AstClass<F>")
            parentArg(classLikeDeclaration, "F", "F")
            +symbol("AstClassSymbol", "F")
            +classKind
            +superTypes(withReplace = true).withTransform()
            +declarations.withTransform()
            +annotations
        }

        regularClass.configure {
            parentArg(klass, "F", regularClass)
            +name
            +visibility
            +expectActual
            +modality
            +symbol("AstRegularClassSymbol")
            +superTypes(withReplace = true)
            +isInline
            +isCompanion
            +isFun
            +isData
            +isInner
            +isExternal
        }

        anonymousObject.configure {
            parentArg(klass, "F", anonymousObject)
            +symbol("AstAnonymousObjectSymbol")
        }

        typeAlias.configure {
            +typeParameters
            parentArg(classLikeDeclaration, "F", typeAlias)
            +name
            +visibility
            +expectActual
            +modality
            +symbol("AstTypeAliasSymbol")
            +field("expandedType", type, withReplace = true)
            +annotations
        }

        anonymousFunction.configure {
            parentArg(function, "F", anonymousFunction)
            +symbol("AstAnonymousFunctionSymbol")
            +field(label, nullable = true)
        }

        typeParameter.configure {
            parentArg(symbolOwner, "F", typeParameter)
            +name
            +symbol("AstTypeParameterSymbol")
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", type, withReplace = true)
            +annotations
        }

        namedFunction.configure {
            parentArg(function, "F", namedFunction)
            parentArg(callableMemberDeclaration, "F", namedFunction)
            +name
            +visibility
            +expectActual
            +modality
            +isExternal
            +isSuspend
            +isOperator
            +isInfix
            +isInline
            +isTailrec
            +symbol("AstFunctionSymbol<AstNamedFunction>")
            +annotations
            +typeParameters
        }

        property.configure {
            parentArg(variable, "F", property)
            parentArg(callableMemberDeclaration, "F", property)
            +symbol("AstPropertySymbol")
            +field("backingFieldSymbol", backingFieldSymbolType)
            +booleanField("isLocal")
            +visibility
            +expectActual
            +modality
            +isInline
            +isConst
            +isLateinit
        }

        propertyAccessor.configure {
            parentArg(function, "F", propertyAccessor)
            +symbol("AstPropertyAccessorSymbol")
            +booleanField("isGetter")
            +booleanField("isSetter")
        }

        constructor.configure {
            parentArg(function, "F", constructor)
            parentArg(callableMemberDeclaration, "F", constructor)
            +annotations
            +symbol("AstConstructorSymbol")
            +field(
                "delegatedConstructor",
                delegatedConstructorCall,
                nullable = true
            ).withTransform()
            +body(nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedType", type, withReplace = true)
            +field("dispatchReceiver", expression, nullable = true).withTransform()
            generateBooleanFields("this", "super")
        }

        valueParameter.configure {
            parentArg(variable, "F", valueParameter)
            +symbol("AstValueParameterSymbol")
            +field("defaultValue", expression, nullable = true)
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            withArg("F", variable)
            parentArg(callableDeclaration, "F", "F")
            +name
            +symbol("AstVariableSymbol", "F")
            +initializer.withTransform()
            +field("delegate", expression, nullable = true).withTransform()
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true).withTransform()
            +field("setter", propertyAccessor, nullable = true).withTransform()
            +annotations
            needTransformOtherChildren()
        }

        enumEntry.configure {
            parentArg(variable, "F", enumEntry)
            parentArg(callableMemberDeclaration, "F", enumEntry)
        }

        field.configure {
            parentArg(variable, "F", field)
            parentArg(callableMemberDeclaration, "F", field)
        }

        anonymousInitializer.configure {
            parentArg(symbolOwner, "E", anonymousInitializer)
            +body(nullable = true)
            +symbol(anonymousInitializerSymbolType.type)
        }

        moduleFragment.configure {
            +stringField("name")
            +files.withTransform()
        }

        file.configure {
            +declarations.withTransform()
            +stringField("name")
            +field("packageFqName", fqNameType)
        }

        classReference.configure {
            +field("classType", type)
        }

        expressionWithSmartcast.configure {
            +field("originalExpression", qualifiedAccess)
            +field(
                "typesFromSmartCast",
                "Collection<AstType>",
                null,
                customType = type
            )
            +field("originalType", type)
        }

        safeCallExpression.configure {
            +field("receiver", expression).withTransform()
            // Special node that might be used as a reference to receiver of a safe call after null check
            +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
            // One that uses checkedReceiver as a receiver
            +field("regularQualifiedAccess", qualifiedAccess, withReplace = true).withTransform()
        }

        checkedSafeCallSubject.configure {
            +field("originalReceiverRef", safeCallOriginalReceiverReferenceType)
        }

        callableReferenceAccess.configure {
            +field("callee", callableSymbolType, withReplace = true).withTransform()
            +booleanField("hasQuestionMarkAtLHS", withReplace = true)
        }

        getClassCall.configure {
            +field("valueArgument", expression)
        }

        wrappedArgumentExpression.configure {
            +booleanField("isSpread")
        }

        namedArgumentExpression.configure {
            +name
        }

        varargArgumentsExpression.configure {
            +fieldList("arguments", expression)
            +field("varargElementType", type)
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("left", expression)
            +field("right", expression).withTransform()
        }

        whenSubjectExpression.configure {
            +field("whenRef", whenRefType)
        }

        wrappedExpression.configure {
            +field(expression)
        }

        wrappedDelegateExpression.configure {
            +field("delegateProvider", expression)
        }

        backingFieldReference.configure {
            +field("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +stringField("labelName", nullable = true)
            +field("superType", type, withReplace = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field(
                "boundSymbol",
                abstractAstSymbolType,
                "*",
                nullable = true,
                withReplace = true
            )
        }

        type.configure {
            +annotations
            +booleanField("isMarkedNullable")
        }

        simpleType.configure {
            +field("classifier", classifierSymbolType)
            +fieldList("arguments", typeProjection)
        }

        thisReceiverExpression.configure {
            +field("calleeReference", thisReference)
        }

        whenExpression.configure {
            +field("subject", expression, nullable = true).withTransform()
            +field("subjectVariable", variable.withArgs("F" to "*"), nullable = true)
            +fieldList("branches", whenBranch).withTransform()
            +booleanField("isExhaustive", withReplace = true)
            needTransformOtherChildren()
        }

        typeProjectionWithVariance.configure {
            +field(type)
            +field(varianceType)
        }
    }
}

private fun Element.withArgs(vararg replacements: Pair<String, String>): AbstractElement {
    val replaceMap = replacements.toMap()
    val newArguments =
        typeArguments.map { replaceMap[it.name]?.let { SimpleTypeArgument(it, null) } ?: it }
    return ElementWithArguments(this, newArguments)
}
