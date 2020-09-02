package com.ivianuu.ast

import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.copy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType

class AstBuiltIns(
    val builtIns: KotlinBuiltIns
) {

    val anyType = builtIns.anyType.toAstType()
    val anyClass = builtIns.any.toAstRegularClassSymbol()
    val anyNType = anyType.copy(isMarkedNullable = true)

    val booleanType = builtIns.booleanType.toAstType()
    val booleanSymbol = builtIns.boolean.toAstRegularClassSymbol()

    val charType = builtIns.charType.toAstType()
    val charSymbol = builtIns.char.toAstRegularClassSymbol()

    val numberType = builtIns.numberType.toAstType()
    val numberSymbol = builtIns.number.toAstRegularClassSymbol()

    val byteType = builtIns.byteType.toAstType()
    val byteSymbol = builtIns.byte.toAstRegularClassSymbol()

    val shortType = builtIns.shortType.toAstType()
    val shortSymbol = builtIns.short.toAstRegularClassSymbol()

    val intType = builtIns.intType.toAstType()
    val intSymbol = builtIns.int.toAstRegularClassSymbol()

    val longType = builtIns.longType.toAstType()
    val longSymbol = builtIns.long.toAstRegularClassSymbol()

    val floatType = builtIns.floatType.toAstType()
    val floatSymbol = builtIns.float.toAstRegularClassSymbol()

    val doubleType = builtIns.doubleType.toAstType()
    val doubleSymbol = builtIns.double.toAstRegularClassSymbol()

    val nothingType = builtIns.nothingType.toAstType()
    val nothingSymbol = builtIns.nothing.toAstRegularClassSymbol()
    val nothingNType = nothingType.copy(isMarkedNullable = true)

    val unitType = builtIns.unitType.toAstType()
    val unitSymbol = builtIns.unit.toAstRegularClassSymbol()

    val stringType = builtIns.stringType.toAstType()
    val stringSymbol = builtIns.string.toAstRegularClassSymbol()

    fun function(n: Int) = builtIns.getFunction(n).toAstRegularClassSymbol()
    fun suspendFunction(n: Int) = builtIns.getSuspendFunction(n).toAstRegularClassSymbol()

    fun kFunction(n: Int) = builtIns.getKFunction(n).toAstRegularClassSymbol()
    fun kSuspendFunction(n: Int) = builtIns.getKSuspendFunction(n).toAstRegularClassSymbol()

    private fun ClassDescriptor.toAstRegularClassSymbol(): AstRegularClassSymbol = TODO()//astProvider.get<AstClass>(this)
    private fun KotlinType.toAstType(): AstType = TODO()//typeMapper.translate(this)

}
