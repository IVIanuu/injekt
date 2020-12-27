package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.defaultType
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.isSubTypeOf
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.typeWith
import com.ivianuu.injekt.test.codegen
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.StringValue
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
    fun testQualifiedIsNotAssignableToUnqualifiedAnnotated() =
        withAnalysisContext {
            stringType.qualified(qualifier1()) shouldNotBeAssignable
                    stringType.copy(unqualified = true)
        }

    private infix fun TypeRef.shouldBeAssignable(other: TypeRef) {
        if (!isAssignableTo(other)) {
            throw AssertionError("'$this' is not assignable '$other'")
        }
    }

    private infix fun TypeRef.shouldNotBeAssignable(other: TypeRef) {
        if (isAssignableTo(other)) {
            throw AssertionError("'$this' is assignable '$other'")
        }
    }

    private infix fun TypeRef.shouldBeSubTypeOf(other: TypeRef) {
        if (!isSubTypeOf(other)) {
            throw AssertionError("'$this' is not sub type of '$other'")
        }
    }

    private infix fun TypeRef.shouldNotBeSubTypeOf(other: TypeRef) {
        if (isSubTypeOf(other)) {
            throw AssertionError("'$this' is sub type of '$other'")
        }
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

        val anyType = typeFor(StandardNames.FqNames.any.toSafe())
        val anyNType = anyType.copy(isMarkedNullable = true)
        val intType = typeFor(StandardNames.FqNames._int.toSafe())
        val stringType = typeFor(StandardNames.FqNames.string.toSafe())
        val listType = typeFor(StandardNames.FqNames.list)
        val starProjectedType = STAR_PROJECTION_TYPE

        fun composableFunction(parameterCount: Int) = typeFor(
            FqName("kotlin.Function$parameterCount")
        ).copy(isComposable = true)

        fun function(parameterCount: Int) = typeFor(
            FqName("kotlin.Function$parameterCount")
        )

        fun qualifier1() = AnnotationDescriptorImpl(
            module.findClassAcrossModuleDependencies(
                ClassId.topLevel(FqName("com.ivianuu.injekt.test.Qualifier1"))
            )!!.defaultType,
            emptyMap(),
            SourceElement.NO_SOURCE
        )

        fun qualifier2(value: String) = AnnotationDescriptorImpl(
            module.findClassAcrossModuleDependencies(
                ClassId.topLevel(FqName("com.ivianuu.injekt.test.Qualifier2"))
            )!!.defaultType,
            mapOf("value".asNameId() to StringValue(value)),
            SourceElement.NO_SOURCE
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
        )!!.defaultType.toTypeRef()

    }

    fun TypeRef.nullable() = copy(isMarkedNullable = true)

    fun TypeRef.nonNull() = copy(isMarkedNullable = false)

    fun TypeRef.qualified(vararg qualifiers: AnnotationDescriptor) =
        copy(qualifiers = qualifiers.toList())

    fun TypeRef.typeWith(vararg typeArguments: TypeRef) =
        copy(typeArguments = typeArguments.toList())

}