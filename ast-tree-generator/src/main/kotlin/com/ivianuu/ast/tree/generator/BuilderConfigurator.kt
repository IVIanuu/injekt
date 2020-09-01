package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.AbstractBuilderConfigurator
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.Implementation
import com.ivianuu.ast.tree.generator.model.IntermediateBuilder
import com.ivianuu.ast.tree.generator.model.LeafBuilder
import com.ivianuu.ast.tree.generator.util.traverseParents

object BuilderConfigurator : AbstractBuilderConfigurator<AstTreeBuilder>(AstTreeBuilder) {
    fun configureBuilders() = with(astTreeBuilder) {
        val annotationContainerBuilder by builder {
            fields from annotationContainer
        }

        val expressionBuilder by builder {
            fields from expression
        }

        val typeParametersOwnerBuilder by builder {
            fields from typeParametersOwner
        }

        val typeParameterRefsOwnerBuilder by builder {
            fields from typeParameterRefsOwner
        }

        val classBuilder by builder {
            parents += annotationContainerBuilder
            fields from klass without listOf("symbol")
        }

        builder(regularClass) {
            parents += classBuilder
            parents += typeParameterRefsOwnerBuilder
            defaultNull("companionObject")
            openBuilder()
        }

        val qualifiedAccessBuilder by builder {
            fields from qualifiedAccess without "calleeReference"
        }

        val callBuilder by builder {
            fields from call
        }

        val loopBuilder by builder {
            fields from loop
        }

        val functionBuilder by builder {
            parents += annotationContainerBuilder
            fields from function without listOf(
                "symbol",
                "receiverTypeRef",
                "typeParameters"
            )
        }

        val loopJumpBuilder by builder {
            fields from loopJump without "typeRef"
        }

        val abstractConstructorBuilder by builder {
            parents += functionBuilder
            fields from constructor without listOf("isPrimary", "attributes")
        }

        for (constructorType in listOf("AstPrimaryConstructor", "AstConstructorImpl")) {
            builder(constructor, constructorType) {
                parents += abstractConstructorBuilder
                defaultNull("delegatedConstructor")
                defaultNull("body")
            }
        }

        builder(constructor, "AstConstructorImpl") {
            openBuilder()
        }

        builder(field) {
            openBuilder()
        }

        builder(anonymousObject) {
            parents += classBuilder
        }

        builder(typeAlias) {
            parents += typeParametersOwnerBuilder
        }

        builder(annotationCall) {
            parents += callBuilder
            default("argumentList") {
                value = "AstEmptyArgumentList"
            }
            default("resolveStatus", "AstAnnotationResolveStatus.Unresolved")
            useTypes(emptyArgumentListType)
        }

        builder(callableReferenceAccess) {
            parents += qualifiedAccessBuilder
            defaultNull("explicitReceiver")
            defaultNoReceivers()
            defaultFalse("hasQuestionMarkAtLHS")
        }

        builder(componentCall) {
            parents += callBuilder
            defaultNoReceivers(notNullExplicitReceiver = true)
            default("argumentList") {
                value = "AstEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(whileLoop) {
            parents += loopBuilder
            defaultNull("label")
        }

        builder(doWhileLoop) {
            parents += loopBuilder
            defaultNull("label")
        }

        builder(delegatedConstructorCall) {
            parents += callBuilder
            default("argumentList") {
                value = "AstEmptyArgumentList"
            }
            default("dispatchReceiver", "AstNoReceiverExpression")
            useTypes(noReceiverExpressionType)
            useTypes(emptyArgumentListType)
        }

        builder(functionCall) {
            parents += qualifiedAccessBuilder
            parents += callBuilder
            defaultNoReceivers()
            openBuilder()
            default("argumentList") {
                value = "AstEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(qualifiedAccessExpression) {
            parents += qualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(getClassCall) {
            parents += callBuilder
        }

        builder(property) {
            parents += typeParametersOwnerBuilder
            defaultNull("getter", "setter", "delegateFieldSymbol")
        }

        builder(typeOperatorCall) {
            parents += callBuilder
            default("argumentList") {
                value = "AstEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(stringConcatenationCall) {
            parents += callBuilder
        }

        builder(thisReceiverExpression) {
            parents += qualifiedAccessBuilder
        }

        builder(variableAssignment) {
            parents += qualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            defaultNull("label", "body")
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body")
        }

        builder(whenExpression) {
            defaultFalse("isExhaustive")
            default("calleeReference", "AstStubReference")
            useTypes(stubReferenceType)
        }

        builder(resolvedTypeRef) {
            defaultFalse("isSuspend")
            defaultNull("delegatedTypeRef")
            withCopy()
        }

        builder(functionTypeRef) {
            withCopy()
        }

        builder(resolvedFunctionTypeRef) {
            withCopy()
        }

        builder(implicitTypeRef) {
            withCopy()
        }

        builder(composedSuperTypeRef) {
            withCopy()
        }

        builder(breakExpression) {
            parents += loopJumpBuilder
        }

        builder(continueExpression) {
            parents += loopJumpBuilder
        }

        builder(valueParameter, type = "AstValueParameterImpl") {
            openBuilder()
            withCopy()
        }

        builder(valueParameter, type = "AstDefaultSetterValueParameter") {
            defaultNull(
                "defaultValue",
                "initializer",
                "delegate",
                "receiverTypeRef",
                "delegateFieldSymbol",
                "getter",
                "setter"
            )
            defaultFalse("isCrossinline", "isNoinline", "isVararg", "isVar")
            defaultTrue("isVal")
        }

        builder(simpleFunction) {
            parents += functionBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("body")
            openBuilder()
            withCopy()
        }

        builder(tryExpression) {
            default("calleeReference", "AstStubReference")
            useTypes(stubReferenceType)
        }

        builder(elvisExpression) {
            default("calleeReference", "AstStubReference")
            useTypes(stubReferenceType)
        }

        val abstractResolvedQualifierBuilder by builder {
            fields from resolvedQualifier
        }

        builder(resolvedQualifier) {
            parents += abstractResolvedQualifierBuilder
            defaultFalse("isNullableLHSForCallableReference")
        }

        val elementsWithDefaultTypeRef = listOf(
            thisReceiverExpression,
            callableReferenceAccess,
            anonymousObject,
            qualifiedAccessExpression,
            functionCall,
            anonymousFunction,
            whenExpression,
            tryExpression,
            resolvedQualifier,
            resolvedReifiedParameterReference,
            expression to "AstExpressionStub",
            varargArgumentsExpression,
            checkedSafeCallSubject,
            safeCallExpression
        )
        elementsWithDefaultTypeRef.forEach {
            val (element, name) = when (it) {
                is Pair<*, *> -> it.first as Element to it.second as String
                is Element -> it to null
                else -> throw IllegalArgumentException()
            }
            builder(element, name) {
                default("typeRef", "AstImplicitTypeRefImpl()")
                useTypes(implicitTypeRefType)
            }
        }

        noBuilder(constExpression)

        // -----------------------------------------------------------------------

        findImplementationsWithElementInParents(annotationContainer) {
            it.type !in setOf("AstImplicitTypeRefImpl")
        }.forEach {
            it.builder?.parents?.add(annotationContainerBuilder)
        }

        findImplementationsWithElementInParents(expression).forEach {
            it.builder?.parents?.add(expressionBuilder)
        }

    }

    private inline fun findImplementationsWithElementInParents(
        element: Element,
        implementationPredicate: (Implementation) -> Boolean = { true }
    ): Collection<Implementation> {
        return AstTreeBuilder.elements.flatMap { it.allImplementations }
            .mapNotNullTo(mutableSetOf()) {
                if (!implementationPredicate(it)) return@mapNotNullTo null
                var hasAnnotations = false
                if (it.element == element) return@mapNotNullTo null
                it.element.traverseParents {
                    if (it == element) {
                        hasAnnotations = true
                    }
                }
                it.takeIf { hasAnnotations }
            }
    }

    private fun configureFieldInAllLeafBuilders(
        field: String,
        builderPredicate: ((LeafBuilder) -> Boolean)? = null,
        fieldPredicate: ((Field) -> Boolean)? = null,
        init: LeafBuilderConfigurationContext.(field: String) -> Unit
    ) {
        val builders =
            AstTreeBuilder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }
        for (builder in builders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            LeafBuilderConfigurationContext(builder).init(field)
        }
    }

    private fun configureFieldInAllIntermediateBuilders(
        field: String,
        builderPredicate: ((IntermediateBuilder) -> Boolean)? = null,
        fieldPredicate: ((Field) -> Boolean)? = null,
        init: IntermediateBuilderConfigurationContext.(field: String) -> Unit
    ) {
        for (builder in AstTreeBuilder.intermediateBuilders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            IntermediateBuilderConfigurationContext(builder).init(field)
        }
    }

}
