package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.CallableId
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
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DescriptorSymbolTable {

    val unboundSymbols: Map<DeclarationDescriptor, AstSymbol<*>>
        get() = (classes +
                constructors +
                namedFunctions +
                properties +
                propertyAccessors +
                typeParameters +
                valueParameters)
            .filterNot { it.value.isBound }

    private val anonymousObjects = mutableMapOf<ClassDescriptor, AstAnonymousObjectSymbol>()
    private val classes = mutableMapOf<ClassDescriptor, AstRegularClassSymbol>()
    private val constructors = mutableMapOf<ConstructorDescriptor, AstConstructorSymbol>()
    private val enumEntries = mutableMapOf<ClassDescriptor, AstEnumEntrySymbol>()
    private val namedFunctions = mutableMapOf<SimpleFunctionDescriptor, AstNamedFunctionSymbol>()
    private val properties = mutableMapOf<VariableDescriptor, AstPropertySymbol>()
    private val propertyAccessors = mutableMapOf<PropertyAccessorDescriptor, AstPropertyAccessorSymbol>()
    private val typeParameters = mutableMapOf<TypeParameterDescriptor, AstTypeParameterSymbol>()
    private val valueParameters = mutableMapOf<ParameterDescriptor, AstValueParameterSymbol>()
    private val typeAliases = mutableMapOf<TypeAliasDescriptor, AstTypeAliasSymbol>()

    fun getAnonymousObjectSymbol(descriptor: ClassDescriptor): AstAnonymousObjectSymbol =
        anonymousObjects.getOrPut(descriptor) {
            AstAnonymousObjectSymbol()
        }

    fun getClassSymbol(descriptor: ClassDescriptor): AstRegularClassSymbol =
        classes.getOrPut(descriptor) {
            AstRegularClassSymbol(descriptor.classId!!)
        }

    fun getConstructorSymbol(descriptor: ConstructorDescriptor): AstConstructorSymbol =
        constructors.getOrPut(descriptor) {
            AstConstructorSymbol(CallableId(descriptor.fqNameSafe))
        }

    fun getNamedFunctionSymbol(descriptor: SimpleFunctionDescriptor): AstNamedFunctionSymbol =
        namedFunctions.getOrPut(descriptor) {
            AstNamedFunctionSymbol(
                CallableId(
                    descriptor.findPackage().fqName,
                    (descriptor.containingDeclaration as? ClassDescriptor)?.fqNameSafe,
                    descriptor.name
                )
            )
        }

    fun getPropertySymbol(descriptor: VariableDescriptor): AstPropertySymbol =
        properties.getOrPut(descriptor) {
            AstPropertySymbol(
                CallableId(
                    descriptor.findPackage().fqName,
                    (descriptor.containingDeclaration as? ClassDescriptor)?.fqNameSafe,
                    descriptor.name
                )
            )
        }

    fun getPropertyAccessorSymbol(descriptor: PropertyAccessorDescriptor): AstPropertyAccessorSymbol =
        propertyAccessors.getOrPut(descriptor) {
            AstPropertyAccessorSymbol()
        }

    fun getTypeParameterSymbol(descriptor: TypeParameterDescriptor): AstTypeParameterSymbol =
        typeParameters.getOrPut(descriptor) {
            AstTypeParameterSymbol()
        }

    fun getValueParameterSymbol(descriptor: ParameterDescriptor): AstValueParameterSymbol =
        valueParameters.getOrPut(descriptor) {
            AstValueParameterSymbol(CallableId(descriptor.name))
        }

    fun getTypeAliasSymbol(descriptor: TypeAliasDescriptor): AstTypeAliasSymbol =
        typeAliases.getOrPut(descriptor) {
            AstTypeAliasSymbol(descriptor.classId!!)
        }

    fun getEnumEntrySymbol(descriptor: ClassDescriptor): AstEnumEntrySymbol =
        enumEntries.getOrPut(descriptor) {
            AstEnumEntrySymbol(descriptor.name)
        }

}
