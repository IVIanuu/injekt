package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.log
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.upperIfFlexible
import java.io.File

@Reader
fun <D : DeclarationDescriptor> KtDeclaration.descriptor() =
    given<BindingContext>()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

@Reader
val moduleDescriptor: ModuleDescriptor
    get() = given()

fun KotlinType.prepare(): KotlinType {
    var tmp = refineType()
    if (constructor is IntersectionTypeConstructor) {
        tmp = CommonSupertypes.commonSupertype(constructor.supertypes)
    }
    tmp = tmp.upperIfFlexible()
    return tmp
}

fun DeclarationDescriptor.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun DeclarationDescriptor.hasAnnotatedAnnotationsWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotatedAnnotations(fqName) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotatedAnnotations(
            fqName
        )) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotatedAnnotations(fqName))

fun ClassDescriptor.getGivenConstructor(): ConstructorDescriptor? {
    constructors
        .firstOrNull { it.hasAnnotation(InjektFqNames.Given) }?.let { return it }
    if (!hasAnnotation(InjektFqNames.Given)) return null
    return unsubstitutedPrimaryConstructor
}

fun String.asNameId() = Name.identifier(this)

fun FqName.toFactoryImplFqName() =
    FqName("${asString()}Impl")

@Reader
fun generateFile(
    packageFqName: FqName,
    fileName: String,
    code: String,
): File {
    val newFile = given<SrcDir>()
        .resolve(packageFqName.asString().replace(".", "/"))
        .also { it.mkdirs() }
        .resolve(fileName)

    log { "generated file $packageFqName.$fileName $code" }

    return newFile
        .also { it.createNewFile() }
        .also { it.writeText(code) }
}

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun joinedNameOf(
    packageFqName: FqName,
    fqName: FqName,
): Name {
    val joinedSegments = fqName.asString()
        .removePrefix(packageFqName.asString() + ".")
        .split(".")
    return joinedSegments.joinToString("_").asNameId()
}

fun String.removeIllegalChars() =
    replace(".", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
        .replace("@", "")
        .replace(",", "")
        .replace(" ", "")
        .replace("-", "")

@Reader
fun FqName.toMemberScope(): MemberScope? {
    val pkg = moduleDescriptor.getPackage(this)

    if (isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

    val parentMemberScope = parent().toMemberScope() ?: return null

    val classDescriptor =
        parentMemberScope.getContributedClassifier(
            shortName(),
            NoLookupLocation.FROM_BACKEND
        ) as? ClassDescriptor ?: return null

    return classDescriptor.unsubstitutedMemberScope
}

@Reader
fun FqName.asClassDescriptor(): ClassDescriptor {
    return parent().toMemberScope()!!.getContributedClassifier(
        shortName(), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
}

@Reader
fun TypeRef.getAllCallables(): List<Callable> {
    val callables = mutableListOf<Callable>()

    fun TypeRef.collect(typeArguments: List<TypeRef>) {
        val substitutionMap = classifier.typeParameters
            .zip(typeArguments)
            .toMap()

        callables += classifier.fqName.asClassDescriptor()
            .unsubstitutedMemberScope
            .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
            .filterIsInstance<CallableDescriptor>()
            .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
            .mapNotNull {
                when (it) {
                    is FunctionDescriptor -> it.toCallableRef()
                    is PropertyDescriptor -> it.getter!!.toCallableRef()
                    else -> null
                }
            }

        superTypes
            .map { it.substitute(substitutionMap) }
            .forEach { it.collect(it.typeArguments) }
    }

    collect(typeArguments)

    return callables
}

@Reader
fun getFactoryForType(type: TypeRef): FactoryDescriptor = FactoryDescriptor(type)

@Reader
fun getModuleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
    val descriptor = type.classifier.fqName.asClassDescriptor()
    val substitutionMap = type.classifier.typeParameters
        .zip(type.typeArguments)
        .toMap()
    return ModuleDescriptor(
        type = type,
        callables = descriptor.unsubstitutedMemberScope.getContributedDescriptors(
            DescriptorKindFilter.CALLABLES
        ).filter {
            it.hasAnnotationWithPropertyAndClass(
                InjektFqNames.Given
            ) || it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) ||
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) ||
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module)
        }
            .mapNotNull {
                when (it) {
                    is PropertyDescriptor -> it.getter!!
                    is FunctionDescriptor -> it
                    else -> null
                }
            }
            .map { it.toCallableRef() }
    )
}

@Reader
fun getFunctionForAlias(aliasType: TypeRef): Callable {
    return aliasType.classifier.fqName.parent().toMemberScope()!!
        .getContributedFunctions(aliasType.classifier.fqName.shortName(),
            NoLookupLocation.FROM_BACKEND)
        .single()
        .toCallableRef()
}
