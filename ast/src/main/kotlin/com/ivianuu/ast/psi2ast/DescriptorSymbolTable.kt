package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
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
    private val namedFunctions = mutableMapOf<SimpleFunctionDescriptor, AstNamedFunctionSymbol>()
    private val properties = mutableMapOf<VariableDescriptor, AstPropertySymbol>()
    private val propertyAccessors = mutableMapOf<PropertyAccessorDescriptor, AstPropertyAccessorSymbol>()
    private val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameterSymbol>()
    private val valueParameters = mutableMapOf<ParameterDescriptor, AstValueParameterSymbol>()

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

    fun getNamedFunctionSymbol(descriptor: SimpleFunctionDescriptor): AstNamedFunctionSymbol {
        return namedFunctions.getOrPut(descriptor) {
            AstNamedFunctionSymbol(
                CallableId(
                    descriptor.findPackage().fqName,
                    (descriptor.containingDeclaration as? ClassDescriptor)?.fqNameSafe,
                    descriptor.name
                )
            )
        }
    }

    fun getPropertySymbol(descriptor: VariableDescriptor): AstPropertySymbol {
        return properties.getOrPut(descriptor) {
            AstPropertySymbol(
                CallableId(
                    descriptor.findPackage().fqName,
                    (descriptor.containingDeclaration as? ClassDescriptor)?.fqNameSafe,
                    descriptor.name
                )
            )
        }
    }

    fun getPropertyAccessorSymbol(descriptor: PropertyAccessorDescriptor): AstPropertyAccessorSymbol {
        return propertyAccessors.getOrPut(descriptor) {
            AstPropertyAccessorSymbol()
        }
    }

    fun getTypeParameterSymbol(descriptor: TypeParameterDescriptor): AstTypeParameterSymbol {
        return typeParameters.getOrPut(descriptor) {
            AstTypeParameterSymbol()
        }
    }

    fun getValueParameterSymbol(descriptor: ParameterDescriptor): AstValueParameterSymbol {
        return valueParameters.getOrPut(descriptor) {
            AstValueParameterSymbol(CallableId(descriptor.name))
        }
    }

}
