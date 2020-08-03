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

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import javax.inject.Inject

class Fib1 @Inject constructor()

@Given
fun fib1() = Fib1()

class Fib2 @Inject constructor()

@Given(ApplicationScoped::class)
fun fib2() = Fib2()

@Singleton
class Fib3 @Inject constructor(
    val fibM1: Fib2,
    val fibM2: Fib1
)

@Given(ApplicationScoped::class)
fun fib3() = Fib3(given(), given())

@Singleton
class Fib4 @Inject constructor(
    val fibM1: Fib3,
    val fibM2: Fib2
)

@Given(ApplicationScoped::class)
fun fib4() = Fib4(given(), given())

@Singleton
class Fib5 @Inject constructor(
    val fibM1: Fib4,
    val fibM2: Fib3
)

@Given(ApplicationScoped::class)
fun fib5() = Fib5(given(), given())

@Singleton
class Fib6 @Inject constructor(
    val fibM1: Fib5,
    val fibM2: Fib4
)

@Given(ApplicationScoped::class)
fun fib6() = Fib6(given(), given())

@Singleton
class Fib7 @Inject constructor(
    val fibM1: Fib6,
    val fibM2: Fib5
)

@Given(ApplicationScoped::class)
fun fib7() = Fib7(given(), given())

@Singleton
class Fib8 @Inject constructor(
    val fibM1: Fib7,
    val fibM2: Fib6
)

@Given(ApplicationScoped::class)
fun fib8() = Fib8(given(), given())

@Singleton
class Fib9 @Inject constructor(
    val fibM1: Fib8,
    val fibM2: Fib7
)

@Given
fun fib9() = Fib9(given(), given())
class Fib10 @Inject constructor(
    val fibM1: Fib9,
    val fibM2: Fib8
)

@Given
fun fib10() = Fib10(given(), given())
class Fib11 @Inject constructor(
    val fibM1: Fib10,
    val fibM2: Fib9
)

@Given
fun fib11() = Fib11(given(), given())
class Fib12 @Inject constructor(
    val fibM1: Fib11,
    val fibM2: Fib10
)

@Given
fun fib12() = Fib12(given(), given())
class Fib13 @Inject constructor(
    val fibM1: Fib12,
    val fibM2: Fib11
)

@Given
fun fib13() = Fib13(given(), given())
class Fib14 @Inject constructor(
    val fibM1: Fib13,
    val fibM2: Fib12
)

@Given
fun fib14() = Fib14(given(), given())
class Fib15 @Inject constructor(
    val fibM1: Fib14,
    val fibM2: Fib13
)

@Given
fun fib15() = Fib15(given(), given())
class Fib16 @Inject constructor(
    val fibM1: Fib15,
    val fibM2: Fib14
)

@Given
fun fib16() = Fib16(given(), given())
class Fib17 @Inject constructor(
    val fibM1: Fib16,
    val fibM2: Fib15
)

@Given
fun fib17() = Fib17(given(), given())
class Fib18 @Inject constructor(
    val fibM1: Fib17,
    val fibM2: Fib16
)

@Given
fun fib18() = Fib18(given(), given())
class Fib19 @Inject constructor(
    val fibM1: Fib18,
    val fibM2: Fib17
)

@Given
fun fib19() = Fib19(given(), given())
class Fib20 @Inject constructor(
    val fibM1: Fib19,
    val fibM2: Fib18
)

@Given
fun fib20() = Fib20(given(), given())
class Fib21 @Inject constructor(
    val fibM1: Fib20,
    val fibM2: Fib19
)

@Given
fun fib21() = Fib21(given(), given())
class Fib22 @Inject constructor(
    val fibM1: Fib21,
    val fibM2: Fib20
)

@Given
fun fib22() = Fib22(given(), given())
class Fib23 @Inject constructor(
    val fibM1: Fib22,
    val fibM2: Fib21
)

@Given
fun fib23() = Fib23(given(), given())
class Fib24 @Inject constructor(
    val fibM1: Fib23,
    val fibM2: Fib22
)

@Given
fun fib24() = Fib24(given(), given())
class Fib25 @Inject constructor(
    val fibM1: Fib24,
    val fibM2: Fib23
)

@Given
fun fib25() = Fib25(given(), given())
class Fib26 @Inject constructor(
    val fibM1: Fib25,
    val fibM2: Fib24
)

@Given
fun fib26() = Fib26(given(), given())
class Fib27 @Inject constructor(
    val fibM1: Fib26,
    val fibM2: Fib25
)

@Given
fun fib27() = Fib27(given(), given())
class Fib28 @Inject constructor(
    val fibM1: Fib27,
    val fibM2: Fib26
)

@Given
fun fib28() = Fib28(given(), given())
class Fib29 @Inject constructor(
    val fibM1: Fib28,
    val fibM2: Fib27
)

@Given
fun fib29() = Fib29(given(), given())
class Fib30 @Inject constructor(
    val fibM1: Fib29,
    val fibM2: Fib28
)

@Given
fun fib30() = Fib30(given(), given())
class Fib31 @Inject constructor(
    val fibM1: Fib30,
    val fibM2: Fib29
)

@Given
fun fib31() = Fib31(given(), given())
class Fib32 @Inject constructor(
    val fibM1: Fib31,
    val fibM2: Fib30
)

@Given
fun fib32() = Fib32(given(), given())
class Fib33 @Inject constructor(
    val fibM1: Fib32,
    val fibM2: Fib31
)

@Given
fun fib33() = Fib33(given(), given())
class Fib34 @Inject constructor(
    val fibM1: Fib33,
    val fibM2: Fib32
)

@Given
fun fib34() = Fib34(given(), given())
class Fib35 @Inject constructor(
    val fibM1: Fib34,
    val fibM2: Fib33
)

@Given
fun fib35() = Fib35(given(), given())
class Fib36 @Inject constructor(
    val fibM1: Fib35,
    val fibM2: Fib34
)

@Given
fun fib36() = Fib36(given(), given())
class Fib37 @Inject constructor(
    val fibM1: Fib36,
    val fibM2: Fib35
)

@Given
fun fib37() = Fib37(given(), given())
class Fib38 @Inject constructor(
    val fibM1: Fib37,
    val fibM2: Fib36
)

@Given
fun fib38() = Fib38(given(), given())
class Fib39 @Inject constructor(
    val fibM1: Fib38,
    val fibM2: Fib37
)

@Given
fun fib39() = Fib39(given(), given())
class Fib40 @Inject constructor(
    val fibM1: Fib39,
    val fibM2: Fib38
)

@Given
fun fib40() = Fib40(given(), given())
class Fib41 @Inject constructor(
    val fibM1: Fib40,
    val fibM2: Fib39
)

@Given
fun fib41() = Fib41(given(), given())
class Fib42 @Inject constructor(
    val fibM1: Fib41,
    val fibM2: Fib40
)

@Given
fun fib42() = Fib42(given(), given())
class Fib43 @Inject constructor(
    val fibM1: Fib42,
    val fibM2: Fib41
)

@Given
fun fib43() = Fib43(given(), given())
class Fib44 @Inject constructor(
    val fibM1: Fib43,
    val fibM2: Fib42
)

@Given
fun fib44() = Fib44(given(), given())
class Fib45 @Inject constructor(
    val fibM1: Fib44,
    val fibM2: Fib43
)

@Given
fun fib45() = Fib45(given(), given())
class Fib46 @Inject constructor(
    val fibM1: Fib45,
    val fibM2: Fib44
)

@Given
fun fib46() = Fib46(given(), given())
class Fib47 @Inject constructor(
    val fibM1: Fib46,
    val fibM2: Fib45
)

@Given
fun fib47() = Fib47(given(), given())
class Fib48 @Inject constructor(
    val fibM1: Fib47,
    val fibM2: Fib46
)

@Given
fun fib48() = Fib48(given(), given())
class Fib49 @Inject constructor(
    val fibM1: Fib48,
    val fibM2: Fib47
)

@Given
fun fib49() = Fib49(given(), given())
class Fib50 @Inject constructor(
    val fibM1: Fib49,
    val fibM2: Fib48
)

@Given
fun fib50() = Fib50(given(), given())
class Fib51 @Inject constructor(
    val fibM1: Fib50,
    val fibM2: Fib49
)

@Given
fun fib51() = Fib51(given(), given())
class Fib52 @Inject constructor(
    val fibM1: Fib51,
    val fibM2: Fib50
)

@Given
fun fib52() = Fib52(given(), given())
class Fib53 @Inject constructor(
    val fibM1: Fib52,
    val fibM2: Fib51
)

@Given
fun fib53() = Fib53(given(), given())
class Fib54 @Inject constructor(
    val fibM1: Fib53,
    val fibM2: Fib52
)

@Given
fun fib54() = Fib54(given(), given())
class Fib55 @Inject constructor(
    val fibM1: Fib54,
    val fibM2: Fib53
)

@Given
fun fib55() = Fib55(given(), given())
class Fib56 @Inject constructor(
    val fibM1: Fib55,
    val fibM2: Fib54
)

@Given
fun fib56() = Fib56(given(), given())
class Fib57 @Inject constructor(
    val fibM1: Fib56,
    val fibM2: Fib55
)

@Given
fun fib57() = Fib57(given(), given())
class Fib58 @Inject constructor(
    val fibM1: Fib57,
    val fibM2: Fib56
)

@Given
fun fib58() = Fib58(given(), given())
class Fib59 @Inject constructor(
    val fibM1: Fib58,
    val fibM2: Fib57
)

@Given
fun fib59() = Fib59(given(), given())
class Fib60 @Inject constructor(
    val fibM1: Fib59,
    val fibM2: Fib58
)

@Given
fun fib60() = Fib60(given(), given())
class Fib61 @Inject constructor(
    val fibM1: Fib60,
    val fibM2: Fib59
)

@Given
fun fib61() = Fib61(given(), given())
class Fib62 @Inject constructor(
    val fibM1: Fib61,
    val fibM2: Fib60
)

@Given
fun fib62() = Fib62(given(), given())
class Fib63 @Inject constructor(
    val fibM1: Fib62,
    val fibM2: Fib61
)

@Given
fun fib63() = Fib63(given(), given())
class Fib64 @Inject constructor(
    val fibM1: Fib63,
    val fibM2: Fib62
)

@Given
fun fib64() = Fib64(given(), given())
class Fib65 @Inject constructor(
    val fibM1: Fib64,
    val fibM2: Fib63
)

@Given
fun fib65() = Fib65(given(), given())
class Fib66 @Inject constructor(
    val fibM1: Fib65,
    val fibM2: Fib64
)

@Given
fun fib66() = Fib66(given(), given())
class Fib67 @Inject constructor(
    val fibM1: Fib66,
    val fibM2: Fib65
)

@Given
fun fib67() = Fib67(given(), given())
class Fib68 @Inject constructor(
    val fibM1: Fib67,
    val fibM2: Fib66
)

@Given
fun fib68() = Fib68(given(), given())
class Fib69 @Inject constructor(
    val fibM1: Fib68,
    val fibM2: Fib67
)

@Given
fun fib69() = Fib69(given(), given())
class Fib70 @Inject constructor(
    val fibM1: Fib69,
    val fibM2: Fib68
)

@Given
fun fib70() = Fib70(given(), given())
class Fib71 @Inject constructor(
    val fibM1: Fib70,
    val fibM2: Fib69
)

@Given
fun fib71() = Fib71(given(), given())
class Fib72 @Inject constructor(
    val fibM1: Fib71,
    val fibM2: Fib70
)

@Given
fun fib72() = Fib72(given(), given())
class Fib73 @Inject constructor(
    val fibM1: Fib72,
    val fibM2: Fib71
)

@Given
fun fib73() = Fib73(given(), given())
class Fib74 @Inject constructor(
    val fibM1: Fib73,
    val fibM2: Fib72
)

@Given
fun fib74() = Fib74(given(), given())
class Fib75 @Inject constructor(
    val fibM1: Fib74,
    val fibM2: Fib73
)

@Given
fun fib75() = Fib75(given(), given())
class Fib76 @Inject constructor(
    val fibM1: Fib75,
    val fibM2: Fib74
)

@Given
fun fib76() = Fib76(given(), given())
class Fib77 @Inject constructor(
    val fibM1: Fib76,
    val fibM2: Fib75
)

@Given
fun fib77() = Fib77(given(), given())
class Fib78 @Inject constructor(
    val fibM1: Fib77,
    val fibM2: Fib76
)

@Given
fun fib78() = Fib78(given(), given())
class Fib79 @Inject constructor(
    val fibM1: Fib78,
    val fibM2: Fib77
)

@Given
fun fib79() = Fib79(given(), given())
class Fib80 @Inject constructor(
    val fibM1: Fib79,
    val fibM2: Fib78
)

@Given
fun fib80() = Fib80(given(), given())
class Fib81 @Inject constructor(
    val fibM1: Fib80,
    val fibM2: Fib79
)

@Given
fun fib81() = Fib81(given(), given())
class Fib82 @Inject constructor(
    val fibM1: Fib81,
    val fibM2: Fib80
)

@Given
fun fib82() = Fib82(given(), given())
class Fib83 @Inject constructor(
    val fibM1: Fib82,
    val fibM2: Fib81
)

@Given
fun fib83() = Fib83(given(), given())
class Fib84 @Inject constructor(
    val fibM1: Fib83,
    val fibM2: Fib82
)

@Given
fun fib84() = Fib84(given(), given())
class Fib85 @Inject constructor(
    val fibM1: Fib84,
    val fibM2: Fib83
)

@Given
fun fib85() = Fib85(given(), given())
class Fib86 @Inject constructor(
    val fibM1: Fib85,
    val fibM2: Fib84
)

@Given
fun fib86() = Fib86(given(), given())
class Fib87 @Inject constructor(
    val fibM1: Fib86,
    val fibM2: Fib85
)

@Given
fun fib87() = Fib87(given(), given())
class Fib88 @Inject constructor(
    val fibM1: Fib87,
    val fibM2: Fib86
)

@Given
fun fib88() = Fib88(given(), given())
class Fib89 @Inject constructor(
    val fibM1: Fib88,
    val fibM2: Fib87
)

@Given
fun fib89() = Fib89(given(), given())
class Fib90 @Inject constructor(
    val fibM1: Fib89,
    val fibM2: Fib88
)

@Given
fun fib90() = Fib90(given(), given())
class Fib91 @Inject constructor(
    val fibM1: Fib90,
    val fibM2: Fib89
)

@Given
fun fib91() = Fib91(given(), given())
class Fib92 @Inject constructor(
    val fibM1: Fib91,
    val fibM2: Fib90
)

@Given
fun fib92() = Fib92(given(), given())
class Fib93 @Inject constructor(
    val fibM1: Fib92,
    val fibM2: Fib91
)

@Given
fun fib93() = Fib93(given(), given())
class Fib94 @Inject constructor(
    val fibM1: Fib93,
    val fibM2: Fib92
)

@Given
fun fib94() = Fib94(given(), given())
class Fib95 @Inject constructor(
    val fibM1: Fib94,
    val fibM2: Fib93
)

@Given
fun fib95() = Fib95(given(), given())
class Fib96 @Inject constructor(
    val fibM1: Fib95,
    val fibM2: Fib94
)

@Given
fun fib96() = Fib96(given(), given())
class Fib97 @Inject constructor(
    val fibM1: Fib96,
    val fibM2: Fib95
)

@Given
fun fib97() = Fib97(given(), given())
class Fib98 @Inject constructor(
    val fibM1: Fib97,
    val fibM2: Fib96
)

@Given
fun fib98() = Fib98(given(), given())
class Fib99 @Inject constructor(
    val fibM1: Fib98,
    val fibM2: Fib97
)

@Given
fun fib99() = Fib99(given(), given())
class Fib100 @Inject constructor(
    val fibM1: Fib99,
    val fibM2: Fib98
)

@Given
fun fib100() = Fib100(given(), given())
class Fib101 @Inject constructor(
    val fibM1: Fib100,
    val fibM2: Fib99
)

@Given
fun fib101() = Fib101(given(), given())
class Fib102 @Inject constructor(
    val fibM1: Fib101,
    val fibM2: Fib100
)

@Given
fun fib102() = Fib102(given(), given())
class Fib103 @Inject constructor(
    val fibM1: Fib102,
    val fibM2: Fib101
)

@Given
fun fib103() = Fib103(given(), given())
class Fib104 @Inject constructor(
    val fibM1: Fib103,
    val fibM2: Fib102
)

@Given
fun fib104() = Fib104(given(), given())
class Fib105 @Inject constructor(
    val fibM1: Fib104,
    val fibM2: Fib103
)

@Given
fun fib105() = Fib105(given(), given())
class Fib106 @Inject constructor(
    val fibM1: Fib105,
    val fibM2: Fib104
)

@Given
fun fib106() = Fib106(given(), given())
class Fib107 @Inject constructor(
    val fibM1: Fib106,
    val fibM2: Fib105
)

@Given
fun fib107() = Fib107(given(), given())
class Fib108 @Inject constructor(
    val fibM1: Fib107,
    val fibM2: Fib106
)

@Given
fun fib108() = Fib108(given(), given())
class Fib109 @Inject constructor(
    val fibM1: Fib108,
    val fibM2: Fib107
)

@Given
fun fib109() = Fib109(given(), given())
class Fib110 @Inject constructor(
    val fibM1: Fib109,
    val fibM2: Fib108
)

@Given
fun fib110() = Fib110(given(), given())
class Fib111 @Inject constructor(
    val fibM1: Fib110,
    val fibM2: Fib109
)

@Given
fun fib111() = Fib111(given(), given())
class Fib112 @Inject constructor(
    val fibM1: Fib111,
    val fibM2: Fib110
)

@Given
fun fib112() = Fib112(given(), given())
class Fib113 @Inject constructor(
    val fibM1: Fib112,
    val fibM2: Fib111
)

@Given
fun fib113() = Fib113(given(), given())
class Fib114 @Inject constructor(
    val fibM1: Fib113,
    val fibM2: Fib112
)

@Given
fun fib114() = Fib114(given(), given())
class Fib115 @Inject constructor(
    val fibM1: Fib114,
    val fibM2: Fib113
)

@Given
fun fib115() = Fib115(given(), given())
class Fib116 @Inject constructor(
    val fibM1: Fib115,
    val fibM2: Fib114
)

@Given
fun fib116() = Fib116(given(), given())
class Fib117 @Inject constructor(
    val fibM1: Fib116,
    val fibM2: Fib115
)

@Given
fun fib117() = Fib117(given(), given())
class Fib118 @Inject constructor(
    val fibM1: Fib117,
    val fibM2: Fib116
)

@Given
fun fib118() = Fib118(given(), given())
class Fib119 @Inject constructor(
    val fibM1: Fib118,
    val fibM2: Fib117
)

@Given
fun fib119() = Fib119(given(), given())
class Fib120 @Inject constructor(
    val fibM1: Fib119,
    val fibM2: Fib118
)

@Given
fun fib120() = Fib120(given(), given())
class Fib121 @Inject constructor(
    val fibM1: Fib120,
    val fibM2: Fib119
)

@Given
fun fib121() = Fib121(given(), given())
class Fib122 @Inject constructor(
    val fibM1: Fib121,
    val fibM2: Fib120
)

@Given
fun fib122() = Fib122(given(), given())
class Fib123 @Inject constructor(
    val fibM1: Fib122,
    val fibM2: Fib121
)

@Given
fun fib123() = Fib123(given(), given())
class Fib124 @Inject constructor(
    val fibM1: Fib123,
    val fibM2: Fib122
)

@Given
fun fib124() = Fib124(given(), given())
class Fib125 @Inject constructor(
    val fibM1: Fib124,
    val fibM2: Fib123
)

@Given
fun fib125() = Fib125(given(), given())
class Fib126 @Inject constructor(
    val fibM1: Fib125,
    val fibM2: Fib124
)

@Given
fun fib126() = Fib126(given(), given())
class Fib127 @Inject constructor(
    val fibM1: Fib126,
    val fibM2: Fib125
)

@Given
fun fib127() = Fib127(given(), given())
class Fib128 @Inject constructor(
    val fibM1: Fib127,
    val fibM2: Fib126
)

@Given
fun fib128() = Fib128(given(), given())
class Fib129 @Inject constructor(
    val fibM1: Fib128,
    val fibM2: Fib127
)

@Given
fun fib129() = Fib129(given(), given())
class Fib130 @Inject constructor(
    val fibM1: Fib129,
    val fibM2: Fib128
)

@Given
fun fib130() = Fib130(given(), given())
class Fib131 @Inject constructor(
    val fibM1: Fib130,
    val fibM2: Fib129
)

@Given
fun fib131() = Fib131(given(), given())
class Fib132 @Inject constructor(
    val fibM1: Fib131,
    val fibM2: Fib130
)

@Given
fun fib132() = Fib132(given(), given())
class Fib133 @Inject constructor(
    val fibM1: Fib132,
    val fibM2: Fib131
)

@Given
fun fib133() = Fib133(given(), given())
class Fib134 @Inject constructor(
    val fibM1: Fib133,
    val fibM2: Fib132
)

@Given
fun fib134() = Fib134(given(), given())
class Fib135 @Inject constructor(
    val fibM1: Fib134,
    val fibM2: Fib133
)

@Given
fun fib135() = Fib135(given(), given())
class Fib136 @Inject constructor(
    val fibM1: Fib135,
    val fibM2: Fib134
)

@Given
fun fib136() = Fib136(given(), given())
class Fib137 @Inject constructor(
    val fibM1: Fib136,
    val fibM2: Fib135
)

@Given
fun fib137() = Fib137(given(), given())
class Fib138 @Inject constructor(
    val fibM1: Fib137,
    val fibM2: Fib136
)

@Given
fun fib138() = Fib138(given(), given())
class Fib139 @Inject constructor(
    val fibM1: Fib138,
    val fibM2: Fib137
)

@Given
fun fib139() = Fib139(given(), given())
class Fib140 @Inject constructor(
    val fibM1: Fib139,
    val fibM2: Fib138
)

@Given
fun fib140() = Fib140(given(), given())
class Fib141 @Inject constructor(
    val fibM1: Fib140,
    val fibM2: Fib139
)

@Given
fun fib141() = Fib141(given(), given())
class Fib142 @Inject constructor(
    val fibM1: Fib141,
    val fibM2: Fib140
)

@Given
fun fib142() = Fib142(given(), given())
class Fib143 @Inject constructor(
    val fibM1: Fib142,
    val fibM2: Fib141
)

@Given
fun fib143() = Fib143(given(), given())
class Fib144 @Inject constructor(
    val fibM1: Fib143,
    val fibM2: Fib142
)

@Given
fun fib144() = Fib144(given(), given())
class Fib145 @Inject constructor(
    val fibM1: Fib144,
    val fibM2: Fib143
)

@Given
fun fib145() = Fib145(given(), given())
class Fib146 @Inject constructor(
    val fibM1: Fib145,
    val fibM2: Fib144
)

@Given
fun fib146() = Fib146(given(), given())
class Fib147 @Inject constructor(
    val fibM1: Fib146,
    val fibM2: Fib145
)

@Given
fun fib147() = Fib147(given(), given())
class Fib148 @Inject constructor(
    val fibM1: Fib147,
    val fibM2: Fib146
)

@Given
fun fib148() = Fib148(given(), given())
class Fib149 @Inject constructor(
    val fibM1: Fib148,
    val fibM2: Fib147
)

@Given
fun fib149() = Fib149(given(), given())
class Fib150 @Inject constructor(
    val fibM1: Fib149,
    val fibM2: Fib148
)

@Given
fun fib150() = Fib150(given(), given())
class Fib151 @Inject constructor(
    val fibM1: Fib150,
    val fibM2: Fib149
)

@Given
fun fib151() = Fib151(given(), given())
class Fib152 @Inject constructor(
    val fibM1: Fib151,
    val fibM2: Fib150
)

@Given
fun fib152() = Fib152(given(), given())
class Fib153 @Inject constructor(
    val fibM1: Fib152,
    val fibM2: Fib151
)

@Given
fun fib153() = Fib153(given(), given())
class Fib154 @Inject constructor(
    val fibM1: Fib153,
    val fibM2: Fib152
)

@Given
fun fib154() = Fib154(given(), given())
class Fib155 @Inject constructor(
    val fibM1: Fib154,
    val fibM2: Fib153
)

@Given
fun fib155() = Fib155(given(), given())
class Fib156 @Inject constructor(
    val fibM1: Fib155,
    val fibM2: Fib154
)

@Given
fun fib156() = Fib156(given(), given())
class Fib157 @Inject constructor(
    val fibM1: Fib156,
    val fibM2: Fib155
)

@Given
fun fib157() = Fib157(given(), given())
class Fib158 @Inject constructor(
    val fibM1: Fib157,
    val fibM2: Fib156
)

@Given
fun fib158() = Fib158(given(), given())
class Fib159 @Inject constructor(
    val fibM1: Fib158,
    val fibM2: Fib157
)

@Given
fun fib159() = Fib159(given(), given())
class Fib160 @Inject constructor(
    val fibM1: Fib159,
    val fibM2: Fib158
)

@Given
fun fib160() = Fib160(given(), given())
class Fib161 @Inject constructor(
    val fibM1: Fib160,
    val fibM2: Fib159
)

@Given
fun fib161() = Fib161(given(), given())
class Fib162 @Inject constructor(
    val fibM1: Fib161,
    val fibM2: Fib160
)

@Given
fun fib162() = Fib162(given(), given())
class Fib163 @Inject constructor(
    val fibM1: Fib162,
    val fibM2: Fib161
)

@Given
fun fib163() = Fib163(given(), given())
class Fib164 @Inject constructor(
    val fibM1: Fib163,
    val fibM2: Fib162
)

@Given
fun fib164() = Fib164(given(), given())
class Fib165 @Inject constructor(
    val fibM1: Fib164,
    val fibM2: Fib163
)

@Given
fun fib165() = Fib165(given(), given())
class Fib166 @Inject constructor(
    val fibM1: Fib165,
    val fibM2: Fib164
)

@Given
fun fib166() = Fib166(given(), given())
class Fib167 @Inject constructor(
    val fibM1: Fib166,
    val fibM2: Fib165
)

@Given
fun fib167() = Fib167(given(), given())
class Fib168 @Inject constructor(
    val fibM1: Fib167,
    val fibM2: Fib166
)

@Given
fun fib168() = Fib168(given(), given())
class Fib169 @Inject constructor(
    val fibM1: Fib168,
    val fibM2: Fib167
)

@Given
fun fib169() = Fib169(given(), given())
class Fib170 @Inject constructor(
    val fibM1: Fib169,
    val fibM2: Fib168
)

@Given
fun fib170() = Fib170(given(), given())
class Fib171 @Inject constructor(
    val fibM1: Fib170,
    val fibM2: Fib169
)

@Given
fun fib171() = Fib171(given(), given())
class Fib172 @Inject constructor(
    val fibM1: Fib171,
    val fibM2: Fib170
)

@Given
fun fib172() = Fib172(given(), given())
class Fib173 @Inject constructor(
    val fibM1: Fib172,
    val fibM2: Fib171
)

@Given
fun fib173() = Fib173(given(), given())
class Fib174 @Inject constructor(
    val fibM1: Fib173,
    val fibM2: Fib172
)

@Given
fun fib174() = Fib174(given(), given())
class Fib175 @Inject constructor(
    val fibM1: Fib174,
    val fibM2: Fib173
)

@Given
fun fib175() = Fib175(given(), given())
class Fib176 @Inject constructor(
    val fibM1: Fib175,
    val fibM2: Fib174
)

@Given
fun fib176() = Fib176(given(), given())
class Fib177 @Inject constructor(
    val fibM1: Fib176,
    val fibM2: Fib175
)

@Given
fun fib177() = Fib177(given(), given())
class Fib178 @Inject constructor(
    val fibM1: Fib177,
    val fibM2: Fib176
)

@Given
fun fib178() = Fib178(given(), given())
class Fib179 @Inject constructor(
    val fibM1: Fib178,
    val fibM2: Fib177
)

@Given
fun fib179() = Fib179(given(), given())
class Fib180 @Inject constructor(
    val fibM1: Fib179,
    val fibM2: Fib178
)

@Given
fun fib180() = Fib180(given(), given())
class Fib181 @Inject constructor(
    val fibM1: Fib180,
    val fibM2: Fib179
)

@Given
fun fib181() = Fib181(given(), given())
class Fib182 @Inject constructor(
    val fibM1: Fib181,
    val fibM2: Fib180
)

@Given
fun fib182() = Fib182(given(), given())
class Fib183 @Inject constructor(
    val fibM1: Fib182,
    val fibM2: Fib181
)

@Given
fun fib183() = Fib183(given(), given())
class Fib184 @Inject constructor(
    val fibM1: Fib183,
    val fibM2: Fib182
)

@Given
fun fib184() = Fib184(given(), given())
class Fib185 @Inject constructor(
    val fibM1: Fib184,
    val fibM2: Fib183
)

@Given
fun fib185() = Fib185(given(), given())
class Fib186 @Inject constructor(
    val fibM1: Fib185,
    val fibM2: Fib184
)

@Given
fun fib186() = Fib186(given(), given())
class Fib187 @Inject constructor(
    val fibM1: Fib186,
    val fibM2: Fib185
)

@Given
fun fib187() = Fib187(given(), given())
class Fib188 @Inject constructor(
    val fibM1: Fib187,
    val fibM2: Fib186
)

@Given
fun fib188() = Fib188(given(), given())
class Fib189 @Inject constructor(
    val fibM1: Fib188,
    val fibM2: Fib187
)

@Given
fun fib189() = Fib189(given(), given())
class Fib190 @Inject constructor(
    val fibM1: Fib189,
    val fibM2: Fib188
)

@Given
fun fib190() = Fib190(given(), given())
class Fib191 @Inject constructor(
    val fibM1: Fib190,
    val fibM2: Fib189
)

@Given
fun fib191() = Fib191(given(), given())
class Fib192 @Inject constructor(
    val fibM1: Fib191,
    val fibM2: Fib190
)

@Given
fun fib192() = Fib192(given(), given())
class Fib193 @Inject constructor(
    val fibM1: Fib192,
    val fibM2: Fib191
)

@Given
fun fib193() = Fib193(given(), given())
class Fib194 @Inject constructor(
    val fibM1: Fib193,
    val fibM2: Fib192
)

@Given
fun fib194() = Fib194(given(), given())
class Fib195 @Inject constructor(
    val fibM1: Fib194,
    val fibM2: Fib193
)

@Given
fun fib195() = Fib195(given(), given())
class Fib196 @Inject constructor(
    val fibM1: Fib195,
    val fibM2: Fib194
)

@Given
fun fib196() = Fib196(given(), given())
class Fib197 @Inject constructor(
    val fibM1: Fib196,
    val fibM2: Fib195
)

@Given
fun fib197() = Fib197(given(), given())
class Fib198 @Inject constructor(
    val fibM1: Fib197,
    val fibM2: Fib196
)

@Given
fun fib198() = Fib198(given(), given())
class Fib199 @Inject constructor(
    val fibM1: Fib198,
    val fibM2: Fib197
)

@Given
fun fib199() = Fib199(given(), given())
class Fib200 @Inject constructor(
    val fibM1: Fib199,
    val fibM2: Fib198
)

@Given
fun fib200() = Fib200(given(), given())
class Fib201 @Inject constructor(
    val fibM1: Fib200,
    val fibM2: Fib199
)

@Given
fun fib201() = Fib201(given(), given())
class Fib202 @Inject constructor(
    val fibM1: Fib201,
    val fibM2: Fib200
)

@Given
fun fib202() = Fib202(given(), given())
class Fib203 @Inject constructor(
    val fibM1: Fib202,
    val fibM2: Fib201
)

@Given
fun fib203() = Fib203(given(), given())
class Fib204 @Inject constructor(
    val fibM1: Fib203,
    val fibM2: Fib202
)

@Given
fun fib204() = Fib204(given(), given())
class Fib205 @Inject constructor(
    val fibM1: Fib204,
    val fibM2: Fib203
)

@Given
fun fib205() = Fib205(given(), given())
class Fib206 @Inject constructor(
    val fibM1: Fib205,
    val fibM2: Fib204
)

@Given
fun fib206() = Fib206(given(), given())
class Fib207 @Inject constructor(
    val fibM1: Fib206,
    val fibM2: Fib205
)

@Given
fun fib207() = Fib207(given(), given())
class Fib208 @Inject constructor(
    val fibM1: Fib207,
    val fibM2: Fib206
)

@Given
fun fib208() = Fib208(given(), given())
class Fib209 @Inject constructor(
    val fibM1: Fib208,
    val fibM2: Fib207
)

@Given
fun fib209() = Fib209(given(), given())
class Fib210 @Inject constructor(
    val fibM1: Fib209,
    val fibM2: Fib208
)

@Given
fun fib210() = Fib210(given(), given())
class Fib211 @Inject constructor(
    val fibM1: Fib210,
    val fibM2: Fib209
)

@Given
fun fib211() = Fib211(given(), given())
class Fib212 @Inject constructor(
    val fibM1: Fib211,
    val fibM2: Fib210
)

@Given
fun fib212() = Fib212(given(), given())
class Fib213 @Inject constructor(
    val fibM1: Fib212,
    val fibM2: Fib211
)

@Given
fun fib213() = Fib213(given(), given())
class Fib214 @Inject constructor(
    val fibM1: Fib213,
    val fibM2: Fib212
)

@Given
fun fib214() = Fib214(given(), given())
class Fib215 @Inject constructor(
    val fibM1: Fib214,
    val fibM2: Fib213
)

@Given
fun fib215() = Fib215(given(), given())
class Fib216 @Inject constructor(
    val fibM1: Fib215,
    val fibM2: Fib214
)

@Given
fun fib216() = Fib216(given(), given())
class Fib217 @Inject constructor(
    val fibM1: Fib216,
    val fibM2: Fib215
)

@Given
fun fib217() = Fib217(given(), given())
class Fib218 @Inject constructor(
    val fibM1: Fib217,
    val fibM2: Fib216
)

@Given
fun fib218() = Fib218(given(), given())
class Fib219 @Inject constructor(
    val fibM1: Fib218,
    val fibM2: Fib217
)

@Given
fun fib219() = Fib219(given(), given())
class Fib220 @Inject constructor(
    val fibM1: Fib219,
    val fibM2: Fib218
)

@Given
fun fib220() = Fib220(given(), given())
class Fib221 @Inject constructor(
    val fibM1: Fib220,
    val fibM2: Fib219
)

@Given
fun fib221() = Fib221(given(), given())
class Fib222 @Inject constructor(
    val fibM1: Fib221,
    val fibM2: Fib220
)

@Given
fun fib222() = Fib222(given(), given())
class Fib223 @Inject constructor(
    val fibM1: Fib222,
    val fibM2: Fib221
)

@Given
fun fib223() = Fib223(given(), given())
class Fib224 @Inject constructor(
    val fibM1: Fib223,
    val fibM2: Fib222
)

@Given
fun fib224() = Fib224(given(), given())
class Fib225 @Inject constructor(
    val fibM1: Fib224,
    val fibM2: Fib223
)

@Given
fun fib225() = Fib225(given(), given())
class Fib226 @Inject constructor(
    val fibM1: Fib225,
    val fibM2: Fib224
)

@Given
fun fib226() = Fib226(given(), given())
class Fib227 @Inject constructor(
    val fibM1: Fib226,
    val fibM2: Fib225
)

@Given
fun fib227() = Fib227(given(), given())
class Fib228 @Inject constructor(
    val fibM1: Fib227,
    val fibM2: Fib226
)

@Given
fun fib228() = Fib228(given(), given())
class Fib229 @Inject constructor(
    val fibM1: Fib228,
    val fibM2: Fib227
)

@Given
fun fib229() = Fib229(given(), given())
class Fib230 @Inject constructor(
    val fibM1: Fib229,
    val fibM2: Fib228
)

@Given
fun fib230() = Fib230(given(), given())
class Fib231 @Inject constructor(
    val fibM1: Fib230,
    val fibM2: Fib229
)

@Given
fun fib231() = Fib231(given(), given())
class Fib232 @Inject constructor(
    val fibM1: Fib231,
    val fibM2: Fib230
)

@Given
fun fib232() = Fib232(given(), given())
class Fib233 @Inject constructor(
    val fibM1: Fib232,
    val fibM2: Fib231
)

@Given
fun fib233() = Fib233(given(), given())
class Fib234 @Inject constructor(
    val fibM1: Fib233,
    val fibM2: Fib232
)

@Given
fun fib234() = Fib234(given(), given())
class Fib235 @Inject constructor(
    val fibM1: Fib234,
    val fibM2: Fib233
)

@Given
fun fib235() = Fib235(given(), given())
class Fib236 @Inject constructor(
    val fibM1: Fib235,
    val fibM2: Fib234
)

@Given
fun fib236() = Fib236(given(), given())
class Fib237 @Inject constructor(
    val fibM1: Fib236,
    val fibM2: Fib235
)

@Given
fun fib237() = Fib237(given(), given())
class Fib238 @Inject constructor(
    val fibM1: Fib237,
    val fibM2: Fib236
)

@Given
fun fib238() = Fib238(given(), given())
class Fib239 @Inject constructor(
    val fibM1: Fib238,
    val fibM2: Fib237
)

@Given
fun fib239() = Fib239(given(), given())
class Fib240 @Inject constructor(
    val fibM1: Fib239,
    val fibM2: Fib238
)

@Given
fun fib240() = Fib240(given(), given())
class Fib241 @Inject constructor(
    val fibM1: Fib240,
    val fibM2: Fib239
)

@Given
fun fib241() = Fib241(given(), given())
class Fib242 @Inject constructor(
    val fibM1: Fib241,
    val fibM2: Fib240
)

@Given
fun fib242() = Fib242(given(), given())
class Fib243 @Inject constructor(
    val fibM1: Fib242,
    val fibM2: Fib241
)

@Given
fun fib243() = Fib243(given(), given())
class Fib244 @Inject constructor(
    val fibM1: Fib243,
    val fibM2: Fib242
)

@Given
fun fib244() = Fib244(given(), given())
class Fib245 @Inject constructor(
    val fibM1: Fib244,
    val fibM2: Fib243
)

@Given
fun fib245() = Fib245(given(), given())
class Fib246 @Inject constructor(
    val fibM1: Fib245,
    val fibM2: Fib244
)

@Given
fun fib246() = Fib246(given(), given())
class Fib247 @Inject constructor(
    val fibM1: Fib246,
    val fibM2: Fib245
)

@Given
fun fib247() = Fib247(given(), given())
class Fib248 @Inject constructor(
    val fibM1: Fib247,
    val fibM2: Fib246
)

@Given
fun fib248() = Fib248(given(), given())
class Fib249 @Inject constructor(
    val fibM1: Fib248,
    val fibM2: Fib247
)

@Given
fun fib249() = Fib249(given(), given())
class Fib250 @Inject constructor(
    val fibM1: Fib249,
    val fibM2: Fib248
)

@Given
fun fib250() = Fib250(given(), given())
class Fib251 @Inject constructor(
    val fibM1: Fib250,
    val fibM2: Fib249
)

@Given
fun fib251() = Fib251(given(), given())
class Fib252 @Inject constructor(
    val fibM1: Fib251,
    val fibM2: Fib250
)

@Given
fun fib252() = Fib252(given(), given())
class Fib253 @Inject constructor(
    val fibM1: Fib252,
    val fibM2: Fib251
)

@Given
fun fib253() = Fib253(given(), given())
class Fib254 @Inject constructor(
    val fibM1: Fib253,
    val fibM2: Fib252
)

@Given
fun fib254() = Fib254(given(), given())
class Fib255 @Inject constructor(
    val fibM1: Fib254,
    val fibM2: Fib253
)

@Given
fun fib255() = Fib255(given(), given())
class Fib256 @Inject constructor(
    val fibM1: Fib255,
    val fibM2: Fib254
)

@Given
fun fib256() = Fib256(given(), given())
class Fib257 @Inject constructor(
    val fibM1: Fib256,
    val fibM2: Fib255
)

@Given
fun fib257() = Fib257(given(), given())
class Fib258 @Inject constructor(
    val fibM1: Fib257,
    val fibM2: Fib256
)

@Given
fun fib258() = Fib258(given(), given())
class Fib259 @Inject constructor(
    val fibM1: Fib258,
    val fibM2: Fib257
)

@Given
fun fib259() = Fib259(given(), given())
class Fib260 @Inject constructor(
    val fibM1: Fib259,
    val fibM2: Fib258
)

@Given
fun fib260() = Fib260(given(), given())
class Fib261 @Inject constructor(
    val fibM1: Fib260,
    val fibM2: Fib259
)

@Given
fun fib261() = Fib261(given(), given())
class Fib262 @Inject constructor(
    val fibM1: Fib261,
    val fibM2: Fib260
)

@Given
fun fib262() = Fib262(given(), given())
class Fib263 @Inject constructor(
    val fibM1: Fib262,
    val fibM2: Fib261
)

@Given
fun fib263() = Fib263(given(), given())
class Fib264 @Inject constructor(
    val fibM1: Fib263,
    val fibM2: Fib262
)

@Given
fun fib264() = Fib264(given(), given())
class Fib265 @Inject constructor(
    val fibM1: Fib264,
    val fibM2: Fib263
)

@Given
fun fib265() = Fib265(given(), given())
class Fib266 @Inject constructor(
    val fibM1: Fib265,
    val fibM2: Fib264
)

@Given
fun fib266() = Fib266(given(), given())
class Fib267 @Inject constructor(
    val fibM1: Fib266,
    val fibM2: Fib265
)

@Given
fun fib267() = Fib267(given(), given())
class Fib268 @Inject constructor(
    val fibM1: Fib267,
    val fibM2: Fib266
)

@Given
fun fib268() = Fib268(given(), given())
class Fib269 @Inject constructor(
    val fibM1: Fib268,
    val fibM2: Fib267
)

@Given
fun fib269() = Fib269(given(), given())
class Fib270 @Inject constructor(
    val fibM1: Fib269,
    val fibM2: Fib268
)

@Given
fun fib270() = Fib270(given(), given())
class Fib271 @Inject constructor(
    val fibM1: Fib270,
    val fibM2: Fib269
)

@Given
fun fib271() = Fib271(given(), given())
class Fib272 @Inject constructor(
    val fibM1: Fib271,
    val fibM2: Fib270
)

@Given
fun fib272() = Fib272(given(), given())
class Fib273 @Inject constructor(
    val fibM1: Fib272,
    val fibM2: Fib271
)

@Given
fun fib273() = Fib273(given(), given())
class Fib274 @Inject constructor(
    val fibM1: Fib273,
    val fibM2: Fib272
)

@Given
fun fib274() = Fib274(given(), given())
class Fib275 @Inject constructor(
    val fibM1: Fib274,
    val fibM2: Fib273
)

@Given
fun fib275() = Fib275(given(), given())
class Fib276 @Inject constructor(
    val fibM1: Fib275,
    val fibM2: Fib274
)

@Given
fun fib276() = Fib276(given(), given())
class Fib277 @Inject constructor(
    val fibM1: Fib276,
    val fibM2: Fib275
)

@Given
fun fib277() = Fib277(given(), given())
class Fib278 @Inject constructor(
    val fibM1: Fib277,
    val fibM2: Fib276
)

@Given
fun fib278() = Fib278(given(), given())
class Fib279 @Inject constructor(
    val fibM1: Fib278,
    val fibM2: Fib277
)

@Given
fun fib279() = Fib279(given(), given())
class Fib280 @Inject constructor(
    val fibM1: Fib279,
    val fibM2: Fib278
)

@Given
fun fib280() = Fib280(given(), given())
class Fib281 @Inject constructor(
    val fibM1: Fib280,
    val fibM2: Fib279
)

@Given
fun fib281() = Fib281(given(), given())
class Fib282 @Inject constructor(
    val fibM1: Fib281,
    val fibM2: Fib280
)

@Given
fun fib282() = Fib282(given(), given())
class Fib283 @Inject constructor(
    val fibM1: Fib282,
    val fibM2: Fib281
)

@Given
fun fib283() = Fib283(given(), given())
class Fib284 @Inject constructor(
    val fibM1: Fib283,
    val fibM2: Fib282
)

@Given
fun fib284() = Fib284(given(), given())
class Fib285 @Inject constructor(
    val fibM1: Fib284,
    val fibM2: Fib283
)

@Given
fun fib285() = Fib285(given(), given())
class Fib286 @Inject constructor(
    val fibM1: Fib285,
    val fibM2: Fib284
)

@Given
fun fib286() = Fib286(given(), given())
class Fib287 @Inject constructor(
    val fibM1: Fib286,
    val fibM2: Fib285
)

@Given
fun fib287() = Fib287(given(), given())
class Fib288 @Inject constructor(
    val fibM1: Fib287,
    val fibM2: Fib286
)

@Given
fun fib288() = Fib288(given(), given())
class Fib289 @Inject constructor(
    val fibM1: Fib288,
    val fibM2: Fib287
)

@Given
fun fib289() = Fib289(given(), given())
class Fib290 @Inject constructor(
    val fibM1: Fib289,
    val fibM2: Fib288
)

@Given
fun fib290() = Fib290(given(), given())
class Fib291 @Inject constructor(
    val fibM1: Fib290,
    val fibM2: Fib289
)

@Given
fun fib291() = Fib291(given(), given())
class Fib292 @Inject constructor(
    val fibM1: Fib291,
    val fibM2: Fib290
)

@Given
fun fib292() = Fib292(given(), given())
class Fib293 @Inject constructor(
    val fibM1: Fib292,
    val fibM2: Fib291
)

@Given
fun fib293() = Fib293(given(), given())
class Fib294 @Inject constructor(
    val fibM1: Fib293,
    val fibM2: Fib292
)

@Given
fun fib294() = Fib294(given(), given())
class Fib295 @Inject constructor(
    val fibM1: Fib294,
    val fibM2: Fib293
)

@Given
fun fib295() = Fib295(given(), given())
class Fib296 @Inject constructor(
    val fibM1: Fib295,
    val fibM2: Fib294
)

@Given
fun fib296() = Fib296(given(), given())
class Fib297 @Inject constructor(
    val fibM1: Fib296,
    val fibM2: Fib295
)

@Given
fun fib297() = Fib297(given(), given())
class Fib298 @Inject constructor(
    val fibM1: Fib297,
    val fibM2: Fib296
)

@Given
fun fib298() = Fib298(given(), given())
class Fib299 @Inject constructor(
    val fibM1: Fib298,
    val fibM2: Fib297
)

@Given
fun fib299() = Fib299(given(), given())
class Fib300 @Inject constructor(
    val fibM1: Fib299,
    val fibM2: Fib298
)

@Given
fun fib300() = Fib300(given(), given())
class Fib301 @Inject constructor(
    val fibM1: Fib300,
    val fibM2: Fib299
)

@Given
fun fib301() = Fib301(given(), given())
class Fib302 @Inject constructor(
    val fibM1: Fib301,
    val fibM2: Fib300
)

@Given
fun fib302() = Fib302(given(), given())
class Fib303 @Inject constructor(
    val fibM1: Fib302,
    val fibM2: Fib301
)

@Given
fun fib303() = Fib303(given(), given())
class Fib304 @Inject constructor(
    val fibM1: Fib303,
    val fibM2: Fib302
)

@Given
fun fib304() = Fib304(given(), given())
class Fib305 @Inject constructor(
    val fibM1: Fib304,
    val fibM2: Fib303
)

@Given
fun fib305() = Fib305(given(), given())
class Fib306 @Inject constructor(
    val fibM1: Fib305,
    val fibM2: Fib304
)

@Given
fun fib306() = Fib306(given(), given())
class Fib307 @Inject constructor(
    val fibM1: Fib306,
    val fibM2: Fib305
)

@Given
fun fib307() = Fib307(given(), given())
class Fib308 @Inject constructor(
    val fibM1: Fib307,
    val fibM2: Fib306
)

@Given
fun fib308() = Fib308(given(), given())
class Fib309 @Inject constructor(
    val fibM1: Fib308,
    val fibM2: Fib307
)

@Given
fun fib309() = Fib309(given(), given())
class Fib310 @Inject constructor(
    val fibM1: Fib309,
    val fibM2: Fib308
)

@Given
fun fib310() = Fib310(given(), given())
class Fib311 @Inject constructor(
    val fibM1: Fib310,
    val fibM2: Fib309
)

@Given
fun fib311() = Fib311(given(), given())
class Fib312 @Inject constructor(
    val fibM1: Fib311,
    val fibM2: Fib310
)

@Given
fun fib312() = Fib312(given(), given())
class Fib313 @Inject constructor(
    val fibM1: Fib312,
    val fibM2: Fib311
)

@Given
fun fib313() = Fib313(given(), given())
class Fib314 @Inject constructor(
    val fibM1: Fib313,
    val fibM2: Fib312
)

@Given
fun fib314() = Fib314(given(), given())
class Fib315 @Inject constructor(
    val fibM1: Fib314,
    val fibM2: Fib313
)

@Given
fun fib315() = Fib315(given(), given())
class Fib316 @Inject constructor(
    val fibM1: Fib315,
    val fibM2: Fib314
)

@Given
fun fib316() = Fib316(given(), given())
class Fib317 @Inject constructor(
    val fibM1: Fib316,
    val fibM2: Fib315
)

@Given
fun fib317() = Fib317(given(), given())
class Fib318 @Inject constructor(
    val fibM1: Fib317,
    val fibM2: Fib316
)

@Given
fun fib318() = Fib318(given(), given())
class Fib319 @Inject constructor(
    val fibM1: Fib318,
    val fibM2: Fib317
)

@Given
fun fib319() = Fib319(given(), given())
class Fib320 @Inject constructor(
    val fibM1: Fib319,
    val fibM2: Fib318
)

@Given
fun fib320() = Fib320(given(), given())
class Fib321 @Inject constructor(
    val fibM1: Fib320,
    val fibM2: Fib319
)

@Given
fun fib321() = Fib321(given(), given())
class Fib322 @Inject constructor(
    val fibM1: Fib321,
    val fibM2: Fib320
)

@Given
fun fib322() = Fib322(given(), given())
class Fib323 @Inject constructor(
    val fibM1: Fib322,
    val fibM2: Fib321
)

@Given
fun fib323() = Fib323(given(), given())
class Fib324 @Inject constructor(
    val fibM1: Fib323,
    val fibM2: Fib322
)

@Given
fun fib324() = Fib324(given(), given())
class Fib325 @Inject constructor(
    val fibM1: Fib324,
    val fibM2: Fib323
)

@Given
fun fib325() = Fib325(given(), given())
class Fib326 @Inject constructor(
    val fibM1: Fib325,
    val fibM2: Fib324
)

@Given
fun fib326() = Fib326(given(), given())
class Fib327 @Inject constructor(
    val fibM1: Fib326,
    val fibM2: Fib325
)

@Given
fun fib327() = Fib327(given(), given())
class Fib328 @Inject constructor(
    val fibM1: Fib327,
    val fibM2: Fib326
)

@Given
fun fib328() = Fib328(given(), given())
class Fib329 @Inject constructor(
    val fibM1: Fib328,
    val fibM2: Fib327
)

@Given
fun fib329() = Fib329(given(), given())
class Fib330 @Inject constructor(
    val fibM1: Fib329,
    val fibM2: Fib328
)

@Given
fun fib330() = Fib330(given(), given())
class Fib331 @Inject constructor(
    val fibM1: Fib330,
    val fibM2: Fib329
)

@Given
fun fib331() = Fib331(given(), given())
class Fib332 @Inject constructor(
    val fibM1: Fib331,
    val fibM2: Fib330
)

@Given
fun fib332() = Fib332(given(), given())
class Fib333 @Inject constructor(
    val fibM1: Fib332,
    val fibM2: Fib331
)

@Given
fun fib333() = Fib333(given(), given())
class Fib334 @Inject constructor(
    val fibM1: Fib333,
    val fibM2: Fib332
)

@Given
fun fib334() = Fib334(given(), given())
class Fib335 @Inject constructor(
    val fibM1: Fib334,
    val fibM2: Fib333
)

@Given
fun fib335() = Fib335(given(), given())
class Fib336 @Inject constructor(
    val fibM1: Fib335,
    val fibM2: Fib334
)

@Given
fun fib336() = Fib336(given(), given())
class Fib337 @Inject constructor(
    val fibM1: Fib336,
    val fibM2: Fib335
)

@Given
fun fib337() = Fib337(given(), given())
class Fib338 @Inject constructor(
    val fibM1: Fib337,
    val fibM2: Fib336
)

@Given
fun fib338() = Fib338(given(), given())
class Fib339 @Inject constructor(
    val fibM1: Fib338,
    val fibM2: Fib337
)

@Given
fun fib339() = Fib339(given(), given())
class Fib340 @Inject constructor(
    val fibM1: Fib339,
    val fibM2: Fib338
)

@Given
fun fib340() = Fib340(given(), given())
class Fib341 @Inject constructor(
    val fibM1: Fib340,
    val fibM2: Fib339
)

@Given
fun fib341() = Fib341(given(), given())
class Fib342 @Inject constructor(
    val fibM1: Fib341,
    val fibM2: Fib340
)

@Given
fun fib342() = Fib342(given(), given())
class Fib343 @Inject constructor(
    val fibM1: Fib342,
    val fibM2: Fib341
)

@Given
fun fib343() = Fib343(given(), given())
class Fib344 @Inject constructor(
    val fibM1: Fib343,
    val fibM2: Fib342
)

@Given
fun fib344() = Fib344(given(), given())
class Fib345 @Inject constructor(
    val fibM1: Fib344,
    val fibM2: Fib343
)

@Given
fun fib345() = Fib345(given(), given())
class Fib346 @Inject constructor(
    val fibM1: Fib345,
    val fibM2: Fib344
)

@Given
fun fib346() = Fib346(given(), given())
class Fib347 @Inject constructor(
    val fibM1: Fib346,
    val fibM2: Fib345
)

@Given
fun fib347() = Fib347(given(), given())
class Fib348 @Inject constructor(
    val fibM1: Fib347,
    val fibM2: Fib346
)

@Given
fun fib348() = Fib348(given(), given())
class Fib349 @Inject constructor(
    val fibM1: Fib348,
    val fibM2: Fib347
)

@Given
fun fib349() = Fib349(given(), given())
class Fib350 @Inject constructor(
    val fibM1: Fib349,
    val fibM2: Fib348
)

@Given
fun fib350() = Fib350(given(), given())
class Fib351 @Inject constructor(
    val fibM1: Fib350,
    val fibM2: Fib349
)

@Given
fun fib351() = Fib351(given(), given())
class Fib352 @Inject constructor(
    val fibM1: Fib351,
    val fibM2: Fib350
)

@Given
fun fib352() = Fib352(given(), given())
class Fib353 @Inject constructor(
    val fibM1: Fib352,
    val fibM2: Fib351
)

@Given
fun fib353() = Fib353(given(), given())
class Fib354 @Inject constructor(
    val fibM1: Fib353,
    val fibM2: Fib352
)

@Given
fun fib354() = Fib354(given(), given())
class Fib355 @Inject constructor(
    val fibM1: Fib354,
    val fibM2: Fib353
)

@Given
fun fib355() = Fib355(given(), given())
class Fib356 @Inject constructor(
    val fibM1: Fib355,
    val fibM2: Fib354
)

@Given
fun fib356() = Fib356(given(), given())
class Fib357 @Inject constructor(
    val fibM1: Fib356,
    val fibM2: Fib355
)

@Given
fun fib357() = Fib357(given(), given())
class Fib358 @Inject constructor(
    val fibM1: Fib357,
    val fibM2: Fib356
)

@Given
fun fib358() = Fib358(given(), given())
class Fib359 @Inject constructor(
    val fibM1: Fib358,
    val fibM2: Fib357
)

@Given
fun fib359() = Fib359(given(), given())
class Fib360 @Inject constructor(
    val fibM1: Fib359,
    val fibM2: Fib358
)

@Given
fun fib360() = Fib360(given(), given())
class Fib361 @Inject constructor(
    val fibM1: Fib360,
    val fibM2: Fib359
)

@Given
fun fib361() = Fib361(given(), given())
class Fib362 @Inject constructor(
    val fibM1: Fib361,
    val fibM2: Fib360
)

@Given
fun fib362() = Fib362(given(), given())
class Fib363 @Inject constructor(
    val fibM1: Fib362,
    val fibM2: Fib361
)

@Given
fun fib363() = Fib363(given(), given())
class Fib364 @Inject constructor(
    val fibM1: Fib363,
    val fibM2: Fib362
)

@Given
fun fib364() = Fib364(given(), given())
class Fib365 @Inject constructor(
    val fibM1: Fib364,
    val fibM2: Fib363
)

@Given
fun fib365() = Fib365(given(), given())
class Fib366 @Inject constructor(
    val fibM1: Fib365,
    val fibM2: Fib364
)

@Given
fun fib366() = Fib366(given(), given())
class Fib367 @Inject constructor(
    val fibM1: Fib366,
    val fibM2: Fib365
)

@Given
fun fib367() = Fib367(given(), given())
class Fib368 @Inject constructor(
    val fibM1: Fib367,
    val fibM2: Fib366
)

@Given
fun fib368() = Fib368(given(), given())
class Fib369 @Inject constructor(
    val fibM1: Fib368,
    val fibM2: Fib367
)

@Given
fun fib369() = Fib369(given(), given())
class Fib370 @Inject constructor(
    val fibM1: Fib369,
    val fibM2: Fib368
)

@Given
fun fib370() = Fib370(given(), given())
class Fib371 @Inject constructor(
    val fibM1: Fib370,
    val fibM2: Fib369
)

@Given
fun fib371() = Fib371(given(), given())
class Fib372 @Inject constructor(
    val fibM1: Fib371,
    val fibM2: Fib370
)

@Given
fun fib372() = Fib372(given(), given())
class Fib373 @Inject constructor(
    val fibM1: Fib372,
    val fibM2: Fib371
)

@Given
fun fib373() = Fib373(given(), given())
class Fib374 @Inject constructor(
    val fibM1: Fib373,
    val fibM2: Fib372
)

@Given
fun fib374() = Fib374(given(), given())
class Fib375 @Inject constructor(
    val fibM1: Fib374,
    val fibM2: Fib373
)

@Given
fun fib375() = Fib375(given(), given())
class Fib376 @Inject constructor(
    val fibM1: Fib375,
    val fibM2: Fib374
)

@Given
fun fib376() = Fib376(given(), given())
class Fib377 @Inject constructor(
    val fibM1: Fib376,
    val fibM2: Fib375
)

@Given
fun fib377() = Fib377(given(), given())
class Fib378 @Inject constructor(
    val fibM1: Fib377,
    val fibM2: Fib376
)

@Given
fun fib378() = Fib378(given(), given())
class Fib379 @Inject constructor(
    val fibM1: Fib378,
    val fibM2: Fib377
)

@Given
fun fib379() = Fib379(given(), given())
class Fib380 @Inject constructor(
    val fibM1: Fib379,
    val fibM2: Fib378
)

@Given
fun fib380() = Fib380(given(), given())
class Fib381 @Inject constructor(
    val fibM1: Fib380,
    val fibM2: Fib379
)

@Given
fun fib381() = Fib381(given(), given())
class Fib382 @Inject constructor(
    val fibM1: Fib381,
    val fibM2: Fib380
)

@Given
fun fib382() = Fib382(given(), given())
class Fib383 @Inject constructor(
    val fibM1: Fib382,
    val fibM2: Fib381
)

@Given
fun fib383() = Fib383(given(), given())
class Fib384 @Inject constructor(
    val fibM1: Fib383,
    val fibM2: Fib382
)

@Given
fun fib384() = Fib384(given(), given())
class Fib385 @Inject constructor(
    val fibM1: Fib384,
    val fibM2: Fib383
)

@Given
fun fib385() = Fib385(given(), given())
class Fib386 @Inject constructor(
    val fibM1: Fib385,
    val fibM2: Fib384
)

@Given
fun fib386() = Fib386(given(), given())
class Fib387 @Inject constructor(
    val fibM1: Fib386,
    val fibM2: Fib385
)

@Given
fun fib387() = Fib387(given(), given())
class Fib388 @Inject constructor(
    val fibM1: Fib387,
    val fibM2: Fib386
)

@Given
fun fib388() = Fib388(given(), given())
class Fib389 @Inject constructor(
    val fibM1: Fib388,
    val fibM2: Fib387
)

@Given
fun fib389() = Fib389(given(), given())
class Fib390 @Inject constructor(
    val fibM1: Fib389,
    val fibM2: Fib388
)

@Given
fun fib390() = Fib390(given(), given())
class Fib391 @Inject constructor(
    val fibM1: Fib390,
    val fibM2: Fib389
)

@Given
fun fib391() = Fib391(given(), given())
class Fib392 @Inject constructor(
    val fibM1: Fib391,
    val fibM2: Fib390
)

@Given
fun fib392() = Fib392(given(), given())
class Fib393 @Inject constructor(
    val fibM1: Fib392,
    val fibM2: Fib391
)

@Given
fun fib393() = Fib393(given(), given())
class Fib394 @Inject constructor(
    val fibM1: Fib393,
    val fibM2: Fib392
)

@Given
fun fib394() = Fib394(given(), given())
class Fib395 @Inject constructor(
    val fibM1: Fib394,
    val fibM2: Fib393
)

@Given
fun fib395() = Fib395(given(), given())
class Fib396 @Inject constructor(
    val fibM1: Fib395,
    val fibM2: Fib394
)

@Given
fun fib396() = Fib396(given(), given())
class Fib397 @Inject constructor(
    val fibM1: Fib396,
    val fibM2: Fib395
)

@Given
fun fib397() = Fib397(given(), given())
class Fib398 @Inject constructor(
    val fibM1: Fib397,
    val fibM2: Fib396
)

@Given
fun fib398() = Fib398(given(), given())
class Fib399 @Inject constructor(
    val fibM1: Fib398,
    val fibM2: Fib397
)

@Given
fun fib399() = Fib399(given(), given())
class Fib400 @Inject constructor(
    val fibM1: Fib399,
    val fibM2: Fib398
)

@Given
fun fib400() = Fib400(given(), given())
