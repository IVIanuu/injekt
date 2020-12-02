package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ErrorCollector
import com.ivianuu.injekt.compiler.generator.QualifierDescriptor
import com.ivianuu.injekt.compiler.generator.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.TypeTranslator
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.isSubTypeOf
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.test.codegen
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.junit.Before
import org.junit.Test

class TypeRefTest {

    private lateinit var analysisContext: AnalysisContext

    @Before
    fun setup() {
        codegen(
            """
            
        """,
            config = {
                compilerPlugins += object : ComponentRegistrar {
                    override fun registerProjectComponents(
                        project: MockProject,
                        configuration: CompilerConfiguration
                    ) {
                        AnalysisHandlerExtension.registerExtension(
                            project,
                            object : AnalysisHandlerExtension {
                                override fun analysisCompleted(
                                    project: Project,
                                    module: ModuleDescriptor,
                                    bindingTrace: BindingTrace,
                                    files: Collection<KtFile>
                                ): AnalysisResult? {
                                    this@TypeRefTest.analysisContext = AnalysisContext(module)
                                    return null
                                }
                            }
                        )
                    }
                }
            }
        )
    }

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
        stringType.nullable() shouldBeAssignable stringType
    }

    @Test
    fun testNullableIsNotAssignableToNonNullable() = withAnalysisContext {
        stringType shouldNotBeAssignable stringType.nullable()
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
    fun testStarProjectedTypeMatchesEffectType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType.copy(effect = 1)
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
        stringType.qualified(qualifier1()) shouldNotBeAssignable  stringType.qualified(qualifier2())
    }

    @Test
    fun testSameQualifiersWithSameArgsIsAssignable() = withAnalysisContext {
        stringType.qualified(qualifier1(mapOf("arg".asNameId() to "a"))) shouldBeAssignable
                stringType.qualified(qualifier1(mapOf("arg".asNameId() to "a")))
    }

    @Test
    fun testSameQualifiersWithDifferentArgsIsAssignable() = withAnalysisContext {
        stringType.qualified(qualifier1(mapOf("arg".asNameId() to "a"))) shouldNotBeAssignable
                stringType.qualified(qualifier1(mapOf("arg".asNameId() to "b")))
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
    fun testUnqualifiedSubTypeOfTypeParameterWithQualifiedUpperBound() = withAnalysisContext {
       /* anyNType
        assertFalse(
            anyNType.copy()
                .isAssignable(typeParameter(stringType.copy(qualifiers = listOf(qualifier1()))))
        )*/
    }

    private infix fun TypeRef.shouldBeAssignable(other: TypeRef) {
        if (!isAssignable(other)) {
            throw AssertionError("'$this' is not assignable '$other'")
        }
    }

    private infix fun TypeRef.shouldNotBeAssignable(other: TypeRef) {
        if (isAssignable(other)) {
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

    // todo effect

    // todo @Test
    fun testGeneric() = codegen(
        """
            @Binding
            class Dep<T : Any>(val value: T)
            
            @Binding
            fun <T> any(): @MyQualifier T = error("")
 
            @Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class MyQualifier
 
            @Component
            abstract class MyComponent {
                abstract val foo: Dep<@MyQualifier Foo>
            }
        """
    )

    private fun withAnalysisContext(
        block: AnalysisContext.() -> Unit
    ) = block(analysisContext)

    class AnalysisContext(val module: ModuleDescriptor) {
        private val declarationStore = DeclarationStore(module)
        private val typeTranslator = TypeTranslator(declarationStore, ErrorCollector())

        val anyType = typeFor(StandardNames.FqNames.any.toSafe())
        val anyNType = anyType.copy(isMarkedNullable = true)
        val intType = typeFor(StandardNames.FqNames._int.toSafe())
        val stringType = typeFor(StandardNames.FqNames.string.toSafe())
        val listType = typeFor(StandardNames.FqNames.list)
        val starProjectedType = STAR_PROJECTION_TYPE

        fun composableFunction(parameterCount: Int) = typeFor(
            FqName("kotlin.Function$parameterCount")
        ).copy(isComposable = true)

        fun qualifier1(args: Map<Name, String> = emptyMap()) =
            qualifier(FqName("Qualifier1"), args)
        fun qualifier2(args: Map<Name, String> = emptyMap()) =
            qualifier(FqName("Qualifier2"), args)

        fun qualifier(
            fqName: FqName = FqName("Qualifier${id++}"),
            args: Map<Name, String>
        ) = QualifierDescriptor(
            ClassifierRef(fqName).defaultType,
            args
        )

        private var id = 0

        fun subType(
            vararg superTypes: TypeRef,
            fqName: FqName = FqName("SubType${id}")
        ) = ClassifierRef(
            fqName = fqName,
            superTypes = superTypes.toList()
        ).defaultType

        fun typeAlias(
            expandedType: TypeRef,
            fqName: FqName = FqName("Alias${id++}")
        ) = ClassifierRef(
            fqName = fqName,
            expandedType = expandedType,
            isTypeAlias = true
        ).defaultType

        fun typeParameter(
            fqName: FqName = FqName("TypeParameter${id++}"),
            nullable: Boolean = true
        ): TypeRef = typeParameter(upperBounds = *emptyArray(), nullable = nullable, fqName = fqName)

        fun typeParameter(
            vararg upperBounds: TypeRef,
            nullable: Boolean = true,
            fqName: FqName = FqName("TypeParameter${id++}")
        ) = ClassifierRef(
            fqName = fqName,
            superTypes = listOf(anyType.copy(isMarkedNullable = nullable)) + upperBounds,
            isTypeParameter = true
        ).defaultType

        fun typeFor(fqName: FqName) = typeTranslator.toTypeRef(
            module.findClassifierAcrossModuleDependencies(
                ClassId.topLevel(fqName)
            )!!.defaultType,
            null as KtFile?
        )

    }

    fun TypeRef.nullable() = copy(isMarkedNullable = true)

    fun TypeRef.nonNull() = copy(isMarkedNullable = false)

    fun TypeRef.qualified(vararg qualifiers: QualifierDescriptor) = copy(qualifiers = qualifiers.toList())

    fun TypeRef.typeWith(vararg typeArguments: TypeRef) = copy(typeArguments = typeArguments.toList())

}
