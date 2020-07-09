/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.comparison.fibonacci

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Unscoped
import com.ivianuu.injekt.get
import javax.inject.Inject

class Fib1 @Inject constructor()
@Unscoped
@Reader
fun fib1() = Fib1()
class Fib2 @Inject constructor()

@Unscoped
@Reader
fun fib2() = Fib2()
class Fib3 @Inject constructor(
    val fibM1: Fib2,
    val fibM2: Fib1
)

@Unscoped
@Reader
fun fib3() = Fib3(get(), get())
class Fib4 @Inject constructor(
    val fibM1: Fib3,
    val fibM2: Fib2
)

@Unscoped
@Reader
fun fib4() = Fib4(get(), get())
class Fib5 @Inject constructor(
    val fibM1: Fib4,
    val fibM2: Fib3
)

@Unscoped
@Reader
fun fib5() = Fib5(get(), get())
class Fib6 @Inject constructor(
    val fibM1: Fib5,
    val fibM2: Fib4
)

@Unscoped
@Reader
fun fib6() = Fib6(get(), get())
class Fib7 @Inject constructor(
    val fibM1: Fib6,
    val fibM2: Fib5
)

@Unscoped
@Reader
fun fib7() = Fib7(get(), get())
class Fib8 @Inject constructor(
    val fibM1: Fib7,
    val fibM2: Fib6
)

@Unscoped
@Reader
fun fib8() = Fib8(get(), get())
class Fib9 @Inject constructor(
    val fibM1: Fib8,
    val fibM2: Fib7
)

@Unscoped
@Reader
fun fib9() = Fib9(get(), get())
class Fib10 @Inject constructor(
    val fibM1: Fib9,
    val fibM2: Fib8
)

@Unscoped
@Reader
fun fib10() = Fib10(get(), get())
class Fib11 @Inject constructor(
    val fibM1: Fib10,
    val fibM2: Fib9
)

@Unscoped
@Reader
fun fib11() = Fib11(get(), get())
class Fib12 @Inject constructor(
    val fibM1: Fib11,
    val fibM2: Fib10
)

@Unscoped
@Reader
fun fib12() = Fib12(get(), get())
class Fib13 @Inject constructor(
    val fibM1: Fib12,
    val fibM2: Fib11
)

@Unscoped
@Reader
fun fib13() = Fib13(get(), get())
class Fib14 @Inject constructor(
    val fibM1: Fib13,
    val fibM2: Fib12
)

@Unscoped
@Reader
fun fib14() = Fib14(get(), get())
class Fib15 @Inject constructor(
    val fibM1: Fib14,
    val fibM2: Fib13
)

@Unscoped
@Reader
fun fib15() = Fib15(get(), get())
class Fib16 @Inject constructor(
    val fibM1: Fib15,
    val fibM2: Fib14
)

@Unscoped
@Reader
fun fib16() = Fib16(get(), get())
class Fib17 @Inject constructor(
    val fibM1: Fib16,
    val fibM2: Fib15
)

@Unscoped
@Reader
fun fib17() = Fib17(get(), get())
class Fib18 @Inject constructor(
    val fibM1: Fib17,
    val fibM2: Fib16
)

@Unscoped
@Reader
fun fib18() = Fib18(get(), get())
class Fib19 @Inject constructor(
    val fibM1: Fib18,
    val fibM2: Fib17
)

@Unscoped
@Reader
fun fib19() = Fib19(get(), get())
class Fib20 @Inject constructor(
    val fibM1: Fib19,
    val fibM2: Fib18
)

@Unscoped
@Reader
fun fib20() = Fib20(get(), get())
class Fib21 @Inject constructor(
    val fibM1: Fib20,
    val fibM2: Fib19
)

@Unscoped
@Reader
fun fib21() = Fib21(get(), get())
class Fib22 @Inject constructor(
    val fibM1: Fib21,
    val fibM2: Fib20
)

@Unscoped
@Reader
fun fib22() = Fib22(get(), get())
class Fib23 @Inject constructor(
    val fibM1: Fib22,
    val fibM2: Fib21
)

@Unscoped
@Reader
fun fib23() = Fib23(get(), get())
class Fib24 @Inject constructor(
    val fibM1: Fib23,
    val fibM2: Fib22
)

@Unscoped
@Reader
fun fib24() = Fib24(get(), get())
class Fib25 @Inject constructor(
    val fibM1: Fib24,
    val fibM2: Fib23
)

@Unscoped
@Reader
fun fib25() = Fib25(get(), get())
class Fib26 @Inject constructor(
    val fibM1: Fib25,
    val fibM2: Fib24
)

@Unscoped
@Reader
fun fib26() = Fib26(get(), get())
class Fib27 @Inject constructor(
    val fibM1: Fib26,
    val fibM2: Fib25
)

@Unscoped
@Reader
fun fib27() = Fib27(get(), get())
class Fib28 @Inject constructor(
    val fibM1: Fib27,
    val fibM2: Fib26
)

@Unscoped
@Reader
fun fib28() = Fib28(get(), get())
class Fib29 @Inject constructor(
    val fibM1: Fib28,
    val fibM2: Fib27
)

@Unscoped
@Reader
fun fib29() = Fib29(get(), get())
class Fib30 @Inject constructor(
    val fibM1: Fib29,
    val fibM2: Fib28
)

@Unscoped
@Reader
fun fib30() = Fib30(get(), get())
class Fib31 @Inject constructor(
    val fibM1: Fib30,
    val fibM2: Fib29
)

@Unscoped
@Reader
fun fib31() = Fib31(get(), get())
class Fib32 @Inject constructor(
    val fibM1: Fib31,
    val fibM2: Fib30
)

@Unscoped
@Reader
fun fib32() = Fib32(get(), get())
class Fib33 @Inject constructor(
    val fibM1: Fib32,
    val fibM2: Fib31
)

@Unscoped
@Reader
fun fib33() = Fib33(get(), get())
class Fib34 @Inject constructor(
    val fibM1: Fib33,
    val fibM2: Fib32
)

@Unscoped
@Reader
fun fib34() = Fib34(get(), get())
class Fib35 @Inject constructor(
    val fibM1: Fib34,
    val fibM2: Fib33
)

@Unscoped
@Reader
fun fib35() = Fib35(get(), get())
class Fib36 @Inject constructor(
    val fibM1: Fib35,
    val fibM2: Fib34
)

@Unscoped
@Reader
fun fib36() = Fib36(get(), get())
class Fib37 @Inject constructor(
    val fibM1: Fib36,
    val fibM2: Fib35
)

@Unscoped
@Reader
fun fib37() = Fib37(get(), get())
class Fib38 @Inject constructor(
    val fibM1: Fib37,
    val fibM2: Fib36
)

@Unscoped
@Reader
fun fib38() = Fib38(get(), get())
class Fib39 @Inject constructor(
    val fibM1: Fib38,
    val fibM2: Fib37
)

@Unscoped
@Reader
fun fib39() = Fib39(get(), get())
class Fib40 @Inject constructor(
    val fibM1: Fib39,
    val fibM2: Fib38
)

@Unscoped
@Reader
fun fib40() = Fib40(get(), get())
class Fib41 @Inject constructor(
    val fibM1: Fib40,
    val fibM2: Fib39
)

@Unscoped
@Reader
fun fib41() = Fib41(get(), get())
class Fib42 @Inject constructor(
    val fibM1: Fib41,
    val fibM2: Fib40
)

@Unscoped
@Reader
fun fib42() = Fib42(get(), get())
class Fib43 @Inject constructor(
    val fibM1: Fib42,
    val fibM2: Fib41
)

@Unscoped
@Reader
fun fib43() = Fib43(get(), get())
class Fib44 @Inject constructor(
    val fibM1: Fib43,
    val fibM2: Fib42
)

@Unscoped
@Reader
fun fib44() = Fib44(get(), get())
class Fib45 @Inject constructor(
    val fibM1: Fib44,
    val fibM2: Fib43
)

@Unscoped
@Reader
fun fib45() = Fib45(get(), get())
class Fib46 @Inject constructor(
    val fibM1: Fib45,
    val fibM2: Fib44
)

@Unscoped
@Reader
fun fib46() = Fib46(get(), get())
class Fib47 @Inject constructor(
    val fibM1: Fib46,
    val fibM2: Fib45
)

@Unscoped
@Reader
fun fib47() = Fib47(get(), get())
class Fib48 @Inject constructor(
    val fibM1: Fib47,
    val fibM2: Fib46
)

@Unscoped
@Reader
fun fib48() = Fib48(get(), get())
class Fib49 @Inject constructor(
    val fibM1: Fib48,
    val fibM2: Fib47
)

@Unscoped
@Reader
fun fib49() = Fib49(get(), get())
class Fib50 @Inject constructor(
    val fibM1: Fib49,
    val fibM2: Fib48
)

@Unscoped
@Reader
fun fib50() = Fib50(get(), get())
class Fib51 @Inject constructor(
    val fibM1: Fib50,
    val fibM2: Fib49
)

@Unscoped
@Reader
fun fib51() = Fib51(get(), get())
class Fib52 @Inject constructor(
    val fibM1: Fib51,
    val fibM2: Fib50
)

@Unscoped
@Reader
fun fib52() = Fib52(get(), get())
class Fib53 @Inject constructor(
    val fibM1: Fib52,
    val fibM2: Fib51
)

@Unscoped
@Reader
fun fib53() = Fib53(get(), get())
class Fib54 @Inject constructor(
    val fibM1: Fib53,
    val fibM2: Fib52
)

@Unscoped
@Reader
fun fib54() = Fib54(get(), get())
class Fib55 @Inject constructor(
    val fibM1: Fib54,
    val fibM2: Fib53
)

@Unscoped
@Reader
fun fib55() = Fib55(get(), get())
class Fib56 @Inject constructor(
    val fibM1: Fib55,
    val fibM2: Fib54
)

@Unscoped
@Reader
fun fib56() = Fib56(get(), get())
class Fib57 @Inject constructor(
    val fibM1: Fib56,
    val fibM2: Fib55
)

@Unscoped
@Reader
fun fib57() = Fib57(get(), get())
class Fib58 @Inject constructor(
    val fibM1: Fib57,
    val fibM2: Fib56
)

@Unscoped
@Reader
fun fib58() = Fib58(get(), get())
class Fib59 @Inject constructor(
    val fibM1: Fib58,
    val fibM2: Fib57
)

@Unscoped
@Reader
fun fib59() = Fib59(get(), get())
class Fib60 @Inject constructor(
    val fibM1: Fib59,
    val fibM2: Fib58
)

@Unscoped
@Reader
fun fib60() = Fib60(get(), get())
class Fib61 @Inject constructor(
    val fibM1: Fib60,
    val fibM2: Fib59
)

@Unscoped
@Reader
fun fib61() = Fib61(get(), get())
class Fib62 @Inject constructor(
    val fibM1: Fib61,
    val fibM2: Fib60
)

@Unscoped
@Reader
fun fib62() = Fib62(get(), get())
class Fib63 @Inject constructor(
    val fibM1: Fib62,
    val fibM2: Fib61
)

@Unscoped
@Reader
fun fib63() = Fib63(get(), get())
class Fib64 @Inject constructor(
    val fibM1: Fib63,
    val fibM2: Fib62
)

@Unscoped
@Reader
fun fib64() = Fib64(get(), get())
class Fib65 @Inject constructor(
    val fibM1: Fib64,
    val fibM2: Fib63
)

@Unscoped
@Reader
fun fib65() = Fib65(get(), get())
class Fib66 @Inject constructor(
    val fibM1: Fib65,
    val fibM2: Fib64
)

@Unscoped
@Reader
fun fib66() = Fib66(get(), get())
class Fib67 @Inject constructor(
    val fibM1: Fib66,
    val fibM2: Fib65
)

@Unscoped
@Reader
fun fib67() = Fib67(get(), get())
class Fib68 @Inject constructor(
    val fibM1: Fib67,
    val fibM2: Fib66
)

@Unscoped
@Reader
fun fib68() = Fib68(get(), get())
class Fib69 @Inject constructor(
    val fibM1: Fib68,
    val fibM2: Fib67
)

@Unscoped
@Reader
fun fib69() = Fib69(get(), get())
class Fib70 @Inject constructor(
    val fibM1: Fib69,
    val fibM2: Fib68
)

@Unscoped
@Reader
fun fib70() = Fib70(get(), get())
class Fib71 @Inject constructor(
    val fibM1: Fib70,
    val fibM2: Fib69
)

@Unscoped
@Reader
fun fib71() = Fib71(get(), get())
class Fib72 @Inject constructor(
    val fibM1: Fib71,
    val fibM2: Fib70
)

@Unscoped
@Reader
fun fib72() = Fib72(get(), get())
class Fib73 @Inject constructor(
    val fibM1: Fib72,
    val fibM2: Fib71
)

@Unscoped
@Reader
fun fib73() = Fib73(get(), get())
class Fib74 @Inject constructor(
    val fibM1: Fib73,
    val fibM2: Fib72
)

@Unscoped
@Reader
fun fib74() = Fib74(get(), get())
class Fib75 @Inject constructor(
    val fibM1: Fib74,
    val fibM2: Fib73
)

@Unscoped
@Reader
fun fib75() = Fib75(get(), get())
class Fib76 @Inject constructor(
    val fibM1: Fib75,
    val fibM2: Fib74
)

@Unscoped
@Reader
fun fib76() = Fib76(get(), get())
class Fib77 @Inject constructor(
    val fibM1: Fib76,
    val fibM2: Fib75
)

@Unscoped
@Reader
fun fib77() = Fib77(get(), get())
class Fib78 @Inject constructor(
    val fibM1: Fib77,
    val fibM2: Fib76
)

@Unscoped
@Reader
fun fib78() = Fib78(get(), get())
class Fib79 @Inject constructor(
    val fibM1: Fib78,
    val fibM2: Fib77
)

@Unscoped
@Reader
fun fib79() = Fib79(get(), get())
class Fib80 @Inject constructor(
    val fibM1: Fib79,
    val fibM2: Fib78
)

@Unscoped
@Reader
fun fib80() = Fib80(get(), get())
class Fib81 @Inject constructor(
    val fibM1: Fib80,
    val fibM2: Fib79
)

@Unscoped
@Reader
fun fib81() = Fib81(get(), get())
class Fib82 @Inject constructor(
    val fibM1: Fib81,
    val fibM2: Fib80
)

@Unscoped
@Reader
fun fib82() = Fib82(get(), get())
class Fib83 @Inject constructor(
    val fibM1: Fib82,
    val fibM2: Fib81
)

@Unscoped
@Reader
fun fib83() = Fib83(get(), get())
class Fib84 @Inject constructor(
    val fibM1: Fib83,
    val fibM2: Fib82
)

@Unscoped
@Reader
fun fib84() = Fib84(get(), get())
class Fib85 @Inject constructor(
    val fibM1: Fib84,
    val fibM2: Fib83
)

@Unscoped
@Reader
fun fib85() = Fib85(get(), get())
class Fib86 @Inject constructor(
    val fibM1: Fib85,
    val fibM2: Fib84
)

@Unscoped
@Reader
fun fib86() = Fib86(get(), get())
class Fib87 @Inject constructor(
    val fibM1: Fib86,
    val fibM2: Fib85
)

@Unscoped
@Reader
fun fib87() = Fib87(get(), get())
class Fib88 @Inject constructor(
    val fibM1: Fib87,
    val fibM2: Fib86
)

@Unscoped
@Reader
fun fib88() = Fib88(get(), get())
class Fib89 @Inject constructor(
    val fibM1: Fib88,
    val fibM2: Fib87
)

@Unscoped
@Reader
fun fib89() = Fib89(get(), get())
class Fib90 @Inject constructor(
    val fibM1: Fib89,
    val fibM2: Fib88
)

@Unscoped
@Reader
fun fib90() = Fib90(get(), get())
class Fib91 @Inject constructor(
    val fibM1: Fib90,
    val fibM2: Fib89
)

@Unscoped
@Reader
fun fib91() = Fib91(get(), get())
class Fib92 @Inject constructor(
    val fibM1: Fib91,
    val fibM2: Fib90
)

@Unscoped
@Reader
fun fib92() = Fib92(get(), get())
class Fib93 @Inject constructor(
    val fibM1: Fib92,
    val fibM2: Fib91
)

@Unscoped
@Reader
fun fib93() = Fib93(get(), get())
class Fib94 @Inject constructor(
    val fibM1: Fib93,
    val fibM2: Fib92
)

@Unscoped
@Reader
fun fib94() = Fib94(get(), get())
class Fib95 @Inject constructor(
    val fibM1: Fib94,
    val fibM2: Fib93
)

@Unscoped
@Reader
fun fib95() = Fib95(get(), get())
class Fib96 @Inject constructor(
    val fibM1: Fib95,
    val fibM2: Fib94
)

@Unscoped
@Reader
fun fib96() = Fib96(get(), get())
class Fib97 @Inject constructor(
    val fibM1: Fib96,
    val fibM2: Fib95
)

@Unscoped
@Reader
fun fib97() = Fib97(get(), get())
class Fib98 @Inject constructor(
    val fibM1: Fib97,
    val fibM2: Fib96
)

@Unscoped
@Reader
fun fib98() = Fib98(get(), get())
class Fib99 @Inject constructor(
    val fibM1: Fib98,
    val fibM2: Fib97
)

@Unscoped
@Reader
fun fib99() = Fib99(get(), get())
class Fib100 @Inject constructor(
    val fibM1: Fib99,
    val fibM2: Fib98
)

@Unscoped
@Reader
fun fib100() = Fib100(get(), get())
class Fib101 @Inject constructor(
    val fibM1: Fib100,
    val fibM2: Fib99
)

@Unscoped
@Reader
fun fib101() = Fib101(get(), get())
class Fib102 @Inject constructor(
    val fibM1: Fib101,
    val fibM2: Fib100
)

@Unscoped
@Reader
fun fib102() = Fib102(get(), get())
class Fib103 @Inject constructor(
    val fibM1: Fib102,
    val fibM2: Fib101
)

@Unscoped
@Reader
fun fib103() = Fib103(get(), get())
class Fib104 @Inject constructor(
    val fibM1: Fib103,
    val fibM2: Fib102
)

@Unscoped
@Reader
fun fib104() = Fib104(get(), get())
class Fib105 @Inject constructor(
    val fibM1: Fib104,
    val fibM2: Fib103
)

@Unscoped
@Reader
fun fib105() = Fib105(get(), get())
class Fib106 @Inject constructor(
    val fibM1: Fib105,
    val fibM2: Fib104
)

@Unscoped
@Reader
fun fib106() = Fib106(get(), get())
class Fib107 @Inject constructor(
    val fibM1: Fib106,
    val fibM2: Fib105
)

@Unscoped
@Reader
fun fib107() = Fib107(get(), get())
class Fib108 @Inject constructor(
    val fibM1: Fib107,
    val fibM2: Fib106
)

@Unscoped
@Reader
fun fib108() = Fib108(get(), get())
class Fib109 @Inject constructor(
    val fibM1: Fib108,
    val fibM2: Fib107
)

@Unscoped
@Reader
fun fib109() = Fib109(get(), get())
class Fib110 @Inject constructor(
    val fibM1: Fib109,
    val fibM2: Fib108
)

@Unscoped
@Reader
fun fib110() = Fib110(get(), get())
class Fib111 @Inject constructor(
    val fibM1: Fib110,
    val fibM2: Fib109
)

@Unscoped
@Reader
fun fib111() = Fib111(get(), get())
class Fib112 @Inject constructor(
    val fibM1: Fib111,
    val fibM2: Fib110
)

@Unscoped
@Reader
fun fib112() = Fib112(get(), get())
class Fib113 @Inject constructor(
    val fibM1: Fib112,
    val fibM2: Fib111
)

@Unscoped
@Reader
fun fib113() = Fib113(get(), get())
class Fib114 @Inject constructor(
    val fibM1: Fib113,
    val fibM2: Fib112
)

@Unscoped
@Reader
fun fib114() = Fib114(get(), get())
class Fib115 @Inject constructor(
    val fibM1: Fib114,
    val fibM2: Fib113
)

@Unscoped
@Reader
fun fib115() = Fib115(get(), get())
class Fib116 @Inject constructor(
    val fibM1: Fib115,
    val fibM2: Fib114
)

@Unscoped
@Reader
fun fib116() = Fib116(get(), get())
class Fib117 @Inject constructor(
    val fibM1: Fib116,
    val fibM2: Fib115
)

@Unscoped
@Reader
fun fib117() = Fib117(get(), get())
class Fib118 @Inject constructor(
    val fibM1: Fib117,
    val fibM2: Fib116
)

@Unscoped
@Reader
fun fib118() = Fib118(get(), get())
class Fib119 @Inject constructor(
    val fibM1: Fib118,
    val fibM2: Fib117
)

@Unscoped
@Reader
fun fib119() = Fib119(get(), get())
class Fib120 @Inject constructor(
    val fibM1: Fib119,
    val fibM2: Fib118
)

@Unscoped
@Reader
fun fib120() = Fib120(get(), get())
class Fib121 @Inject constructor(
    val fibM1: Fib120,
    val fibM2: Fib119
)

@Unscoped
@Reader
fun fib121() = Fib121(get(), get())
class Fib122 @Inject constructor(
    val fibM1: Fib121,
    val fibM2: Fib120
)

@Unscoped
@Reader
fun fib122() = Fib122(get(), get())
class Fib123 @Inject constructor(
    val fibM1: Fib122,
    val fibM2: Fib121
)

@Unscoped
@Reader
fun fib123() = Fib123(get(), get())
class Fib124 @Inject constructor(
    val fibM1: Fib123,
    val fibM2: Fib122
)

@Unscoped
@Reader
fun fib124() = Fib124(get(), get())
class Fib125 @Inject constructor(
    val fibM1: Fib124,
    val fibM2: Fib123
)

@Unscoped
@Reader
fun fib125() = Fib125(get(), get())
class Fib126 @Inject constructor(
    val fibM1: Fib125,
    val fibM2: Fib124
)

@Unscoped
@Reader
fun fib126() = Fib126(get(), get())
class Fib127 @Inject constructor(
    val fibM1: Fib126,
    val fibM2: Fib125
)

@Unscoped
@Reader
fun fib127() = Fib127(get(), get())
class Fib128 @Inject constructor(
    val fibM1: Fib127,
    val fibM2: Fib126
)

@Unscoped
@Reader
fun fib128() = Fib128(get(), get())
class Fib129 @Inject constructor(
    val fibM1: Fib128,
    val fibM2: Fib127
)

@Unscoped
@Reader
fun fib129() = Fib129(get(), get())
class Fib130 @Inject constructor(
    val fibM1: Fib129,
    val fibM2: Fib128
)

@Unscoped
@Reader
fun fib130() = Fib130(get(), get())
class Fib131 @Inject constructor(
    val fibM1: Fib130,
    val fibM2: Fib129
)

@Unscoped
@Reader
fun fib131() = Fib131(get(), get())
class Fib132 @Inject constructor(
    val fibM1: Fib131,
    val fibM2: Fib130
)

@Unscoped
@Reader
fun fib132() = Fib132(get(), get())
class Fib133 @Inject constructor(
    val fibM1: Fib132,
    val fibM2: Fib131
)

@Unscoped
@Reader
fun fib133() = Fib133(get(), get())
class Fib134 @Inject constructor(
    val fibM1: Fib133,
    val fibM2: Fib132
)

@Unscoped
@Reader
fun fib134() = Fib134(get(), get())
class Fib135 @Inject constructor(
    val fibM1: Fib134,
    val fibM2: Fib133
)

@Unscoped
@Reader
fun fib135() = Fib135(get(), get())
class Fib136 @Inject constructor(
    val fibM1: Fib135,
    val fibM2: Fib134
)

@Unscoped
@Reader
fun fib136() = Fib136(get(), get())
class Fib137 @Inject constructor(
    val fibM1: Fib136,
    val fibM2: Fib135
)

@Unscoped
@Reader
fun fib137() = Fib137(get(), get())
class Fib138 @Inject constructor(
    val fibM1: Fib137,
    val fibM2: Fib136
)

@Unscoped
@Reader
fun fib138() = Fib138(get(), get())
class Fib139 @Inject constructor(
    val fibM1: Fib138,
    val fibM2: Fib137
)

@Unscoped
@Reader
fun fib139() = Fib139(get(), get())
class Fib140 @Inject constructor(
    val fibM1: Fib139,
    val fibM2: Fib138
)

@Unscoped
@Reader
fun fib140() = Fib140(get(), get())
class Fib141 @Inject constructor(
    val fibM1: Fib140,
    val fibM2: Fib139
)

@Unscoped
@Reader
fun fib141() = Fib141(get(), get())
class Fib142 @Inject constructor(
    val fibM1: Fib141,
    val fibM2: Fib140
)

@Unscoped
@Reader
fun fib142() = Fib142(get(), get())
class Fib143 @Inject constructor(
    val fibM1: Fib142,
    val fibM2: Fib141
)

@Unscoped
@Reader
fun fib143() = Fib143(get(), get())
class Fib144 @Inject constructor(
    val fibM1: Fib143,
    val fibM2: Fib142
)

@Unscoped
@Reader
fun fib144() = Fib144(get(), get())
class Fib145 @Inject constructor(
    val fibM1: Fib144,
    val fibM2: Fib143
)

@Unscoped
@Reader
fun fib145() = Fib145(get(), get())
class Fib146 @Inject constructor(
    val fibM1: Fib145,
    val fibM2: Fib144
)

@Unscoped
@Reader
fun fib146() = Fib146(get(), get())
class Fib147 @Inject constructor(
    val fibM1: Fib146,
    val fibM2: Fib145
)

@Unscoped
@Reader
fun fib147() = Fib147(get(), get())
class Fib148 @Inject constructor(
    val fibM1: Fib147,
    val fibM2: Fib146
)

@Unscoped
@Reader
fun fib148() = Fib148(get(), get())
class Fib149 @Inject constructor(
    val fibM1: Fib148,
    val fibM2: Fib147
)

@Unscoped
@Reader
fun fib149() = Fib149(get(), get())
class Fib150 @Inject constructor(
    val fibM1: Fib149,
    val fibM2: Fib148
)

@Unscoped
@Reader
fun fib150() = Fib150(get(), get())
class Fib151 @Inject constructor(
    val fibM1: Fib150,
    val fibM2: Fib149
)

@Unscoped
@Reader
fun fib151() = Fib151(get(), get())
class Fib152 @Inject constructor(
    val fibM1: Fib151,
    val fibM2: Fib150
)

@Unscoped
@Reader
fun fib152() = Fib152(get(), get())
class Fib153 @Inject constructor(
    val fibM1: Fib152,
    val fibM2: Fib151
)

@Unscoped
@Reader
fun fib153() = Fib153(get(), get())
class Fib154 @Inject constructor(
    val fibM1: Fib153,
    val fibM2: Fib152
)

@Unscoped
@Reader
fun fib154() = Fib154(get(), get())
class Fib155 @Inject constructor(
    val fibM1: Fib154,
    val fibM2: Fib153
)

@Unscoped
@Reader
fun fib155() = Fib155(get(), get())
class Fib156 @Inject constructor(
    val fibM1: Fib155,
    val fibM2: Fib154
)

@Unscoped
@Reader
fun fib156() = Fib156(get(), get())
class Fib157 @Inject constructor(
    val fibM1: Fib156,
    val fibM2: Fib155
)

@Unscoped
@Reader
fun fib157() = Fib157(get(), get())
class Fib158 @Inject constructor(
    val fibM1: Fib157,
    val fibM2: Fib156
)

@Unscoped
@Reader
fun fib158() = Fib158(get(), get())
class Fib159 @Inject constructor(
    val fibM1: Fib158,
    val fibM2: Fib157
)

@Unscoped
@Reader
fun fib159() = Fib159(get(), get())
class Fib160 @Inject constructor(
    val fibM1: Fib159,
    val fibM2: Fib158
)

@Unscoped
@Reader
fun fib160() = Fib160(get(), get())
class Fib161 @Inject constructor(
    val fibM1: Fib160,
    val fibM2: Fib159
)

@Unscoped
@Reader
fun fib161() = Fib161(get(), get())
class Fib162 @Inject constructor(
    val fibM1: Fib161,
    val fibM2: Fib160
)

@Unscoped
@Reader
fun fib162() = Fib162(get(), get())
class Fib163 @Inject constructor(
    val fibM1: Fib162,
    val fibM2: Fib161
)

@Unscoped
@Reader
fun fib163() = Fib163(get(), get())
class Fib164 @Inject constructor(
    val fibM1: Fib163,
    val fibM2: Fib162
)

@Unscoped
@Reader
fun fib164() = Fib164(get(), get())
class Fib165 @Inject constructor(
    val fibM1: Fib164,
    val fibM2: Fib163
)

@Unscoped
@Reader
fun fib165() = Fib165(get(), get())
class Fib166 @Inject constructor(
    val fibM1: Fib165,
    val fibM2: Fib164
)

@Unscoped
@Reader
fun fib166() = Fib166(get(), get())
class Fib167 @Inject constructor(
    val fibM1: Fib166,
    val fibM2: Fib165
)

@Unscoped
@Reader
fun fib167() = Fib167(get(), get())
class Fib168 @Inject constructor(
    val fibM1: Fib167,
    val fibM2: Fib166
)

@Unscoped
@Reader
fun fib168() = Fib168(get(), get())
class Fib169 @Inject constructor(
    val fibM1: Fib168,
    val fibM2: Fib167
)

@Unscoped
@Reader
fun fib169() = Fib169(get(), get())
class Fib170 @Inject constructor(
    val fibM1: Fib169,
    val fibM2: Fib168
)

@Unscoped
@Reader
fun fib170() = Fib170(get(), get())
class Fib171 @Inject constructor(
    val fibM1: Fib170,
    val fibM2: Fib169
)

@Unscoped
@Reader
fun fib171() = Fib171(get(), get())
class Fib172 @Inject constructor(
    val fibM1: Fib171,
    val fibM2: Fib170
)

@Unscoped
@Reader
fun fib172() = Fib172(get(), get())
class Fib173 @Inject constructor(
    val fibM1: Fib172,
    val fibM2: Fib171
)

@Unscoped
@Reader
fun fib173() = Fib173(get(), get())
class Fib174 @Inject constructor(
    val fibM1: Fib173,
    val fibM2: Fib172
)

@Unscoped
@Reader
fun fib174() = Fib174(get(), get())
class Fib175 @Inject constructor(
    val fibM1: Fib174,
    val fibM2: Fib173
)

@Unscoped
@Reader
fun fib175() = Fib175(get(), get())
class Fib176 @Inject constructor(
    val fibM1: Fib175,
    val fibM2: Fib174
)

@Unscoped
@Reader
fun fib176() = Fib176(get(), get())
class Fib177 @Inject constructor(
    val fibM1: Fib176,
    val fibM2: Fib175
)

@Unscoped
@Reader
fun fib177() = Fib177(get(), get())
class Fib178 @Inject constructor(
    val fibM1: Fib177,
    val fibM2: Fib176
)

@Unscoped
@Reader
fun fib178() = Fib178(get(), get())
class Fib179 @Inject constructor(
    val fibM1: Fib178,
    val fibM2: Fib177
)

@Unscoped
@Reader
fun fib179() = Fib179(get(), get())
class Fib180 @Inject constructor(
    val fibM1: Fib179,
    val fibM2: Fib178
)

@Unscoped
@Reader
fun fib180() = Fib180(get(), get())
class Fib181 @Inject constructor(
    val fibM1: Fib180,
    val fibM2: Fib179
)

@Unscoped
@Reader
fun fib181() = Fib181(get(), get())
class Fib182 @Inject constructor(
    val fibM1: Fib181,
    val fibM2: Fib180
)

@Unscoped
@Reader
fun fib182() = Fib182(get(), get())
class Fib183 @Inject constructor(
    val fibM1: Fib182,
    val fibM2: Fib181
)

@Unscoped
@Reader
fun fib183() = Fib183(get(), get())
class Fib184 @Inject constructor(
    val fibM1: Fib183,
    val fibM2: Fib182
)

@Unscoped
@Reader
fun fib184() = Fib184(get(), get())
class Fib185 @Inject constructor(
    val fibM1: Fib184,
    val fibM2: Fib183
)

@Unscoped
@Reader
fun fib185() = Fib185(get(), get())
class Fib186 @Inject constructor(
    val fibM1: Fib185,
    val fibM2: Fib184
)

@Unscoped
@Reader
fun fib186() = Fib186(get(), get())
class Fib187 @Inject constructor(
    val fibM1: Fib186,
    val fibM2: Fib185
)

@Unscoped
@Reader
fun fib187() = Fib187(get(), get())
class Fib188 @Inject constructor(
    val fibM1: Fib187,
    val fibM2: Fib186
)

@Unscoped
@Reader
fun fib188() = Fib188(get(), get())
class Fib189 @Inject constructor(
    val fibM1: Fib188,
    val fibM2: Fib187
)

@Unscoped
@Reader
fun fib189() = Fib189(get(), get())
class Fib190 @Inject constructor(
    val fibM1: Fib189,
    val fibM2: Fib188
)

@Unscoped
@Reader
fun fib190() = Fib190(get(), get())
class Fib191 @Inject constructor(
    val fibM1: Fib190,
    val fibM2: Fib189
)

@Unscoped
@Reader
fun fib191() = Fib191(get(), get())
class Fib192 @Inject constructor(
    val fibM1: Fib191,
    val fibM2: Fib190
)

@Unscoped
@Reader
fun fib192() = Fib192(get(), get())
class Fib193 @Inject constructor(
    val fibM1: Fib192,
    val fibM2: Fib191
)

@Unscoped
@Reader
fun fib193() = Fib193(get(), get())
class Fib194 @Inject constructor(
    val fibM1: Fib193,
    val fibM2: Fib192
)

@Unscoped
@Reader
fun fib194() = Fib194(get(), get())
class Fib195 @Inject constructor(
    val fibM1: Fib194,
    val fibM2: Fib193
)

@Unscoped
@Reader
fun fib195() = Fib195(get(), get())
class Fib196 @Inject constructor(
    val fibM1: Fib195,
    val fibM2: Fib194
)

@Unscoped
@Reader
fun fib196() = Fib196(get(), get())
class Fib197 @Inject constructor(
    val fibM1: Fib196,
    val fibM2: Fib195
)

@Unscoped
@Reader
fun fib197() = Fib197(get(), get())
class Fib198 @Inject constructor(
    val fibM1: Fib197,
    val fibM2: Fib196
)

@Unscoped
@Reader
fun fib198() = Fib198(get(), get())
class Fib199 @Inject constructor(
    val fibM1: Fib198,
    val fibM2: Fib197
)

@Unscoped
@Reader
fun fib199() = Fib199(get(), get())
class Fib200 @Inject constructor(
    val fibM1: Fib199,
    val fibM2: Fib198
)

@Unscoped
@Reader
fun fib200() = Fib200(get(), get())
class Fib201 @Inject constructor(
    val fibM1: Fib200,
    val fibM2: Fib199
)

@Unscoped
@Reader
fun fib201() = Fib201(get(), get())
class Fib202 @Inject constructor(
    val fibM1: Fib201,
    val fibM2: Fib200
)

@Unscoped
@Reader
fun fib202() = Fib202(get(), get())
class Fib203 @Inject constructor(
    val fibM1: Fib202,
    val fibM2: Fib201
)

@Unscoped
@Reader
fun fib203() = Fib203(get(), get())
class Fib204 @Inject constructor(
    val fibM1: Fib203,
    val fibM2: Fib202
)

@Unscoped
@Reader
fun fib204() = Fib204(get(), get())
class Fib205 @Inject constructor(
    val fibM1: Fib204,
    val fibM2: Fib203
)

@Unscoped
@Reader
fun fib205() = Fib205(get(), get())
class Fib206 @Inject constructor(
    val fibM1: Fib205,
    val fibM2: Fib204
)

@Unscoped
@Reader
fun fib206() = Fib206(get(), get())
class Fib207 @Inject constructor(
    val fibM1: Fib206,
    val fibM2: Fib205
)

@Unscoped
@Reader
fun fib207() = Fib207(get(), get())
class Fib208 @Inject constructor(
    val fibM1: Fib207,
    val fibM2: Fib206
)

@Unscoped
@Reader
fun fib208() = Fib208(get(), get())
class Fib209 @Inject constructor(
    val fibM1: Fib208,
    val fibM2: Fib207
)

@Unscoped
@Reader
fun fib209() = Fib209(get(), get())
class Fib210 @Inject constructor(
    val fibM1: Fib209,
    val fibM2: Fib208
)

@Unscoped
@Reader
fun fib210() = Fib210(get(), get())
class Fib211 @Inject constructor(
    val fibM1: Fib210,
    val fibM2: Fib209
)

@Unscoped
@Reader
fun fib211() = Fib211(get(), get())
class Fib212 @Inject constructor(
    val fibM1: Fib211,
    val fibM2: Fib210
)

@Unscoped
@Reader
fun fib212() = Fib212(get(), get())
class Fib213 @Inject constructor(
    val fibM1: Fib212,
    val fibM2: Fib211
)

@Unscoped
@Reader
fun fib213() = Fib213(get(), get())
class Fib214 @Inject constructor(
    val fibM1: Fib213,
    val fibM2: Fib212
)

@Unscoped
@Reader
fun fib214() = Fib214(get(), get())
class Fib215 @Inject constructor(
    val fibM1: Fib214,
    val fibM2: Fib213
)

@Unscoped
@Reader
fun fib215() = Fib215(get(), get())
class Fib216 @Inject constructor(
    val fibM1: Fib215,
    val fibM2: Fib214
)

@Unscoped
@Reader
fun fib216() = Fib216(get(), get())
class Fib217 @Inject constructor(
    val fibM1: Fib216,
    val fibM2: Fib215
)

@Unscoped
@Reader
fun fib217() = Fib217(get(), get())
class Fib218 @Inject constructor(
    val fibM1: Fib217,
    val fibM2: Fib216
)

@Unscoped
@Reader
fun fib218() = Fib218(get(), get())
class Fib219 @Inject constructor(
    val fibM1: Fib218,
    val fibM2: Fib217
)

@Unscoped
@Reader
fun fib219() = Fib219(get(), get())
class Fib220 @Inject constructor(
    val fibM1: Fib219,
    val fibM2: Fib218
)

@Unscoped
@Reader
fun fib220() = Fib220(get(), get())
class Fib221 @Inject constructor(
    val fibM1: Fib220,
    val fibM2: Fib219
)

@Unscoped
@Reader
fun fib221() = Fib221(get(), get())
class Fib222 @Inject constructor(
    val fibM1: Fib221,
    val fibM2: Fib220
)

@Unscoped
@Reader
fun fib222() = Fib222(get(), get())
class Fib223 @Inject constructor(
    val fibM1: Fib222,
    val fibM2: Fib221
)

@Unscoped
@Reader
fun fib223() = Fib223(get(), get())
class Fib224 @Inject constructor(
    val fibM1: Fib223,
    val fibM2: Fib222
)

@Unscoped
@Reader
fun fib224() = Fib224(get(), get())
class Fib225 @Inject constructor(
    val fibM1: Fib224,
    val fibM2: Fib223
)

@Unscoped
@Reader
fun fib225() = Fib225(get(), get())
class Fib226 @Inject constructor(
    val fibM1: Fib225,
    val fibM2: Fib224
)

@Unscoped
@Reader
fun fib226() = Fib226(get(), get())
class Fib227 @Inject constructor(
    val fibM1: Fib226,
    val fibM2: Fib225
)

@Unscoped
@Reader
fun fib227() = Fib227(get(), get())
class Fib228 @Inject constructor(
    val fibM1: Fib227,
    val fibM2: Fib226
)

@Unscoped
@Reader
fun fib228() = Fib228(get(), get())
class Fib229 @Inject constructor(
    val fibM1: Fib228,
    val fibM2: Fib227
)

@Unscoped
@Reader
fun fib229() = Fib229(get(), get())
class Fib230 @Inject constructor(
    val fibM1: Fib229,
    val fibM2: Fib228
)

@Unscoped
@Reader
fun fib230() = Fib230(get(), get())
class Fib231 @Inject constructor(
    val fibM1: Fib230,
    val fibM2: Fib229
)

@Unscoped
@Reader
fun fib231() = Fib231(get(), get())
class Fib232 @Inject constructor(
    val fibM1: Fib231,
    val fibM2: Fib230
)

@Unscoped
@Reader
fun fib232() = Fib232(get(), get())
class Fib233 @Inject constructor(
    val fibM1: Fib232,
    val fibM2: Fib231
)

@Unscoped
@Reader
fun fib233() = Fib233(get(), get())
class Fib234 @Inject constructor(
    val fibM1: Fib233,
    val fibM2: Fib232
)

@Unscoped
@Reader
fun fib234() = Fib234(get(), get())
class Fib235 @Inject constructor(
    val fibM1: Fib234,
    val fibM2: Fib233
)

@Unscoped
@Reader
fun fib235() = Fib235(get(), get())
class Fib236 @Inject constructor(
    val fibM1: Fib235,
    val fibM2: Fib234
)

@Unscoped
@Reader
fun fib236() = Fib236(get(), get())
class Fib237 @Inject constructor(
    val fibM1: Fib236,
    val fibM2: Fib235
)

@Unscoped
@Reader
fun fib237() = Fib237(get(), get())
class Fib238 @Inject constructor(
    val fibM1: Fib237,
    val fibM2: Fib236
)

@Unscoped
@Reader
fun fib238() = Fib238(get(), get())
class Fib239 @Inject constructor(
    val fibM1: Fib238,
    val fibM2: Fib237
)

@Unscoped
@Reader
fun fib239() = Fib239(get(), get())
class Fib240 @Inject constructor(
    val fibM1: Fib239,
    val fibM2: Fib238
)

@Unscoped
@Reader
fun fib240() = Fib240(get(), get())
class Fib241 @Inject constructor(
    val fibM1: Fib240,
    val fibM2: Fib239
)

@Unscoped
@Reader
fun fib241() = Fib241(get(), get())
class Fib242 @Inject constructor(
    val fibM1: Fib241,
    val fibM2: Fib240
)

@Unscoped
@Reader
fun fib242() = Fib242(get(), get())
class Fib243 @Inject constructor(
    val fibM1: Fib242,
    val fibM2: Fib241
)

@Unscoped
@Reader
fun fib243() = Fib243(get(), get())
class Fib244 @Inject constructor(
    val fibM1: Fib243,
    val fibM2: Fib242
)

@Unscoped
@Reader
fun fib244() = Fib244(get(), get())
class Fib245 @Inject constructor(
    val fibM1: Fib244,
    val fibM2: Fib243
)

@Unscoped
@Reader
fun fib245() = Fib245(get(), get())
class Fib246 @Inject constructor(
    val fibM1: Fib245,
    val fibM2: Fib244
)

@Unscoped
@Reader
fun fib246() = Fib246(get(), get())
class Fib247 @Inject constructor(
    val fibM1: Fib246,
    val fibM2: Fib245
)

@Unscoped
@Reader
fun fib247() = Fib247(get(), get())
class Fib248 @Inject constructor(
    val fibM1: Fib247,
    val fibM2: Fib246
)

@Unscoped
@Reader
fun fib248() = Fib248(get(), get())
class Fib249 @Inject constructor(
    val fibM1: Fib248,
    val fibM2: Fib247
)

@Unscoped
@Reader
fun fib249() = Fib249(get(), get())
class Fib250 @Inject constructor(
    val fibM1: Fib249,
    val fibM2: Fib248
)

@Unscoped
@Reader
fun fib250() = Fib250(get(), get())
class Fib251 @Inject constructor(
    val fibM1: Fib250,
    val fibM2: Fib249
)

@Unscoped
@Reader
fun fib251() = Fib251(get(), get())
class Fib252 @Inject constructor(
    val fibM1: Fib251,
    val fibM2: Fib250
)

@Unscoped
@Reader
fun fib252() = Fib252(get(), get())
class Fib253 @Inject constructor(
    val fibM1: Fib252,
    val fibM2: Fib251
)

@Unscoped
@Reader
fun fib253() = Fib253(get(), get())
class Fib254 @Inject constructor(
    val fibM1: Fib253,
    val fibM2: Fib252
)

@Unscoped
@Reader
fun fib254() = Fib254(get(), get())
class Fib255 @Inject constructor(
    val fibM1: Fib254,
    val fibM2: Fib253
)

@Unscoped
@Reader
fun fib255() = Fib255(get(), get())
class Fib256 @Inject constructor(
    val fibM1: Fib255,
    val fibM2: Fib254
)

@Unscoped
@Reader
fun fib256() = Fib256(get(), get())
class Fib257 @Inject constructor(
    val fibM1: Fib256,
    val fibM2: Fib255
)

@Unscoped
@Reader
fun fib257() = Fib257(get(), get())
class Fib258 @Inject constructor(
    val fibM1: Fib257,
    val fibM2: Fib256
)

@Unscoped
@Reader
fun fib258() = Fib258(get(), get())
class Fib259 @Inject constructor(
    val fibM1: Fib258,
    val fibM2: Fib257
)

@Unscoped
@Reader
fun fib259() = Fib259(get(), get())
class Fib260 @Inject constructor(
    val fibM1: Fib259,
    val fibM2: Fib258
)

@Unscoped
@Reader
fun fib260() = Fib260(get(), get())
class Fib261 @Inject constructor(
    val fibM1: Fib260,
    val fibM2: Fib259
)

@Unscoped
@Reader
fun fib261() = Fib261(get(), get())
class Fib262 @Inject constructor(
    val fibM1: Fib261,
    val fibM2: Fib260
)

@Unscoped
@Reader
fun fib262() = Fib262(get(), get())
class Fib263 @Inject constructor(
    val fibM1: Fib262,
    val fibM2: Fib261
)

@Unscoped
@Reader
fun fib263() = Fib263(get(), get())
class Fib264 @Inject constructor(
    val fibM1: Fib263,
    val fibM2: Fib262
)

@Unscoped
@Reader
fun fib264() = Fib264(get(), get())
class Fib265 @Inject constructor(
    val fibM1: Fib264,
    val fibM2: Fib263
)

@Unscoped
@Reader
fun fib265() = Fib265(get(), get())
class Fib266 @Inject constructor(
    val fibM1: Fib265,
    val fibM2: Fib264
)

@Unscoped
@Reader
fun fib266() = Fib266(get(), get())
class Fib267 @Inject constructor(
    val fibM1: Fib266,
    val fibM2: Fib265
)

@Unscoped
@Reader
fun fib267() = Fib267(get(), get())
class Fib268 @Inject constructor(
    val fibM1: Fib267,
    val fibM2: Fib266
)

@Unscoped
@Reader
fun fib268() = Fib268(get(), get())
class Fib269 @Inject constructor(
    val fibM1: Fib268,
    val fibM2: Fib267
)

@Unscoped
@Reader
fun fib269() = Fib269(get(), get())
class Fib270 @Inject constructor(
    val fibM1: Fib269,
    val fibM2: Fib268
)

@Unscoped
@Reader
fun fib270() = Fib270(get(), get())
class Fib271 @Inject constructor(
    val fibM1: Fib270,
    val fibM2: Fib269
)

@Unscoped
@Reader
fun fib271() = Fib271(get(), get())
class Fib272 @Inject constructor(
    val fibM1: Fib271,
    val fibM2: Fib270
)

@Unscoped
@Reader
fun fib272() = Fib272(get(), get())
class Fib273 @Inject constructor(
    val fibM1: Fib272,
    val fibM2: Fib271
)

@Unscoped
@Reader
fun fib273() = Fib273(get(), get())
class Fib274 @Inject constructor(
    val fibM1: Fib273,
    val fibM2: Fib272
)

@Unscoped
@Reader
fun fib274() = Fib274(get(), get())
class Fib275 @Inject constructor(
    val fibM1: Fib274,
    val fibM2: Fib273
)

@Unscoped
@Reader
fun fib275() = Fib275(get(), get())
class Fib276 @Inject constructor(
    val fibM1: Fib275,
    val fibM2: Fib274
)

@Unscoped
@Reader
fun fib276() = Fib276(get(), get())
class Fib277 @Inject constructor(
    val fibM1: Fib276,
    val fibM2: Fib275
)

@Unscoped
@Reader
fun fib277() = Fib277(get(), get())
class Fib278 @Inject constructor(
    val fibM1: Fib277,
    val fibM2: Fib276
)

@Unscoped
@Reader
fun fib278() = Fib278(get(), get())
class Fib279 @Inject constructor(
    val fibM1: Fib278,
    val fibM2: Fib277
)

@Unscoped
@Reader
fun fib279() = Fib279(get(), get())
class Fib280 @Inject constructor(
    val fibM1: Fib279,
    val fibM2: Fib278
)

@Unscoped
@Reader
fun fib280() = Fib280(get(), get())
class Fib281 @Inject constructor(
    val fibM1: Fib280,
    val fibM2: Fib279
)

@Unscoped
@Reader
fun fib281() = Fib281(get(), get())
class Fib282 @Inject constructor(
    val fibM1: Fib281,
    val fibM2: Fib280
)

@Unscoped
@Reader
fun fib282() = Fib282(get(), get())
class Fib283 @Inject constructor(
    val fibM1: Fib282,
    val fibM2: Fib281
)

@Unscoped
@Reader
fun fib283() = Fib283(get(), get())
class Fib284 @Inject constructor(
    val fibM1: Fib283,
    val fibM2: Fib282
)

@Unscoped
@Reader
fun fib284() = Fib284(get(), get())
class Fib285 @Inject constructor(
    val fibM1: Fib284,
    val fibM2: Fib283
)

@Unscoped
@Reader
fun fib285() = Fib285(get(), get())
class Fib286 @Inject constructor(
    val fibM1: Fib285,
    val fibM2: Fib284
)

@Unscoped
@Reader
fun fib286() = Fib286(get(), get())
class Fib287 @Inject constructor(
    val fibM1: Fib286,
    val fibM2: Fib285
)

@Unscoped
@Reader
fun fib287() = Fib287(get(), get())
class Fib288 @Inject constructor(
    val fibM1: Fib287,
    val fibM2: Fib286
)

@Unscoped
@Reader
fun fib288() = Fib288(get(), get())
class Fib289 @Inject constructor(
    val fibM1: Fib288,
    val fibM2: Fib287
)

@Unscoped
@Reader
fun fib289() = Fib289(get(), get())
class Fib290 @Inject constructor(
    val fibM1: Fib289,
    val fibM2: Fib288
)

@Unscoped
@Reader
fun fib290() = Fib290(get(), get())
class Fib291 @Inject constructor(
    val fibM1: Fib290,
    val fibM2: Fib289
)

@Unscoped
@Reader
fun fib291() = Fib291(get(), get())
class Fib292 @Inject constructor(
    val fibM1: Fib291,
    val fibM2: Fib290
)

@Unscoped
@Reader
fun fib292() = Fib292(get(), get())
class Fib293 @Inject constructor(
    val fibM1: Fib292,
    val fibM2: Fib291
)

@Unscoped
@Reader
fun fib293() = Fib293(get(), get())
class Fib294 @Inject constructor(
    val fibM1: Fib293,
    val fibM2: Fib292
)

@Unscoped
@Reader
fun fib294() = Fib294(get(), get())
class Fib295 @Inject constructor(
    val fibM1: Fib294,
    val fibM2: Fib293
)

@Unscoped
@Reader
fun fib295() = Fib295(get(), get())
class Fib296 @Inject constructor(
    val fibM1: Fib295,
    val fibM2: Fib294
)

@Unscoped
@Reader
fun fib296() = Fib296(get(), get())
class Fib297 @Inject constructor(
    val fibM1: Fib296,
    val fibM2: Fib295
)

@Unscoped
@Reader
fun fib297() = Fib297(get(), get())
class Fib298 @Inject constructor(
    val fibM1: Fib297,
    val fibM2: Fib296
)

@Unscoped
@Reader
fun fib298() = Fib298(get(), get())
class Fib299 @Inject constructor(
    val fibM1: Fib298,
    val fibM2: Fib297
)

@Unscoped
@Reader
fun fib299() = Fib299(get(), get())
class Fib300 @Inject constructor(
    val fibM1: Fib299,
    val fibM2: Fib298
)

@Unscoped
@Reader
fun fib300() = Fib300(get(), get())
class Fib301 @Inject constructor(
    val fibM1: Fib300,
    val fibM2: Fib299
)

@Unscoped
@Reader
fun fib301() = Fib301(get(), get())
class Fib302 @Inject constructor(
    val fibM1: Fib301,
    val fibM2: Fib300
)

@Unscoped
@Reader
fun fib302() = Fib302(get(), get())
class Fib303 @Inject constructor(
    val fibM1: Fib302,
    val fibM2: Fib301
)

@Unscoped
@Reader
fun fib303() = Fib303(get(), get())
class Fib304 @Inject constructor(
    val fibM1: Fib303,
    val fibM2: Fib302
)

@Unscoped
@Reader
fun fib304() = Fib304(get(), get())
class Fib305 @Inject constructor(
    val fibM1: Fib304,
    val fibM2: Fib303
)

@Unscoped
@Reader
fun fib305() = Fib305(get(), get())
class Fib306 @Inject constructor(
    val fibM1: Fib305,
    val fibM2: Fib304
)

@Unscoped
@Reader
fun fib306() = Fib306(get(), get())
class Fib307 @Inject constructor(
    val fibM1: Fib306,
    val fibM2: Fib305
)

@Unscoped
@Reader
fun fib307() = Fib307(get(), get())
class Fib308 @Inject constructor(
    val fibM1: Fib307,
    val fibM2: Fib306
)

@Unscoped
@Reader
fun fib308() = Fib308(get(), get())
class Fib309 @Inject constructor(
    val fibM1: Fib308,
    val fibM2: Fib307
)

@Unscoped
@Reader
fun fib309() = Fib309(get(), get())
class Fib310 @Inject constructor(
    val fibM1: Fib309,
    val fibM2: Fib308
)

@Unscoped
@Reader
fun fib310() = Fib310(get(), get())
class Fib311 @Inject constructor(
    val fibM1: Fib310,
    val fibM2: Fib309
)

@Unscoped
@Reader
fun fib311() = Fib311(get(), get())
class Fib312 @Inject constructor(
    val fibM1: Fib311,
    val fibM2: Fib310
)

@Unscoped
@Reader
fun fib312() = Fib312(get(), get())
class Fib313 @Inject constructor(
    val fibM1: Fib312,
    val fibM2: Fib311
)

@Unscoped
@Reader
fun fib313() = Fib313(get(), get())
class Fib314 @Inject constructor(
    val fibM1: Fib313,
    val fibM2: Fib312
)

@Unscoped
@Reader
fun fib314() = Fib314(get(), get())
class Fib315 @Inject constructor(
    val fibM1: Fib314,
    val fibM2: Fib313
)

@Unscoped
@Reader
fun fib315() = Fib315(get(), get())
class Fib316 @Inject constructor(
    val fibM1: Fib315,
    val fibM2: Fib314
)

@Unscoped
@Reader
fun fib316() = Fib316(get(), get())
class Fib317 @Inject constructor(
    val fibM1: Fib316,
    val fibM2: Fib315
)

@Unscoped
@Reader
fun fib317() = Fib317(get(), get())
class Fib318 @Inject constructor(
    val fibM1: Fib317,
    val fibM2: Fib316
)

@Unscoped
@Reader
fun fib318() = Fib318(get(), get())
class Fib319 @Inject constructor(
    val fibM1: Fib318,
    val fibM2: Fib317
)

@Unscoped
@Reader
fun fib319() = Fib319(get(), get())
class Fib320 @Inject constructor(
    val fibM1: Fib319,
    val fibM2: Fib318
)

@Unscoped
@Reader
fun fib320() = Fib320(get(), get())
class Fib321 @Inject constructor(
    val fibM1: Fib320,
    val fibM2: Fib319
)

@Unscoped
@Reader
fun fib321() = Fib321(get(), get())
class Fib322 @Inject constructor(
    val fibM1: Fib321,
    val fibM2: Fib320
)

@Unscoped
@Reader
fun fib322() = Fib322(get(), get())
class Fib323 @Inject constructor(
    val fibM1: Fib322,
    val fibM2: Fib321
)

@Unscoped
@Reader
fun fib323() = Fib323(get(), get())
class Fib324 @Inject constructor(
    val fibM1: Fib323,
    val fibM2: Fib322
)

@Unscoped
@Reader
fun fib324() = Fib324(get(), get())
class Fib325 @Inject constructor(
    val fibM1: Fib324,
    val fibM2: Fib323
)

@Unscoped
@Reader
fun fib325() = Fib325(get(), get())
class Fib326 @Inject constructor(
    val fibM1: Fib325,
    val fibM2: Fib324
)

@Unscoped
@Reader
fun fib326() = Fib326(get(), get())
class Fib327 @Inject constructor(
    val fibM1: Fib326,
    val fibM2: Fib325
)

@Unscoped
@Reader
fun fib327() = Fib327(get(), get())
class Fib328 @Inject constructor(
    val fibM1: Fib327,
    val fibM2: Fib326
)

@Unscoped
@Reader
fun fib328() = Fib328(get(), get())
class Fib329 @Inject constructor(
    val fibM1: Fib328,
    val fibM2: Fib327
)

@Unscoped
@Reader
fun fib329() = Fib329(get(), get())
class Fib330 @Inject constructor(
    val fibM1: Fib329,
    val fibM2: Fib328
)

@Unscoped
@Reader
fun fib330() = Fib330(get(), get())
class Fib331 @Inject constructor(
    val fibM1: Fib330,
    val fibM2: Fib329
)

@Unscoped
@Reader
fun fib331() = Fib331(get(), get())
class Fib332 @Inject constructor(
    val fibM1: Fib331,
    val fibM2: Fib330
)

@Unscoped
@Reader
fun fib332() = Fib332(get(), get())
class Fib333 @Inject constructor(
    val fibM1: Fib332,
    val fibM2: Fib331
)

@Unscoped
@Reader
fun fib333() = Fib333(get(), get())
class Fib334 @Inject constructor(
    val fibM1: Fib333,
    val fibM2: Fib332
)

@Unscoped
@Reader
fun fib334() = Fib334(get(), get())
class Fib335 @Inject constructor(
    val fibM1: Fib334,
    val fibM2: Fib333
)

@Unscoped
@Reader
fun fib335() = Fib335(get(), get())
class Fib336 @Inject constructor(
    val fibM1: Fib335,
    val fibM2: Fib334
)

@Unscoped
@Reader
fun fib336() = Fib336(get(), get())
class Fib337 @Inject constructor(
    val fibM1: Fib336,
    val fibM2: Fib335
)

@Unscoped
@Reader
fun fib337() = Fib337(get(), get())
class Fib338 @Inject constructor(
    val fibM1: Fib337,
    val fibM2: Fib336
)

@Unscoped
@Reader
fun fib338() = Fib338(get(), get())
class Fib339 @Inject constructor(
    val fibM1: Fib338,
    val fibM2: Fib337
)

@Unscoped
@Reader
fun fib339() = Fib339(get(), get())
class Fib340 @Inject constructor(
    val fibM1: Fib339,
    val fibM2: Fib338
)

@Unscoped
@Reader
fun fib340() = Fib340(get(), get())
class Fib341 @Inject constructor(
    val fibM1: Fib340,
    val fibM2: Fib339
)

@Unscoped
@Reader
fun fib341() = Fib341(get(), get())
class Fib342 @Inject constructor(
    val fibM1: Fib341,
    val fibM2: Fib340
)

@Unscoped
@Reader
fun fib342() = Fib342(get(), get())
class Fib343 @Inject constructor(
    val fibM1: Fib342,
    val fibM2: Fib341
)

@Unscoped
@Reader
fun fib343() = Fib343(get(), get())
class Fib344 @Inject constructor(
    val fibM1: Fib343,
    val fibM2: Fib342
)

@Unscoped
@Reader
fun fib344() = Fib344(get(), get())
class Fib345 @Inject constructor(
    val fibM1: Fib344,
    val fibM2: Fib343
)

@Unscoped
@Reader
fun fib345() = Fib345(get(), get())
class Fib346 @Inject constructor(
    val fibM1: Fib345,
    val fibM2: Fib344
)

@Unscoped
@Reader
fun fib346() = Fib346(get(), get())
class Fib347 @Inject constructor(
    val fibM1: Fib346,
    val fibM2: Fib345
)

@Unscoped
@Reader
fun fib347() = Fib347(get(), get())
class Fib348 @Inject constructor(
    val fibM1: Fib347,
    val fibM2: Fib346
)

@Unscoped
@Reader
fun fib348() = Fib348(get(), get())
class Fib349 @Inject constructor(
    val fibM1: Fib348,
    val fibM2: Fib347
)

@Unscoped
@Reader
fun fib349() = Fib349(get(), get())
class Fib350 @Inject constructor(
    val fibM1: Fib349,
    val fibM2: Fib348
)

@Unscoped
@Reader
fun fib350() = Fib350(get(), get())
class Fib351 @Inject constructor(
    val fibM1: Fib350,
    val fibM2: Fib349
)

@Unscoped
@Reader
fun fib351() = Fib351(get(), get())
class Fib352 @Inject constructor(
    val fibM1: Fib351,
    val fibM2: Fib350
)

@Unscoped
@Reader
fun fib352() = Fib352(get(), get())
class Fib353 @Inject constructor(
    val fibM1: Fib352,
    val fibM2: Fib351
)

@Unscoped
@Reader
fun fib353() = Fib353(get(), get())
class Fib354 @Inject constructor(
    val fibM1: Fib353,
    val fibM2: Fib352
)

@Unscoped
@Reader
fun fib354() = Fib354(get(), get())
class Fib355 @Inject constructor(
    val fibM1: Fib354,
    val fibM2: Fib353
)

@Unscoped
@Reader
fun fib355() = Fib355(get(), get())
class Fib356 @Inject constructor(
    val fibM1: Fib355,
    val fibM2: Fib354
)

@Unscoped
@Reader
fun fib356() = Fib356(get(), get())
class Fib357 @Inject constructor(
    val fibM1: Fib356,
    val fibM2: Fib355
)

@Unscoped
@Reader
fun fib357() = Fib357(get(), get())
class Fib358 @Inject constructor(
    val fibM1: Fib357,
    val fibM2: Fib356
)

@Unscoped
@Reader
fun fib358() = Fib358(get(), get())
class Fib359 @Inject constructor(
    val fibM1: Fib358,
    val fibM2: Fib357
)

@Unscoped
@Reader
fun fib359() = Fib359(get(), get())
class Fib360 @Inject constructor(
    val fibM1: Fib359,
    val fibM2: Fib358
)

@Unscoped
@Reader
fun fib360() = Fib360(get(), get())
class Fib361 @Inject constructor(
    val fibM1: Fib360,
    val fibM2: Fib359
)

@Unscoped
@Reader
fun fib361() = Fib361(get(), get())
class Fib362 @Inject constructor(
    val fibM1: Fib361,
    val fibM2: Fib360
)

@Unscoped
@Reader
fun fib362() = Fib362(get(), get())
class Fib363 @Inject constructor(
    val fibM1: Fib362,
    val fibM2: Fib361
)

@Unscoped
@Reader
fun fib363() = Fib363(get(), get())
class Fib364 @Inject constructor(
    val fibM1: Fib363,
    val fibM2: Fib362
)

@Unscoped
@Reader
fun fib364() = Fib364(get(), get())
class Fib365 @Inject constructor(
    val fibM1: Fib364,
    val fibM2: Fib363
)

@Unscoped
@Reader
fun fib365() = Fib365(get(), get())
class Fib366 @Inject constructor(
    val fibM1: Fib365,
    val fibM2: Fib364
)

@Unscoped
@Reader
fun fib366() = Fib366(get(), get())
class Fib367 @Inject constructor(
    val fibM1: Fib366,
    val fibM2: Fib365
)

@Unscoped
@Reader
fun fib367() = Fib367(get(), get())
class Fib368 @Inject constructor(
    val fibM1: Fib367,
    val fibM2: Fib366
)

@Unscoped
@Reader
fun fib368() = Fib368(get(), get())
class Fib369 @Inject constructor(
    val fibM1: Fib368,
    val fibM2: Fib367
)

@Unscoped
@Reader
fun fib369() = Fib369(get(), get())
class Fib370 @Inject constructor(
    val fibM1: Fib369,
    val fibM2: Fib368
)

@Unscoped
@Reader
fun fib370() = Fib370(get(), get())
class Fib371 @Inject constructor(
    val fibM1: Fib370,
    val fibM2: Fib369
)

@Unscoped
@Reader
fun fib371() = Fib371(get(), get())
class Fib372 @Inject constructor(
    val fibM1: Fib371,
    val fibM2: Fib370
)

@Unscoped
@Reader
fun fib372() = Fib372(get(), get())
class Fib373 @Inject constructor(
    val fibM1: Fib372,
    val fibM2: Fib371
)

@Unscoped
@Reader
fun fib373() = Fib373(get(), get())
class Fib374 @Inject constructor(
    val fibM1: Fib373,
    val fibM2: Fib372
)

@Unscoped
@Reader
fun fib374() = Fib374(get(), get())
class Fib375 @Inject constructor(
    val fibM1: Fib374,
    val fibM2: Fib373
)

@Unscoped
@Reader
fun fib375() = Fib375(get(), get())
class Fib376 @Inject constructor(
    val fibM1: Fib375,
    val fibM2: Fib374
)

@Unscoped
@Reader
fun fib376() = Fib376(get(), get())
class Fib377 @Inject constructor(
    val fibM1: Fib376,
    val fibM2: Fib375
)

@Unscoped
@Reader
fun fib377() = Fib377(get(), get())
class Fib378 @Inject constructor(
    val fibM1: Fib377,
    val fibM2: Fib376
)

@Unscoped
@Reader
fun fib378() = Fib378(get(), get())
class Fib379 @Inject constructor(
    val fibM1: Fib378,
    val fibM2: Fib377
)

@Unscoped
@Reader
fun fib379() = Fib379(get(), get())
class Fib380 @Inject constructor(
    val fibM1: Fib379,
    val fibM2: Fib378
)

@Unscoped
@Reader
fun fib380() = Fib380(get(), get())
class Fib381 @Inject constructor(
    val fibM1: Fib380,
    val fibM2: Fib379
)

@Unscoped
@Reader
fun fib381() = Fib381(get(), get())
class Fib382 @Inject constructor(
    val fibM1: Fib381,
    val fibM2: Fib380
)

@Unscoped
@Reader
fun fib382() = Fib382(get(), get())
class Fib383 @Inject constructor(
    val fibM1: Fib382,
    val fibM2: Fib381
)

@Unscoped
@Reader
fun fib383() = Fib383(get(), get())
class Fib384 @Inject constructor(
    val fibM1: Fib383,
    val fibM2: Fib382
)

@Unscoped
@Reader
fun fib384() = Fib384(get(), get())
class Fib385 @Inject constructor(
    val fibM1: Fib384,
    val fibM2: Fib383
)

@Unscoped
@Reader
fun fib385() = Fib385(get(), get())
class Fib386 @Inject constructor(
    val fibM1: Fib385,
    val fibM2: Fib384
)

@Unscoped
@Reader
fun fib386() = Fib386(get(), get())
class Fib387 @Inject constructor(
    val fibM1: Fib386,
    val fibM2: Fib385
)

@Unscoped
@Reader
fun fib387() = Fib387(get(), get())
class Fib388 @Inject constructor(
    val fibM1: Fib387,
    val fibM2: Fib386
)

@Unscoped
@Reader
fun fib388() = Fib388(get(), get())
class Fib389 @Inject constructor(
    val fibM1: Fib388,
    val fibM2: Fib387
)

@Unscoped
@Reader
fun fib389() = Fib389(get(), get())
class Fib390 @Inject constructor(
    val fibM1: Fib389,
    val fibM2: Fib388
)

@Unscoped
@Reader
fun fib390() = Fib390(get(), get())
class Fib391 @Inject constructor(
    val fibM1: Fib390,
    val fibM2: Fib389
)

@Unscoped
@Reader
fun fib391() = Fib391(get(), get())
class Fib392 @Inject constructor(
    val fibM1: Fib391,
    val fibM2: Fib390
)

@Unscoped
@Reader
fun fib392() = Fib392(get(), get())
class Fib393 @Inject constructor(
    val fibM1: Fib392,
    val fibM2: Fib391
)

@Unscoped
@Reader
fun fib393() = Fib393(get(), get())
class Fib394 @Inject constructor(
    val fibM1: Fib393,
    val fibM2: Fib392
)

@Unscoped
@Reader
fun fib394() = Fib394(get(), get())
class Fib395 @Inject constructor(
    val fibM1: Fib394,
    val fibM2: Fib393
)

@Unscoped
@Reader
fun fib395() = Fib395(get(), get())
class Fib396 @Inject constructor(
    val fibM1: Fib395,
    val fibM2: Fib394
)

@Unscoped
@Reader
fun fib396() = Fib396(get(), get())
class Fib397 @Inject constructor(
    val fibM1: Fib396,
    val fibM2: Fib395
)

@Unscoped
@Reader
fun fib397() = Fib397(get(), get())
class Fib398 @Inject constructor(
    val fibM1: Fib397,
    val fibM2: Fib396
)

@Unscoped
@Reader
fun fib398() = Fib398(get(), get())
class Fib399 @Inject constructor(
    val fibM1: Fib398,
    val fibM2: Fib397
)

@Unscoped
@Reader
fun fib399() = Fib399(get(), get())
class Fib400 @Inject constructor(
    val fibM1: Fib399,
    val fibM2: Fib398
)

@Unscoped
@Reader
fun fib400() = Fib400(get(), get())
