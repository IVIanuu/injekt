package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.log
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.upperIfFlexible
import java.io.File

@Reader
fun <D : DeclarationDescriptor> KtDeclaration.descriptor() =
    given<BindingContext>()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as D

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
