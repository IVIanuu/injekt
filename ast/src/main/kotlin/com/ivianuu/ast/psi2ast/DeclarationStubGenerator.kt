package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.builder.buildConstructor
import com.ivianuu.ast.declarations.builder.buildNamedFunction
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.declarations.builder.buildTypeAlias
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

class DeclarationStubGenerator(
    private val constantValueGenerator: ConstantValueGenerator,
    private val module: ModuleDescriptor,
    private val symbolTable: DescriptorSymbolTable,
    private val typeConverter: TypeConverter
) {

    fun getDeclaration(
        symbol: AstSymbol<*>,
        descriptor: DeclarationDescriptor
    ): AstDeclaration? = when {
        descriptor is ClassDescriptor && symbol is AstRegularClassSymbol ->
            descriptor.toClassStub(symbol)
        descriptor is SimpleFunctionDescriptor && symbol is AstNamedFunctionSymbol ->
            descriptor.toNamedFunctionStub(symbol)
        descriptor is ConstructorDescriptor && symbol is AstConstructorSymbol ->
            descriptor.toConstructorStub(symbol)
        descriptor is VariableDescriptor && symbol is AstPropertySymbol ->
            descriptor.toPropertyStub(symbol)
        descriptor is TypeParameterDescriptor && symbol is AstTypeParameterSymbol ->
            descriptor.toTypeParameterStub(symbol)
        descriptor is ParameterDescriptor && symbol is AstValueParameterSymbol ->
            descriptor.toValueParameterStub(symbol)
        descriptor is TypeAliasDescriptor && symbol is AstTypeAliasSymbol ->
            descriptor.toTypeAliasStub(symbol)
        else -> error("Unexpected declaration $descriptor $symbol")
    }

    private fun ClassDescriptor.toClassStub(
        symbol: AstRegularClassSymbol = AstRegularClassSymbol(classId!!)
    ) = buildRegularClass {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toClassStub.name

        /*this += descriptor.constructors.map { stubGenerator.get(it) as AstFunction }
        this += descriptor.defaultType.memberScope.getContributedDescriptors()
            .filterNot { it is PropertyAccessorDescriptor }
            .map { stubGenerator.get(it) as AstDeclaration }
        this += descriptor.staticScope.getContributedDescriptors()
            .map { stubGenerator.get(it) as AstDeclaration }*/
    }

    private fun SimpleFunctionDescriptor.toNamedFunctionStub(symbol: AstNamedFunctionSymbol) = buildNamedFunction {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toNamedFunctionStub.name
        this.returnType = this@toNamedFunctionStub.returnType!!.toAstType()
        visibility = this@toNamedFunctionStub.visibility.toAstVisibility()
        platformStatus = this@toNamedFunctionStub.platformStatus
        isInfix = this@toNamedFunctionStub.isInfix
        isOperator = this@toNamedFunctionStub.isOperator
        isTailrec = this@toNamedFunctionStub.isTailrec
        isSuspend = this@toNamedFunctionStub.isSuspend
        this.dispatchReceiverType = this@toNamedFunctionStub.dispatchReceiverParameter?.type?.toAstType()
        this.extensionReceiverType = this@toNamedFunctionStub.extensionReceiverParameter?.type?.toAstType()
        this.typeParameters += this@toNamedFunctionStub.typeParameters.map {
            it.toTypeParameterStub(symbolTable.getTypeParameterSymbol(it))
        }
        this.valueParameters += this@toNamedFunctionStub.valueParameters.map {
            it.toValueParameterStub(symbolTable.getValueParameterSymbol(it))
        }
        this.annotations += this@toNamedFunctionStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun ConstructorDescriptor.toConstructorStub(
        symbol: AstConstructorSymbol = AstConstructorSymbol(CallableId(fqNameSafe))
    ) = buildConstructor {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.returnType = this@toConstructorStub.returnType.toAstType()
        this.isPrimary = this@toConstructorStub.isPrimary
        this.valueParameters += this@toConstructorStub.valueParameters.map {
            it.toValueParameterStub(symbolTable.getValueParameterSymbol(it))
        }
        this.annotations += this@toConstructorStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun VariableDescriptor.toPropertyStub(symbol: AstPropertySymbol) = buildProperty {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toPropertyStub.name
        this.returnType = this@toPropertyStub.type.toAstType()
        this.dispatchReceiverType = this@toPropertyStub.dispatchReceiverParameter?.type?.toAstType()
        this.extensionReceiverType = this@toPropertyStub.extensionReceiverParameter?.type?.toAstType()
        this.isVar = this@toPropertyStub.isVar
        this.isLateinit = this@toPropertyStub.isLateInit
        this.isConst = this@toPropertyStub.isConst
        if (this@toPropertyStub is PropertyDescriptor) {
            modality = this@toPropertyStub.modality
            platformStatus = this@toPropertyStub.platformStatus
            isExternal = this@toPropertyStub.isExternal
        }
        this.annotations += this@toPropertyStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun TypeParameterDescriptor.toTypeParameterStub(symbol: AstTypeParameterSymbol) = buildTypeParameter {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toTypeParameterStub.name
        this.isReified = this@toTypeParameterStub.isReified
        this.variance = this@toTypeParameterStub.variance
        this.bounds += this@toTypeParameterStub.upperBounds.map { it.toAstType() }
        this.annotations += this@toTypeParameterStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun ParameterDescriptor.toValueParameterStub(symbol: AstValueParameterSymbol) = buildValueParameter {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toValueParameterStub.name
        this.returnType = this@toValueParameterStub.type.toAstType()
        if (this@toValueParameterStub is ValueParameterDescriptor) {
            this.defaultValue = if (this@toValueParameterStub.declaresDefaultValue())
                buildBlock { type = this@buildValueParameter.returnType }
            else null
            this.isVararg = this@toValueParameterStub.isVararg
            this.isNoinline = this@toValueParameterStub.isNoinline
            this.isCrossinline = this@toValueParameterStub.isCrossinline
        }
        this.annotations += this@toValueParameterStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun TypeAliasDescriptor.toTypeAliasStub(symbol: AstTypeAliasSymbol) = buildTypeAlias {
        this.symbol = symbol
        this.origin = AstDeclarationOrigin.Library
        this.name = this@toTypeAliasStub.name
        this.expandedType = this@toTypeAliasStub.expandedType.toAstType()
        this.visibility = this@toTypeAliasStub.visibility.toAstVisibility()
        this.platformStatus = this@toTypeAliasStub.platformStatus
        this.modality = this@toTypeAliasStub.modality
        this.annotations += this@toTypeAliasStub.annotations.mapNotNull {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }

    private fun KotlinType.toAstType() = typeConverter.convert(this)

}
