package com.ivianuu.ast

import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.name.FqName

object AstIntrinsics {
    val Package = FqName("ast.intrinsics")

    val LessThan = Package.child("lessThan".nameAsSafeName())
    val GreaterThan = Package.child("greaterThan".nameAsSafeName())
    val LessThanEqual = Package.child("lessThanEqual".nameAsSafeName())
    val GreaterThanEqual = Package.child("greaterThanEqual".nameAsSafeName())

    val StructuralEqual = Package.child("structuralEqual".nameAsSafeName())
    val StructuralNotEqual = Package.child("structuralNotEqual".nameAsSafeName())
    val IdentityEqual = Package.child("identityEqual".nameAsSafeName())
    val IdentityNotEqual = Package.child("identityNotEqual".nameAsSafeName())

    val LazyAnd = Package.child("lazyAnd".nameAsSafeName())
    val LazyOr = Package.child("lazyOr".nameAsSafeName())
}
