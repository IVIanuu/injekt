package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.backend.asNameId
import com.ivianuu.injekt.compiler.generator.uniqueKey
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun DeclarationDescriptor.getContextName(): Name {
    return contextNameOf(
        findPackage().fqName,
        fqNameSafe,
        uniqueKey()
    )
}

fun contextNameOf(
    packageFqName: FqName,
    fqName: FqName,
    uniqueKey: String
) = (getJoinedName(packageFqName, fqName).asString()
    .removeIllegalChars() + "${uniqueKey.hashCode()}__Context")
    .removeIllegalChars()
    .asNameId()

fun getJoinedName(
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
