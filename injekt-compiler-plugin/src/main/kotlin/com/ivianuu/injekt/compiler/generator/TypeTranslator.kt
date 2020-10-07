package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.ErrorType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

@Binding
class TypeTranslator(
    private val declarationStore: DeclarationStore
) {

    init {
        declarationStore.typeTranslator = this
    }

    fun toClassifierRef(
        descriptor: ClassifierDescriptor,
        fixType: Boolean = true
    ): ClassifierRef {
        return ClassifierRef(
            descriptor.original.fqNameSafe,
            (descriptor.original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
                ?.map { toClassifierRef(it, fixType) } ?: emptyList(),
            (descriptor.original as? TypeParameterDescriptor)?.upperBounds?.map {
                toTypeRef(it, descriptor, Variance.INVARIANT)
            } ?: emptyList(),
            descriptor is TypeParameterDescriptor,
            descriptor is ClassDescriptor && descriptor.kind == ClassKind.OBJECT,
            descriptor.hasAnnotation(InjektFqNames.FunctionAlias)
        )
    }

    fun toTypeRef2(
        type: KotlinType,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = toTypeRef(type, null as? KtFile, variance, fixType)

    fun toTypeRef(
        type: KotlinType,
        file: KtFile?,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = KotlinTypeRef(type, this, variance)
        .let { if (fixType) fixType(it, file) else it }

    fun toTypeRef(
        type: KotlinType,
        fromDescriptor: DeclarationDescriptor?,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = toTypeRef(type, fromDescriptor?.findPsi()?.containingFile as? KtFile, variance, fixType)

    fun fixType(type: TypeRef, file: KtFile?): TypeRef {
        if (type is KotlinTypeRef) {
            val kotlinType = type.kotlinType
            if (kotlinType is ErrorType) {
                file ?: error("Cannot fix types without file context ${type.render()}")
                val simpleName = kotlinType.presentableName.substringBefore("<").asNameId()
                val imports = file.importDirectives
                val fqName = imports
                    .mapNotNull { it.importPath }
                    .singleOrNull { it.fqName.shortName() == simpleName }
                    ?.fqName
                    ?: file.packageFqName.child(simpleName)
                val generatedClassifier = declarationStore.generatedClassifierFor(fqName)
                if (generatedClassifier != null) {
                    return type.copy(
                        classifier = generatedClassifier,
                        typeArguments = type.typeArguments.map { fixType(it, file) },
                        expandedType = type.expandedType?.let { fixType(it, file) }
                    )
                } else {
                    error("Cannot resolve $type in ${file.virtualFilePath} guessed name '$fqName' " +
                            "Do not use function aliases with '*' imports and import them explicitly")
                }
            }
        }
        return type.copy(
            classifier = type.classifier,
            typeArguments = type.typeArguments.map { fixType(it, file) },
            expandedType = type.expandedType?.let { fixType(it, file) }
        )
    }

}
