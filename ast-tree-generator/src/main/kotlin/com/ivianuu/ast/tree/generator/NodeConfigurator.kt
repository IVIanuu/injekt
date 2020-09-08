/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.FieldSets.annotations
import com.ivianuu.ast.tree.generator.FieldSets.body
import com.ivianuu.ast.tree.generator.FieldSets.classKind
import com.ivianuu.ast.tree.generator.FieldSets.declarations
import com.ivianuu.ast.tree.generator.FieldSets.files
import com.ivianuu.ast.tree.generator.FieldSets.initializer
import com.ivianuu.ast.tree.generator.FieldSets.isCompanion
import com.ivianuu.ast.tree.generator.FieldSets.isConst
import com.ivianuu.ast.tree.generator.FieldSets.isData
import com.ivianuu.ast.tree.generator.FieldSets.isExternal
import com.ivianuu.ast.tree.generator.FieldSets.isFun
import com.ivianuu.ast.tree.generator.FieldSets.isInfix
import com.ivianuu.ast.tree.generator.FieldSets.isInline
import com.ivianuu.ast.tree.generator.FieldSets.isInner
import com.ivianuu.ast.tree.generator.FieldSets.isLateinit
import com.ivianuu.ast.tree.generator.FieldSets.isOperator
import com.ivianuu.ast.tree.generator.FieldSets.isSuspend
import com.ivianuu.ast.tree.generator.FieldSets.isTailrec
import com.ivianuu.ast.tree.generator.FieldSets.modality
import com.ivianuu.ast.tree.generator.FieldSets.name
import com.ivianuu.ast.tree.generator.FieldSets.platformStatus
import com.ivianuu.ast.tree.generator.FieldSets.receivers
import com.ivianuu.ast.tree.generator.FieldSets.superTypes
import com.ivianuu.ast.tree.generator.FieldSets.symbol
import com.ivianuu.ast.tree.generator.FieldSets.symbolWithPackage
import com.ivianuu.ast.tree.generator.FieldSets.typeArguments
import com.ivianuu.ast.tree.generator.FieldSets.typeField
import com.ivianuu.ast.tree.generator.FieldSets.typeParameters
import com.ivianuu.ast.tree.generator.FieldSets.valueArguments
import com.ivianuu.ast.tree.generator.FieldSets.visibility
import com.ivianuu.ast.tree.generator.context.AbstractFieldConfigurator
import com.ivianuu.ast.tree.generator.context.type
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
            +symbolWithPackage("ast.symbols", "AstSymbol", "E")
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
        declarationContainer.configure {
            +declarations
        }
        namedDeclaration.configure {
            +name
        }
        memberDeclaration.configure {
            +visibility
            +modality
            +platformStatus
        }

        callableDeclaration.configure {
            withArg("F", "AstCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("dispatchReceiverType", type, nullable = true)
            +field("extensionReceiverType", type, nullable = true)
            +field("returnType", type)
            +symbol("AstCallableSymbol", "F")
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
        }

        calleeReference.configure {
            +field(
                "callee",
                astSymbolType,
                "*"
            )
        }

        call.configure {
            +field("callee", functionSymbolType, "*")
            +valueArguments
        }

        block.configure {
            +fieldList(statement)
            +typeField
        }

        jump.configure {
            withArg("E", targetElement)
            +field("target", targetType.withArgs("E"))
        }

        loopJump.configure {
            parentArg(jump, "E", loop)
        }

        returnExpression.configure {
            parentArg(jump, "E", function.withArgs("F" to "*"))
            +field("result", expression)
        }

        loop.configure {
            +field("body", expression)
            +stringField("label", nullable = true)
        }

        whileLoop.configure {
            +field("condition", expression)
            +field("body", expression)
        }

        doWhileLoop.configure {
            +field("condition", expression)
            +field("body", expression)
        }

        forLoop.configure {
            +field("body", expression)
            +field("loopRange", expression)
            +field("loopParameter", valueParameter)
        }

        catchClause.configure {
            +field("parameter", valueParameter)
            +field("body", expression)
        }

        tryExpression.configure {
            +field("tryBody", expression)
            +fieldList("catches", catchClause)
            +field("finallyBody", expression, nullable = true)
        }

        baseQualifiedAccess.configure {
            +typeArguments
            +receivers
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"))
            +field("value", "T", null)
        }

        whenBranch.configure {
            +field("condition", expression)
            +field("result", expression)
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
            +superTypes
            +fieldList("delegateInitializers", delegateInitializer)
        }

        regularClass.configure {
            parentArg(klass, "F", regularClass)
            +symbol("AstRegularClassSymbol")
            +superTypes
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
            +symbol("AstTypeAliasSymbol")
            +field("expandedType", type)
        }

        enumEntry.configure {
            parentArg(klass, "F", enumEntry)
            +symbol("AstEnumEntrySymbol")
        }

        anonymousFunction.configure {
            parentArg(function, "F", anonymousFunction)
            +stringField("label", nullable = true)
            +symbol("AstAnonymousFunctionSymbol")
        }

        typeParameter.configure {
            parentArg(symbolOwner, "F", typeParameter)
            +name
            +symbol("AstTypeParameterSymbol")
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", type)
        }

        namedFunction.configure {
            parentArg(function, "F", namedFunction)
            parentArg(callableDeclaration, "F", namedFunction)
            +isExternal
            +isSuspend
            +isOperator
            +isInfix
            +isInline
            +isTailrec
            +fieldList("overriddenFunctions", namedFunctionSymbolType)
            +symbol("AstNamedFunctionSymbol")
        }

        property.configure {
            parentArg(variable, "F", property)
            parentArg(callableDeclaration, "F", property)
            +symbol("AstPropertySymbol")
            +booleanField("isLocal")
            +isInline
            +isConst
            +isLateinit
            +isExternal
            +fieldList("overriddenProperties", propertySymbolType)
        }

        propertyAccessor.configure {
            parentArg(function, "F", propertyAccessor)
            +symbol("AstPropertyAccessorSymbol")
            +booleanField("isSetter")
        }

        constructor.configure {
            parentArg(function, "F", constructor)
            parentArg(callableDeclaration, "F", constructor)
            +symbol("AstConstructorSymbol")
            +field(
                "delegatedConstructor",
                delegatedConstructorCall,
                nullable = true
            )
            +body(nullable = true)
            +visibility
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("callee", constructorSymbolType)
                .also {
                    it.overriddenTypes += type("ast.symbols", "AstSymbol<*>")
                }
            +field("dispatchReceiver", expression, nullable = true)
            +field("kind", delegatedConstructorCallKindType)
        }

        delegateInitializer.configure {
            +field("delegatedSuperType", type)
            +field("expression", expression)
        }

        valueParameter.configure {
            parentArg(variable, "F", valueParameter)
            +symbol("AstValueParameterSymbol")
            +field("defaultValue", expression, nullable = true)
            generateBooleanFields("crossinline", "noinline", "vararg")
            +field("correspondingProperty", propertySymbolType, nullable = true)
        }

        variable.configure {
            withArg("F", variable)
            parentArg(callableDeclaration, "F", "F")
            +symbol("AstVariableSymbol", "F")
            +initializer
            +field("delegate", expression, nullable = true)
            generateBooleanFields("var")
            +field("getter", propertyAccessor, nullable = true)
            +field("setter", propertyAccessor, nullable = true)
        }

        anonymousInitializer.configure {
            parentArg(symbolOwner, "E", anonymousInitializer)
            +body()
            +symbol(anonymousInitializerSymbolType.type)
        }

        moduleFragment.configure {
            +name
            +files
        }

        packageFragment.configure {

        }

        file.configure {
            +declarations
            +stringField("name")
            +field("packageFqName", fqNameType)
        }

        classReference.configure {
            +field("classifier", classifierSymbolType, "*")
        }

        callableReference.configure {
            +field("callee", callableSymbolType, "*")
            +booleanField("hasQuestionMarkAtLHS")
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("callee", variableSymbol, "*")
            +field("value", expression)
        }

        vararg.configure {
            +fieldList("elements", varargElement)
        }

        spreadElement.configure {
            +field("expression", expression)
        }

        propertyBackingFieldReference.configure {
            +field("property", propertySymbolType)
        }

        superReference.configure {
            +field("superType", type, nullable = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field(
                "boundSymbol",
                astSymbolType,
                "*",
                nullable = true
            )
        }

        whenExpression.configure {
            +fieldList("branches", whenBranch)
        }

        type.configure {
            +field("classifier", classifierSymbolType, "*")
            +fieldList("arguments", typeProjection)
            +booleanField("isMarkedNullable")
            element.equalsExpression = "return this === other || (other is AstType &&\n" +
                    "classifier == other.classifier &&\n" +
                    "isMarkedNullable == other.isMarkedNullable &&\n" +
                    "arguments.size == other.arguments.size &&\n" +
                    "arguments.zip(other.arguments).all { it.first == it.second })"
            element.hashCodeExpression = "var result = classifier.hashCode()\n" +
                    "result += 31 * result + isMarkedNullable.hashCode()\n" +
                    "result += 31 * result + arguments.hashCode()\n" +
                    "return result"
        }

        typeProjectionWithVariance.configure {
            +field(type)
            +field(varianceType)
            element.equalsExpression = "return this === other || (other is AstTypeProjectionWithVariance && " +
                    "type == other.type && " +
                    "variance == other.variance)"
            element.hashCodeExpression = "var result = type.hashCode()\n" +
                    "result += 31 * result + variance.hashCode()\n" +
                    "return result"
        }

        starProjection.configure {
            element.equalsExpression = "return other is AstStarProjection"
        }

        typeOperation.configure {
            +field("operator", typeOperatorType)
            +field("argument", expression)
            +field("typeOperand", type)
        }
    }
}

private fun Element.withArgs(vararg replacements: Pair<String, String>): AbstractElement {
    val replaceMap = replacements.toMap()
    val newArguments =
        typeArguments.map { replaceMap[it.name]?.let { SimpleTypeArgument(it, null) } ?: it }
    return ElementWithArguments(this, newArguments)
}
