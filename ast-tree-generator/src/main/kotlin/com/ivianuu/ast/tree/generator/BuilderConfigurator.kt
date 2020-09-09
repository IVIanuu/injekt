package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.AbstractBuilderConfigurator
import com.ivianuu.ast.tree.generator.context.generatedType
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

        val declarationBuilder by builder {
            parents += annotationContainerBuilder
            fields from declaration
        }

        val namedDeclarationBuilder by builder {
            parents += declarationBuilder
            fields from namedDeclaration
        }

        val memberDeclarationBuilder by builder {
            parents += namedDeclarationBuilder
            fields from memberDeclaration
        }

        val expressionBuilder by builder {
            parents += annotationContainerBuilder
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
            defaultLazy("name", "symbol.fqName.shortName()")
            default("classKind", "ClassKind.CLASS")
            defaultFalse("isInline")
            defaultFalse("isCompanion")
            defaultFalse("isFun")
            defaultFalse("isData")
            defaultFalse("isInner")
            openBuilder()
        }

        builder(enumEntry) {
            defaultLazy("name", "symbol.fqName.shortName()")
        }

        val baseQualifiedAccessBuilder by builder {
            fields from baseQualifiedAccess without listOf("callee")
        }

        val callBuilder by builder {
            fields from call without listOf("callee")
        }

        val loopBuilder by builder {
            fields from loop
        }

        val functionBuilder by builder {
            parents += annotationContainerBuilder
            fields from function without listOf(
                "symbol",
                "dispatchReceiverType",
                "extensionReceiverType",
                "typeParameters"
            )
        }

        val loopJumpBuilder by builder {
            fields from loopJump without "type"
        }

        val abstractConstructorBuilder by builder {
            parents += functionBuilder
            fields from constructor without listOf("isPrimary")
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

        builder(anonymousInitializer) {
            parents += declarationBuilder
        }

        builder(anonymousObject) {
            parents += classBuilder
        }

        builder(typeAlias) {
            parents + memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            defaultLazy("name", "symbol.fqName.shortName()")
        }

        builder(block) {
            defaultLazy("type", "(statements.lastOrNull() as? AstExpression)?.type ?: context.builtIns.unitType")
            useTypes(expression)
        }

        builder(callableReference) {
            parents += baseQualifiedAccessBuilder
            defaultNoReceivers()
            defaultFalse("hasQuestionMarkAtLHS")
        }

        builder(whileLoop) {
            parents += loopBuilder
        }

        builder(doWhileLoop) {
            parents += loopBuilder
        }

        builder(forLoop) {
            parents += loopBuilder
        }

        builder(delegatedConstructorCall) {
            parents += callBuilder
            defaultLazy("type", "callee.owner.returnType")
            defaultNull("dispatchReceiver")
        }

        builder(functionCall) {
            parents += baseQualifiedAccessBuilder
            parents += callBuilder
            defaultLazy("type", "callee.owner.returnType")
            defaultNoReceivers()
            openBuilder()
        }

        builder(qualifiedAccess) {
            parents += baseQualifiedAccessBuilder
            defaultLazy("type", "(callee as? AstCallableSymbol<*>)?.owner?.returnType ?: error(\"type must be specified\")")
            useTypes(callableSymbolType)
            defaultNoReceivers()
        }

        builder(property) {
            parents += memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            defaultLazy("name", "symbol.fqName.shortName()")
            defaultNull("getter", "setter")
            defaultFalse("isVar")
            defaultFalse("isLocal")
            defaultFalse("isConst")
            defaultFalse("isLateinit")
            defaultFalse("isInline")
        }

        builder(variableAssignment) {
            parents += baseQualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(typeOperation) {
            defaultLazy("type",
                """
                    when (operator) {
                        AstTypeOperator.AS -> typeOperand
                        AstTypeOperator.SAFE_AS -> typeOperand.makeNullable()
                        else -> context.builtIns.booleanType
                    }
                """
            )
            useTypes(generatedType("types", "makeNullable"))
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            default("returnType", "context.builtIns.unitType")
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body")
            defaultFalse("isSetter")
            defaultLazy("name", "if (isSetter) Name.special(\"<setter>\") else Name.special(\"<getter>\")")
        }

        builder(breakExpression) {
            parents += loopJumpBuilder
        }

        builder(continueExpression) {
            parents += loopJumpBuilder
        }

        builder(valueParameter) {
            parents += namedDeclarationBuilder
            openBuilder()
            defaultFalse("isCrossinline", "isNoinline", "isVararg")
            defaultNull("correspondingProperty")
            defaultLazy("name", "symbol.fqName.shortName()")
        }

        builder(namedFunction) {
            parents += functionBuilder
            parents + memberDeclarationBuilder
            parents += typeParametersOwnerBuilder
            defaultLazy("name", "symbol.fqName.shortName()")
            defaultNull("body")
            defaultFalse("isSuspend")
            defaultFalse("isOperator")
            defaultFalse("isInfix")
            defaultFalse("isInline")
            defaultFalse("isTailrec")
            default("returnType", "context.builtIns.unitType")
            openBuilder()
        }

        builder(typeParameter) {
            parents += namedDeclarationBuilder
            defaultFalse("isReified")
            default("variance", "Variance.INVARIANT")
        }

        builder(typeProjectionWithVariance) {
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
        configureFieldInAllLeafBuilders(field = "attributes") {
            default(it, "AstDeclarationAttributes()")
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
