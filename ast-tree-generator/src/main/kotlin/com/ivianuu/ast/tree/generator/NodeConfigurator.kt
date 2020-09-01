/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.FieldSets.annotations
import com.ivianuu.ast.tree.generator.FieldSets.arguments
import com.ivianuu.ast.tree.generator.FieldSets.body
import com.ivianuu.ast.tree.generator.FieldSets.calleeReference
import com.ivianuu.ast.tree.generator.FieldSets.classKind
import com.ivianuu.ast.tree.generator.FieldSets.declarations
import com.ivianuu.ast.tree.generator.FieldSets.initializer
import com.ivianuu.ast.tree.generator.FieldSets.modality
import com.ivianuu.ast.tree.generator.FieldSets.name
import com.ivianuu.ast.tree.generator.FieldSets.receivers
import com.ivianuu.ast.tree.generator.FieldSets.returnTypeRef
import com.ivianuu.ast.tree.generator.FieldSets.status
import com.ivianuu.ast.tree.generator.FieldSets.superTypeRefs
import com.ivianuu.ast.tree.generator.FieldSets.symbol
import com.ivianuu.ast.tree.generator.FieldSets.symbolWithPackage
import com.ivianuu.ast.tree.generator.FieldSets.typeArguments
import com.ivianuu.ast.tree.generator.FieldSets.typeParameterRefs
import com.ivianuu.ast.tree.generator.FieldSets.typeParameters
import com.ivianuu.ast.tree.generator.FieldSets.typeRefField
import com.ivianuu.ast.tree.generator.FieldSets.valueParameters
import com.ivianuu.ast.tree.generator.FieldSets.visibility
import com.ivianuu.ast.tree.generator.context.AbstractFieldConfigurator
import com.ivianuu.ast.tree.generator.model.AbstractElement
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.ElementWithArguments
import com.ivianuu.ast.tree.generator.model.SimpleTypeArgument
import com.ivianuu.ast.tree.generator.model.booleanField
import com.ivianuu.ast.tree.generator.model.field
import com.ivianuu.ast.tree.generator.model.fieldList
import com.ivianuu.ast.tree.generator.model.intField
import com.ivianuu.ast.tree.generator.model.stringField
import com.ivianuu.ast.tree.generator.model.withTransform

object NodeConfigurator : AbstractFieldConfigurator<AstTreeBuilder>(AstTreeBuilder) {
    fun configureFields() = configure {
        annotationContainer.configure {
            +annotations
        }

        symbolOwner.configure {
            withArg("E", symbolOwner, declaration)
            +symbolWithPackage("ast.symbols", "AbstractAstBasedSymbol", "E")
        }

        typeParameterRef.configure {
            +symbol(typeParameterSymbolType.type)
        }

        typeParametersOwner.configure {
            +typeParameters.withTransform()
        }

        typeParameterRefsOwner.configure {
            +typeParameterRefs.withTransform()
        }

        resolvable.configure {
            +calleeReference.withTransform()
        }

        declaration.configure {
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
        }

        typedDeclaration.configure {
            +field("returnTypeRef", typeRef, withReplace = true).withTransform()
        }

        callableDeclaration.configure {
            withArg("F", "AstCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("receiverTypeRef", typeRef, nullable = true, withReplace = true).withTransform()
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

        memberDeclaration.configure {
            +typeParameterRefs
            +status.withTransform()
        }

        expression.configure {
            +typeRefField
            +annotations
        }

        argumentList.configure {
            +arguments.withTransform()
        }

        call.configure {
            +field(argumentList, withReplace = true)
        }

        block.configure {
            +fieldList(statement).withTransform()
            +typeRefField
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
            +field("calleeReference", namedReference)
        }

        comparisonExpression.configure {
            +field("operation", operationType)
            +field("compareToCall", functionCall)
        }

        typeOperatorCall.configure {
            +field("operation", operationType)
            +field("conversionTypeRef", typeRef).withTransform()
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
            +superTypeRefs(withReplace = true).withTransform()
            +declarations.withTransform()
            +annotations
        }

        regularClass.configure {
            parentArg(klass, "F", regularClass)
            +name
            +symbol("AstRegularClassSymbol")
            +field("companionObject", regularClass, nullable = true).withTransform()
            +booleanField("hasLazyNestedClassifiers")
            +superTypeRefs(withReplace = true)
        }

        anonymousObject.configure {
            parentArg(klass, "F", anonymousObject)
            +symbol("AstAnonymousObjectSymbol")
        }

        typeAlias.configure {
            +typeParameters
            parentArg(classLikeDeclaration, "F", typeAlias)
            +name
            +symbol("AstTypeAliasSymbol")
            +field("expandedTypeRef", typeRef, withReplace = true)
            +annotations
        }

        anonymousFunction.configure {
            parentArg(function, "F", anonymousFunction)
            +symbol("AstAnonymousFunctionSymbol")
            +field(label, nullable = true)
            +booleanField("isLambda")
            +typeParameters
        }

        typeParameter.configure {
            parentArg(symbolOwner, "F", typeParameter)
            +name
            +symbol("AstTypeParameterSymbol")
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", typeRef, withReplace = true)
            +annotations
        }

        simpleFunction.configure {
            parentArg(function, "F", simpleFunction)
            parentArg(callableMemberDeclaration, "F", simpleFunction)
            +name
            +symbol("AstFunctionSymbol<AstSimpleFunction>")
            +annotations
            +typeParameters
        }

        property.configure {
            parentArg(variable, "F", property)
            parentArg(callableMemberDeclaration, "F", property)
            +symbol("AstPropertySymbol")
            +field("backingFieldSymbol", backingFieldSymbolType)
            +booleanField("isLocal")
            +status
        }

        propertyAccessor.configure {
            parentArg(function, "F", propertyAccessor)
            +symbol("AstPropertyAccessorSymbol")
            +booleanField("isGetter")
            +booleanField("isSetter")
            +status.withTransform()
            +annotations
            +typeParameters
        }

        declarationStatus.configure {
            +visibility
            +modality
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "inline", "tailRec",
                "external", "const", "lateInit", "inner", "companion", "data", "suspend", "static",
                "fromSealedClass", "fromEnumClass", "fun"
            )
        }

        resolvedDeclarationStatus.configure {
            shouldBeAnInterface()
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
            +field("constructedTypeRef", typeRef, withReplace = true)
            +field("dispatchReceiver", expression).withTransform()
            +field("calleeReference", reference, withReplace = true)
            generateBooleanFields("this", "super")
        }

        valueParameter.configure {
            parentArg(variable, "F", valueParameter)
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
            +field("delegateFieldSymbol", delegateFieldSymbolType, "F", nullable = true)
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

        file.configure {
            +fieldList(import).withTransform()
            +declarations.withTransform()
            +stringField("name")
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +booleanField("isAllUnder")
            +field("aliasName", nameType, nullable = true)
        }

        resolvedImport.configure {
            +field("delegate", import)
            +field("packageFqName", fqNameType)
            +field("relativeClassName", fqNameType, nullable = true)
            +field("resolvedClassId", classIdType, nullable = true)
            +field(
                "importedName",
                nameType,
                nullable = true
            )
        }

        annotationCall.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true)
            +field("annotationTypeRef", typeRef).withTransform()
            +field("resolveStatus", annotationResolveStatusType, withReplace = true)
        }

        arraySetCall.configure {
            +field("assignCall", functionCall)
            +field("setGetBlock", block)
            +field("operation", operationType)
            +field("calleeReference", reference, withReplace = true)
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +field("explicitReceiver", expression)
            +intField("componentIndex")
        }

        expressionWithSmartcast.configure {
            +field("originalExpression", qualifiedAccessExpression)
            +field(
                "typesFromSmartCast",
                "Collection<ConeKotlinType>",
                null,
                customType = coneKotlinTypeType
            )
            +field("originalType", typeRef)
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
            +field("calleeReference", namedReference, withReplace = true).withTransform()
            +booleanField("hasQuestionMarkAtLHS", withReplace = true)
        }

        getClassCall.configure {
            +field("argument", expression)
        }

        wrappedArgumentExpression.configure {
            +booleanField("isSpread")
        }

        namedArgumentExpression.configure {
            +name
        }

        varargArgumentsExpression.configure {
            +fieldList("arguments", expression)
            +field("varargElementType", typeRef)
        }

        resolvedQualifier.configure {
            +field("packageFqName", fqNameType)
            +field("relativeClassFqName", fqNameType, nullable = true)
            +field("classId", classIdType, nullable = true)
            +field("symbol", classLikeSymbolType, nullable = true)
            +booleanField("isNullableLHSForCallableReference", withReplace = true)
            +typeArguments.withTransform()
        }

        resolvedReifiedParameterReference.configure {
            +field("symbol", typeParameterSymbolType)
        }

        stringConcatenationCall.configure {
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("lValue", reference)
            +field("rValue", expression).withTransform()
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

        namedReference.configure {
            +name
            +field("candidateSymbol", abstractAstBasedSymbolType, "*", nullable = true)
        }

        resolvedNamedReference.configure {
            +field("resolvedSymbol", abstractAstBasedSymbolType, "*")
        }

        resolvedCallableReference.configure {
            +fieldList("inferredTypeArguments", coneKotlinTypeType)
        }

        delegateFieldReference.configure {
            +field("resolvedSymbol", delegateFieldSymbolType.withArgs("*"))
        }

        backingFieldReference.configure {
            +field("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +stringField("labelName", nullable = true)
            +field("superTypeRef", typeRef, withReplace = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field(
                "boundSymbol",
                abstractAstBasedSymbolType,
                "*",
                nullable = true,
                withReplace = true
            )
        }

        typeRef.configure {
            +annotations
        }

        resolvedTypeRef.configure {
            +field("type", coneKotlinTypeType)
            +field("delegatedTypeRef", typeRef, nullable = true)
            +booleanField("isSuspend")
        }

        typeRefWithNullability.configure {
            +booleanField("isMarkedNullable")
        }

        userTypeRef.configure {
            +fieldList("qualifier", astQualifierPartType)
        }

        functionTypeRef.configure {
            +field("receiverTypeRef", typeRef, nullable = true)
            +valueParameters
            +returnTypeRef
            +booleanField("isSuspend")
        }

        composedSuperTypeRef.configure {
            +fieldList("superTypeRefs", resolvedTypeRef)
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
            +field(typeRef)
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
