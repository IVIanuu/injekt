package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

class DescriptorSymbolTable {

    val unboundSymbols: List<AstSymbol<*>>
        get() = (classes.values + typeParameters.values)
            .filterNot { it.isBound }

    private val classes = mutableMapOf<ClassDescriptor, AstRegularClassSymbol>()
    private val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameterSymbol>()

    fun getClassSymbol(descriptor: ClassDescriptor): AstRegularClassSymbol {
        return classes.getOrPut(descriptor) {
            AstRegularClassSymbol(descriptor.classId!!)
        }
    }

    fun getTypeParameterSymbol(descriptor: TypeParameterDescriptor): AstTypeParameterSymbol {
        return typeParameters.getOrPut(descriptor) {
            AstTypeParameterSymbol()
        }
    }

}
