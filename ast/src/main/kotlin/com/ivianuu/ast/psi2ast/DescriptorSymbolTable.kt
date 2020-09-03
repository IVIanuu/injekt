package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DescriptorSymbolTable {

    val unboundSymbols: List<AstSymbol<*>>
        get() = (classes.values +
                constructors.values +
                typeParameters.values)
            .filterNot { it.isBound }

    private val classes = mutableMapOf<ClassDescriptor, AstRegularClassSymbol>()
    private val constructors = mutableMapOf<ConstructorDescriptor, AstConstructorSymbol>()
    private val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameterSymbol>()

    fun getClassSymbol(descriptor: ClassDescriptor): AstRegularClassSymbol {
        return classes.getOrPut(descriptor) {
            AstRegularClassSymbol(descriptor.classId!!)
        }
    }

    fun getConstructorSymbol(descriptor: ConstructorDescriptor): AstConstructorSymbol {
        return constructors.getOrPut(descriptor) {
            AstConstructorSymbol(CallableId(descriptor.fqNameSafe))
        }
    }

    fun getTypeParameterSymbol(descriptor: TypeParameterDescriptor): AstTypeParameterSymbol {
        return typeParameters.getOrPut(descriptor) {
            AstTypeParameterSymbol()
        }
    }

}
