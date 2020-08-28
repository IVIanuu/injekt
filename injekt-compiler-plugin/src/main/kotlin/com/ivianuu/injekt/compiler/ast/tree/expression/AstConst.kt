package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor

class AstConst<T>(
    override var type: AstType,
    val kind: Kind<T>,
    var value: T
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitConst(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

    sealed class Kind<T>(val asString: kotlin.String) {
        object Null : Kind<Nothing?>("Null")
        object Boolean : Kind<kotlin.Boolean>("Boolean")
        object Char : Kind<kotlin.Char>("Char")
        object Byte : Kind<kotlin.Byte>("Byte")
        object Short : Kind<kotlin.Short>("Short")
        object Int : Kind<kotlin.Int>("Int")
        object Long : Kind<kotlin.Long>("Long")
        object String : Kind<kotlin.String>("String")
        object Float : Kind<kotlin.Float>("Float")
        object Double : Kind<kotlin.Double>("Double")

        override fun toString() = asString
    }

    companion object {
        fun string(type: AstType, value: String): AstConst<String> =
            AstConst(type, Kind.String, value)

        fun int(type: AstType, value: Int): AstConst<Int> =
            AstConst(type, Kind.Int, value)

        fun constNull(type: AstType): AstConst<Nothing?> =
            AstConst(type, Kind.Null, null)

        fun boolean(type: AstType, value: Boolean): AstConst<Boolean> =
            AstConst(type, Kind.Boolean, value)

        fun constTrue(type: AstType): AstConst<Boolean> =
            boolean(type, true)

        fun constFalse(type: AstType): AstConst<Boolean> =
            boolean(type, false)

        fun long(type: AstType, value: Long): AstConst<Long> =
            AstConst(type, Kind.Long, value)

        fun float(type: AstType, value: Float): AstConst<Float> =
            AstConst(type, Kind.Float, value)

        fun double(type: AstType, value: Double): AstConst<Double> =
            AstConst(type, Kind.Double, value)

        fun char(type: AstType, value: Char): AstConst<Char> =
            AstConst(type, Kind.Char, value)

        fun byte(type: AstType, value: Byte): AstConst<Byte> =
            AstConst(type, Kind.Byte, value)

        fun short(type: AstType, value: Short): AstConst<Short> =
            AstConst(type, Kind.Short, value)

    }

}
