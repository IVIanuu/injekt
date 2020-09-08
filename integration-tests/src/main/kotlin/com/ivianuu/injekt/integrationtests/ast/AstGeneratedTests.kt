package com.ivianuu.injekt.integrationtests.ast
import com.ivianuu.injekt.test.compile
import com.ivianuu.injekt.test.source
import org.junit.Test
import com.ivianuu.ast.psi2ast.astEnabled
import com.ivianuu.injekt.test.assertOk
import java.io.File
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

class GeneratedAstTests {

    init {
        astEnabled = true
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_temporaryInInitBlockkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/temporaryInInitBlock.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_typeOperatorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/typeOperators.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_varargkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/vararg.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_reflectionLiteralskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/reflectionLiterals.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_thisReferenceBeforeClassDeclaredkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/thisReferenceBeforeClassDeclared.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_breakContinuekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/breakContinue.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_typeParameterClassLiteralkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/typeParameterClassLiteral.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_variableAsFunctionCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/variableAsFunctionCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_identitykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/identity.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt36956kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt36956.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt16905kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt16905.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_lambdaInCAOkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/lambdaInCAO.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_extFunSafeInvokekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/extFunSafeInvoke.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_inkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/in.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_forkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/for.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_arrayAccesskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/arrayAccess.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_referenceskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/references.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_fieldkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/field.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_implicitCastToTypeParameterkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/implicitCastToTypeParameter.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_arrayAugmentedAssignment1kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/arrayAugmentedAssignment1.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_memberTypeArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/memberTypeArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_argumentMappedWithErrorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/argumentMappedWithError.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_tryCatchkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/tryCatch.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_sam_samConstructorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/sam/samConstructors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_arrayAssignmentkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/arrayAssignment.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callWithReorderedArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callWithReorderedArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_elviskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/elvis.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenReturnkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whenReturn.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_throwkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/throw.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_stringPluskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/stringPlus.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_valueskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/values.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_interfaceThisRefkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/interfaceThisRef.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_simpleUnaryOperatorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/simpleUnaryOperators.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_safeAssignmentkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/safeAssignment.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_jvmStaticFieldReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/jvmStaticFieldReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_boundCallableReferenceskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/boundCallableReferences.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt28456akt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt28456a.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt37570kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt37570.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_objectAsCallablekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/objectAsCallable.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenUnusedExpressionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whenUnusedExpression.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt35730kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt35730.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_funImportedFromObjectkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/funImportedFromObject.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_implicitCastInReturnFromConstructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/implicitCastInReturnFromConstructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_safeCallWithIncrementDecrementkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/safeCallWithIncrementDecrement.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt30796kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt30796.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt30020kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt30020.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenReturnUnitkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whenReturnUnit.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_smartCastsWithDestructuringkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/smartCastsWithDestructuring.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/when.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_chainOfSafeCallskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/chainOfSafeCalls.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_augmentedAssignmentWithExpressionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/augmentedAssignmentWithExpression.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_thisRefToObjectInNestedClassConstructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/thisRefToObjectInNestedClassConstructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_forWithImplicitReceiverskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/forWithImplicitReceivers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_useImportedMemberkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/useImportedMember.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_coercionToUnitkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/coercionToUnit.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt28006kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt28006.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_membersImportedFromObjectkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/membersImportedFromObject.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_objectReferenceInClosureInSuperConstructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/objectReferenceInClosureInSuperConstructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_safeCallskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/safeCalls.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whileDoWhilekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whileDoWhile.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_dotQualifiedkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/dotQualified.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_primitivesImplicitConversionskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/primitivesImplicitConversions.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_contructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/contructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_outerClassInstanceReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/outerClassInstanceReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_multipleThisReferenceskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/multipleThisReferences.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_incrementDecrementkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/incrementDecrement.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_typeAliasConstructorReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/typeAliasConstructorReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_implicitCastOnPlatformTypekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/implicitCastOnPlatformType.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_varargWithImplicitCastkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/varargWithImplicitCast.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_objectReferenceInFieldInitializerkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/objectReferenceInFieldInitializer.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_primitiveComparisonskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/primitiveComparisons.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_enumEntryReferenceFromEnumEntryClasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/enumEntryReferenceFromEnumEntryClass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_objectClassReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/objectClassReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_castToTypeParameterkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/castToTypeParameter.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_thisOfGenericOuterClasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/thisOfGenericOuterClass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_complexAugmentedAssignmentkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/complexAugmentedAssignment.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_temporaryInEnumEntryInitializerkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/temporaryInEnumEntryInitializer.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_classReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/classReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_arrayAugmentedAssignment2kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/arrayAugmentedAssignment2.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_typeArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/typeArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_breakContinueInLoopHeaderkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/breakContinueInLoopHeader.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_booleanOperatorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/booleanOperators.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt28456kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt28456.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_augmentedAssignment1kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/augmentedAssignment1.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_implicitCastToNonNullkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/implicitCastToNonNull.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_assignmentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/assignments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt36963kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt36963.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_specializedTypeAliasConstructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/specializedTypeAliasConstructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_genericPropertyCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/genericPropertyCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_tryCatchWithImplicitCastkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/tryCatchWithImplicitCast.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt23030kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt23030.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_stringTemplateskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/stringTemplates.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_forWithBreakContinuekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/forWithBreakContinue.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_floatingPointCompareTokt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/floatingPointCompareTo.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_nullableFloatingPointEqeqkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/nullableFloatingPointEqeq.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_comparableWithDoubleOrFloatkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/comparableWithDoubleOrFloat.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_floatingPointLesskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/floatingPointLess.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_typeParameterWithPrimitiveNumericSupertypekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/typeParameterWithPrimitiveNumericSupertype.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_floatingPointEqeqkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/floatingPointEqeq.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_eqeqRhsConditionPossiblyAffectingLhskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/eqeqRhsConditionPossiblyAffectingLhs.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_nullableAnyAsIntToDoublekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/nullableAnyAsIntToDouble.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_floatingPointEqualskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/floatingPointEquals.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_floatingPointExcleqkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/floatingPointExcleq.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_floatingPointComparisons_whenByFloatingPointkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/floatingPointComparisons/whenByFloatingPoint.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_extensionPropertyGetterCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/extensionPropertyGetterCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_genericConstructorCallWithTypeArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/genericConstructorCallWithTypeArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_catchParameterAccesskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/catchParameterAccess.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenCoercedToUnitkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whenCoercedToUnit.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_destructuringWithUnderscorekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/destructuringWithUnderscore.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_objectReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/objectReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_equalitykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/equality.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt28456bkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt28456b.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_ifElseIfkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/ifElseIf.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_smartCastskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/smartCasts.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_boxOkkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/boxOk.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/calls.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt24804kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt24804.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_whenElsekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/whenElse.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_bangbangkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/bangbang.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_kt27933kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/kt27933.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_genericPropertyRefkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/genericPropertyRef.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_stringComparisonskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/stringComparisons.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_conventionComparisonskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/conventionComparisons.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_simpleOperatorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/simpleOperators.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_destructuring1kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/destructuring1.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_augmentedAssignment2kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/augmentedAssignment2.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_unsignedIntegerLiteralskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/unsignedIntegerLiterals.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_enumEntryAsReceiverkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/enumEntryAsReceiver.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_literalskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/literals.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_booleanConstsInAndAndOrOrkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/booleanConstsInAndAndOrOr.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_extFunInvokeAsFunkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/extFunInvokeAsFun.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_importedFromObjectkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/importedFromObject.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_funWithDefaultParametersAsKCallableStarkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/funWithDefaultParametersAsKCallableStar.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_genericMemberkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/genericMember.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    class A<T> {
        fun foo() {}
        val bar = 42
    }

    val test1 = A<String>::foo
    val test2 = A<String>::bar

    init {
        val a = A<String>()
        val hehe: KProperty0<Int> = a::bar
        val hehe1: KProperty1<A<String>, Int>
        hehe1 = A<String>::bar
    }

    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_boundInlineAdaptedReferencekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/boundInlineAdaptedReference.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_typeArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/typeArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_boundInnerGenericConstructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/boundInnerGenericConstructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_kt37131kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/kt37131.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_expressions_callableReferences_adaptedWithCoercionToUnitkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/expressions/callableReferences/adaptedWithCoercionToUnit.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_localDifferentReceiverskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/localDifferentReceivers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_memberExtensionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/memberExtension.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_memberkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/member.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_differentReceiverskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/differentReceivers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_topLevelkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/topLevel.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_provideDelegate_localkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/provideDelegate/local.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_lambdaskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/lambdas.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_classkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/class.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_localFunkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/localFun.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_typeParameterBeforeBoundkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/typeParameterBeforeBound.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_propertyAccessorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/propertyAccessors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_dataClassMemberskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/dataClassMembers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_funkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/fun.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_constructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/constructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_defaultPropertyAccessorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/defaultPropertyAccessors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_useNextParamInLambdakt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/useNextParamInLambda.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_genericInnerClasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/genericInnerClass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_delegatedMemberskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/delegatedMembers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_parameters_typeParameterBoundedBySubclasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/parameters/typeParameterBoundedBySubclass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_catchParameterInTopLevelPropertykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/catchParameterInTopLevelProperty.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_kt35550kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/kt35550.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_packageLevelPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/packageLevelProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_fileWithTypeAliasesOnlykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/fileWithTypeAliasesOnly.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_constValInitializerskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/constValInitializers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_localVarInDoWhilekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/localVarInDoWhile.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_genericDelegatedPropertykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/genericDelegatedProperty.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_extensionPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/extensionProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_primaryCtorPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/primaryCtorProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_fileWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/fileWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_typeAliaskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/typeAlias.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_localDelegatedPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/localDelegatedProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_interfacePropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/interfaceProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_kt27005kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/kt27005.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_primaryCtorDefaultArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/primaryCtorDefaultArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_spreadOperatorInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/spreadOperatorInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_fileAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/fileAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_receiverParameterWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/receiverParameterWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_localDelegatedPropertiesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/localDelegatedPropertiesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_classesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/classesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_annotationsWithVarargParameterskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/annotationsWithVarargParameters.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_multipleAnnotationsInSquareBracketskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/multipleAnnotationsInSquareBrackets.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_primaryConstructorParameterWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/primaryConstructorParameterWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_typeParametersWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/typeParametersWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_classLiteralInAnnotationkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/classLiteralInAnnotation.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_constructorsWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/constructorsWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_annotationsInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/annotationsInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_delegateFieldWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/delegateFieldWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_constExpressionsInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/constExpressionsInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_propertyAccessorsWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/propertyAccessorsWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_propertySetterParameterWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/propertySetterParameterWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_annotationsWithDefaultParameterValueskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/annotationsWithDefaultParameterValues.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_propertiesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/propertiesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_varargsInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/varargsInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_fieldsWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/fieldsWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_functionsWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/functionsWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_variablesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/variablesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_enumEntriesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/enumEntriesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_valueParametersWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/valueParametersWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_arrayInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/arrayInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_propertyAccessorsFromClassHeaderWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/propertyAccessorsFromClassHeaderWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_typeAliasesWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/typeAliasesWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_enumsInAnnotationArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/enumsInAnnotationArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_annotations_delegatedPropertyAccessorsWithAnnotationskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/annotations/delegatedPropertyAccessorsWithAnnotations.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_localClassWithOverrideskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/localClassWithOverrides.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_fakeOverrideskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/fakeOverrides.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_classLevelPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/classLevelProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_deprecatedPropertykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/deprecatedProperty.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_defaultArgumentskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/defaultArguments.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_declarations_delegatedPropertieskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/declarations/delegatedProperties.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_stubs_constFromBuiltinskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/stubs/constFromBuiltins.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_stubs_simplekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/stubs/simple.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_stubs_jdkClassSyntheticPropertykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/stubs/jdkClassSyntheticProperty.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_stubs_builtinMapkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/stubs/builtinMap.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_firProblems_deprecatedkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/firProblems/deprecated.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_firProblems_putIfAbsentkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/firProblems/putIfAbsent.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_errors_suppressedNonPublicCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/errors/suppressedNonPublicCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_regressions_integerCoercionToTkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/regressions/integerCoercionToT.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_regressions_typeAliasCtorForGenericClasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/regressions/typeAliasCtorForGenericClass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_regressions_kt24114kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/regressions/kt24114.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_regressions_newInference_fixationOrder1kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/regressions/newInference/fixationOrder1.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_regressions_coercionInLoopkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/regressions/coercionInLoop.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_singletons_objectkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/singletons/object.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_singletons_enumEntrykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/singletons/enumEntry.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_singletons_companionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/singletons/companion.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_genericFunWithStarkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/genericFunWithStar.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_smartCastOnFakeOverrideReceiverkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/smartCastOnFakeOverrideReceiver.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_genericDelegatedDeepPropertykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/genericDelegatedDeepProperty.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_smartCastOnReceiverOfGenericTypekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/smartCastOnReceiverOfGenericType.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_abbreviatedTypeskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/abbreviatedTypes.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_kt36143kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/kt36143.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_types_genericPropertyReferenceTypekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/types/genericPropertyReferenceType.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_destructuringInLambdakt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/destructuringInLambda.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_anonymousFunctionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/anonymousFunction.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_extensionLambdakt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/extensionLambda.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_samAdapterkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/samAdapter.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_multipleImplicitReceiverskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/multipleImplicitReceivers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_nonLocalReturnkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/nonLocalReturn.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_localFunctionkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/localFunction.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_lambdas_justLambdakt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/lambdas/justLambda.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_localClasseskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/localClasses.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_kt31649kt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/kt31649.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_initVarkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/initVar.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_enumWithSecondaryCtorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/enumWithSecondaryCtor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_delegatedImplementationWithExplicitOverridekt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/delegatedImplementationWithExplicitOverride.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_enumWithMultipleCtorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/enumWithMultipleCtors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_annotationClasseskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/annotationClasses.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_enumkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/enum.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_delegatedImplementationkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/delegatedImplementation.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_companionObjectkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/companionObject.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_objectLiteralExpressionskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/objectLiteralExpressions.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_delegatingConstructorCallsInSecondaryConstructorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/delegatingConstructorCallsInSecondaryConstructors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_sealedClasseskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/sealedClasses.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_secondaryConstructorWithInitializersFromClassBodykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/secondaryConstructorWithInitializersFromClassBody.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_innerClasskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/innerClass.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_superCallsComposedkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/superCallsComposed.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_dataClassesGenerickt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/dataClassesGeneric.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_initValkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/initVal.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_lambdaInDataClassDefaultParameterkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/lambdaInDataClassDefaultParameter.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_dataClasseskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/dataClasses.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_dataClassWithArrayMemberskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/dataClassWithArrayMembers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_objectWithInitializerskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/objectWithInitializers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_primaryConstructorWithSuperConstructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/primaryConstructorWithSuperConstructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_primaryConstructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/primaryConstructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_outerClassAccesskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/outerClassAccess.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_innerClassWithDelegatingConstructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/innerClassWithDelegatingConstructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_enumClassModalitykt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/enumClassModality.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_delegatedGenericImplementationkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/delegatedGenericImplementation.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_secondaryConstructorskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/secondaryConstructors.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_classeskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/classes.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_initValInLambdakt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/initValInLambda.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_initBlockkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/initBlock.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_classMemberskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/classMembers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_argumentReorderingInDelegatingConstructorCallkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/argumentReorderingInDelegatingConstructorCall.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_abstractMemberskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/abstractMembers.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_superCallskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/superCalls.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_qualifiedSuperCallskt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/qualifiedSuperCalls.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }


    @Test
    fun test_home_ivianuu_otherprojects_kotlin_compiler_testData_ir_irText_classes_delegatingConstructorCallToTypeAliasConstructorkt() {
        compile {
            sources += source(
                source = File("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText/classes/delegatingConstructorCallToTypeAliasConstructor.kt").readText(),
                injektImports = false,
                initializeInjekt = false
            )
        }.apply {
            assertOk()
        }
    }

}
