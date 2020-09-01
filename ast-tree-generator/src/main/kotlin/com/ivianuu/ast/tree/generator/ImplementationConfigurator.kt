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
        noImpl(resolvedDeclarationStatus)

        impl(regularClass) {
            defaultFalse("hasLazyNestedClassifiers", withGetter = true)
        }

        impl(anonymousObject)

        impl(typeAlias)

        impl(import)

        impl(resolvedImport) {
            delegateFields(listOf("aliasName", "importedFqName", "isAllUnder"), "delegate")

            default("resolvedClassId") {
                delegate = "relativeClassName"
                delegateCall = "let { ClassId(packageFqName, it, false) }"
                withGetter = true
            }

            default("importedName") {
                delegate = "importedFqName"
                delegateCall = "shortName()"
                withGetter = true
            }

            default("delegate") {
                needAcceptAndTransform = false
            }
        }

        impl(annotationCall) {
            default("typeRef") {
                value = "annotationTypeRef"
                withGetter = true
            }
        }

        impl(arrayOfCall)

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
                "if (isThis) AstExplicitThisReference(null) else AstExplicitSuperReference(null, constructedTypeRef)"
            )
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            useTypes(explicitThisReferenceType, explicitSuperReferenceType)
        }

        impl(expression, "AstElseIfTrueCondition") {
            defaultTypeRefWithSource("AstImplicitBooleanTypeRef")
            useTypes(implicitBooleanTypeRefType)
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
                "receiverTypeRef",
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
                "receiverTypeRef",
                "delegate",
                "getter",
                "setter",
                withGetter = true
            )
        }

        impl(namedArgumentExpression) {
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(lambdaArgumentExpression) {
            default("isSpread") {
                value = "false"
                withGetter = true
            }
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(spreadArgumentExpression) {
            default("isSpread") {
                value = "true"
                withGetter = true
            }
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(comparisonExpression) {
            default("typeRef", "AstImplicitBooleanTypeRef()")
            useTypes(implicitBooleanTypeRefType)
        }

        impl(typeOperatorCall)

        impl(assignmentOperatorStatement)

        impl(equalityOperatorCall) {
            default("typeRef", "AstImplicitBooleanTypeRef()")
            useTypes(implicitBooleanTypeRefType)
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
            defaultTypeRefWithSource("AstImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(stringConcatenationCall) {
            defaultTypeRefWithSource("AstImplicitStringTypeRef")
            useTypes(implicitStringTypeRefType)
        }

        impl(throwExpression) {
            defaultTypeRefWithSource("AstImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(thisReceiverExpression) {
            defaultNoReceivers()
        }

        impl(expression, "AstUnitExpression") {
            defaultTypeRefWithSource("AstImplicitUnitTypeRef")
            useTypes(implicitUnitTypeRefType)
            publicImplementation()
        }

        impl(variableAssignment) {
            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
        }

        impl(propertyAccessor) {
            default("receiverTypeRef") {
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
            default("typeRef") {
                value = "whenRef.value.subject!!.typeRef"
                withGetter = true
            }
            useTypes(whenExpressionType)
        }

        impl(wrappedDelegateExpression) {
            default("typeRef") {
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

        impl(resolvedTypeRef) {
            publicImplementation()
        }

        impl(resolvedFunctionTypeRef) {
            default("delegatedTypeRef") {
                value = "null"
                withGetter = true
            }
        }

        impl(functionTypeRef)
        impl(implicitTypeRef) {
            defaultEmptyList("annotations")
        }

        impl(composedSuperTypeRef)

        impl(reference, "AstStubReference") {
            kind = Object
        }

        impl(typeProjection, "AstTypePlaceholderProjection") {
            kind = Object
        }

        impl(breakExpression) {
            defaultTypeRefWithSource("AstImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(continueExpression) {
            defaultTypeRefWithSource("AstImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
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
                "receiverTypeRef",
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

        noImpl(userTypeRef)
    }

    private fun configureAllImplementations() {
        configureFieldInAllImplementations(
            field = "controlFlowGraphReference",
            implementationPredicate = { it.type != "AstAnonymousFunctionImpl" }
        ) {
            defaultNull(it)
        }

        val implementationWithConfigurableTypeRef = listOf(
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
            field = "typeRef",
            implementationPredicate = { it.type !in implementationWithConfigurableTypeRef },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "AstImplicitTypeRefImpl()")
            useTypes(implicitTypeRefType)
        }

        configureFieldInAllImplementations(
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }
    }
}
