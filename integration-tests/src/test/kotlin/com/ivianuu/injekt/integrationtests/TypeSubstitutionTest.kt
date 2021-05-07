/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import io.kotest.matchers.*
import io.kotest.matchers.maps.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class TypeSubstitutionTest {
    @Test
    fun testGetSubstitutionMap() = withTypeCheckerContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(stringType, superType)
        map[superType.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithQualifierInSuperType() = withTypeCheckerContext {
        val qualifier = ClassifierRef(
            "MyQualifier",
            FqName("MyQualifier"),
            typeParameters = listOf(
                ClassifierRef(
                    key = "MyQualifier.T",
                    fqName = FqName("MyQualifier.T")
                )
            )
        ).defaultType
        val classType = classType(any.qualified(qualifier.typeWith(stringType)))
        val typeParameter = typeParameter()
        val superType = typeParameter(nullableAny.qualified(qualifier.typeWith(typeParameter)))
        val map = getSubstitutionMap(classType, superType)
        map[superType.classifier] shouldBe classType
        map[typeParameter.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithExtraTypeParameter() = withTypeCheckerContext {
        val typeParameterU = typeParameter(fqName = FqName("U"))
        val typeParameterS = typeParameter(listType.typeWith(typeParameterU), fqName = FqName("S"))
        val typeParameterT = typeParameter(typeParameterS, fqName = FqName("T"))
        val substitutionType = listType.typeWith(stringType)
        val map = getSubstitutionMap(substitutionType, typeParameterT)
        map[typeParameterT.classifier] shouldBe substitutionType
        map[typeParameterS.classifier] shouldBe substitutionType
        map[typeParameterU.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithNestedGenerics() = withTypeCheckerContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(listType.typeWith(stringType), listType.typeWith(superType))
        map[superType.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithQualifiers() = withTypeCheckerContext {
        val unqualifiedSuperType = typeParameter()
        val qualifiedSuperType = unqualifiedSuperType.qualified(qualifier1)
        val substitutionType = stringType.qualified(qualifier1)
        val map = getSubstitutionMap(substitutionType, qualifiedSuperType)
        map[unqualifiedSuperType.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithGenericQualifierArguments() = withTypeCheckerContext {
        val typeParameter1 = typeParameter()
        val typeParameter2 = typeParameter()
        val qualifier = ClassifierRef(
            "MyQualifier",
            FqName("MyQualifier"),
            typeParameters = listOf(
                ClassifierRef(
                    key = "MyQualifier.T",
                    fqName = FqName("MyQualifier.T")
                )
            )
        )
        // @MyQualifier<TP2> TP1
        val superType = typeParameter1.qualified(qualifier.defaultType.typeWith(typeParameter2))
        // @MyQualifier<Int> String
        val substitutionType = stringType.qualified(qualifier.defaultType.typeWith(intType))
        val map = getSubstitutionMap(substitutionType, superType)
        map[typeParameter1.classifier] shouldBe stringType
        map[typeParameter2.classifier] shouldBe intType
    }

    @Test
    fun testGetSubstitutionMapWithSubClass() = withTypeCheckerContext {
        val classType = classType(listType.typeWith(stringType))
        val typeParameter = typeParameter()
        val map = getSubstitutionMap(classType, listType.typeWith(typeParameter))
        map.shouldHaveSize(1)
        map.shouldContain(typeParameter.classifier, stringType)
    }

    @Test
    fun testGetSubstitutionMapWithSameQualifiers() = withTypeCheckerContext {
        val typeParameterS = typeParameter()
        val typeParameterT = typeParameter(typeParameterS.qualified(qualifier1))
        val substitutionType = stringType.qualified(qualifier1)
        val map = getSubstitutionMap(substitutionType, typeParameterT)
        map[typeParameterT.classifier] shouldBe substitutionType
        map[typeParameterS.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithSameQualifiers2() = withTypeCheckerContext {
        val typeParameterS = typeParameter()
        val typeParameterT = typeParameter(typeParameterS.qualified(qualifier1))
        val substitutionType = stringType.qualified(qualifier1, qualifier2)
        val map = getSubstitutionMap(substitutionType, typeParameterT)
        map[typeParameterT.classifier] shouldBe substitutionType
        map[typeParameterS.classifier] shouldBe stringType.qualified(qualifier2)
    }

    @Test
    fun testGetSubstitutionMapWithQualifiedNullableAny() = withTypeCheckerContext {
        val qualifier = ClassifierRef(
            "Trigger",
            FqName("Trigger"),
            typeParameters = listOf(
                ClassifierRef(
                    key = "Trigger.S",
                    fqName = FqName("Trigger.S")
                )
            )
        ).defaultType

        val triggerImplS = typeParameter(fqName = FqName("triggerImpl.S"))
        val triggerImplT = typeParameter(
            nullableAny.qualified(qualifier.typeWith(triggerImplS)),
            fqName = FqName("triggerImpl.T")
        )

        val candidate = stringType.qualified(qualifier.typeWith(intType))

        val map = getSubstitutionMap(candidate, triggerImplT)
        map[triggerImplS.classifier] shouldBe intType
        map[triggerImplT.classifier] shouldBe candidate
    }

    @Test
    fun testGetSubstitutionMapWithQualifiedAny() = withTypeCheckerContext {
        val scopedAnnotation = typeFor(FqName("com.ivianuu.injekt.scope.Scoped"))
        val (scopedImplT, scopedImplU, scopedImplS) = injektContext.memberScopeForFqName(FqName("com.ivianuu.injekt.scope.Scoped.Companion"))!!
            .getContributedFunctions("scopedValue".asNameId(), NoLookupLocation.FROM_BACKEND)
            .single()
            .typeParameters
            .map { it.toClassifierRef(injektContext, null) }
        val appGivenScope = typeFor(FqName("com.ivianuu.injekt.scope.AppGivenScope"))
        val map = getSubstitutionMap(
            stringType.qualified(scopedAnnotation.typeWith(appGivenScope)),
            scopedImplT.defaultType
        )
        map[scopedImplT] shouldBe stringType.qualified(scopedAnnotation.typeWith(appGivenScope))
        map[scopedImplU] shouldBe stringType
        map[scopedImplS] shouldBe appGivenScope
    }

    private fun TypeCheckerTestContext.getSubstitutionMap(
        a: TypeRef,
        b: TypeRef
    ): Map<ClassifierRef, TypeRef> {
        val context = a.buildContext(injektContext, emptyList(), b)
        return context.getSubstitutionMap()
    }
}