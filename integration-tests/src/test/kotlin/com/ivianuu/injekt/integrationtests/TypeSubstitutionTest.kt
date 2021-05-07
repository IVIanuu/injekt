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
        val qualifiedSuperType = qualifier1.wrap(unqualifiedSuperType)
        val substitutionType = qualifier1.wrap(stringType)
        val map = getSubstitutionMap(substitutionType, qualifiedSuperType)
        map[unqualifiedSuperType.classifier] shouldBe stringType
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
        val typeParameterT = typeParameter(qualifier1.wrap(typeParameterS))
        val substitutionType = qualifier1.wrap(stringType)
        val map = getSubstitutionMap(substitutionType, typeParameterT)
        map[typeParameterT.classifier] shouldBe substitutionType
        map[typeParameterS.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapInScopedLikeScenario() = withTypeCheckerContext {
        val scoped = typeFor(FqName("com.ivianuu.injekt.scope.Scoped"))
        val (scopedT, scopedU, scopedS) = injektContext.memberScopeForFqName(FqName("com.ivianuu.injekt.scope.Scoped.Companion"))!!
            .getContributedFunctions("scopedValue".asNameId(), NoLookupLocation.FROM_BACKEND)
            .single()
            .let { injektContext.callableInfoFor(it, null) }!!
            .typeParameters
            .map { it.toClassifierRef(injektContext, null) }
        val appGivenScope = typeFor(FqName("com.ivianuu.injekt.scope.AppGivenScope"))
        val substitutionType = scoped.wrap(stringType)
            .let {
                it.typeWith(listOf(appGivenScope) + it.arguments.drop(1))
            }
        val map = getSubstitutionMap(substitutionType, scopedT.defaultType)
        map[scopedT] shouldBe substitutionType
        map[scopedU] shouldBe stringType
        map[scopedS] shouldBe appGivenScope
    }

    @Test
    fun testGetSubstitutionMapInInstallElementLikeScenario() = withTypeCheckerContext {
        val (installElementModuleT, installElementModuleU, installElementModuleS) =
            typeFor(FqName("com.ivianuu.injekt.scope.InstallElement.Companion.Module"))
                .classifier
                .typeParameters
        val givenCoroutineScopeElementReturnType = injektContext.memberScopeForFqName(FqName("com.ivianuu.injekt.coroutines"))!!
            .getContributedFunctions("givenCoroutineScopeElement".asNameId(), NoLookupLocation.FROM_BACKEND)
            .single()
            .let { injektContext.callableInfoFor(it, null) }!!
            .type
            .toTypeRef(injektContext, null)
        val givenCoroutineScopeElementS = givenCoroutineScopeElementReturnType.arguments
            .first().classifier
        val map = getSubstitutionMap(
            givenCoroutineScopeElementReturnType,
            installElementModuleT.defaultType
        )
        map[installElementModuleT] shouldBe givenCoroutineScopeElementReturnType
        map[installElementModuleU] shouldBe
                givenCoroutineScopeElementReturnType.typeWith(installElementModuleS.defaultType)
        map[installElementModuleS] shouldBe givenCoroutineScopeElementS
    }

    private fun TypeCheckerTestContext.getSubstitutionMap(
        subType: TypeRef,
        superType: TypeRef,
        staticTypeParameters: List<ClassifierRef> = emptyList()
    ): Map<ClassifierRef, TypeRef> {
        val context = subType.buildContext(injektContext, staticTypeParameters, superType)
        return context.getSubstitutionMap()
    }
}
