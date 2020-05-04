package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Lazy

class ProviderOfLazy0<R>(private val provider: () -> R) : () -> @Lazy () -> R {
    init {
        println("init provider of lazy $provider")
    }

    override fun invoke() = DoubleCheck0(provider)
}

class ProviderOfLazy1<P1, R>(private val provider: (P1) -> R) : (P1) -> @Lazy (P1) -> R {
    override fun invoke(p1: P1) = DoubleCheck1(provider)
}

class ProviderOfLazy2<P1, P2, R>(private val provider: (P1, P2) -> R) :
        (P1, P2) -> @Lazy (P1, P2) -> R {
    override fun invoke(p1: P1, p2: P2) = DoubleCheck2(provider)
}

class ProviderOfLazy3<P1, P2, P3, R>(private val provider: (P1, P2, P3) -> R) :
        (P1, P2, P3) -> @Lazy (P1, P2, P3) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3) = DoubleCheck3(provider)
}

class ProviderOfLazy4<P1, P2, P3, P4, R>(private val provider: (P1, P2, P3, P4) -> R) :
        (P1, P2, P3, P4) -> @Lazy (P1, P2, P3, P4) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4) = DoubleCheck4(provider)
}

class ProviderOfLazy5<P1, P2, P3, P4, P5, R>(private val provider: (P1, P2, P3, P4, P5) -> R) :
        (P1, P2, P3, P4, P5) -> @Lazy (P1, P2, P3, P4, P5) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) = DoubleCheck5(provider)
}

class ProviderOfLazy6<P1, P2, P3, P4, P5, P6, R>(private val provider: (P1, P2, P3, P4, P5, P6) -> R) :
        (P1, P2, P3, P4, P5, P6) -> @Lazy (P1, P2, P3, P4, P5, P6) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) = DoubleCheck6(provider)
}

class ProviderOfLazy7<P1, P2, P3, P4, P5, P6, P7, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7) -> R) :
        (P1, P2, P3, P4, P5, P6, P7) -> @Lazy (P1, P2, P3, P4, P5, P6, P7) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) =
        DoubleCheck7(provider)
}

class ProviderOfLazy8<P1, P2, P3, P4, P5, P6, P7, P8, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) =
        DoubleCheck8(provider)
}

class ProviderOfLazy9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R {
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) =
        DoubleCheck9(provider)
}

class ProviderOfLazy10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10
    ) = DoubleCheck10(provider)
}

class ProviderOfLazy11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11
    ) = DoubleCheck11(provider)
}

class ProviderOfLazy12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12
    ) = DoubleCheck12(provider)
}

class ProviderOfLazy13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13
    ) = DoubleCheck13(provider)
}

class ProviderOfLazy14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14
    ) = DoubleCheck14(provider)
}

class ProviderOfLazy15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R>(private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15
    ) = DoubleCheck15(provider)
}

class ProviderOfLazy16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16
    ) = DoubleCheck16(provider)
}

class ProviderOfLazy17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17
    ) = DoubleCheck17(provider)
}

class ProviderOfLazy18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18
    ) = DoubleCheck18(provider)
}

class ProviderOfLazy19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        p19: P19
    ) = DoubleCheck19(provider)
}

class ProviderOfLazy20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        p19: P19,
        p20: P20
    ) = DoubleCheck20(provider)
}

class ProviderOfLazy21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        p19: P19,
        p20: P20,
        p21: P21
    ) = DoubleCheck21(provider)
}

class ProviderOfLazy22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R>(
    private val provider: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> @Lazy (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R {
    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        p19: P19,
        p20: P20,
        p21: P21,
        p22: P22
    ) = DoubleCheck22(provider)
}
