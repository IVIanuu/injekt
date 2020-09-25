package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.generator.uniqueKey
import com.ivianuu.injekt.compiler.irtransform.asNameId
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun DeclarationDescriptor.getContextName(): Name {
    val owner = when (this) {
        is ConstructorDescriptor -> constructedClass.original
        is PropertyAccessorDescriptor -> correspondingProperty.original
        else -> original
    }
    return contextNameOf(
        owner.findPackage().fqName,
        owner.fqNameSafe,
        owner.uniqueKey()
    )
}

fun contextNameOf(
    packageFqName: FqName,
    fqName: FqName,
    uniqueKey: String
) = (joinedNameOf(packageFqName, fqName).asString() + "${uniqueKey.hashCode()}__Context")
    .removeIllegalChars()
    .asNameId()

fun filePositionOf(
    filePath: String,
    startOffset: Int
) = "$filePath:$startOffset"

fun joinedNameOf(
    packageFqName: FqName,
    fqName: FqName
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
