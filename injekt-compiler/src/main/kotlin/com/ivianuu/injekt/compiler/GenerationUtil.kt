package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSType
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val KSFunctionDeclaration.isConstructor: Boolean
    get() = simpleName.asString() == "<init>"

fun KSAnnotated.hasAnnotationWithPropertyAndClass(
    target: KSType
): Boolean = hasAnnotation(target) ||
    (this is KSPropertyAccessor && receiver.hasAnnotation(target)) ||
    (this is KSFunctionDeclaration && isConstructor && returnType!!.resolve().declaration.hasAnnotation(target))

fun KSAnnotated.hasAnnotatedAnnotations(target: KSType): Boolean =
    getAnnotatedAnnotations(target).isNotEmpty()

@JvmName("hasAnnotatedAnnotationsNullable")
fun KSAnnotated.hasAnnotatedAnnotations(target: KSType?): Boolean =
    getAnnotatedAnnotations(target).isNotEmpty()

fun KSAnnotated.getAnnotatedAnnotations(target: KSType): List<KSAnnotation> =
    annotations
        .filter { it.annotationType.resolve().declaration.hasAnnotation(target) }

@JvmName("getAnnotatedAnnotationsNullable")
fun KSAnnotated.getAnnotatedAnnotations(target: KSType?): List<KSAnnotation> {
    if (target == null) return emptyList()
    return annotations
        .filter { it.annotationType.resolve().declaration.hasAnnotation(target) }
}

fun KSClassDeclaration.getInjectConstructor(injektTypes: InjektTypes): KSFunctionDeclaration? {
    getAllFunctions()
        .filter { it.isConstructor }
        .firstOrNull {
            it.hasAnnotation(injektTypes.binding) ||
                    it.hasAnnotatedAnnotations(injektTypes.bindingModule)
        }?.let { return it }
    if (!hasAnnotation(injektTypes.binding) && !hasAnnotatedAnnotations(injektTypes.bindingModule)) return null
    return primaryConstructor
}

fun String.asNameId() = Name.identifier(this)

fun String.toComponentImplFqName() = "${this}Impl"

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

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

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
    annotations.hasAnnotation(fqName)

fun getBindingFunctionType(
    assistedParameters: List<TypeRef>,
    isSuspend: Boolean,
    returnType: TypeRef,
    resolver: Resolver,
    injektTypes: InjektTypes
): TypeRef {
    return (
            if (isSuspend) resolver.getClassDeclarationByName("kotlin.SuspendFunction${assistedParameters.size}")!!
            else resolver.getClassDeclarationByName("kotlin.Function${assistedParameters.size}")!!
            )
        .asType()
        .toTypeRef(injektTypes)
        .typeWith(assistedParameters + returnType)
}

fun AnnotationDescriptor.hasAnnotation(annotation: FqName): Boolean =
    type.constructor.declarationDescriptor!!.hasAnnotation(annotation)

fun Resolver.getClassDeclarationByName(fqName: String): KSClassDeclaration {
    return getClassDeclarationByName(getKSNameFromString(fqName)) ?: error("Class '$fqName' not found.")
}

fun Resolver.findClassDeclarationByName(fqName: String): KSClassDeclaration? {
    return getClassDeclarationByName(getKSNameFromString(fqName))
}

fun KSClassDeclaration.asType() = asType(emptyList())

fun KSAnnotated.hasAnnotation(target: KSType): Boolean {
    return findAnnotationWithType(target) != null
}

@JvmName("hasAnnotationNullable")
fun KSAnnotated.hasAnnotation(target: KSType?): Boolean {
    if (target == null) return false
    return findAnnotationWithType(target) != null
}

fun KSType.hasAnnotation(target: KSType): Boolean {
    return findAnnotationWithType(target) != null
}

@JvmName("hasAnnotationNullable")
fun KSType.hasAnnotation(target: KSType?): Boolean {
    if (target == null) return false
    return findAnnotationWithType(target) != null
}

inline fun <reified T : Annotation> KSAnnotated.findAnnotationWithType(resolver: Resolver, ): KSAnnotation? {
    return findAnnotationWithType(resolver.getClassDeclarationByName<T>()!!.asType())
}

inline fun <reified T : Annotation> KSType.findAnnotationWithType(resolver: Resolver, ): KSAnnotation? {
    return findAnnotationWithType(resolver.getClassDeclarationByName<T>()!!.asType())
}

fun KSAnnotated.findAnnotationWithType(target: KSType): KSAnnotation? {
    return annotations.find { it.annotationType.resolve() == target }
}

fun KSType.findAnnotationWithType(target: KSType): KSAnnotation? {
    return annotations.find { it.annotationType.resolve() == target }
}

inline fun <reified T> KSAnnotation.getMember(name: String): T {
    val matchingArg = arguments.find { it.name?.asString() == name }
        ?: error(
            "No member name found for '$name'. All arguments: ${arguments.map { it.name?.asString() }}")
    return when (val argValue = matchingArg.value) {
        is List<*> -> {
            if (argValue.isEmpty()) {
                argValue as T
            } else {
                val first = argValue[0]
                if (first is KSType) {
                    argValue.map { (it as KSType) } as T
                } else {
                    argValue as T
                }
            }
        }
        is KSType -> argValue as T
        else -> {
            argValue as? T ?: error("No value found for $name. Was ${matchingArg.value}")
        }
    }
}

fun String.shortName() = substringAfterLast(".")

fun String.parent() = substringBeforeLast(".")

fun String.child(name: String) = "$this.$name"

fun CodeGenerator.generateFile(
    packageName: String,
    fileName: String,
    code: String
) {
    createNewFile(packageName, fileName).bufferedWriter().use { it.write(code) }
    println("wrote file $packageName.$fileName.kt\n$code")
}