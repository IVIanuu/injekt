package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeImplementationConfigurator
import com.ivianuu.ast.tree.generator.model.Implementation.Kind.Object
import com.ivianuu.ast.tree.generator.model.Implementation.Kind.OpenClass

object ImplementationConfigurator : AbstractAstTreeImplementationConfigurator() {
    fun configureImplementations() {
        configure()
        generateDefaultImplementations(AstTreeBuilder)
        configureAllImplementations()
    }

    private fun configure() = with(AstTreeBuilder) {
        impl(regularClass)

        impl(anonymousObject) {
            default("classKind") {
                value = "ClassKind.CLASS"
                withGetter = true
            }
        }

        impl(anonymousFunction) {
            defaultNull("dispatchReceiverType", withGetter = true)
        }

        impl(typeAlias)

        impl(callableReference)

        impl(whileLoop)

        impl(doWhileLoop)

        impl(delegatedConstructorCall)

        impl(functionCall) {
            kind = OpenClass
        }

        impl(qualifiedAccess)

        impl(property)

        impl(enumEntry) {
            default("classKind") {
                value = "ClassKind.ENUM_ENTRY"
                withGetter = true
            }
            defaultEmptyList("superTypes")
        }

        impl(propertyAccessor) {
            default("dispatchReceiverType") {
                value = "null"
                withGetter = true
            }
            default("extensionReceiverType") {
                value = "null"
                withGetter = true
            }
            default("platformStatus") {
                value = "PlatformStatus.DEFAULT"
                withGetter = true
            }
            default("typeParameters") {
                value = "emptyList()"
                withGetter = true
            }
        }

        impl(thisReference) {
            default("boundSymbol") {
                value = "null"
                isMutable = true
            }
        }

        impl(valueParameter) {
            kind = OpenClass
            default("isVar") {
                value = "false"
            }
            defaultNull(
                "getter",
                "setter",
                "initializer",
                "delegate",
                "dispatchReceiverType",
                "extensionReceiverType",
                withGetter = true
            )
        }

        impl(namedFunction) {
            kind = OpenClass
        }

        val unitTypeExpressions = listOf(
            doWhileLoop,
            whileLoop,
            forLoop,
            variableAssignment
        )

        unitTypeExpressions.forEach {
            impl(it) {
                default("type") {
                    value = "context.builtIns.unitType"
                    withGetter = true
                }
            }
        }

        val nothingTypeExpressions = listOf(
            breakExpression,
            continueExpression,
            returnExpression,
            throwExpression
        )

        nothingTypeExpressions.forEach {
            impl(it) {
                default("type") {
                    value = "context.builtIns.nothingType"
                    withGetter = true
                }
            }
        }
    }

    private fun configureAllImplementations() {
        configureFieldInAllImplementations(
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }
    }
}
