package com.ivianuu.injekt.internal

class DoubleCheck0<R>(private var delegate: (() -> R)?) : () -> R {
    private var value: Any? = this
    override fun invoke(): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!()
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck1<P1, R>(private var delegate: ((P1) -> R)?) : (P1) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck2<P1, P2, R>(private var delegate: ((P1, P2) -> R)?) : (P1, P2) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck3<P1, P2, P3, R>(private var delegate: ((P1, P2, P3) -> R)?) : (P1, P2, P3) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck4<P1, P2, P3, P4, R>(private var delegate: ((P1, P2, P3, P4) -> R)?) :
        (P1, P2, P3, P4) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck5<P1, P2, P3, P4, P5, R>(private var delegate: ((P1, P2, P3, P4, P5) -> R)?) :
        (P1, P2, P3, P4, P5) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck6<P1, P2, P3, P4, P5, P6, R>(private var delegate: ((P1, P2, P3, P4, P5, P6) -> R)?) :
        (P1, P2, P3, P4, P5, P6) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck7<P1, P2, P3, P4, P5, P6, P7, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck8<P1, P2, P3, P4, P5, P6, P7, P8, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R {
    private var value: Any? = this
    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R>(private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R)?) :
        (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value =
                        delegate!!(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15)
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17,
                        p18
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17,
                        p18,
                        p19
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17,
                        p18,
                        p19,
                        p20
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17,
                        p18,
                        p19,
                        p20,
                        p21
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}

class DoubleCheck22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R>(
    private var delegate: ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R)?
) : (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R {
    private var value: Any? = this
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
    ): R {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!(
                        p1,
                        p2,
                        p3,
                        p4,
                        p5,
                        p6,
                        p7,
                        p8,
                        p9,
                        p10,
                        p11,
                        p12,
                        p13,
                        p14,
                        p15,
                        p16,
                        p17,
                        p18,
                        p19,
                        p20,
                        p21,
                        p22
                    )
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as R
    }
}
