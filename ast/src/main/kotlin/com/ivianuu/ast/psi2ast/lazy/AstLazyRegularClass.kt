package com.ivianuu.ast.psi2ast.lazy

import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.psi2ast.Psi2AstGeneratorContext
import com.ivianuu.ast.psi2ast.platformStatus
import com.ivianuu.ast.psi2ast.toAstVisibility
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name

class AstLazyRegularClass(
    override val symbol: AstRegularClassSymbol,
    private val descriptor: ClassDescriptor,
    override val context: Psi2AstGeneratorContext
) : AstRegularClass() {
    override val annotations: MutableList<AstFunctionCall> by lazyVar {
        descriptor.annotations.mapNotNullTo(mutableListOf()) {
            context.constantValueGenerator.generateAnnotationConstructorCall(
                it
            )
        }
    }
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Library
    override var attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override var name: Name = descriptor.name
    override var visibility: Visibility = descriptor.visibility.toAstVisibility()
    override var modality: Modality = descriptor.modality
    override var platformStatus: PlatformStatus = descriptor.platformStatus
    override val typeParameters: MutableList<AstTypeParameter> by lazyVar {
        descriptor.declaredTypeParameters.mapTo(mutableListOf()) {
            context.symbolTable.getSymbol<AstTypeParameterSymbol>(it)
            context.stubGenerator.getDeclaration(it) as AstTypeParameter
        }
    }
    override val declarations: MutableList<AstDeclaration> by lazyVar {
        mutableListOf<AstDeclaration>().apply {
            this += descriptor.constructors
                .onEach { context.symbolTable.getSymbol<AstConstructorSymbol>(it) }
                .map { context.stubGenerator.getDeclaration(it) as AstConstructor }
            this += (descriptor.defaultType.memberScope.getContributedDescriptors()
                .filterNot { it is PropertyAccessorDescriptor } + descriptor.staticScope.getContributedDescriptors())
                .onEach { context.symbolTable.getSymbol<AstSymbol<*>>(it) }
                .map { context.stubGenerator.getDeclaration(it) as AstDeclaration }
        }
    }
    override var classKind: ClassKind = descriptor.kind
    override val delegateInitializers: MutableList<AstDelegateInitializer> = mutableListOf()
    override val superTypes: MutableList<AstType> by lazyVar {
        descriptor.typeConstructor.supertypes.mapTo(mutableListOf()) {
            context.typeConverter.convert(it)
        }
    }
    override var isInline: Boolean = descriptor.isInline
    override var isCompanion: Boolean = descriptor.isCompanionObject
    override var isFun: Boolean = descriptor.isFun
    override var isData: Boolean = descriptor.isData
    override var isInner: Boolean = descriptor.isInner
    override var isExternal: Boolean = descriptor.isExternal

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        delegateInitializers.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstLazyRegularClass {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        delegateInitializers.transformInplace(transformer, data)
        superTypes.transformInplace(transformer, data)
        return this
    }

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin) {
        origin = newOrigin
    }

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes) {
        attributes = newAttributes
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations += newAnnotations
    }

    override fun replaceVisibility(newVisibility: Visibility) {
        visibility = newVisibility
    }

    override fun replaceModality(newModality: Modality) {
        modality = newModality
    }

    override fun replacePlatformStatus(newPlatformStatus: PlatformStatus) {
        platformStatus = newPlatformStatus
    }

    override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>) {
        typeParameters.clear()
        typeParameters += newTypeParameters
    }

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>) {
        declarations.clear()
        declarations += newDeclarations
    }

    override fun replaceClassKind(newClassKind: ClassKind) {
        classKind = newClassKind
    }

    override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>) {
        delegateInitializers.clear()
        delegateInitializers += newDelegateInitializers
    }

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {
        superTypes.clear()
        superTypes += newSuperTypes
    }

    override fun replaceIsInline(newIsInline: Boolean) {
        isInline = newIsInline
    }

    override fun replaceIsCompanion(newIsCompanion: Boolean) {
        isCompanion = newIsCompanion
    }

    override fun replaceIsFun(newIsFun: Boolean) {
        isFun = newIsFun
    }

    override fun replaceIsData(newIsData: Boolean) {
        isData = newIsData
    }

    override fun replaceIsInner(newIsInner: Boolean) {
        isInner = newIsInner
    }

    override fun replaceIsExternal(newIsExternal: Boolean) {
        isExternal = newIsExternal
    }

}
