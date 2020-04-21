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

import com.ivianuu.injekt.Factory
import magnet.Instance
import magnet.Scoping
import javax.inject.Inject

@Factory
@Instance(type = Fib1::class, scoping = Scoping.UNSCOPED)
class Fib1 @Inject constructor()

@Factory
@Instance(type = Fib2::class, scoping = Scoping.UNSCOPED)
class Fib2 @Inject constructor()

@Factory
@Instance(type = Fib3::class, scoping = Scoping.UNSCOPED)
class Fib3 @Inject constructor(
    val fibM1: Fib2,
    val fibM2: Fib1
)

@Factory
@Instance(type = Fib4::class, scoping = Scoping.UNSCOPED)
class Fib4 @Inject constructor(
    val fibM1: Fib3,
    val fibM2: Fib2
)

@Factory
@Instance(type = Fib5::class, scoping = Scoping.UNSCOPED)
class Fib5 @Inject constructor(
    val fibM1: Fib4,
    val fibM2: Fib3
)

@Factory
@Instance(type = Fib6::class, scoping = Scoping.UNSCOPED)
class Fib6 @Inject constructor(
    val fibM1: Fib5,
    val fibM2: Fib4
)

@Factory
@Instance(type = Fib7::class, scoping = Scoping.UNSCOPED)
class Fib7 @Inject constructor(
    val fibM1: Fib6,
    val fibM2: Fib5
)

@Factory
@Instance(type = Fib8::class, scoping = Scoping.UNSCOPED)
class Fib8 @Inject constructor(
    val fibM1: Fib7,
    val fibM2: Fib6
)

@Factory
@Instance(type = Fib9::class, scoping = Scoping.UNSCOPED)
class Fib9 @Inject constructor(
    val fibM1: Fib8,
    val fibM2: Fib7
)

@Factory
@Instance(type = Fib10::class, scoping = Scoping.UNSCOPED)
class Fib10 @Inject constructor(
    val fibM1: Fib9,
    val fibM2: Fib8
)

@Factory
@Instance(type = Fib11::class, scoping = Scoping.UNSCOPED)
class Fib11 @Inject constructor(
    val fibM1: Fib10,
    val fibM2: Fib9
)

@Factory
@Instance(type = Fib12::class, scoping = Scoping.UNSCOPED)
class Fib12 @Inject constructor(
    val fibM1: Fib11,
    val fibM2: Fib10
)

@Factory
@Instance(type = Fib13::class, scoping = Scoping.UNSCOPED)
class Fib13 @Inject constructor(
    val fibM1: Fib12,
    val fibM2: Fib11
)

@Factory
@Instance(type = Fib14::class, scoping = Scoping.UNSCOPED)
class Fib14 @Inject constructor(
    val fibM1: Fib13,
    val fibM2: Fib12
)

@Factory
@Instance(type = Fib15::class, scoping = Scoping.UNSCOPED)
class Fib15 @Inject constructor(
    val fibM1: Fib14,
    val fibM2: Fib13
)

@Factory
@Instance(type = Fib16::class, scoping = Scoping.UNSCOPED)
class Fib16 @Inject constructor(
    val fibM1: Fib15,
    val fibM2: Fib14
)

@Factory
@Instance(type = Fib17::class, scoping = Scoping.UNSCOPED)
class Fib17 @Inject constructor(
    val fibM1: Fib16,
    val fibM2: Fib15
)

@Factory
@Instance(type = Fib18::class, scoping = Scoping.UNSCOPED)
class Fib18 @Inject constructor(
    val fibM1: Fib17,
    val fibM2: Fib16
)

@Factory
@Instance(type = Fib19::class, scoping = Scoping.UNSCOPED)
class Fib19 @Inject constructor(
    val fibM1: Fib18,
    val fibM2: Fib17
)

@Factory
@Instance(type = Fib20::class, scoping = Scoping.UNSCOPED)
class Fib20 @Inject constructor(
    val fibM1: Fib19,
    val fibM2: Fib18
)

@Factory
@Instance(type = Fib21::class, scoping = Scoping.UNSCOPED)
class Fib21 @Inject constructor(
    val fibM1: Fib20,
    val fibM2: Fib19
)

@Factory
@Instance(type = Fib22::class, scoping = Scoping.UNSCOPED)
class Fib22 @Inject constructor(
    val fibM1: Fib21,
    val fibM2: Fib20
)

@Factory
@Instance(type = Fib23::class, scoping = Scoping.UNSCOPED)
class Fib23 @Inject constructor(
    val fibM1: Fib22,
    val fibM2: Fib21
)

@Factory
@Instance(type = Fib24::class, scoping = Scoping.UNSCOPED)
class Fib24 @Inject constructor(
    val fibM1: Fib23,
    val fibM2: Fib22
)

@Factory
@Instance(type = Fib25::class, scoping = Scoping.UNSCOPED)
class Fib25 @Inject constructor(
    val fibM1: Fib24,
    val fibM2: Fib23
)

@Factory
@Instance(type = Fib26::class, scoping = Scoping.UNSCOPED)
class Fib26 @Inject constructor(
    val fibM1: Fib25,
    val fibM2: Fib24
)

@Factory
@Instance(type = Fib27::class, scoping = Scoping.UNSCOPED)
class Fib27 @Inject constructor(
    val fibM1: Fib26,
    val fibM2: Fib25
)

@Factory
@Instance(type = Fib28::class, scoping = Scoping.UNSCOPED)
class Fib28 @Inject constructor(
    val fibM1: Fib27,
    val fibM2: Fib26
)

@Factory
@Instance(type = Fib29::class, scoping = Scoping.UNSCOPED)
class Fib29 @Inject constructor(
    val fibM1: Fib28,
    val fibM2: Fib27
)

@Factory
@Instance(type = Fib30::class, scoping = Scoping.UNSCOPED)
class Fib30 @Inject constructor(
    val fibM1: Fib29,
    val fibM2: Fib28
)

@Factory
@Instance(type = Fib31::class, scoping = Scoping.UNSCOPED)
class Fib31 @Inject constructor(
    val fibM1: Fib30,
    val fibM2: Fib29
)

@Factory
@Instance(type = Fib32::class, scoping = Scoping.UNSCOPED)
class Fib32 @Inject constructor(
    val fibM1: Fib31,
    val fibM2: Fib30
)

@Factory
@Instance(type = Fib33::class, scoping = Scoping.UNSCOPED)
class Fib33 @Inject constructor(
    val fibM1: Fib32,
    val fibM2: Fib31
)

@Factory
@Instance(type = Fib34::class, scoping = Scoping.UNSCOPED)
class Fib34 @Inject constructor(
    val fibM1: Fib33,
    val fibM2: Fib32
)

@Factory
@Instance(type = Fib35::class, scoping = Scoping.UNSCOPED)
class Fib35 @Inject constructor(
    val fibM1: Fib34,
    val fibM2: Fib33
)

@Factory
@Instance(type = Fib36::class, scoping = Scoping.UNSCOPED)
class Fib36 @Inject constructor(
    val fibM1: Fib35,
    val fibM2: Fib34
)

@Factory
@Instance(type = Fib37::class, scoping = Scoping.UNSCOPED)
class Fib37 @Inject constructor(
    val fibM1: Fib36,
    val fibM2: Fib35
)

@Factory
@Instance(type = Fib38::class, scoping = Scoping.UNSCOPED)
class Fib38 @Inject constructor(
    val fibM1: Fib37,
    val fibM2: Fib36
)

@Factory
@Instance(type = Fib39::class, scoping = Scoping.UNSCOPED)
class Fib39 @Inject constructor(
    val fibM1: Fib38,
    val fibM2: Fib37
)

@Factory
@Instance(type = Fib40::class, scoping = Scoping.UNSCOPED)
class Fib40 @Inject constructor(
    val fibM1: Fib39,
    val fibM2: Fib38
)

@Factory
@Instance(type = Fib41::class, scoping = Scoping.UNSCOPED)
class Fib41 @Inject constructor(
    val fibM1: Fib40,
    val fibM2: Fib39
)

@Factory
@Instance(type = Fib42::class, scoping = Scoping.UNSCOPED)
class Fib42 @Inject constructor(
    val fibM1: Fib41,
    val fibM2: Fib40
)

@Factory
@Instance(type = Fib43::class, scoping = Scoping.UNSCOPED)
class Fib43 @Inject constructor(
    val fibM1: Fib42,
    val fibM2: Fib41
)

@Factory
@Instance(type = Fib44::class, scoping = Scoping.UNSCOPED)
class Fib44 @Inject constructor(
    val fibM1: Fib43,
    val fibM2: Fib42
)

@Factory
@Instance(type = Fib45::class, scoping = Scoping.UNSCOPED)
class Fib45 @Inject constructor(
    val fibM1: Fib44,
    val fibM2: Fib43
)

@Factory
@Instance(type = Fib46::class, scoping = Scoping.UNSCOPED)
class Fib46 @Inject constructor(
    val fibM1: Fib45,
    val fibM2: Fib44
)

@Factory
@Instance(type = Fib47::class, scoping = Scoping.UNSCOPED)
class Fib47 @Inject constructor(
    val fibM1: Fib46,
    val fibM2: Fib45
)

@Factory
@Instance(type = Fib48::class, scoping = Scoping.UNSCOPED)
class Fib48 @Inject constructor(
    val fibM1: Fib47,
    val fibM2: Fib46
)

@Factory
@Instance(type = Fib49::class, scoping = Scoping.UNSCOPED)
class Fib49 @Inject constructor(
    val fibM1: Fib48,
    val fibM2: Fib47
)

@Factory
@Instance(type = Fib50::class, scoping = Scoping.UNSCOPED)
class Fib50 @Inject constructor(
    val fibM1: Fib49,
    val fibM2: Fib48
)

@Factory
@Instance(type = Fib51::class, scoping = Scoping.UNSCOPED)
class Fib51 @Inject constructor(
    val fibM1: Fib50,
    val fibM2: Fib49
)

@Factory
@Instance(type = Fib52::class, scoping = Scoping.UNSCOPED)
class Fib52 @Inject constructor(
    val fibM1: Fib51,
    val fibM2: Fib50
)

@Factory
@Instance(type = Fib53::class, scoping = Scoping.UNSCOPED)
class Fib53 @Inject constructor(
    val fibM1: Fib52,
    val fibM2: Fib51
)

@Factory
@Instance(type = Fib54::class, scoping = Scoping.UNSCOPED)
class Fib54 @Inject constructor(
    val fibM1: Fib53,
    val fibM2: Fib52
)

@Factory
@Instance(type = Fib55::class, scoping = Scoping.UNSCOPED)
class Fib55 @Inject constructor(
    val fibM1: Fib54,
    val fibM2: Fib53
)

@Factory
@Instance(type = Fib56::class, scoping = Scoping.UNSCOPED)
class Fib56 @Inject constructor(
    val fibM1: Fib55,
    val fibM2: Fib54
)

@Factory
@Instance(type = Fib57::class, scoping = Scoping.UNSCOPED)
class Fib57 @Inject constructor(
    val fibM1: Fib56,
    val fibM2: Fib55
)

@Factory
@Instance(type = Fib58::class, scoping = Scoping.UNSCOPED)
class Fib58 @Inject constructor(
    val fibM1: Fib57,
    val fibM2: Fib56
)

@Factory
@Instance(type = Fib59::class, scoping = Scoping.UNSCOPED)
class Fib59 @Inject constructor(
    val fibM1: Fib58,
    val fibM2: Fib57
)

@Factory
@Instance(type = Fib60::class, scoping = Scoping.UNSCOPED)
class Fib60 @Inject constructor(
    val fibM1: Fib59,
    val fibM2: Fib58
)

@Factory
@Instance(type = Fib61::class, scoping = Scoping.UNSCOPED)
class Fib61 @Inject constructor(
    val fibM1: Fib60,
    val fibM2: Fib59
)

@Factory
@Instance(type = Fib62::class, scoping = Scoping.UNSCOPED)
class Fib62 @Inject constructor(
    val fibM1: Fib61,
    val fibM2: Fib60
)

@Factory
@Instance(type = Fib63::class, scoping = Scoping.UNSCOPED)
class Fib63 @Inject constructor(
    val fibM1: Fib62,
    val fibM2: Fib61
)

@Factory
@Instance(type = Fib64::class, scoping = Scoping.UNSCOPED)
class Fib64 @Inject constructor(
    val fibM1: Fib63,
    val fibM2: Fib62
)

@Factory
@Instance(type = Fib65::class, scoping = Scoping.UNSCOPED)
class Fib65 @Inject constructor(
    val fibM1: Fib64,
    val fibM2: Fib63
)

@Factory
@Instance(type = Fib66::class, scoping = Scoping.UNSCOPED)
class Fib66 @Inject constructor(
    val fibM1: Fib65,
    val fibM2: Fib64
)

@Factory
@Instance(type = Fib67::class, scoping = Scoping.UNSCOPED)
class Fib67 @Inject constructor(
    val fibM1: Fib66,
    val fibM2: Fib65
)

@Factory
@Instance(type = Fib68::class, scoping = Scoping.UNSCOPED)
class Fib68 @Inject constructor(
    val fibM1: Fib67,
    val fibM2: Fib66
)

@Factory
@Instance(type = Fib69::class, scoping = Scoping.UNSCOPED)
class Fib69 @Inject constructor(
    val fibM1: Fib68,
    val fibM2: Fib67
)

@Factory
@Instance(type = Fib70::class, scoping = Scoping.UNSCOPED)
class Fib70 @Inject constructor(
    val fibM1: Fib69,
    val fibM2: Fib68
)

@Factory
@Instance(type = Fib71::class, scoping = Scoping.UNSCOPED)
class Fib71 @Inject constructor(
    val fibM1: Fib70,
    val fibM2: Fib69
)

@Factory
@Instance(type = Fib72::class, scoping = Scoping.UNSCOPED)
class Fib72 @Inject constructor(
    val fibM1: Fib71,
    val fibM2: Fib70
)

@Factory
@Instance(type = Fib73::class, scoping = Scoping.UNSCOPED)
class Fib73 @Inject constructor(
    val fibM1: Fib72,
    val fibM2: Fib71
)

@Factory
@Instance(type = Fib74::class, scoping = Scoping.UNSCOPED)
class Fib74 @Inject constructor(
    val fibM1: Fib73,
    val fibM2: Fib72
)

@Factory
@Instance(type = Fib75::class, scoping = Scoping.UNSCOPED)
class Fib75 @Inject constructor(
    val fibM1: Fib74,
    val fibM2: Fib73
)

@Factory
@Instance(type = Fib76::class, scoping = Scoping.UNSCOPED)
class Fib76 @Inject constructor(
    val fibM1: Fib75,
    val fibM2: Fib74
)

@Factory
@Instance(type = Fib77::class, scoping = Scoping.UNSCOPED)
class Fib77 @Inject constructor(
    val fibM1: Fib76,
    val fibM2: Fib75
)

@Factory
@Instance(type = Fib78::class, scoping = Scoping.UNSCOPED)
class Fib78 @Inject constructor(
    val fibM1: Fib77,
    val fibM2: Fib76
)

@Factory
@Instance(type = Fib79::class, scoping = Scoping.UNSCOPED)
class Fib79 @Inject constructor(
    val fibM1: Fib78,
    val fibM2: Fib77
)

@Factory
@Instance(type = Fib80::class, scoping = Scoping.UNSCOPED)
class Fib80 @Inject constructor(
    val fibM1: Fib79,
    val fibM2: Fib78
)

@Factory
@Instance(type = Fib81::class, scoping = Scoping.UNSCOPED)
class Fib81 @Inject constructor(
    val fibM1: Fib80,
    val fibM2: Fib79
)

@Factory
@Instance(type = Fib82::class, scoping = Scoping.UNSCOPED)
class Fib82 @Inject constructor(
    val fibM1: Fib81,
    val fibM2: Fib80
)

@Factory
@Instance(type = Fib83::class, scoping = Scoping.UNSCOPED)
class Fib83 @Inject constructor(
    val fibM1: Fib82,
    val fibM2: Fib81
)

@Factory
@Instance(type = Fib84::class, scoping = Scoping.UNSCOPED)
class Fib84 @Inject constructor(
    val fibM1: Fib83,
    val fibM2: Fib82
)

@Factory
@Instance(type = Fib85::class, scoping = Scoping.UNSCOPED)
class Fib85 @Inject constructor(
    val fibM1: Fib84,
    val fibM2: Fib83
)

@Factory
@Instance(type = Fib86::class, scoping = Scoping.UNSCOPED)
class Fib86 @Inject constructor(
    val fibM1: Fib85,
    val fibM2: Fib84
)

@Factory
@Instance(type = Fib87::class, scoping = Scoping.UNSCOPED)
class Fib87 @Inject constructor(
    val fibM1: Fib86,
    val fibM2: Fib85
)

@Factory
@Instance(type = Fib88::class, scoping = Scoping.UNSCOPED)
class Fib88 @Inject constructor(
    val fibM1: Fib87,
    val fibM2: Fib86
)

@Factory
@Instance(type = Fib89::class, scoping = Scoping.UNSCOPED)
class Fib89 @Inject constructor(
    val fibM1: Fib88,
    val fibM2: Fib87
)

@Factory
@Instance(type = Fib90::class, scoping = Scoping.UNSCOPED)
class Fib90 @Inject constructor(
    val fibM1: Fib89,
    val fibM2: Fib88
)

@Factory
@Instance(type = Fib91::class, scoping = Scoping.UNSCOPED)
class Fib91 @Inject constructor(
    val fibM1: Fib90,
    val fibM2: Fib89
)

@Factory
@Instance(type = Fib92::class, scoping = Scoping.UNSCOPED)
class Fib92 @Inject constructor(
    val fibM1: Fib91,
    val fibM2: Fib90
)

@Factory
@Instance(type = Fib93::class, scoping = Scoping.UNSCOPED)
class Fib93 @Inject constructor(
    val fibM1: Fib92,
    val fibM2: Fib91
)

@Factory
@Instance(type = Fib94::class, scoping = Scoping.UNSCOPED)
class Fib94 @Inject constructor(
    val fibM1: Fib93,
    val fibM2: Fib92
)

@Factory
@Instance(type = Fib95::class, scoping = Scoping.UNSCOPED)
class Fib95 @Inject constructor(
    val fibM1: Fib94,
    val fibM2: Fib93
)

@Factory
@Instance(type = Fib96::class, scoping = Scoping.UNSCOPED)
class Fib96 @Inject constructor(
    val fibM1: Fib95,
    val fibM2: Fib94
)

@Factory
@Instance(type = Fib97::class, scoping = Scoping.UNSCOPED)
class Fib97 @Inject constructor(
    val fibM1: Fib96,
    val fibM2: Fib95
)

@Factory
@Instance(type = Fib98::class, scoping = Scoping.UNSCOPED)
class Fib98 @Inject constructor(
    val fibM1: Fib97,
    val fibM2: Fib96
)

@Factory
@Instance(type = Fib99::class, scoping = Scoping.UNSCOPED)
class Fib99 @Inject constructor(
    val fibM1: Fib98,
    val fibM2: Fib97
)

@Factory
@Instance(type = Fib100::class, scoping = Scoping.UNSCOPED)
class Fib100 @Inject constructor(
    val fibM1: Fib99,
    val fibM2: Fib98
)

@Factory
@Instance(type = Fib101::class, scoping = Scoping.UNSCOPED)
class Fib101 @Inject constructor(
    val fibM1: Fib100,
    val fibM2: Fib99
)

@Factory
@Instance(type = Fib102::class, scoping = Scoping.UNSCOPED)
class Fib102 @Inject constructor(
    val fibM1: Fib101,
    val fibM2: Fib100
)

@Factory
@Instance(type = Fib103::class, scoping = Scoping.UNSCOPED)
class Fib103 @Inject constructor(
    val fibM1: Fib102,
    val fibM2: Fib101
)

@Factory
@Instance(type = Fib104::class, scoping = Scoping.UNSCOPED)
class Fib104 @Inject constructor(
    val fibM1: Fib103,
    val fibM2: Fib102
)

@Factory
@Instance(type = Fib105::class, scoping = Scoping.UNSCOPED)
class Fib105 @Inject constructor(
    val fibM1: Fib104,
    val fibM2: Fib103
)

@Factory
@Instance(type = Fib106::class, scoping = Scoping.UNSCOPED)
class Fib106 @Inject constructor(
    val fibM1: Fib105,
    val fibM2: Fib104
)

@Factory
@Instance(type = Fib107::class, scoping = Scoping.UNSCOPED)
class Fib107 @Inject constructor(
    val fibM1: Fib106,
    val fibM2: Fib105
)

@Factory
@Instance(type = Fib108::class, scoping = Scoping.UNSCOPED)
class Fib108 @Inject constructor(
    val fibM1: Fib107,
    val fibM2: Fib106
)

@Factory
@Instance(type = Fib109::class, scoping = Scoping.UNSCOPED)
class Fib109 @Inject constructor(
    val fibM1: Fib108,
    val fibM2: Fib107
)

@Factory
@Instance(type = Fib110::class, scoping = Scoping.UNSCOPED)
class Fib110 @Inject constructor(
    val fibM1: Fib109,
    val fibM2: Fib108
)

@Factory
@Instance(type = Fib111::class, scoping = Scoping.UNSCOPED)
class Fib111 @Inject constructor(
    val fibM1: Fib110,
    val fibM2: Fib109
)

@Factory
@Instance(type = Fib112::class, scoping = Scoping.UNSCOPED)
class Fib112 @Inject constructor(
    val fibM1: Fib111,
    val fibM2: Fib110
)

@Factory
@Instance(type = Fib113::class, scoping = Scoping.UNSCOPED)
class Fib113 @Inject constructor(
    val fibM1: Fib112,
    val fibM2: Fib111
)

@Factory
@Instance(type = Fib114::class, scoping = Scoping.UNSCOPED)
class Fib114 @Inject constructor(
    val fibM1: Fib113,
    val fibM2: Fib112
)

@Factory
@Instance(type = Fib115::class, scoping = Scoping.UNSCOPED)
class Fib115 @Inject constructor(
    val fibM1: Fib114,
    val fibM2: Fib113
)

@Factory
@Instance(type = Fib116::class, scoping = Scoping.UNSCOPED)
class Fib116 @Inject constructor(
    val fibM1: Fib115,
    val fibM2: Fib114
)

@Factory
@Instance(type = Fib117::class, scoping = Scoping.UNSCOPED)
class Fib117 @Inject constructor(
    val fibM1: Fib116,
    val fibM2: Fib115
)

@Factory
@Instance(type = Fib118::class, scoping = Scoping.UNSCOPED)
class Fib118 @Inject constructor(
    val fibM1: Fib117,
    val fibM2: Fib116
)

@Factory
@Instance(type = Fib119::class, scoping = Scoping.UNSCOPED)
class Fib119 @Inject constructor(
    val fibM1: Fib118,
    val fibM2: Fib117
)

@Factory
@Instance(type = Fib120::class, scoping = Scoping.UNSCOPED)
class Fib120 @Inject constructor(
    val fibM1: Fib119,
    val fibM2: Fib118
)

@Factory
@Instance(type = Fib121::class, scoping = Scoping.UNSCOPED)
class Fib121 @Inject constructor(
    val fibM1: Fib120,
    val fibM2: Fib119
)

@Factory
@Instance(type = Fib122::class, scoping = Scoping.UNSCOPED)
class Fib122 @Inject constructor(
    val fibM1: Fib121,
    val fibM2: Fib120
)

@Factory
@Instance(type = Fib123::class, scoping = Scoping.UNSCOPED)
class Fib123 @Inject constructor(
    val fibM1: Fib122,
    val fibM2: Fib121
)

@Factory
@Instance(type = Fib124::class, scoping = Scoping.UNSCOPED)
class Fib124 @Inject constructor(
    val fibM1: Fib123,
    val fibM2: Fib122
)

@Factory
@Instance(type = Fib125::class, scoping = Scoping.UNSCOPED)
class Fib125 @Inject constructor(
    val fibM1: Fib124,
    val fibM2: Fib123
)

@Factory
@Instance(type = Fib126::class, scoping = Scoping.UNSCOPED)
class Fib126 @Inject constructor(
    val fibM1: Fib125,
    val fibM2: Fib124
)

@Factory
@Instance(type = Fib127::class, scoping = Scoping.UNSCOPED)
class Fib127 @Inject constructor(
    val fibM1: Fib126,
    val fibM2: Fib125
)

@Factory
@Instance(type = Fib128::class, scoping = Scoping.UNSCOPED)
class Fib128 @Inject constructor(
    val fibM1: Fib127,
    val fibM2: Fib126
)

@Factory
@Instance(type = Fib129::class, scoping = Scoping.UNSCOPED)
class Fib129 @Inject constructor(
    val fibM1: Fib128,
    val fibM2: Fib127
)

@Factory
@Instance(type = Fib130::class, scoping = Scoping.UNSCOPED)
class Fib130 @Inject constructor(
    val fibM1: Fib129,
    val fibM2: Fib128
)

@Factory
@Instance(type = Fib131::class, scoping = Scoping.UNSCOPED)
class Fib131 @Inject constructor(
    val fibM1: Fib130,
    val fibM2: Fib129
)

@Factory
@Instance(type = Fib132::class, scoping = Scoping.UNSCOPED)
class Fib132 @Inject constructor(
    val fibM1: Fib131,
    val fibM2: Fib130
)

@Factory
@Instance(type = Fib133::class, scoping = Scoping.UNSCOPED)
class Fib133 @Inject constructor(
    val fibM1: Fib132,
    val fibM2: Fib131
)

@Factory
@Instance(type = Fib134::class, scoping = Scoping.UNSCOPED)
class Fib134 @Inject constructor(
    val fibM1: Fib133,
    val fibM2: Fib132
)

@Factory
@Instance(type = Fib135::class, scoping = Scoping.UNSCOPED)
class Fib135 @Inject constructor(
    val fibM1: Fib134,
    val fibM2: Fib133
)

@Factory
@Instance(type = Fib136::class, scoping = Scoping.UNSCOPED)
class Fib136 @Inject constructor(
    val fibM1: Fib135,
    val fibM2: Fib134
)

@Factory
@Instance(type = Fib137::class, scoping = Scoping.UNSCOPED)
class Fib137 @Inject constructor(
    val fibM1: Fib136,
    val fibM2: Fib135
)

@Factory
@Instance(type = Fib138::class, scoping = Scoping.UNSCOPED)
class Fib138 @Inject constructor(
    val fibM1: Fib137,
    val fibM2: Fib136
)

@Factory
@Instance(type = Fib139::class, scoping = Scoping.UNSCOPED)
class Fib139 @Inject constructor(
    val fibM1: Fib138,
    val fibM2: Fib137
)

@Factory
@Instance(type = Fib140::class, scoping = Scoping.UNSCOPED)
class Fib140 @Inject constructor(
    val fibM1: Fib139,
    val fibM2: Fib138
)

@Factory
@Instance(type = Fib141::class, scoping = Scoping.UNSCOPED)
class Fib141 @Inject constructor(
    val fibM1: Fib140,
    val fibM2: Fib139
)

@Factory
@Instance(type = Fib142::class, scoping = Scoping.UNSCOPED)
class Fib142 @Inject constructor(
    val fibM1: Fib141,
    val fibM2: Fib140
)

@Factory
@Instance(type = Fib143::class, scoping = Scoping.UNSCOPED)
class Fib143 @Inject constructor(
    val fibM1: Fib142,
    val fibM2: Fib141
)

@Factory
@Instance(type = Fib144::class, scoping = Scoping.UNSCOPED)
class Fib144 @Inject constructor(
    val fibM1: Fib143,
    val fibM2: Fib142
)

@Factory
@Instance(type = Fib145::class, scoping = Scoping.UNSCOPED)
class Fib145 @Inject constructor(
    val fibM1: Fib144,
    val fibM2: Fib143
)

@Factory
@Instance(type = Fib146::class, scoping = Scoping.UNSCOPED)
class Fib146 @Inject constructor(
    val fibM1: Fib145,
    val fibM2: Fib144
)

@Factory
@Instance(type = Fib147::class, scoping = Scoping.UNSCOPED)
class Fib147 @Inject constructor(
    val fibM1: Fib146,
    val fibM2: Fib145
)

@Factory
@Instance(type = Fib148::class, scoping = Scoping.UNSCOPED)
class Fib148 @Inject constructor(
    val fibM1: Fib147,
    val fibM2: Fib146
)

@Factory
@Instance(type = Fib149::class, scoping = Scoping.UNSCOPED)
class Fib149 @Inject constructor(
    val fibM1: Fib148,
    val fibM2: Fib147
)

@Factory
@Instance(type = Fib150::class, scoping = Scoping.UNSCOPED)
class Fib150 @Inject constructor(
    val fibM1: Fib149,
    val fibM2: Fib148
)

@Factory
@Instance(type = Fib151::class, scoping = Scoping.UNSCOPED)
class Fib151 @Inject constructor(
    val fibM1: Fib150,
    val fibM2: Fib149
)

@Factory
@Instance(type = Fib152::class, scoping = Scoping.UNSCOPED)
class Fib152 @Inject constructor(
    val fibM1: Fib151,
    val fibM2: Fib150
)

@Factory
@Instance(type = Fib153::class, scoping = Scoping.UNSCOPED)
class Fib153 @Inject constructor(
    val fibM1: Fib152,
    val fibM2: Fib151
)

@Factory
@Instance(type = Fib154::class, scoping = Scoping.UNSCOPED)
class Fib154 @Inject constructor(
    val fibM1: Fib153,
    val fibM2: Fib152
)

@Factory
@Instance(type = Fib155::class, scoping = Scoping.UNSCOPED)
class Fib155 @Inject constructor(
    val fibM1: Fib154,
    val fibM2: Fib153
)

@Factory
@Instance(type = Fib156::class, scoping = Scoping.UNSCOPED)
class Fib156 @Inject constructor(
    val fibM1: Fib155,
    val fibM2: Fib154
)

@Factory
@Instance(type = Fib157::class, scoping = Scoping.UNSCOPED)
class Fib157 @Inject constructor(
    val fibM1: Fib156,
    val fibM2: Fib155
)

@Factory
@Instance(type = Fib158::class, scoping = Scoping.UNSCOPED)
class Fib158 @Inject constructor(
    val fibM1: Fib157,
    val fibM2: Fib156
)

@Factory
@Instance(type = Fib159::class, scoping = Scoping.UNSCOPED)
class Fib159 @Inject constructor(
    val fibM1: Fib158,
    val fibM2: Fib157
)

@Factory
@Instance(type = Fib160::class, scoping = Scoping.UNSCOPED)
class Fib160 @Inject constructor(
    val fibM1: Fib159,
    val fibM2: Fib158
)

@Factory
@Instance(type = Fib161::class, scoping = Scoping.UNSCOPED)
class Fib161 @Inject constructor(
    val fibM1: Fib160,
    val fibM2: Fib159
)

@Factory
@Instance(type = Fib162::class, scoping = Scoping.UNSCOPED)
class Fib162 @Inject constructor(
    val fibM1: Fib161,
    val fibM2: Fib160
)

@Factory
@Instance(type = Fib163::class, scoping = Scoping.UNSCOPED)
class Fib163 @Inject constructor(
    val fibM1: Fib162,
    val fibM2: Fib161
)

@Factory
@Instance(type = Fib164::class, scoping = Scoping.UNSCOPED)
class Fib164 @Inject constructor(
    val fibM1: Fib163,
    val fibM2: Fib162
)

@Factory
@Instance(type = Fib165::class, scoping = Scoping.UNSCOPED)
class Fib165 @Inject constructor(
    val fibM1: Fib164,
    val fibM2: Fib163
)

@Factory
@Instance(type = Fib166::class, scoping = Scoping.UNSCOPED)
class Fib166 @Inject constructor(
    val fibM1: Fib165,
    val fibM2: Fib164
)

@Factory
@Instance(type = Fib167::class, scoping = Scoping.UNSCOPED)
class Fib167 @Inject constructor(
    val fibM1: Fib166,
    val fibM2: Fib165
)

@Factory
@Instance(type = Fib168::class, scoping = Scoping.UNSCOPED)
class Fib168 @Inject constructor(
    val fibM1: Fib167,
    val fibM2: Fib166
)

@Factory
@Instance(type = Fib169::class, scoping = Scoping.UNSCOPED)
class Fib169 @Inject constructor(
    val fibM1: Fib168,
    val fibM2: Fib167
)

@Factory
@Instance(type = Fib170::class, scoping = Scoping.UNSCOPED)
class Fib170 @Inject constructor(
    val fibM1: Fib169,
    val fibM2: Fib168
)

@Factory
@Instance(type = Fib171::class, scoping = Scoping.UNSCOPED)
class Fib171 @Inject constructor(
    val fibM1: Fib170,
    val fibM2: Fib169
)

@Factory
@Instance(type = Fib172::class, scoping = Scoping.UNSCOPED)
class Fib172 @Inject constructor(
    val fibM1: Fib171,
    val fibM2: Fib170
)

@Factory
@Instance(type = Fib173::class, scoping = Scoping.UNSCOPED)
class Fib173 @Inject constructor(
    val fibM1: Fib172,
    val fibM2: Fib171
)

@Factory
@Instance(type = Fib174::class, scoping = Scoping.UNSCOPED)
class Fib174 @Inject constructor(
    val fibM1: Fib173,
    val fibM2: Fib172
)

@Factory
@Instance(type = Fib175::class, scoping = Scoping.UNSCOPED)
class Fib175 @Inject constructor(
    val fibM1: Fib174,
    val fibM2: Fib173
)

@Factory
@Instance(type = Fib176::class, scoping = Scoping.UNSCOPED)
class Fib176 @Inject constructor(
    val fibM1: Fib175,
    val fibM2: Fib174
)

@Factory
@Instance(type = Fib177::class, scoping = Scoping.UNSCOPED)
class Fib177 @Inject constructor(
    val fibM1: Fib176,
    val fibM2: Fib175
)

@Factory
@Instance(type = Fib178::class, scoping = Scoping.UNSCOPED)
class Fib178 @Inject constructor(
    val fibM1: Fib177,
    val fibM2: Fib176
)

@Factory
@Instance(type = Fib179::class, scoping = Scoping.UNSCOPED)
class Fib179 @Inject constructor(
    val fibM1: Fib178,
    val fibM2: Fib177
)

@Factory
@Instance(type = Fib180::class, scoping = Scoping.UNSCOPED)
class Fib180 @Inject constructor(
    val fibM1: Fib179,
    val fibM2: Fib178
)

@Factory
@Instance(type = Fib181::class, scoping = Scoping.UNSCOPED)
class Fib181 @Inject constructor(
    val fibM1: Fib180,
    val fibM2: Fib179
)

@Factory
@Instance(type = Fib182::class, scoping = Scoping.UNSCOPED)
class Fib182 @Inject constructor(
    val fibM1: Fib181,
    val fibM2: Fib180
)

@Factory
@Instance(type = Fib183::class, scoping = Scoping.UNSCOPED)
class Fib183 @Inject constructor(
    val fibM1: Fib182,
    val fibM2: Fib181
)

@Factory
@Instance(type = Fib184::class, scoping = Scoping.UNSCOPED)
class Fib184 @Inject constructor(
    val fibM1: Fib183,
    val fibM2: Fib182
)

@Factory
@Instance(type = Fib185::class, scoping = Scoping.UNSCOPED)
class Fib185 @Inject constructor(
    val fibM1: Fib184,
    val fibM2: Fib183
)

@Factory
@Instance(type = Fib186::class, scoping = Scoping.UNSCOPED)
class Fib186 @Inject constructor(
    val fibM1: Fib185,
    val fibM2: Fib184
)

@Factory
@Instance(type = Fib187::class, scoping = Scoping.UNSCOPED)
class Fib187 @Inject constructor(
    val fibM1: Fib186,
    val fibM2: Fib185
)

@Factory
@Instance(type = Fib188::class, scoping = Scoping.UNSCOPED)
class Fib188 @Inject constructor(
    val fibM1: Fib187,
    val fibM2: Fib186
)

@Factory
@Instance(type = Fib189::class, scoping = Scoping.UNSCOPED)
class Fib189 @Inject constructor(
    val fibM1: Fib188,
    val fibM2: Fib187
)

@Factory
@Instance(type = Fib190::class, scoping = Scoping.UNSCOPED)
class Fib190 @Inject constructor(
    val fibM1: Fib189,
    val fibM2: Fib188
)

@Factory
@Instance(type = Fib191::class, scoping = Scoping.UNSCOPED)
class Fib191 @Inject constructor(
    val fibM1: Fib190,
    val fibM2: Fib189
)

@Factory
@Instance(type = Fib192::class, scoping = Scoping.UNSCOPED)
class Fib192 @Inject constructor(
    val fibM1: Fib191,
    val fibM2: Fib190
)

@Factory
@Instance(type = Fib193::class, scoping = Scoping.UNSCOPED)
class Fib193 @Inject constructor(
    val fibM1: Fib192,
    val fibM2: Fib191
)

@Factory
@Instance(type = Fib194::class, scoping = Scoping.UNSCOPED)
class Fib194 @Inject constructor(
    val fibM1: Fib193,
    val fibM2: Fib192
)

@Factory
@Instance(type = Fib195::class, scoping = Scoping.UNSCOPED)
class Fib195 @Inject constructor(
    val fibM1: Fib194,
    val fibM2: Fib193
)

@Factory
@Instance(type = Fib196::class, scoping = Scoping.UNSCOPED)
class Fib196 @Inject constructor(
    val fibM1: Fib195,
    val fibM2: Fib194
)

@Factory
@Instance(type = Fib197::class, scoping = Scoping.UNSCOPED)
class Fib197 @Inject constructor(
    val fibM1: Fib196,
    val fibM2: Fib195
)

@Factory
@Instance(type = Fib198::class, scoping = Scoping.UNSCOPED)
class Fib198 @Inject constructor(
    val fibM1: Fib197,
    val fibM2: Fib196
)

@Factory
@Instance(type = Fib199::class, scoping = Scoping.UNSCOPED)
class Fib199 @Inject constructor(
    val fibM1: Fib198,
    val fibM2: Fib197
)

@Factory
@Instance(type = Fib200::class, scoping = Scoping.UNSCOPED)
class Fib200 @Inject constructor(
    val fibM1: Fib199,
    val fibM2: Fib198
)

@Factory
@Instance(type = Fib201::class, scoping = Scoping.UNSCOPED)
class Fib201 @Inject constructor(
    val fibM1: Fib200,
    val fibM2: Fib199
)

@Factory
@Instance(type = Fib202::class, scoping = Scoping.UNSCOPED)
class Fib202 @Inject constructor(
    val fibM1: Fib201,
    val fibM2: Fib200
)

@Factory
@Instance(type = Fib203::class, scoping = Scoping.UNSCOPED)
class Fib203 @Inject constructor(
    val fibM1: Fib202,
    val fibM2: Fib201
)

@Factory
@Instance(type = Fib204::class, scoping = Scoping.UNSCOPED)
class Fib204 @Inject constructor(
    val fibM1: Fib203,
    val fibM2: Fib202
)

@Factory
@Instance(type = Fib205::class, scoping = Scoping.UNSCOPED)
class Fib205 @Inject constructor(
    val fibM1: Fib204,
    val fibM2: Fib203
)

@Factory
@Instance(type = Fib206::class, scoping = Scoping.UNSCOPED)
class Fib206 @Inject constructor(
    val fibM1: Fib205,
    val fibM2: Fib204
)

@Factory
@Instance(type = Fib207::class, scoping = Scoping.UNSCOPED)
class Fib207 @Inject constructor(
    val fibM1: Fib206,
    val fibM2: Fib205
)

@Factory
@Instance(type = Fib208::class, scoping = Scoping.UNSCOPED)
class Fib208 @Inject constructor(
    val fibM1: Fib207,
    val fibM2: Fib206
)

@Factory
@Instance(type = Fib209::class, scoping = Scoping.UNSCOPED)
class Fib209 @Inject constructor(
    val fibM1: Fib208,
    val fibM2: Fib207
)

@Factory
@Instance(type = Fib210::class, scoping = Scoping.UNSCOPED)
class Fib210 @Inject constructor(
    val fibM1: Fib209,
    val fibM2: Fib208
)

@Factory
@Instance(type = Fib211::class, scoping = Scoping.UNSCOPED)
class Fib211 @Inject constructor(
    val fibM1: Fib210,
    val fibM2: Fib209
)

@Factory
@Instance(type = Fib212::class, scoping = Scoping.UNSCOPED)
class Fib212 @Inject constructor(
    val fibM1: Fib211,
    val fibM2: Fib210
)

@Factory
@Instance(type = Fib213::class, scoping = Scoping.UNSCOPED)
class Fib213 @Inject constructor(
    val fibM1: Fib212,
    val fibM2: Fib211
)

@Factory
@Instance(type = Fib214::class, scoping = Scoping.UNSCOPED)
class Fib214 @Inject constructor(
    val fibM1: Fib213,
    val fibM2: Fib212
)

@Factory
@Instance(type = Fib215::class, scoping = Scoping.UNSCOPED)
class Fib215 @Inject constructor(
    val fibM1: Fib214,
    val fibM2: Fib213
)

@Factory
@Instance(type = Fib216::class, scoping = Scoping.UNSCOPED)
class Fib216 @Inject constructor(
    val fibM1: Fib215,
    val fibM2: Fib214
)

@Factory
@Instance(type = Fib217::class, scoping = Scoping.UNSCOPED)
class Fib217 @Inject constructor(
    val fibM1: Fib216,
    val fibM2: Fib215
)

@Factory
@Instance(type = Fib218::class, scoping = Scoping.UNSCOPED)
class Fib218 @Inject constructor(
    val fibM1: Fib217,
    val fibM2: Fib216
)

@Factory
@Instance(type = Fib219::class, scoping = Scoping.UNSCOPED)
class Fib219 @Inject constructor(
    val fibM1: Fib218,
    val fibM2: Fib217
)

@Factory
@Instance(type = Fib220::class, scoping = Scoping.UNSCOPED)
class Fib220 @Inject constructor(
    val fibM1: Fib219,
    val fibM2: Fib218
)

@Factory
@Instance(type = Fib221::class, scoping = Scoping.UNSCOPED)
class Fib221 @Inject constructor(
    val fibM1: Fib220,
    val fibM2: Fib219
)

@Factory
@Instance(type = Fib222::class, scoping = Scoping.UNSCOPED)
class Fib222 @Inject constructor(
    val fibM1: Fib221,
    val fibM2: Fib220
)

@Factory
@Instance(type = Fib223::class, scoping = Scoping.UNSCOPED)
class Fib223 @Inject constructor(
    val fibM1: Fib222,
    val fibM2: Fib221
)

@Factory
@Instance(type = Fib224::class, scoping = Scoping.UNSCOPED)
class Fib224 @Inject constructor(
    val fibM1: Fib223,
    val fibM2: Fib222
)

@Factory
@Instance(type = Fib225::class, scoping = Scoping.UNSCOPED)
class Fib225 @Inject constructor(
    val fibM1: Fib224,
    val fibM2: Fib223
)

@Factory
@Instance(type = Fib226::class, scoping = Scoping.UNSCOPED)
class Fib226 @Inject constructor(
    val fibM1: Fib225,
    val fibM2: Fib224
)

@Factory
@Instance(type = Fib227::class, scoping = Scoping.UNSCOPED)
class Fib227 @Inject constructor(
    val fibM1: Fib226,
    val fibM2: Fib225
)

@Factory
@Instance(type = Fib228::class, scoping = Scoping.UNSCOPED)
class Fib228 @Inject constructor(
    val fibM1: Fib227,
    val fibM2: Fib226
)

@Factory
@Instance(type = Fib229::class, scoping = Scoping.UNSCOPED)
class Fib229 @Inject constructor(
    val fibM1: Fib228,
    val fibM2: Fib227
)

@Factory
@Instance(type = Fib230::class, scoping = Scoping.UNSCOPED)
class Fib230 @Inject constructor(
    val fibM1: Fib229,
    val fibM2: Fib228
)

@Factory
@Instance(type = Fib231::class, scoping = Scoping.UNSCOPED)
class Fib231 @Inject constructor(
    val fibM1: Fib230,
    val fibM2: Fib229
)

@Factory
@Instance(type = Fib232::class, scoping = Scoping.UNSCOPED)
class Fib232 @Inject constructor(
    val fibM1: Fib231,
    val fibM2: Fib230
)

@Factory
@Instance(type = Fib233::class, scoping = Scoping.UNSCOPED)
class Fib233 @Inject constructor(
    val fibM1: Fib232,
    val fibM2: Fib231
)

@Factory
@Instance(type = Fib234::class, scoping = Scoping.UNSCOPED)
class Fib234 @Inject constructor(
    val fibM1: Fib233,
    val fibM2: Fib232
)

@Factory
@Instance(type = Fib235::class, scoping = Scoping.UNSCOPED)
class Fib235 @Inject constructor(
    val fibM1: Fib234,
    val fibM2: Fib233
)

@Factory
@Instance(type = Fib236::class, scoping = Scoping.UNSCOPED)
class Fib236 @Inject constructor(
    val fibM1: Fib235,
    val fibM2: Fib234
)

@Factory
@Instance(type = Fib237::class, scoping = Scoping.UNSCOPED)
class Fib237 @Inject constructor(
    val fibM1: Fib236,
    val fibM2: Fib235
)

@Factory
@Instance(type = Fib238::class, scoping = Scoping.UNSCOPED)
class Fib238 @Inject constructor(
    val fibM1: Fib237,
    val fibM2: Fib236
)

@Factory
@Instance(type = Fib239::class, scoping = Scoping.UNSCOPED)
class Fib239 @Inject constructor(
    val fibM1: Fib238,
    val fibM2: Fib237
)

@Factory
@Instance(type = Fib240::class, scoping = Scoping.UNSCOPED)
class Fib240 @Inject constructor(
    val fibM1: Fib239,
    val fibM2: Fib238
)

@Factory
@Instance(type = Fib241::class, scoping = Scoping.UNSCOPED)
class Fib241 @Inject constructor(
    val fibM1: Fib240,
    val fibM2: Fib239
)

@Factory
@Instance(type = Fib242::class, scoping = Scoping.UNSCOPED)
class Fib242 @Inject constructor(
    val fibM1: Fib241,
    val fibM2: Fib240
)

@Factory
@Instance(type = Fib243::class, scoping = Scoping.UNSCOPED)
class Fib243 @Inject constructor(
    val fibM1: Fib242,
    val fibM2: Fib241
)

@Factory
@Instance(type = Fib244::class, scoping = Scoping.UNSCOPED)
class Fib244 @Inject constructor(
    val fibM1: Fib243,
    val fibM2: Fib242
)

@Factory
@Instance(type = Fib245::class, scoping = Scoping.UNSCOPED)
class Fib245 @Inject constructor(
    val fibM1: Fib244,
    val fibM2: Fib243
)

@Factory
@Instance(type = Fib246::class, scoping = Scoping.UNSCOPED)
class Fib246 @Inject constructor(
    val fibM1: Fib245,
    val fibM2: Fib244
)

@Factory
@Instance(type = Fib247::class, scoping = Scoping.UNSCOPED)
class Fib247 @Inject constructor(
    val fibM1: Fib246,
    val fibM2: Fib245
)

@Factory
@Instance(type = Fib248::class, scoping = Scoping.UNSCOPED)
class Fib248 @Inject constructor(
    val fibM1: Fib247,
    val fibM2: Fib246
)

@Factory
@Instance(type = Fib249::class, scoping = Scoping.UNSCOPED)
class Fib249 @Inject constructor(
    val fibM1: Fib248,
    val fibM2: Fib247
)

@Factory
@Instance(type = Fib250::class, scoping = Scoping.UNSCOPED)
class Fib250 @Inject constructor(
    val fibM1: Fib249,
    val fibM2: Fib248
)

@Factory
@Instance(type = Fib251::class, scoping = Scoping.UNSCOPED)
class Fib251 @Inject constructor(
    val fibM1: Fib250,
    val fibM2: Fib249
)

@Factory
@Instance(type = Fib252::class, scoping = Scoping.UNSCOPED)
class Fib252 @Inject constructor(
    val fibM1: Fib251,
    val fibM2: Fib250
)

@Factory
@Instance(type = Fib253::class, scoping = Scoping.UNSCOPED)
class Fib253 @Inject constructor(
    val fibM1: Fib252,
    val fibM2: Fib251
)

@Factory
@Instance(type = Fib254::class, scoping = Scoping.UNSCOPED)
class Fib254 @Inject constructor(
    val fibM1: Fib253,
    val fibM2: Fib252
)

@Factory
@Instance(type = Fib255::class, scoping = Scoping.UNSCOPED)
class Fib255 @Inject constructor(
    val fibM1: Fib254,
    val fibM2: Fib253
)

@Factory
@Instance(type = Fib256::class, scoping = Scoping.UNSCOPED)
class Fib256 @Inject constructor(
    val fibM1: Fib255,
    val fibM2: Fib254
)

@Factory
@Instance(type = Fib257::class, scoping = Scoping.UNSCOPED)
class Fib257 @Inject constructor(
    val fibM1: Fib256,
    val fibM2: Fib255
)

@Factory
@Instance(type = Fib258::class, scoping = Scoping.UNSCOPED)
class Fib258 @Inject constructor(
    val fibM1: Fib257,
    val fibM2: Fib256
)

@Factory
@Instance(type = Fib259::class, scoping = Scoping.UNSCOPED)
class Fib259 @Inject constructor(
    val fibM1: Fib258,
    val fibM2: Fib257
)

@Factory
@Instance(type = Fib260::class, scoping = Scoping.UNSCOPED)
class Fib260 @Inject constructor(
    val fibM1: Fib259,
    val fibM2: Fib258
)

@Factory
@Instance(type = Fib261::class, scoping = Scoping.UNSCOPED)
class Fib261 @Inject constructor(
    val fibM1: Fib260,
    val fibM2: Fib259
)

@Factory
@Instance(type = Fib262::class, scoping = Scoping.UNSCOPED)
class Fib262 @Inject constructor(
    val fibM1: Fib261,
    val fibM2: Fib260
)

@Factory
@Instance(type = Fib263::class, scoping = Scoping.UNSCOPED)
class Fib263 @Inject constructor(
    val fibM1: Fib262,
    val fibM2: Fib261
)

@Factory
@Instance(type = Fib264::class, scoping = Scoping.UNSCOPED)
class Fib264 @Inject constructor(
    val fibM1: Fib263,
    val fibM2: Fib262
)

@Factory
@Instance(type = Fib265::class, scoping = Scoping.UNSCOPED)
class Fib265 @Inject constructor(
    val fibM1: Fib264,
    val fibM2: Fib263
)

@Factory
@Instance(type = Fib266::class, scoping = Scoping.UNSCOPED)
class Fib266 @Inject constructor(
    val fibM1: Fib265,
    val fibM2: Fib264
)

@Factory
@Instance(type = Fib267::class, scoping = Scoping.UNSCOPED)
class Fib267 @Inject constructor(
    val fibM1: Fib266,
    val fibM2: Fib265
)

@Factory
@Instance(type = Fib268::class, scoping = Scoping.UNSCOPED)
class Fib268 @Inject constructor(
    val fibM1: Fib267,
    val fibM2: Fib266
)

@Factory
@Instance(type = Fib269::class, scoping = Scoping.UNSCOPED)
class Fib269 @Inject constructor(
    val fibM1: Fib268,
    val fibM2: Fib267
)

@Factory
@Instance(type = Fib270::class, scoping = Scoping.UNSCOPED)
class Fib270 @Inject constructor(
    val fibM1: Fib269,
    val fibM2: Fib268
)

@Factory
@Instance(type = Fib271::class, scoping = Scoping.UNSCOPED)
class Fib271 @Inject constructor(
    val fibM1: Fib270,
    val fibM2: Fib269
)

@Factory
@Instance(type = Fib272::class, scoping = Scoping.UNSCOPED)
class Fib272 @Inject constructor(
    val fibM1: Fib271,
    val fibM2: Fib270
)

@Factory
@Instance(type = Fib273::class, scoping = Scoping.UNSCOPED)
class Fib273 @Inject constructor(
    val fibM1: Fib272,
    val fibM2: Fib271
)

@Factory
@Instance(type = Fib274::class, scoping = Scoping.UNSCOPED)
class Fib274 @Inject constructor(
    val fibM1: Fib273,
    val fibM2: Fib272
)

@Factory
@Instance(type = Fib275::class, scoping = Scoping.UNSCOPED)
class Fib275 @Inject constructor(
    val fibM1: Fib274,
    val fibM2: Fib273
)

@Factory
@Instance(type = Fib276::class, scoping = Scoping.UNSCOPED)
class Fib276 @Inject constructor(
    val fibM1: Fib275,
    val fibM2: Fib274
)

@Factory
@Instance(type = Fib277::class, scoping = Scoping.UNSCOPED)
class Fib277 @Inject constructor(
    val fibM1: Fib276,
    val fibM2: Fib275
)

@Factory
@Instance(type = Fib278::class, scoping = Scoping.UNSCOPED)
class Fib278 @Inject constructor(
    val fibM1: Fib277,
    val fibM2: Fib276
)

@Factory
@Instance(type = Fib279::class, scoping = Scoping.UNSCOPED)
class Fib279 @Inject constructor(
    val fibM1: Fib278,
    val fibM2: Fib277
)

@Factory
@Instance(type = Fib280::class, scoping = Scoping.UNSCOPED)
class Fib280 @Inject constructor(
    val fibM1: Fib279,
    val fibM2: Fib278
)

@Factory
@Instance(type = Fib281::class, scoping = Scoping.UNSCOPED)
class Fib281 @Inject constructor(
    val fibM1: Fib280,
    val fibM2: Fib279
)

@Factory
@Instance(type = Fib282::class, scoping = Scoping.UNSCOPED)
class Fib282 @Inject constructor(
    val fibM1: Fib281,
    val fibM2: Fib280
)

@Factory
@Instance(type = Fib283::class, scoping = Scoping.UNSCOPED)
class Fib283 @Inject constructor(
    val fibM1: Fib282,
    val fibM2: Fib281
)

@Factory
@Instance(type = Fib284::class, scoping = Scoping.UNSCOPED)
class Fib284 @Inject constructor(
    val fibM1: Fib283,
    val fibM2: Fib282
)

@Factory
@Instance(type = Fib285::class, scoping = Scoping.UNSCOPED)
class Fib285 @Inject constructor(
    val fibM1: Fib284,
    val fibM2: Fib283
)

@Factory
@Instance(type = Fib286::class, scoping = Scoping.UNSCOPED)
class Fib286 @Inject constructor(
    val fibM1: Fib285,
    val fibM2: Fib284
)

@Factory
@Instance(type = Fib287::class, scoping = Scoping.UNSCOPED)
class Fib287 @Inject constructor(
    val fibM1: Fib286,
    val fibM2: Fib285
)

@Factory
@Instance(type = Fib288::class, scoping = Scoping.UNSCOPED)
class Fib288 @Inject constructor(
    val fibM1: Fib287,
    val fibM2: Fib286
)

@Factory
@Instance(type = Fib289::class, scoping = Scoping.UNSCOPED)
class Fib289 @Inject constructor(
    val fibM1: Fib288,
    val fibM2: Fib287
)

@Factory
@Instance(type = Fib290::class, scoping = Scoping.UNSCOPED)
class Fib290 @Inject constructor(
    val fibM1: Fib289,
    val fibM2: Fib288
)

@Factory
@Instance(type = Fib291::class, scoping = Scoping.UNSCOPED)
class Fib291 @Inject constructor(
    val fibM1: Fib290,
    val fibM2: Fib289
)

@Factory
@Instance(type = Fib292::class, scoping = Scoping.UNSCOPED)
class Fib292 @Inject constructor(
    val fibM1: Fib291,
    val fibM2: Fib290
)

@Factory
@Instance(type = Fib293::class, scoping = Scoping.UNSCOPED)
class Fib293 @Inject constructor(
    val fibM1: Fib292,
    val fibM2: Fib291
)

@Factory
@Instance(type = Fib294::class, scoping = Scoping.UNSCOPED)
class Fib294 @Inject constructor(
    val fibM1: Fib293,
    val fibM2: Fib292
)

@Factory
@Instance(type = Fib295::class, scoping = Scoping.UNSCOPED)
class Fib295 @Inject constructor(
    val fibM1: Fib294,
    val fibM2: Fib293
)

@Factory
@Instance(type = Fib296::class, scoping = Scoping.UNSCOPED)
class Fib296 @Inject constructor(
    val fibM1: Fib295,
    val fibM2: Fib294
)

@Factory
@Instance(type = Fib297::class, scoping = Scoping.UNSCOPED)
class Fib297 @Inject constructor(
    val fibM1: Fib296,
    val fibM2: Fib295
)

@Factory
@Instance(type = Fib298::class, scoping = Scoping.UNSCOPED)
class Fib298 @Inject constructor(
    val fibM1: Fib297,
    val fibM2: Fib296
)

@Factory
@Instance(type = Fib299::class, scoping = Scoping.UNSCOPED)
class Fib299 @Inject constructor(
    val fibM1: Fib298,
    val fibM2: Fib297
)

@Factory
@Instance(type = Fib300::class, scoping = Scoping.UNSCOPED)
class Fib300 @Inject constructor(
    val fibM1: Fib299,
    val fibM2: Fib298
)

@Factory
@Instance(type = Fib301::class, scoping = Scoping.UNSCOPED)
class Fib301 @Inject constructor(
    val fibM1: Fib300,
    val fibM2: Fib299
)

@Factory
@Instance(type = Fib302::class, scoping = Scoping.UNSCOPED)
class Fib302 @Inject constructor(
    val fibM1: Fib301,
    val fibM2: Fib300
)

@Factory
@Instance(type = Fib303::class, scoping = Scoping.UNSCOPED)
class Fib303 @Inject constructor(
    val fibM1: Fib302,
    val fibM2: Fib301
)

@Factory
@Instance(type = Fib304::class, scoping = Scoping.UNSCOPED)
class Fib304 @Inject constructor(
    val fibM1: Fib303,
    val fibM2: Fib302
)

@Factory
@Instance(type = Fib305::class, scoping = Scoping.UNSCOPED)
class Fib305 @Inject constructor(
    val fibM1: Fib304,
    val fibM2: Fib303
)

@Factory
@Instance(type = Fib306::class, scoping = Scoping.UNSCOPED)
class Fib306 @Inject constructor(
    val fibM1: Fib305,
    val fibM2: Fib304
)

@Factory
@Instance(type = Fib307::class, scoping = Scoping.UNSCOPED)
class Fib307 @Inject constructor(
    val fibM1: Fib306,
    val fibM2: Fib305
)

@Factory
@Instance(type = Fib308::class, scoping = Scoping.UNSCOPED)
class Fib308 @Inject constructor(
    val fibM1: Fib307,
    val fibM2: Fib306
)

@Factory
@Instance(type = Fib309::class, scoping = Scoping.UNSCOPED)
class Fib309 @Inject constructor(
    val fibM1: Fib308,
    val fibM2: Fib307
)

@Factory
@Instance(type = Fib310::class, scoping = Scoping.UNSCOPED)
class Fib310 @Inject constructor(
    val fibM1: Fib309,
    val fibM2: Fib308
)

@Factory
@Instance(type = Fib311::class, scoping = Scoping.UNSCOPED)
class Fib311 @Inject constructor(
    val fibM1: Fib310,
    val fibM2: Fib309
)

@Factory
@Instance(type = Fib312::class, scoping = Scoping.UNSCOPED)
class Fib312 @Inject constructor(
    val fibM1: Fib311,
    val fibM2: Fib310
)

@Factory
@Instance(type = Fib313::class, scoping = Scoping.UNSCOPED)
class Fib313 @Inject constructor(
    val fibM1: Fib312,
    val fibM2: Fib311
)

@Factory
@Instance(type = Fib314::class, scoping = Scoping.UNSCOPED)
class Fib314 @Inject constructor(
    val fibM1: Fib313,
    val fibM2: Fib312
)

@Factory
@Instance(type = Fib315::class, scoping = Scoping.UNSCOPED)
class Fib315 @Inject constructor(
    val fibM1: Fib314,
    val fibM2: Fib313
)

@Factory
@Instance(type = Fib316::class, scoping = Scoping.UNSCOPED)
class Fib316 @Inject constructor(
    val fibM1: Fib315,
    val fibM2: Fib314
)

@Factory
@Instance(type = Fib317::class, scoping = Scoping.UNSCOPED)
class Fib317 @Inject constructor(
    val fibM1: Fib316,
    val fibM2: Fib315
)

@Factory
@Instance(type = Fib318::class, scoping = Scoping.UNSCOPED)
class Fib318 @Inject constructor(
    val fibM1: Fib317,
    val fibM2: Fib316
)

@Factory
@Instance(type = Fib319::class, scoping = Scoping.UNSCOPED)
class Fib319 @Inject constructor(
    val fibM1: Fib318,
    val fibM2: Fib317
)

@Factory
@Instance(type = Fib320::class, scoping = Scoping.UNSCOPED)
class Fib320 @Inject constructor(
    val fibM1: Fib319,
    val fibM2: Fib318
)

@Factory
@Instance(type = Fib321::class, scoping = Scoping.UNSCOPED)
class Fib321 @Inject constructor(
    val fibM1: Fib320,
    val fibM2: Fib319
)

@Factory
@Instance(type = Fib322::class, scoping = Scoping.UNSCOPED)
class Fib322 @Inject constructor(
    val fibM1: Fib321,
    val fibM2: Fib320
)

@Factory
@Instance(type = Fib323::class, scoping = Scoping.UNSCOPED)
class Fib323 @Inject constructor(
    val fibM1: Fib322,
    val fibM2: Fib321
)

@Factory
@Instance(type = Fib324::class, scoping = Scoping.UNSCOPED)
class Fib324 @Inject constructor(
    val fibM1: Fib323,
    val fibM2: Fib322
)

@Factory
@Instance(type = Fib325::class, scoping = Scoping.UNSCOPED)
class Fib325 @Inject constructor(
    val fibM1: Fib324,
    val fibM2: Fib323
)

@Factory
@Instance(type = Fib326::class, scoping = Scoping.UNSCOPED)
class Fib326 @Inject constructor(
    val fibM1: Fib325,
    val fibM2: Fib324
)

@Factory
@Instance(type = Fib327::class, scoping = Scoping.UNSCOPED)
class Fib327 @Inject constructor(
    val fibM1: Fib326,
    val fibM2: Fib325
)

@Factory
@Instance(type = Fib328::class, scoping = Scoping.UNSCOPED)
class Fib328 @Inject constructor(
    val fibM1: Fib327,
    val fibM2: Fib326
)

@Factory
@Instance(type = Fib329::class, scoping = Scoping.UNSCOPED)
class Fib329 @Inject constructor(
    val fibM1: Fib328,
    val fibM2: Fib327
)

@Factory
@Instance(type = Fib330::class, scoping = Scoping.UNSCOPED)
class Fib330 @Inject constructor(
    val fibM1: Fib329,
    val fibM2: Fib328
)

@Factory
@Instance(type = Fib331::class, scoping = Scoping.UNSCOPED)
class Fib331 @Inject constructor(
    val fibM1: Fib330,
    val fibM2: Fib329
)

@Factory
@Instance(type = Fib332::class, scoping = Scoping.UNSCOPED)
class Fib332 @Inject constructor(
    val fibM1: Fib331,
    val fibM2: Fib330
)

@Factory
@Instance(type = Fib333::class, scoping = Scoping.UNSCOPED)
class Fib333 @Inject constructor(
    val fibM1: Fib332,
    val fibM2: Fib331
)

@Factory
@Instance(type = Fib334::class, scoping = Scoping.UNSCOPED)
class Fib334 @Inject constructor(
    val fibM1: Fib333,
    val fibM2: Fib332
)

@Factory
@Instance(type = Fib335::class, scoping = Scoping.UNSCOPED)
class Fib335 @Inject constructor(
    val fibM1: Fib334,
    val fibM2: Fib333
)

@Factory
@Instance(type = Fib336::class, scoping = Scoping.UNSCOPED)
class Fib336 @Inject constructor(
    val fibM1: Fib335,
    val fibM2: Fib334
)

@Factory
@Instance(type = Fib337::class, scoping = Scoping.UNSCOPED)
class Fib337 @Inject constructor(
    val fibM1: Fib336,
    val fibM2: Fib335
)

@Factory
@Instance(type = Fib338::class, scoping = Scoping.UNSCOPED)
class Fib338 @Inject constructor(
    val fibM1: Fib337,
    val fibM2: Fib336
)

@Factory
@Instance(type = Fib339::class, scoping = Scoping.UNSCOPED)
class Fib339 @Inject constructor(
    val fibM1: Fib338,
    val fibM2: Fib337
)

@Factory
@Instance(type = Fib340::class, scoping = Scoping.UNSCOPED)
class Fib340 @Inject constructor(
    val fibM1: Fib339,
    val fibM2: Fib338
)

@Factory
@Instance(type = Fib341::class, scoping = Scoping.UNSCOPED)
class Fib341 @Inject constructor(
    val fibM1: Fib340,
    val fibM2: Fib339
)

@Factory
@Instance(type = Fib342::class, scoping = Scoping.UNSCOPED)
class Fib342 @Inject constructor(
    val fibM1: Fib341,
    val fibM2: Fib340
)

@Factory
@Instance(type = Fib343::class, scoping = Scoping.UNSCOPED)
class Fib343 @Inject constructor(
    val fibM1: Fib342,
    val fibM2: Fib341
)

@Factory
@Instance(type = Fib344::class, scoping = Scoping.UNSCOPED)
class Fib344 @Inject constructor(
    val fibM1: Fib343,
    val fibM2: Fib342
)

@Factory
@Instance(type = Fib345::class, scoping = Scoping.UNSCOPED)
class Fib345 @Inject constructor(
    val fibM1: Fib344,
    val fibM2: Fib343
)

@Factory
@Instance(type = Fib346::class, scoping = Scoping.UNSCOPED)
class Fib346 @Inject constructor(
    val fibM1: Fib345,
    val fibM2: Fib344
)

@Factory
@Instance(type = Fib347::class, scoping = Scoping.UNSCOPED)
class Fib347 @Inject constructor(
    val fibM1: Fib346,
    val fibM2: Fib345
)

@Factory
@Instance(type = Fib348::class, scoping = Scoping.UNSCOPED)
class Fib348 @Inject constructor(
    val fibM1: Fib347,
    val fibM2: Fib346
)

@Factory
@Instance(type = Fib349::class, scoping = Scoping.UNSCOPED)
class Fib349 @Inject constructor(
    val fibM1: Fib348,
    val fibM2: Fib347
)

@Factory
@Instance(type = Fib350::class, scoping = Scoping.UNSCOPED)
class Fib350 @Inject constructor(
    val fibM1: Fib349,
    val fibM2: Fib348
)

@Factory
@Instance(type = Fib351::class, scoping = Scoping.UNSCOPED)
class Fib351 @Inject constructor(
    val fibM1: Fib350,
    val fibM2: Fib349
)

@Factory
@Instance(type = Fib352::class, scoping = Scoping.UNSCOPED)
class Fib352 @Inject constructor(
    val fibM1: Fib351,
    val fibM2: Fib350
)

@Factory
@Instance(type = Fib353::class, scoping = Scoping.UNSCOPED)
class Fib353 @Inject constructor(
    val fibM1: Fib352,
    val fibM2: Fib351
)

@Factory
@Instance(type = Fib354::class, scoping = Scoping.UNSCOPED)
class Fib354 @Inject constructor(
    val fibM1: Fib353,
    val fibM2: Fib352
)

@Factory
@Instance(type = Fib355::class, scoping = Scoping.UNSCOPED)
class Fib355 @Inject constructor(
    val fibM1: Fib354,
    val fibM2: Fib353
)

@Factory
@Instance(type = Fib356::class, scoping = Scoping.UNSCOPED)
class Fib356 @Inject constructor(
    val fibM1: Fib355,
    val fibM2: Fib354
)

@Factory
@Instance(type = Fib357::class, scoping = Scoping.UNSCOPED)
class Fib357 @Inject constructor(
    val fibM1: Fib356,
    val fibM2: Fib355
)

@Factory
@Instance(type = Fib358::class, scoping = Scoping.UNSCOPED)
class Fib358 @Inject constructor(
    val fibM1: Fib357,
    val fibM2: Fib356
)

@Factory
@Instance(type = Fib359::class, scoping = Scoping.UNSCOPED)
class Fib359 @Inject constructor(
    val fibM1: Fib358,
    val fibM2: Fib357
)

@Factory
@Instance(type = Fib360::class, scoping = Scoping.UNSCOPED)
class Fib360 @Inject constructor(
    val fibM1: Fib359,
    val fibM2: Fib358
)

@Factory
@Instance(type = Fib361::class, scoping = Scoping.UNSCOPED)
class Fib361 @Inject constructor(
    val fibM1: Fib360,
    val fibM2: Fib359
)

@Factory
@Instance(type = Fib362::class, scoping = Scoping.UNSCOPED)
class Fib362 @Inject constructor(
    val fibM1: Fib361,
    val fibM2: Fib360
)

@Factory
@Instance(type = Fib363::class, scoping = Scoping.UNSCOPED)
class Fib363 @Inject constructor(
    val fibM1: Fib362,
    val fibM2: Fib361
)

@Factory
@Instance(type = Fib364::class, scoping = Scoping.UNSCOPED)
class Fib364 @Inject constructor(
    val fibM1: Fib363,
    val fibM2: Fib362
)

@Factory
@Instance(type = Fib365::class, scoping = Scoping.UNSCOPED)
class Fib365 @Inject constructor(
    val fibM1: Fib364,
    val fibM2: Fib363
)

@Factory
@Instance(type = Fib366::class, scoping = Scoping.UNSCOPED)
class Fib366 @Inject constructor(
    val fibM1: Fib365,
    val fibM2: Fib364
)

@Factory
@Instance(type = Fib367::class, scoping = Scoping.UNSCOPED)
class Fib367 @Inject constructor(
    val fibM1: Fib366,
    val fibM2: Fib365
)

@Factory
@Instance(type = Fib368::class, scoping = Scoping.UNSCOPED)
class Fib368 @Inject constructor(
    val fibM1: Fib367,
    val fibM2: Fib366
)

@Factory
@Instance(type = Fib369::class, scoping = Scoping.UNSCOPED)
class Fib369 @Inject constructor(
    val fibM1: Fib368,
    val fibM2: Fib367
)

@Factory
@Instance(type = Fib370::class, scoping = Scoping.UNSCOPED)
class Fib370 @Inject constructor(
    val fibM1: Fib369,
    val fibM2: Fib368
)

@Factory
@Instance(type = Fib371::class, scoping = Scoping.UNSCOPED)
class Fib371 @Inject constructor(
    val fibM1: Fib370,
    val fibM2: Fib369
)

@Factory
@Instance(type = Fib372::class, scoping = Scoping.UNSCOPED)
class Fib372 @Inject constructor(
    val fibM1: Fib371,
    val fibM2: Fib370
)

@Factory
@Instance(type = Fib373::class, scoping = Scoping.UNSCOPED)
class Fib373 @Inject constructor(
    val fibM1: Fib372,
    val fibM2: Fib371
)

@Factory
@Instance(type = Fib374::class, scoping = Scoping.UNSCOPED)
class Fib374 @Inject constructor(
    val fibM1: Fib373,
    val fibM2: Fib372
)

@Factory
@Instance(type = Fib375::class, scoping = Scoping.UNSCOPED)
class Fib375 @Inject constructor(
    val fibM1: Fib374,
    val fibM2: Fib373
)

@Factory
@Instance(type = Fib376::class, scoping = Scoping.UNSCOPED)
class Fib376 @Inject constructor(
    val fibM1: Fib375,
    val fibM2: Fib374
)

@Factory
@Instance(type = Fib377::class, scoping = Scoping.UNSCOPED)
class Fib377 @Inject constructor(
    val fibM1: Fib376,
    val fibM2: Fib375
)

@Factory
@Instance(type = Fib378::class, scoping = Scoping.UNSCOPED)
class Fib378 @Inject constructor(
    val fibM1: Fib377,
    val fibM2: Fib376
)

@Factory
@Instance(type = Fib379::class, scoping = Scoping.UNSCOPED)
class Fib379 @Inject constructor(
    val fibM1: Fib378,
    val fibM2: Fib377
)

@Factory
@Instance(type = Fib380::class, scoping = Scoping.UNSCOPED)
class Fib380 @Inject constructor(
    val fibM1: Fib379,
    val fibM2: Fib378
)

@Factory
@Instance(type = Fib381::class, scoping = Scoping.UNSCOPED)
class Fib381 @Inject constructor(
    val fibM1: Fib380,
    val fibM2: Fib379
)

@Factory
@Instance(type = Fib382::class, scoping = Scoping.UNSCOPED)
class Fib382 @Inject constructor(
    val fibM1: Fib381,
    val fibM2: Fib380
)

@Factory
@Instance(type = Fib383::class, scoping = Scoping.UNSCOPED)
class Fib383 @Inject constructor(
    val fibM1: Fib382,
    val fibM2: Fib381
)

@Factory
@Instance(type = Fib384::class, scoping = Scoping.UNSCOPED)
class Fib384 @Inject constructor(
    val fibM1: Fib383,
    val fibM2: Fib382
)

@Factory
@Instance(type = Fib385::class, scoping = Scoping.UNSCOPED)
class Fib385 @Inject constructor(
    val fibM1: Fib384,
    val fibM2: Fib383
)

@Factory
@Instance(type = Fib386::class, scoping = Scoping.UNSCOPED)
class Fib386 @Inject constructor(
    val fibM1: Fib385,
    val fibM2: Fib384
)

@Factory
@Instance(type = Fib387::class, scoping = Scoping.UNSCOPED)
class Fib387 @Inject constructor(
    val fibM1: Fib386,
    val fibM2: Fib385
)

@Factory
@Instance(type = Fib388::class, scoping = Scoping.UNSCOPED)
class Fib388 @Inject constructor(
    val fibM1: Fib387,
    val fibM2: Fib386
)

@Factory
@Instance(type = Fib389::class, scoping = Scoping.UNSCOPED)
class Fib389 @Inject constructor(
    val fibM1: Fib388,
    val fibM2: Fib387
)

@Factory
@Instance(type = Fib390::class, scoping = Scoping.UNSCOPED)
class Fib390 @Inject constructor(
    val fibM1: Fib389,
    val fibM2: Fib388
)

@Factory
@Instance(type = Fib391::class, scoping = Scoping.UNSCOPED)
class Fib391 @Inject constructor(
    val fibM1: Fib390,
    val fibM2: Fib389
)

@Factory
@Instance(type = Fib392::class, scoping = Scoping.UNSCOPED)
class Fib392 @Inject constructor(
    val fibM1: Fib391,
    val fibM2: Fib390
)

@Factory
@Instance(type = Fib393::class, scoping = Scoping.UNSCOPED)
class Fib393 @Inject constructor(
    val fibM1: Fib392,
    val fibM2: Fib391
)

@Factory
@Instance(type = Fib394::class, scoping = Scoping.UNSCOPED)
class Fib394 @Inject constructor(
    val fibM1: Fib393,
    val fibM2: Fib392
)

@Factory
@Instance(type = Fib395::class, scoping = Scoping.UNSCOPED)
class Fib395 @Inject constructor(
    val fibM1: Fib394,
    val fibM2: Fib393
)

@Factory
@Instance(type = Fib396::class, scoping = Scoping.UNSCOPED)
class Fib396 @Inject constructor(
    val fibM1: Fib395,
    val fibM2: Fib394
)

@Factory
@Instance(type = Fib397::class, scoping = Scoping.UNSCOPED)
class Fib397 @Inject constructor(
    val fibM1: Fib396,
    val fibM2: Fib395
)

@Factory
@Instance(type = Fib398::class, scoping = Scoping.UNSCOPED)
class Fib398 @Inject constructor(
    val fibM1: Fib397,
    val fibM2: Fib396
)

@Factory
@Instance(type = Fib399::class, scoping = Scoping.UNSCOPED)
class Fib399 @Inject constructor(
    val fibM1: Fib398,
    val fibM2: Fib397
)

@Factory
@Instance(type = Fib400::class, scoping = Scoping.UNSCOPED)
class Fib400 @Inject constructor(
    val fibM1: Fib399,
    val fibM2: Fib398
)
