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

        val declarationContainerBuilder by builder {
            fields from declarationContainer
        }

        val namedDeclarationBuilder by builder {
            fields from namedDeclaration
        }

        val memberDeclarationBuilder by builder {
            parents += namedDeclarationBuilder
            fields from memberDeclaration
        }

        val expressionBuilder by builder {
            fields from expression
        }

        val typeParametersOwnerBuilder by builder {
            fields from typeParametersOwner
        }

        val classBuilder by builder {
            parents += annotationContainerBuilder
            fields from klass without listOf("symbol")
        }

        builder(regularClass) {
            parents += classBuilder
            parents + memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            parents += declarationContainerBuilder
            default("classKind", "ClassKind.CLASS")
            defaultFalse("isInline")
            defaultFalse("isCompanion")
            defaultFalse("isFun")
            defaultFalse("isData")
            defaultFalse("isInner")
            openBuilder()
        }

        val baseQualifiedAccessBuilder by builder {
            fields from baseQualifiedAccess without listOf("callee")
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
                "receiverType",
                "typeParameters"
            )
        }

        val loopJumpBuilder by builder {
            fields from loopJump without "type"
        }

        val abstractConstructorBuilder by builder {
            parents += functionBuilder
            fields from constructor without listOf("isPrimary", "attributes")
        }

        val packageFragmentBuilder by builder {
            parents += declarationContainerBuilder
            fields from packageFragment
        }

        builder(file) {
            parents += packageFragmentBuilder
        }

        builder(constructor) {
            parents += abstractConstructorBuilder
            defaultNull("delegatedConstructor")
            defaultNull("body")
            defaultFalse("isPrimary")
        }

        builder(anonymousObject) {
            parents += classBuilder
            default("classKind", "ClassKind.CLASS")
        }

        builder(typeAlias) {
            parents + memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
        }

        builder(callableReference) {
            parents += baseQualifiedAccessBuilder
            defaultNoReceivers()
            defaultFalse("hasQuestionMarkAtLHS")
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
            defaultNull("dispatchReceiver")
        }

        builder(functionCall) {
            parents += baseQualifiedAccessBuilder
            parents += callBuilder
            defaultNoReceivers()
            openBuilder()
        }

        builder(qualifiedAccess) {
            parents += baseQualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(property) {
            parents += memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("getter", "setter")
            defaultFalse("isVar")
            defaultFalse("hasBackingField")
            defaultFalse("isLocal")
            defaultFalse("isConst")
            defaultFalse("isLateinit")
            defaultFalse("isInline")
        }

        builder(variableAssignment) {
            parents += baseQualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            defaultNull("label", "body")
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body")
            defaultFalse("isSetter")
        }

        builder(breakExpression) {
            parents += loopJumpBuilder
        }

        builder(continueExpression) {
            parents += loopJumpBuilder
        }

        builder(valueParameter) {
            openBuilder()
            defaultFalse("isCrossinline", "isNoinline", "isVararg")
        }

        builder(namedFunction) {
            parents += functionBuilder
            parents + memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("body")
            defaultFalse("isSuspend")
            defaultFalse("isOperator")
            defaultFalse("isInfix")
            defaultFalse("isInline")
            defaultFalse("isTailrec")
            openBuilder()
        }

        builder(typeParameter) {
            defaultFalse("isReified")
            default("variance", "Variance.INVARIANT")
        }

        noBuilder(constExpression)

        findImplementationsWithElementInParents(expression).forEach {
            it.builder?.parents?.add(expressionBuilder)
        }

        configureFieldInAllLeafBuilders(field = "isMarkedNullable") {
            defaultFalse(it)
        }

        configureFieldInAllLeafBuilders(field = "origin") {
            default(it, "AstDeclarationOrigin.Source")
        }

        configureFieldInAllLeafBuilders(field = "visibility") {
            default(it, "Visibilities.Public")
            useTypes(visibilitiesType)
        }
        configureFieldInAllLeafBuilders(field = "modality") {
            default(it, "Modality.FINAL")
        }
        configureFieldInAllLeafBuilders(field = "platformStatus") {
            default("platformStatus", "PlatformStatus.DEFAULT")
        }
        configureFieldInAllLeafBuilders(field = "isExternal") {
            defaultFalse(it)
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
