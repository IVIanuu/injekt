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
            +typeParameters
        }

        declaration.configure {
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
        }

        typedDeclaration.configure {
            +field("returnType", type)
        }

        callableDeclaration.configure {
            withArg("F", "AstCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("receiverType", type, nullable = true)
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
            +fieldList(valueParameter)
            +body(nullable = true)
        }

        expression.configure {
            +typeField
            +annotations
        }

        call.configure {
            +valueArguments
        }

        block.configure {
            +fieldList(statement)
            +typeField
            needTransformOtherChildren()
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression)
            +field("rightOperand", expression)
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
            +field("result", expression)
            needTransformOtherChildren()
        }

        label.configure {
            +stringField("name")
        }

        loop.configure {
            +field(block)
            +field("condition", expression)
            +field(label, nullable = true)
            needTransformOtherChildren()
        }

        whileLoop.configure {
            +field("condition", expression)
            +field(block)
        }

        catchClause.configure {
            +field("parameter", valueParameter)
            +field(block)
            needTransformOtherChildren()
        }

        tryExpression.configure {
            +field("tryBlock", block)
            +fieldList("catches", catchClause)
            +field("finallyBlock", block, nullable = true)
            needTransformOtherChildren()
        }

        elvisExpression.configure {
            +field("lhs", expression)
            +field("rhs", expression)
        }

        qualifiedAccess.configure {
            +typeArguments
            +receivers
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"))
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
            +field("conversionType", type)
            needTransformOtherChildren()
        }

        assignmentOperatorStatement.configure {
            +field("operation", operationType)
            +field("leftArgument", expression)
            +field("rightArgument", expression)
        }

        equalityOperatorCall.configure {
            +field("operation", operationType)
        }

        whenBranch.configure {
            +field("condition", expression)
            +field("result", block)
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
            +superTypes()
            +declarations
            +annotations
        }

        regularClass.configure {
            parentArg(klass, "F", regularClass)
            +name
            +visibility
            +expectActual
            +modality
            +symbol("AstRegularClassSymbol")
            +superTypes()
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
            +field("expandedType", type)
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
            +fieldList("bounds", type)
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
            )
            +body(nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedType", type)
            +field("dispatchReceiver", expression, nullable = true)
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
            +initializer
            +field("delegate", expression, nullable = true)
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true)
            +field("setter", propertyAccessor, nullable = true)
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
            +files
        }

        file.configure {
            +declarations
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
            +field("receiver", expression)
            // Special node that might be used as a reference to receiver of a safe call after null check
            +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
            // One that uses checkedReceiver as a receiver
            +field("regularQualifiedAccess", qualifiedAccess)
        }

        checkedSafeCallSubject.configure {
            +field("originalReceiverRef", safeCallOriginalReceiverReferenceType)
        }

        callableReferenceAccess.configure {
            +field("callee", callableSymbolType)
            +booleanField("hasQuestionMarkAtLHS")
        }

        getClassCall.configure {
            +field("valueArgument", expression)
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("left", expression)
            +field("right", expression)
        }

        whenSubjectExpression.configure {
            +field("whenRef", whenRefType)
        }

        vararg.configure {
            +fieldList("elements", varargElement)
        }

        backingFieldReference.configure {
            +field("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +stringField("labelName", nullable = true)
            +field("superType", type)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field(
                "boundSymbol",
                abstractAstSymbolType,
                "*",
                nullable = true
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
            +field("subject", expression, nullable = true)
            +field("subjectVariable", variable.withArgs("F" to "*"), nullable = true)
            +fieldList("branches", whenBranch)
            +booleanField("isExhaustive")
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
