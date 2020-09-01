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

        impl(annotationCall) {
            default("type") {
                value = "annotationType"
                withGetter = true
            }
        }

        impl(callableReferenceAccess)

        impl(componentCall) {
            default(
                "calleeReference",
                "AstSimpleNamedReference(Name.identifier(\"component\$componentIndex\"), null)"
            )
            useTypes(simpleNamedReferenceType, nameType)
            optInToInternals()
        }

        impl(whileLoop)

        impl(doWhileLoop)

        impl(delegatedConstructorCall) {
            default(
                "calleeReference",
                "if (isThis) AstExplicitThisReference(null) else AstExplicitSuperReference(null, constructedType)"
            )
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            useTypes(explicitThisReferenceType, explicitSuperReferenceType)
        }

        impl(expression, "AstElseIfTrueCondition") {
            defaultType("AstImplicitBooleanType")
            useTypes(implicitBooleanTypeType)
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

        impl(comparisonExpression) {
            default("type", "AstImplicitBooleanType()")
            useTypes(implicitBooleanTypeType)
        }

        impl(typeOperatorCall)

        impl(assignmentOperatorStatement)

        impl(equalityOperatorCall) {
            default("type", "AstImplicitBooleanType()")
            useTypes(implicitBooleanTypeType)
        }

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

        impl(returnExpression) {
            defaultType("AstImplicitNothingType")
            useTypes(implicitNothingTypeType)
        }

        impl(stringConcatenationCall) {
            defaultType("AstImplicitStringType")
            useTypes(implicitStringTypeType)
        }

        impl(throwExpression) {
            defaultType("AstImplicitNothingType")
            useTypes(implicitNothingTypeType)
        }

        impl(thisReceiverExpression) {
            defaultNoReceivers()
        }

        impl(expression, "AstUnitExpression") {
            defaultType("AstImplicitUnitType")
            useTypes(implicitUnitTypeType)
            publicImplementation()
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

        impl(breakExpression) {
            defaultType("AstImplicitNothingType")
            useTypes(implicitNothingTypeType)
        }

        impl(continueExpression) {
            defaultType("AstImplicitNothingType")
            useTypes(implicitNothingTypeType)
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

        val implementationWithConfigurableType = listOf(
            "AstTypeProjectionWithVarianceImpl",
            "AstCallableReferenceAccessImpl",
            "AstThisReceiverExpressionImpl",
            "AstAnonymousObjectImpl",
            "AstQualifiedAccessExpressionImpl",
            "AstFunctionCallImpl",
            "AstAnonymousFunctionImpl",
            "AstWhenExpressionImpl",
            "AstTryExpressionImpl",
            "AstCheckNotNullCallImpl",
            "AstResolvedQualifierImpl",
            "AstResolvedReifiedParameterReferenceImpl",
            "AstExpressionStub",
            "AstVarargArgumentsExpressionImpl",
            "AstSafeCallExpressionImpl",
            "AstCheckedSafeCallSubjectImpl",
        )
        configureFieldInAllImplementations(
            field = "type",
            implementationPredicate = { it.type !in implementationWithConfigurableType },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "AstImplicitTypeImpl()")
            useTypes(implicitTypeType)
        }

        configureFieldInAllImplementations(
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }
    }
}
