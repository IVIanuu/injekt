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

        impl(anonymousObject)

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
            defaultFalse("isVar", withGetter = true)
            defaultNull(
                "receiverType",
                "delegate",
                "getter",
                "setter",
                withGetter = true
            )
        }

        impl(propertyAccessor) {
            default("receiverType") {
                value = "null"
                withGetter = true
            }
            useTypes(modalityType)
            kind = OpenClass
        }

        impl(thisReference) {
            default("boundSymbol") {
                value = "null"
                isMutable = true
            }
        }

        impl(valueParameter) {
            kind = OpenClass
            defaultNull(
                "getter",
                "setter",
                "initializer",
                "delegate",
                "receiverType",
                withGetter = true
            )
        }

        impl(namedFunction) {
            kind = OpenClass
        }

        impl(starProjection) {
            kind = Object
        }

        impl(delegatedType) {
            default("isMarkedNullable") {
                value = "type.isMarkedNullable"
                withGetter = true
            }
            default("annotations") {
                value = "type.annotations"
                withGetter = true
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
