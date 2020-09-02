package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.FieldSets.expectActual
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
        impl(constructor) {
            defaultFalse("isPrimary", withGetter = true)
        }

        impl(regularClass)

        impl(anonymousObject)

        impl(typeAlias)

        impl(callableReferenceAccess)

        impl(whileLoop)

        impl(doWhileLoop)

        impl(delegatedConstructorCall) {
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
        }

        impl(block)

        impl(functionCall) {
            kind = OpenClass
        }

        impl(qualifiedAccess)

        noImpl(expressionWithSmartcast)

        impl(getClassCall) {
            default("valueArgument") {
                value = "valueArguments.first()"
                withGetter = true
            }
        }

        impl(property) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }
            default("backingFieldSymbol", "AstBackingFieldSymbol(symbol.callableId)")
            useTypes(field)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            defaultNull(
                "receiverType",
                "initializer",
                "delegate",
                "getter",
                "setter",
                withGetter = true
            )
        }

        impl(enumEntry) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull(
                "receiverType",
                "delegate",
                "getter",
                "setter",
                withGetter = true
            )
        }

        impl(typeOperatorCall)

        impl(assignmentOperatorStatement)

        impl(thisReceiverExpression) {
            defaultNoReceivers()
        }

        impl(anonymousFunction)

        impl(propertyAccessor) {
            default("receiverType") {
                value = "null"
                withGetter = true
            }
            default("isSetter") {
                value = "!isGetter"
                withGetter = true
            }
            useTypes(modalityType)
            kind = OpenClass
        }

        impl(whenSubjectExpression) {
            default("type") {
                value = "whenRef.value.subject!!.type"
                withGetter = true
            }
            useTypes(whenExpressionType)
        }

        impl(backingFieldReference)

        impl(thisReference, "AstExplicitThisReference") {
            default("boundSymbol") {
                value = "null"
                isMutable = true
            }
        }

        impl(thisReference, "AstImplicitThisReference") {
            default("labelName") {
                value = "null"
                withGetter = true
            }
            default("boundSymbol") {
                isMutable = false
            }
        }

        impl(superReference, "AstExplicitSuperReference")

        impl(typeProjection, "AstTypePlaceholderProjection") {
            kind = Object
        }

        impl(valueParameter) {
            kind = OpenClass
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull(
                "getter",
                "setter",
                "initializer",
                "delegate",
                "receiverType",
                withGetter = true
            )
        }

        impl(valueParameter, "AstDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
        }

        impl(namedFunction) {
            kind = OpenClass
        }

        impl(safeCallExpression) {
            useTypes(safeCallCheckedSubjectType)
        }

        impl(checkedSafeCallSubject) {
            useTypes(expressionType)
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
