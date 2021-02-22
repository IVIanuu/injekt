/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.compiler.resolution.AnnotationRef
import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.resolution.StringValue
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.index.CliIndexStore
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.test.codegen
import junit.framework.Assert.assertEquals
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.junit.Test

class TypeRefTest {

    @Test
    fun testSimpleTypeWithSameClassifierIsAssignable() = withAnalysisContext {
        stringType shouldBeAssignable stringType
    }

    @Test
    fun testSimpleTypeWithDifferentClassifierIsNotAssignable() = withAnalysisContext {
        stringType shouldNotBeAssignable intType
    }

    @Test
    fun testNonNullIsAssignableToNullable() = withAnalysisContext {
        stringType shouldBeAssignable stringType.nullable()
    }

    @Test
    fun testNullableIsNotAssignableToNonNullable() = withAnalysisContext {
        stringType.nullable() shouldNotBeAssignable stringType
    }

    @Test
    fun testMatchingGenericTypeIsAssignable() = withAnalysisContext {
        listType.typeWith(listOf(stringType)) shouldBeAssignable listType
    }

    @Test
    fun testNotMatchingGenericTypeIsNotAssignable() = withAnalysisContext {
        listType.typeWith(stringType) shouldNotBeAssignable listType.typeWith(intType)
    }

    @Test
    fun testAnyTypeIsAssignableToStarProjectedType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType
    }

    @Test
    fun testStarProjectedTypeMatchesNullableType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType.nullable()
    }

    @Test
    fun testStarProjectedTypeMatchesQualifiedType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType.qualified(qualifier1())
    }

    @Test
    fun testTypeAliasIsNotAssignableToExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldNotBeAssignable stringType
    }

    @Test
    fun testTypeAliasIsNotAssignableToOtherTypeAliasOfTheSameExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsAssignableToOtherTypeAliasOfTheSameExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsSubTypeOfExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldBeSubTypeOf stringType
    }

    @Test
    fun testNestedTypeAliasIsSubTypeOfExpandedType() = withAnalysisContext {
        typeAlias(typeAlias(stringType)) shouldBeSubTypeOf stringType
    }

    @Test
    fun testSameComposabilityIsAssignable() = withAnalysisContext {
        composableFunction(0) shouldBeAssignable composableFunction(0)
    }

    @Test
    fun testComposableTypeAliasIsSubTypeOfComposableFunctionUpperBound() = withAnalysisContext {
        typeAlias(composableFunction(0)) shouldBeAssignable typeParameter(composableFunction(0))
    }

    @Test
    fun testSameQualifiersIsAssignable() = withAnalysisContext {
        stringType.qualified(qualifier1()) shouldBeAssignable stringType.qualified(qualifier1())
    }

    @Test
    fun testDifferentQualifiersIsNotAssignable() = withAnalysisContext {
        stringType.qualified(qualifier1()) shouldNotBeAssignable stringType.qualified(qualifier2("a"))
    }

    @Test
    fun testSameQualifiersWithSameArgsIsAssignable() = withAnalysisContext {
        stringType.qualified(qualifier2("a")) shouldBeAssignable
                stringType.qualified(qualifier2("a"))
    }

    @Test
    fun testSameQualifiersWithDifferentArgsIsAssignable() = withAnalysisContext {
        stringType.qualified(qualifier2("a")) shouldNotBeAssignable
                stringType.qualified(qualifier2("b"))
    }

    @Test
    fun testSubTypeOfTypeParameterWithNullableAnyUpperBound() = withAnalysisContext {
        stringType shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableSubTypeOfTypeParameterWithNullableAnyUpperBound() = withAnalysisContext {
        composableFunction(0) shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableIsNotSubTypeOfNonComposable() = withAnalysisContext {
        composableFunction(0) shouldNotBeAssignable typeParameter(function(0))
    }

    @Test
    fun testSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withAnalysisContext {
        stringType shouldBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testNullableSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withAnalysisContext {
        stringType.nullable() shouldNotBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testSubTypeOfTypeParameterWithUpperBound() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType)
    }

    @Test
    fun testSubTypeOfTypeAliasWithNonNullExpandedType() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeAlias(stringType)
    }

    @Test
    fun testSubTypeOfTypeAliasWithNullableExpandedType() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeAlias(stringType.nullable())
    }

    @Test
    fun testSubTypeOfTypeParameterWithNullableUpperBound() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType.nullable())
    }

    @Test
    fun testQualifiedSubTypeOfQualifiedTypeParameter() = withAnalysisContext {
        stringType.qualified(qualifier1()) shouldBeAssignable
                typeParameter(nullable = false).qualified(qualifier1())
    }

    @Test
    fun testNestedQualifiedSubTypeOfNestedQualifiedTypeParameter() = withAnalysisContext {
        listType.typeWith(stringType.qualified(qualifier1())) shouldBeAssignable
                listType.typeWith(typeParameter(nullable = false).qualified(qualifier1()))
    }

    @Test
    fun testUnqualifiedSubTypeOfTypeParameterWithQualifiedUpperBound() = withAnalysisContext {
        stringType shouldNotBeAssignable
                typeParameter(anyNType.qualified(qualifier1()))
    }

    @Test
    fun testNestedUnqualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withAnalysisContext {
            listType.typeWith(stringType) shouldNotBeAssignable
                    listType.typeWith(typeParameter(anyNType.qualified(qualifier1())))
        }

    @Test
    fun testNestedQualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withAnalysisContext {
            listType.typeWith(stringType.qualified(qualifier1())) shouldBeAssignable
                    listType.typeWith(typeParameter(anyNType.qualified(qualifier1())))
        }

    @Test
    fun testQualifiedTypeIsSubTypeOfTypeParameterWithQualifiedUpperBound() = withAnalysisContext {
        val sTypeParameter = typeParameter(listType.typeWith(stringType))
        val tTypeParameter = typeParameter(sTypeParameter.qualified(qualifier1()))
        listType.typeWith(stringType)
            .qualified(qualifier1()) shouldBeSubTypeOf tTypeParameter
    }

    @Test
    fun testQualifiedTypeAliasIsSubTypeOfTypeParameterWithSameQualifiers() = withAnalysisContext {
        typeAlias(
            function(0)
                .copy(isComposable = true)
                .qualified(qualifier1())
        ) shouldBeSubTypeOf typeParameter(
            function(0)
                .copy(isComposable = true)
                .qualified(qualifier1())
        )
    }

    @Test
    fun testQualifiedTypeAliasIsNotSubTypeOfTypeParameterWithOtherQualifiers() = withAnalysisContext {
        typeAlias(
            function(0)
                .copy(isComposable = true)
                .qualified(qualifier1())
        ) shouldNotBeSubTypeOf typeParameter(
                function(0)
                    .copy(isComposable = true)
                    .qualified(qualifier2(""))
            )
    }

    @Test
    fun testTypeAliasIsNotSubTypeOfTypeParameterWithOtherTypeAliasUpperBound() = withAnalysisContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(function(0).typeWith(intType))
        val typeParameter = typeParameter(typeAlias1)
        typeAlias2 shouldNotBeSubTypeOf typeParameter
    }

    @Test
    fun testTypeAliasIsSubTypeOfOtherTypeAlias() = withAnalysisContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(typeAlias1)
        typeAlias2 shouldBeSubTypeOf typeAlias1
    }

    @Test
    fun testTypeAliasIsSubTypeOfTypeParameterWithTypeAliasUpperBound() = withAnalysisContext {
        val superTypeAlias = typeAlias(function(0))
        val typeParameterS = typeParameter(superTypeAlias)
        val typeParameterT = typeParameter(typeParameterS.qualified(qualifier1()))
        val subTypeAlias = typeAlias(superTypeAlias)
        subTypeAlias.qualified(qualifier1()) shouldBeSubTypeOf typeParameterT
    }

    @Test
    fun testGetSubstitutionMap() = withAnalysisContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(declarationStore, listOf(stringType to superType))
        assertEquals(stringType, map[superType.classifier])
    }

    @Test
    fun testGetSubstitutionMapWithNestedGenerics() = withAnalysisContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(declarationStore, listOf(listType.typeWith(stringType) to listType.typeWith(superType)))
        assertEquals(stringType, map[superType.classifier])
    }

    @Test
    fun testGetSubstitutionMapWithQualifiers() = withAnalysisContext {
        val unqualifiedSuperType = typeParameter()
        val qualifiedSuperType = unqualifiedSuperType.qualified(qualifier1())
        val substitutionType = stringType.qualified(qualifier1())
        val map = getSubstitutionMap(declarationStore, listOf(substitutionType to qualifiedSuperType))
        assertEquals(stringType, map[unqualifiedSuperType.classifier])
    }

    @Test
    fun testGetSubstitutionMapWithGenericQualifierArguments() = withAnalysisContext {
        val typeParameter1 = typeParameter()
        val typeParameter2 = typeParameter()
        val qualifier = ClassifierRef(
            FqName("MyQualifier"),
            typeParameters = listOf(
                ClassifierRef(
                    fqName = FqName("MyQualifier.T")
                )
            )
        )
        val superType = typeParameter1.qualified(
            AnnotationRef(
                qualifier.defaultType.typeWith(typeParameter2),
                emptyMap()
            )
        )
        val substitutionType = stringType.qualified(
            AnnotationRef(
                qualifier.defaultType.typeWith(intType),
                emptyMap()
            )
        )
        val map = getSubstitutionMap(declarationStore, listOf(substitutionType to superType))
        assertEquals(stringType, map[typeParameter1.classifier])
        assertEquals(intType, map[typeParameter2.classifier])
    }

    @Test
    fun testGetSubstitutionMapPrefersInput() = withAnalysisContext {
        val typeParameter1 = typeParameter()
        val typeParameter2 = typeParameter(typeParameter1)
        val map = getSubstitutionMap(
            declarationStore,
            listOf(
                listType.typeWith(stringType) to listType.typeWith(typeParameter2),
                charSequenceType to typeParameter1
            )
        )
        assertEquals(charSequenceType, map[typeParameter1.classifier])
        assertEquals(stringType, map[typeParameter2.classifier])
    }

    // todo type parameter multuple upper bounds

    private fun withAnalysisContext(
        block: AnalysisContext.() -> Unit,
    ) {
        codegen(
            """
            
        """,
            config = {
                compilerPlugins += object : ComponentRegistrar {
                    override fun registerProjectComponents(
                        project: MockProject,
                        configuration: CompilerConfiguration,
                    ) {
                        AnalysisHandlerExtension.registerExtension(
                            project,
                            object : AnalysisHandlerExtension {
                                override fun analysisCompleted(
                                    project: Project,
                                    module: ModuleDescriptor,
                                    bindingTrace: BindingTrace,
                                    files: Collection<KtFile>,
                                ): AnalysisResult? {
                                    block(AnalysisContext(module))
                                    return null
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    class AnalysisContext(val module: ModuleDescriptor) {

        val declarationStore = DeclarationStore(CliIndexStore(module), module)

        val anyType = typeFor(StandardNames.FqNames.any.toSafe())
        val anyNType = anyType.copy(isMarkedNullable = true)
        val intType = typeFor(StandardNames.FqNames._int.toSafe())
        val stringType = typeFor(StandardNames.FqNames.string.toSafe())
        val charSequenceType = typeFor(StandardNames.FqNames.charSequence.toSafe())
        val listType = typeFor(StandardNames.FqNames.list)
        val starProjectedType = STAR_PROJECTION_TYPE

        fun composableFunction(parameterCount: Int) = typeFor(
            FqName("kotlin.Function$parameterCount")
        ).copy(isComposable = true)

        fun function(parameterCount: Int) = typeFor(
            FqName("kotlin.Function$parameterCount")
        )

        fun qualifier1() = AnnotationRef(
            typeFor(FqName("com.ivianuu.injekt.test.Qualifier1")),
            emptyMap()
        )

        fun qualifier2(value: String) = AnnotationRef(
            typeFor(FqName("com.ivianuu.injekt.test.Qualifier2")),
            mapOf("value".asNameId() to StringValue(value, stringType))
        )

        private var id = 0

        fun subType(
            vararg superTypes: TypeRef,
            fqName: FqName = FqName("SubType${id}"),
        ) = ClassifierRef(
            fqName = fqName,
            superTypes = superTypes.toList()
        ).defaultType

        fun typeAlias(
            expandedType: TypeRef,
            fqName: FqName = FqName("Alias${id++}"),
        ) = ClassifierRef(
            fqName = fqName,
            expandedType = expandedType,
            isTypeAlias = true
        ).defaultType

        fun typeParameter(
            fqName: FqName = FqName("TypeParameter${id++}"),
            nullable: Boolean = true,
        ): TypeRef =
            typeParameter(upperBounds = *emptyArray(), nullable = nullable, fqName = fqName)

        fun typeParameter(
            vararg upperBounds: TypeRef,
            nullable: Boolean = true,
            fqName: FqName = FqName("TypeParameter${id++}"),
        ) = ClassifierRef(
            fqName = fqName,
            superTypes = listOf(anyType.copy(isMarkedNullable = nullable)) + upperBounds,
            isTypeParameter = true
        ).defaultType

        fun typeFor(fqName: FqName) = module.findClassifierAcrossModuleDependencies(
            ClassId.topLevel(fqName)
        )!!.defaultType.toTypeRef(declarationStore)

        infix fun TypeRef.shouldBeAssignable(other: TypeRef) {
            if (!isAssignableTo(declarationStore, other)) {
                throw AssertionError("'$this' is not assignable to '$other'")
            }
        }

        infix fun TypeRef.shouldNotBeAssignable(other: TypeRef) {
            if (isAssignableTo(declarationStore, other)) {
                throw AssertionError("'$this' is assignable to '$other'")
            }
        }

        infix fun TypeRef.shouldBeSubTypeOf(other: TypeRef) {
            if (!isSubTypeOf(declarationStore, other)) {
                throw AssertionError("'$this' is not sub type of '$other'")
            }
        }

        infix fun TypeRef.shouldNotBeSubTypeOf(other: TypeRef) {
            if (isSubTypeOf(declarationStore, other)) {
                throw AssertionError("'$this' is sub type of '$other'")
            }
        }
    }

    fun TypeRef.nullable() = copy(isMarkedNullable = true)

    fun TypeRef.nonNull() = copy(isMarkedNullable = false)

    fun TypeRef.qualified(vararg qualifiers: AnnotationRef) =
        copy(qualifiers = qualifiers.toList())

    fun TypeRef.typeWith(vararg typeArguments: TypeRef) =
        copy(arguments = typeArguments.toList())

}