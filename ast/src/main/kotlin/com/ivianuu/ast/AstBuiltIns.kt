package com.ivianuu.ast

import com.ivianuu.ast.declarations.builder.buildNamedFunction
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.psi2ast.DescriptorSymbolTable
import com.ivianuu.ast.psi2ast.Psi2AstGeneratorContext
import com.ivianuu.ast.psi2ast.TypeConverter
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.builder.copy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinType

class AstBuiltIns(
    private val kotlinBuiltIns: KotlinBuiltIns,
    private val typeConverter: TypeConverter,
    private val symbolTable: DescriptorSymbolTable,
    private val context: Psi2AstGeneratorContext
) {

    init {
        context.builtIns = this
    }
    
    val anyType = kotlinBuiltIns.anyType.toAstType()
    val anySymbol = kotlinBuiltIns.any.toAstRegularClassSymbol()
    val anyNType = anyType.copy { isMarkedNullable = true }

    val annotationType = kotlinBuiltIns.annotationType.toAstType()
    val annotationSymbol = kotlinBuiltIns.annotation.toAstRegularClassSymbol()

    val enumType = kotlinBuiltIns.enum.defaultType.toAstType()
    val enumSymbol = kotlinBuiltIns.enum.toAstRegularClassSymbol()

    val booleanType = kotlinBuiltIns.booleanType.toAstType()
    val booleanSymbol = kotlinBuiltIns.boolean.toAstRegularClassSymbol()
    val booleanNotSymbol = symbolTable.getSymbol<AstNamedFunctionSymbol>(
        kotlinBuiltIns.boolean.unsubstitutedMemberScope
            .findSingleFunction(Name.identifier("not")) as SimpleFunctionDescriptor
    )

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

    val arraySymbol = kotlinBuiltIns.array.toAstRegularClassSymbol()

    fun function(n: Int) = kotlinBuiltIns.getFunction(n).toAstRegularClassSymbol()
    fun suspendFunction(n: Int) = kotlinBuiltIns.getSuspendFunction(n).toAstRegularClassSymbol()

    fun kFunction(n: Int) = kotlinBuiltIns.getKFunction(n).toAstRegularClassSymbol()
    fun kSuspendFunction(n: Int) = kotlinBuiltIns.getKSuspendFunction(n).toAstRegularClassSymbol()

    val checkNotNull = context.referenceFunctions(FqName("kotlin.checkNotNull"))
        .single { it.owner.valueParameters.size == 1 }

    private fun intrinsicFunction(
        fqName: FqName,
        returnType: AstType,
        valueParameterCount: Int
    ): AstNamedFunctionSymbol {
        return context.buildNamedFunction {
            symbol = AstNamedFunctionSymbol(fqName)
            this.returnType = returnType
            valueParameters += (0 until valueParameterCount)
                .map { index ->
                    buildValueParameter {
                        symbol = AstValueParameterSymbol("p$index".nameAsSafeName())
                        this.returnType = anyNType
                    }
                }
        }.symbol
    }

    val lessThanSymbol = intrinsicFunction(AstIntrinsics.LessThan, booleanType, 2)
    val greaterThanSymbol = intrinsicFunction(AstIntrinsics.GreaterThan, booleanType, 2)
    val lessThanEqualSymbol = intrinsicFunction(AstIntrinsics.LessThanEqual, booleanType, 2)
    val greaterThanEqualSymbol = intrinsicFunction(AstIntrinsics.GreaterThanEqual, booleanType, 2)

    val structuralEqualSymbol = intrinsicFunction(AstIntrinsics.StructuralEqual, booleanType, 2)
    val structuralNotEqualSymbol = intrinsicFunction(AstIntrinsics.StructuralNotEqual, booleanType, 2)
    val identityEqualSymbol = intrinsicFunction(AstIntrinsics.IdentityEqual, booleanType, 2)
    val identityNotEqualSymbol = intrinsicFunction(AstIntrinsics.IdentityNotEqual, booleanType, 2)

    val lazyAndSymbol = intrinsicFunction(AstIntrinsics.LazyAnd, booleanType, 2)
    val lazyOrSymbol = intrinsicFunction(AstIntrinsics.LazyOr, booleanType, 2)

    private fun ClassDescriptor.toAstRegularClassSymbol() = symbolTable.getSymbol<AstRegularClassSymbol>(this)
        .also { context.stubGenerator.getDeclaration(this) }
    private fun KotlinType.toAstType() = typeConverter.convert(this)

}
