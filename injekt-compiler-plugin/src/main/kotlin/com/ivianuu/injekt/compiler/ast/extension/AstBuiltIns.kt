package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.copy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType

class AstBuiltIns(
    private val builtIns: KotlinBuiltIns,
    private val toAstClass: (ClassDescriptor) -> AstClass,
    private val toAstType: (KotlinType) -> AstType
) {

    private fun ClassDescriptor.toAstClass() = toAstClass.invoke(this)
    private fun KotlinType.toAstType() = toAstType.invoke(this)

    val anyType = builtIns.anyType.toAstType()
    val anyClass = builtIns.any.toAstClass()
    val anyNType = anyType.copy(hasQuestionMark = true)

    val booleanType = builtIns.booleanType.toAstType()
    val booleanClass = builtIns.boolean.toAstClass()

    val charType = builtIns.charType.toAstType()
    val charClass = builtIns.char.toAstClass()

    val numberType = builtIns.numberType.toAstType()
    val numberClass = builtIns.number.toAstClass()

    val byteType = builtIns.byteType.toAstType()
    val byteClass = builtIns.byte.toAstClass()

    val shortType = builtIns.shortType.toAstType()
    val shortClass = builtIns.short.toAstClass()

    val intType = builtIns.intType.toAstType()
    val intClass = builtIns.int.toAstClass()

    val longType = builtIns.longType.toAstType()
    val longClass = builtIns.long.toAstClass()

    val floatType = builtIns.floatType.toAstType()
    val floatClass = builtIns.float.toAstClass()

    val doubleType = builtIns.doubleType.toAstType()
    val doubleClass = builtIns.double.toAstClass()

    val nothingType = builtIns.nothingType.toAstType()
    val nothingClass = builtIns.nothing.toAstClass()
    val nothingNType = nothingType.copy(hasQuestionMark = true)

    val unitType = builtIns.unitType.toAstType()
    val unitClass = builtIns.unit.toAstClass()

    val stringType = builtIns.stringType.toAstType()
    val stringClass = builtIns.string.toAstClass()

    fun function(n: Int) = builtIns.getFunction(n).toAstClass()
    fun suspendFunction(n: Int) = builtIns.getSuspendFunction(n).toAstClass()

    fun kFunction(n: Int) = builtIns.getKFunction(n).toAstClass()
    fun kSuspendFunction(n: Int) = builtIns.getKSuspendFunction(n).toAstClass()

}