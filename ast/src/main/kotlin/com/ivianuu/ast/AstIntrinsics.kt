package com.ivianuu.ast

import com.ivianuu.ast.symbols.CallableId
import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.name.FqName

object AstIntrinsics {
    val Package = FqName("ast.intrinsics")

    val LessThan = CallableId(Package.child("lessThan".nameAsSafeName()))
    val GreaterThan = CallableId(Package.child("greaterThan".nameAsSafeName()))
    val LessThanEqual = CallableId(Package.child("lessThanEqual".nameAsSafeName()))
    val GreaterThanEqual = CallableId(Package.child("greaterThanEqual".nameAsSafeName()))

    val StructuralEqual = CallableId(Package.child("structuralEqual".nameAsSafeName()))
    val StructuralNotEqual = CallableId(Package.child("structuralNotEqual".nameAsSafeName()))
    val IdentityEqual = CallableId(Package.child("identityEqual".nameAsSafeName()))
    val IdentityNotEqual = CallableId(Package.child("identityNotEqual".nameAsSafeName()))

    val LazyAnd = CallableId(Package.child("lazyAnd".nameAsSafeName()))
    val LazyOr = CallableId(Package.child("lazyOr".nameAsSafeName()))


}
