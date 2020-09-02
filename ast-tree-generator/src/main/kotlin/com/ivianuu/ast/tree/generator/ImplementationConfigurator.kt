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
        impl(constructor) {
            defaultFalse("isPrimary", withGetter = true)
        }

        impl(constructor, "AstPrimaryConstructor") {
            defaultTrue("isPrimary", withGetter = true)
        }

        impl(typeParameterRef, "AstOuterClassTypeParameterRef")
        impl(typeParameterRef, "AstConstructedClassTypeParameterRef")

        noImpl(declarationStatus)

        impl(regularClass) {
            defaultFalse("hasLazyNestedClassifiers", withGetter = true)
        }

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

        impl(expression, "AstElseIfTrueCondition") {
            publicImplementation()
        }

        impl(block)

        val emptyExpressionBlock = impl(block, "AstEmptyExpressionBlock") {
            defaultEmptyList("statements")
            defaultEmptyList("annotations")
            publicImplementation()
        }

        impl(expression, "AstExpressionStub") {
            publicImplementation()
        }

        impl(functionCall) {
            kind = OpenClass
        }

        impl(qualifiedAccessExpression)

        noImpl(expressionWithSmartcast)

        impl(getClassCall) {
            default("argument") {
                value = "argumentList.arguments.first()"
                withGetter = true
            }
        }

        impl(property) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            default("backingFieldSymbol", "AstBackingFieldSymbol(symbol.callableId)")
            useTypes(field, delegateFieldReference)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            defaultNull(
                "delegateFieldSymbol",
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
                "delegateFieldSymbol",
                "receiverType",
                "delegate",
                "getter",
                "setter",
                withGetter = true
            )
        }

        impl(namedArgumentExpression) {
            default("type") {
                delegate = "expression"
            }
        }

        impl(lambdaArgumentExpression) {
            default("isSpread") {
                value = "false"
                withGetter = true
            }
            default("type") {
                delegate = "expression"
            }
        }

        impl(spreadArgumentExpression) {
            default("isSpread") {
                value = "true"
                withGetter = true
            }
            default("type") {
                delegate = "expression"
            }
        }

        impl(typeOperatorCall)

        impl(assignmentOperatorStatement)

        impl(resolvedQualifier) {
            isMutable("packageFqName", "relativeClassFqName", "isNullableLHSForCallableReference")
            default("classId") {
                value = """
                    |relativeClassFqName?.let {
                    |    ClassId(packageFqName, it, false)
                    |}
                """.trimMargin()
                withGetter = true
            }
        }

        impl(resolvedReifiedParameterReference)

        impl(thisReceiverExpression) {
            defaultNoReceivers()
        }

        impl(variableAssignment) {
            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
        }

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

        impl(wrappedDelegateExpression) {
            default("type") {
                delegate = "expression"
            }
        }

        impl(resolvedNamedReference) {
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(resolvedNamedReference, "AstPropertyFromParameterResolvedNamedReference") {
            defaultNull("candidateSymbol", withGetter = true)
            publicImplementation()
        }

        impl(resolvedCallableReference) {
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(namedReference, "AstSimpleNamedReference") {
            kind = OpenClass
        }

        impl(delegateFieldReference) {
            default("name") {
                value = "Name.identifier(\"\\\$delegate\")"
                withGetter = true
            }
        }

        impl(backingFieldReference) {
            default("name") {
                value = "Name.identifier(\"\\\$field\")"
                withGetter = true
            }
        }

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

        impl(reference, "AstStubReference") {
            kind = Object
        }

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
                "delegateFieldSymbol",
                withGetter = true
            )
        }

        impl(valueParameter, "AstDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
        }

        impl(simpleFunction) {
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
            field = "controlFlowGraphReference",
            implementationPredicate = { it.type != "AstAnonymousFunctionImpl" }
        ) {
            defaultNull(it)
        }

        configureFieldInAllImplementations(
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }
    }
}
