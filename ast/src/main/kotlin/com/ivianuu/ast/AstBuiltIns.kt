package com.ivianuu.ast

import com.ivianuu.ast.declarations.builder.buildNamedFunction
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.psi2ast.DescriptorSymbolTable
import com.ivianuu.ast.psi2ast.TypeConverter
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.builder.copy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.types.KotlinType

class AstBuiltIns(
    private val kotlinBuiltIns: KotlinBuiltIns,
    private val typeConverter: TypeConverter,
    private val symbolTable: DescriptorSymbolTable
) {

    val anyType = kotlinBuiltIns.anyType.toAstType()
    val anyClass = kotlinBuiltIns.any.toAstRegularClassSymbol()
    val anyNType = anyType.copy { isMarkedNullable = true }

    val booleanType = kotlinBuiltIns.booleanType.toAstType()
    val booleanSymbol = kotlinBuiltIns.boolean.toAstRegularClassSymbol()

    val charType = kotlinBuiltIns.charType.toAstType()
    val charSymbol = kotlinBuiltIns.char.toAstRegularClassSymbol()

    val numberType = kotlinBuiltIns.numberType.toAstType()
    val numberSymbol = kotlinBuiltIns.number.toAstRegularClassSymbol()

    val byteType = kotlinBuiltIns.byteType.toAstType()
    val byteSymbol = kotlinBuiltIns.byte.toAstRegularClassSymbol()

    val shortType = kotlinBuiltIns.shortType.toAstType()
    val shortSymbol = kotlinBuiltIns.short.toAstRegularClassSymbol()

    val intType = kotlinBuiltIns.intType.toAstType()
    val intSymbol = kotlinBuiltIns.int.toAstRegularClassSymbol()

    val longType = kotlinBuiltIns.longType.toAstType()
    val longSymbol = kotlinBuiltIns.long.toAstRegularClassSymbol()

    val floatType = kotlinBuiltIns.floatType.toAstType()
    val floatSymbol = kotlinBuiltIns.float.toAstRegularClassSymbol()

    val doubleType = kotlinBuiltIns.doubleType.toAstType()
    val doubleSymbol = kotlinBuiltIns.double.toAstRegularClassSymbol()

    val nothingType = kotlinBuiltIns.nothingType.toAstType()
    val nothingSymbol = kotlinBuiltIns.nothing.toAstRegularClassSymbol()
    val nothingNType = nothingType.copy { isMarkedNullable = true }

    val unitType = kotlinBuiltIns.unitType.toAstType()
    val unitSymbol = kotlinBuiltIns.unit.toAstRegularClassSymbol()

    val stringType = kotlinBuiltIns.stringType.toAstType()
    val stringSymbol = kotlinBuiltIns.string.toAstRegularClassSymbol()

    fun function(n: Int) = kotlinBuiltIns.getFunction(n).toAstRegularClassSymbol()
    fun suspendFunction(n: Int) = kotlinBuiltIns.getSuspendFunction(n).toAstRegularClassSymbol()

    fun kFunction(n: Int) = kotlinBuiltIns.getKFunction(n).toAstRegularClassSymbol()
    fun kSuspendFunction(n: Int) = kotlinBuiltIns.getKSuspendFunction(n).toAstRegularClassSymbol()

    private fun intrinsicFunction(
        callableId: CallableId,
        returnType: AstType,
        valueParameterCount: Int
    ): AstNamedFunctionSymbol {
        return buildNamedFunction {
            symbol = AstNamedFunctionSymbol(callableId)
            name = callableId.callableName
            this.returnType = returnType
            valueParameters += (0 until valueParameterCount)
                .map { index ->
                    buildValueParameter {
                        symbol = AstValueParameterSymbol(
                            CallableId("p$index".nameAsSafeName())
                        )
                        name = symbol.callableId.callableName
                        this.returnType = anyNType
                    }
                }
        }.symbol
    }

    val lessThanOperation = intrinsicFunction(AstIntrinsics.LessThan, booleanType, 2)
    val greaterThanOperation = intrinsicFunction(AstIntrinsics.GreaterThan, booleanType, 2)
    val lessThanEqualOperation = intrinsicFunction(AstIntrinsics.LessThanEqual, booleanType, 2)
    val greaterThanEqualOperation = intrinsicFunction(AstIntrinsics.GreaterThanEqual, booleanType, 2)

    val equalOperation = intrinsicFunction(AstIntrinsics.Equal, booleanType, 2)
    val notEqualOperation = intrinsicFunction(AstIntrinsics.NotEqual, booleanType, 2)
    val identityOperation = intrinsicFunction(AstIntrinsics.Identity, booleanType, 2)
    val notIdentityOperation = intrinsicFunction(AstIntrinsics.NotIdentity, booleanType, 2)

    val andOperation = intrinsicFunction(AstIntrinsics.And, booleanType, 2)
    val orOperation = intrinsicFunction(AstIntrinsics.Or, booleanType, 2)

    val isOperation = intrinsicFunction(AstIntrinsics.Is, booleanType, 1)
    val notIsOperation = intrinsicFunction(AstIntrinsics.NotIs, booleanType, 1)
    fun asOperation(type: AstType) = intrinsicFunction(AstIntrinsics.As, type, 1)
    fun safeAsOperation(type: AstType) = intrinsicFunction(AstIntrinsics.SafeAs,
        (type as AstSimpleType).copy { isMarkedNullable = true }, 1)

    private fun ClassDescriptor.toAstRegularClassSymbol() = symbolTable.getClassSymbol(this)
    private fun KotlinType.toAstType() = typeConverter.convert(this)

}
