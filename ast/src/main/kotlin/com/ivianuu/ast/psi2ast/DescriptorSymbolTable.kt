package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstEnumEntrySymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.FakeKotlinCallArgumentForCallableReference
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DescriptorSymbolTable {

    val unboundSymbols: Map<DeclarationDescriptor, AstSymbol<*>>
        get() = allSymbols.filterNot { it.value.isBound }

    private val _allSymbols = mutableMapOf<DeclarationDescriptor, AstSymbol<*>>()
    val allSymbols: Map<DeclarationDescriptor, AstSymbol<*>>
        get() = _allSymbols

    fun <S : AstSymbol<*>> getSymbol(descriptor: DeclarationDescriptor): S {
        return _allSymbols.getOrPut(descriptor) {
            when (descriptor) {
                is FakeCallableDescriptorForObject -> getSymbol(descriptor.classDescriptor)
                is ClassDescriptor -> {
                    when {
                        descriptor.kind == ClassKind.ENUM_ENTRY -> AstEnumEntrySymbol(descriptor.fqNameSafe)
                        descriptor.visibility == Visibilities.LOCAL &&
                                descriptor.name.isSpecial -> AstAnonymousObjectSymbol()
                        else -> AstRegularClassSymbol(descriptor.fqNameSafe)
                    }
                }
                is TypeParameterDescriptor -> AstTypeParameterSymbol(descriptor.fqNameSafe)
                is TypeAliasDescriptor -> AstTypeAliasSymbol(descriptor.fqNameSafe)
                is ImportedFromObjectCallableDescriptor<*> -> getSymbol(descriptor.callableFromObject)
                is FunctionDescriptor -> {
                    when {
                        descriptor is ConstructorDescriptor -> AstConstructorSymbol(descriptor.fqNameSafe)
                        descriptor is PropertyAccessorDescriptor -> AstPropertyAccessorSymbol()
                        descriptor.visibility == Visibilities.LOCAL -> AstAnonymousFunctionSymbol()
                        else -> AstNamedFunctionSymbol(descriptor.fqNameSafe)
                    }
                }
                is LocalVariableDescriptor -> AstPropertySymbol(descriptor.name)
                is SyntheticFieldDescriptor -> AstPropertySymbol(Name.identifier("field"))
                is PropertyDescriptor -> AstPropertySymbol(descriptor.fqNameSafe)
                is ValueParameterDescriptor -> AstValueParameterSymbol(descriptor.fqNameSafe)
                else -> error("Unexpected descriptor $descriptor")
            }
        } as S
    }

}
