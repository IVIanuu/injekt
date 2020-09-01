package com.ivianuu.ast.psi

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.declaration.AstClass
import com.ivianuu.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.ast.tree.declaration.AstFunction
import com.ivianuu.ast.tree.declaration.AstProperty
import com.ivianuu.ast.tree.declaration.AstTypeAlias
import com.ivianuu.ast.tree.declaration.AstTypeParameter
import com.ivianuu.ast.tree.declaration.AstValueParameter
import com.ivianuu.ast.tree.declaration.addChild
import com.ivianuu.ast.tree.expression.AstBlock
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class Psi2AstStubGenerator(private val storage: Psi2AstStorage) {

    lateinit var typeMapper: TypeMapper

    fun <T : AstElement> get(descriptor: DeclarationDescriptor): T {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor.toAstPackageFragment()
            is ClassDescriptor -> getClassStub(descriptor)
            is FunctionDescriptor -> getFunctionStub(descriptor)
            is PropertyDescriptor -> getPropertyStub(descriptor)
            is TypeParameterDescriptor -> getTypeParameterStub(descriptor)
            is ValueParameterDescriptor -> getValueParameterStub(descriptor)
            is TypeAliasDescriptor -> getTypeAliasStub(descriptor)
            else -> error("Unexpected descriptor $descriptor ${descriptor.javaClass}")
        } as T
    }

    private fun PackageFragmentDescriptor.toAstPackageFragment() =
        storage.externalPackageFragments.getOrPut(this) {
            AstExternalPackageFragment(fqName)
        }

    private fun getClassStub(descriptor: ClassDescriptor): AstClass {
        storage.classes[descriptor.original]?.let { return it }
        return AstLazyClass(
            descriptor = descriptor,
            stubGenerator = this
        ).apply {
            storage.classes[descriptor.original] = this
            descriptor.getContainingDeclarationContainer().addChild(this)
        }
    }

    private fun getFunctionStub(descriptor: FunctionDescriptor): AstFunction {
        storage.functions[descriptor.original]?.let { return it }
        return AstFunction(
            name = descriptor.name,
            kind = descriptor.toAstFunctionKind(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
            modality = descriptor.modality.toAstModality(),
            returnType = typeMapper.translate(descriptor.returnType!!),
            isInline = descriptor.isInline,
            isExternal = descriptor.isExternal,
            isInfix = descriptor.isInfix,
            isOperator = descriptor.isOperator,
            isTailrec = descriptor.isTailrec,
            isSuspend = descriptor.isSuspend
        ).apply {
            storage.functions[descriptor.original] = this
            descriptor.getContainingDeclarationContainer().addChild(this)
            // todo annotations += this@toAstFunctionStub.annotations
            typeParameters += descriptor.typeParameters
                .map { get<AstTypeParameter>(it) }
                .onEach { it.parent = this@apply }
            dispatchReceiverType =
                descriptor.dispatchReceiverParameter?.type?.let { typeMapper.translate(it) }
            extensionReceiverType =
                descriptor.extensionReceiverParameter?.type?.let { typeMapper.translate(it) }
            valueParameters += descriptor.valueParameters
                .map { get<AstValueParameter>(it) }
                .onEach { it.parent = this@apply }
            overriddenDeclarations += descriptor.overriddenDescriptors
                .map { getFunctionStub(it) }
        }
    }

    private fun getPropertyStub(descriptor: PropertyDescriptor): AstProperty {
        storage.properties[descriptor.original]?.let { return it }
        return AstProperty(
            name = descriptor.name,
            type = typeMapper.translate(descriptor.type),
            kind = when {
                descriptor.isConst -> AstProperty.Kind.CONST_VAL
                descriptor.isLateInit -> AstProperty.Kind.LATEINIT_VAR
                descriptor.isVar -> AstProperty.Kind.VAR
                else -> AstProperty.Kind.VAl
            },
            modality = descriptor.modality.toAstModality(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
            isExternal = descriptor.isExternal
        ).apply {
            storage.properties[descriptor.original] = this
            descriptor.getContainingDeclarationContainer().addChild(this)
            // todo annotations += this@toAstPropertyStub.annotations.toAstAnnotations()
            typeParameters += descriptor.typeParameters
                .map { get<AstTypeParameter>(it) }
                .onEach { it.parent = this@apply }
            dispatchReceiverType =
                descriptor.dispatchReceiverParameter?.type?.let { typeMapper.translate(it) }
            extensionReceiverType =
                descriptor.extensionReceiverParameter?.type?.let { typeMapper.translate(it) }
        }
    }

    private fun getTypeParameterStub(descriptor: TypeParameterDescriptor): AstTypeParameter {
        storage.typeParameters[descriptor.original]?.let { return it }
        return AstTypeParameter(
            name = descriptor.name,
            isReified = descriptor.isReified,
            variance = descriptor.variance.toAstVariance()
        ).apply {
            // todo annotations += tdes.annotationEntries.map { it.accept() }
            superTypes += descriptor.upperBounds.map { typeMapper.translate(it) }
        }
    }

    private fun getValueParameterStub(descriptor: ValueParameterDescriptor): AstValueParameter {
        storage.valueParameters[descriptor]?.let { return it }
        return AstValueParameter(
            name = descriptor.name,
            type = typeMapper.translate(descriptor.type),
            isVararg = descriptor.isVararg,
            inlineHint = when {
                descriptor.isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                descriptor.isNoinline -> AstValueParameter.InlineHint.NOINLINE
                else -> null
            }
        ).apply {
            storage.valueParameters[descriptor] = this
            // todo annotations += parameter.annotationEntries.map { it.accept() }
            defaultValue = if (descriptor.hasDefaultValue()) AstBlock(type) else null
        }
    }

    private fun getTypeAliasStub(descriptor: TypeAliasDescriptor): AstTypeAlias {
        storage.typeAliases[descriptor.original]?.let { return it }
        return AstTypeAlias(
            name = descriptor.name,
            type = typeMapper.translate(descriptor.expandedType),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect)
        ).apply {
            storage.typeAliases[descriptor.original] = this
            descriptor.getContainingDeclarationContainer().addChild(this)
            // todo annotations += this@toAstTypeAliasStub.annotations.toAstAnnotations()
            typeParameters += descriptor.declaredTypeParameters
                .map { get<AstTypeParameter>(it) }
                .onEach { it.parent = this@apply }
        }
    }

    private fun DeclarationDescriptor.getContainingDeclarationContainer() =
        containingDeclaration!!.asContainingDeclaration()

    private fun DeclarationDescriptor.asContainingDeclaration(): AstDeclarationContainer =
        when (this) {
            is TypeAliasDescriptor -> classDescriptor!!.asContainingDeclaration()
            else -> get(this) as AstDeclarationContainer
        }

}
