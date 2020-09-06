package com.ivianuu.ast.expressions

import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildTypeOperation
import com.ivianuu.ast.expressions.builder.buildWhen
import com.ivianuu.ast.expressions.builder.buildWhenBranch
import com.ivianuu.ast.expressions.impl.AstConstImpl
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name

fun AstBuilder.buildConstNull(): AstExpression = buildConst(null, AstConstKind.Null)
fun AstBuilder.buildConstBoolean(value: Boolean): AstExpression = buildConst(value, AstConstKind.Boolean)
fun AstBuilder.buildConstChar(value: Char): AstExpression = buildConst(value, AstConstKind.Char)
fun AstBuilder.buildConstByte(value: Byte): AstExpression = buildConst(value, AstConstKind.Byte)
fun AstBuilder.buildConstUByte(value: UByte): AstExpression = buildConst(value, AstConstKind.UByte)
fun AstBuilder.buildConstShort(value: Short): AstExpression = buildConst(value, AstConstKind.Short)
fun AstBuilder.buildConstUShort(value: UShort): AstExpression = buildConst(value, AstConstKind.UShort)
fun AstBuilder.buildConstInt(value: Int): AstExpression = buildConst(value, AstConstKind.Int)
fun AstBuilder.buildConstUInt(value: UInt): AstExpression = buildConst(value, AstConstKind.UInt)
fun AstBuilder.buildConstLong(value: Long): AstExpression = buildConst(value, AstConstKind.Long)
fun AstBuilder.buildConstULong(value: ULong): AstExpression = buildConst(value, AstConstKind.ULong)
fun AstBuilder.buildConstString(value: String): AstExpression = buildConst(value, AstConstKind.String)
fun AstBuilder.buildConstFloat(value: Float): AstExpression = buildConst(value, AstConstKind.Float)
fun AstBuilder.buildConstDouble(value: Double): AstExpression = buildConst(value, AstConstKind.Double)

fun <T> AstBuilder.buildConst(
    value: T,
    kind: AstConstKind<T>,
    annotations: MutableList<AstFunctionCall> = mutableListOf(),
): AstConst<T> {
    val type = when (kind) {
        AstConstKind.Null -> context.builtIns.nothingNType
        AstConstKind.Boolean -> context.builtIns.booleanType
        AstConstKind.Char -> context.builtIns.charType
        AstConstKind.Byte, AstConstKind.UByte -> context.builtIns.byteType
        AstConstKind.Short, AstConstKind.UShort -> context.builtIns.shortType
        AstConstKind.Int, AstConstKind.UInt -> context.builtIns.intType
        AstConstKind.Long, AstConstKind.ULong -> context.builtIns.longType
        AstConstKind.String -> context.builtIns.stringType
        AstConstKind.Float -> context.builtIns.floatType
        AstConstKind.Double -> context.builtIns.doubleType
    }
    return AstConstImpl(context, annotations, type, kind, value)
}

fun AstBuilder.buildTemporaryVariable(
    value: AstExpression,
    type: AstType = value.type,
    name: Name = Name.special("<tmp>"),
    isVar: Boolean = false
) = buildTemporaryVariable(
    type = type,
    name = name,
    initializer = value,
    isVar = isVar
)

fun AstBuilder.buildTemporaryVariable(
    type: AstType,
    name: Name = Name.special("<tmp>"),
    initializer: AstExpression? = null,
    delegate: AstExpression? = null,
    isVar: Boolean = false
) = buildProperty {
    symbol = AstPropertySymbol(name)
    returnType = type
    this.isVar = isVar
    visibility = Visibilities.Local
    isLocal = true
    this.initializer = initializer
    this.delegate = delegate
}

fun AstBuilder.buildFunctionCall(
    callee: AstFunctionSymbol<*>,
    init: AstFunctionCallBuilder.() -> Unit = {}
): AstFunctionCall = buildFunctionCall {
    this.type = callee.owner.returnType
    this.callee = callee
    init(this)
}

fun AstBuilder.buildUnitExpression() = buildQualifiedAccess {
    type = context.builtIns.unitType
    callee = context.builtIns.unitSymbol
}

fun AstBuilder.buildElvisExpression(
    type: AstType,
    left: AstExpression,
    right: AstExpression
) = buildBlock {
    val tmp = buildTemporaryVariable(left)
        .also { statements += it }

    statements += buildWhen {
        this.type = type
        branches += buildWhenBranch {
            condition = buildFunctionCall {
                callee = context.builtIns.structuralNotEqualSymbol
                valueArguments += buildQualifiedAccess { callee = tmp.symbol }
                valueArguments += buildConstNull()
            }
            result = buildQualifiedAccess { callee = tmp.symbol }
        }
        branches += buildElseBranch(right)
    }
}

fun AstBuilder.buildElseBranch(result: AstExpression) = buildWhenBranch {
    condition = buildConstBoolean(true)
    this.result = result
}
