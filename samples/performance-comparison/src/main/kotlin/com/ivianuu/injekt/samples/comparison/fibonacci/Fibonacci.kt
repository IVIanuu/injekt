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

package com.ivianuu.injekt.samples.comparison.fibonacci

import com.ivianuu.injekt.Binding
import javax.inject.Inject

@Binding
class Fib1 @Inject constructor()

@Binding
class Fib2 @Inject constructor()

@Binding
class Fib3 @Inject constructor(
    val fibM1: Fib2,
    val fibM2: Fib1,
)

@Binding
class Fib4 @Inject constructor(
    val fibM1: Fib3,
    val fibM2: Fib2,
)

@Binding
class Fib5 @Inject constructor(
    val fibM1: Fib4,
    val fibM2: Fib3,
)

@Binding
class Fib6 @Inject constructor(
    val fibM1: Fib5,
    val fibM2: Fib4,
)

@Binding
class Fib7 @Inject constructor(
    val fibM1: Fib6,
    val fibM2: Fib5,
)

@Binding
class Fib8 @Inject constructor(
    val fibM1: Fib7,
    val fibM2: Fib6,
)

@Binding
class Fib9 @Inject constructor(
    val fibM1: Fib8,
    val fibM2: Fib7,
)

@Binding
class Fib10 @Inject constructor(
    val fibM1: Fib9,
    val fibM2: Fib8,
)

@Binding
class Fib11 @Inject constructor(
    val fibM1: Fib10,
    val fibM2: Fib9,
)

@Binding
class Fib12 @Inject constructor(
    val fibM1: Fib11,
    val fibM2: Fib10,
)

@Binding
class Fib13 @Inject constructor(
    val fibM1: Fib12,
    val fibM2: Fib11,
)

@Binding
class Fib14 @Inject constructor(
    val fibM1: Fib13,
    val fibM2: Fib12,
)

@Binding
class Fib15 @Inject constructor(
    val fibM1: Fib14,
    val fibM2: Fib13,
)

@Binding
class Fib16 @Inject constructor(
    val fibM1: Fib15,
    val fibM2: Fib14,
)

@Binding
class Fib17 @Inject constructor(
    val fibM1: Fib16,
    val fibM2: Fib15,
)

@Binding
class Fib18 @Inject constructor(
    val fibM1: Fib17,
    val fibM2: Fib16,
)

@Binding
class Fib19 @Inject constructor(
    val fibM1: Fib18,
    val fibM2: Fib17,
)

@Binding
class Fib20 @Inject constructor(
    val fibM1: Fib19,
    val fibM2: Fib18,
)

@Binding
class Fib21 @Inject constructor(
    val fibM1: Fib20,
    val fibM2: Fib19,
)

@Binding
class Fib22 @Inject constructor(
    val fibM1: Fib21,
    val fibM2: Fib20,
)

@Binding
class Fib23 @Inject constructor(
    val fibM1: Fib22,
    val fibM2: Fib21,
)

@Binding
class Fib24 @Inject constructor(
    val fibM1: Fib23,
    val fibM2: Fib22,
)

@Binding
class Fib25 @Inject constructor(
    val fibM1: Fib24,
    val fibM2: Fib23,
)

@Binding
class Fib26 @Inject constructor(
    val fibM1: Fib25,
    val fibM2: Fib24,
)

@Binding
class Fib27 @Inject constructor(
    val fibM1: Fib26,
    val fibM2: Fib25,
)

@Binding
class Fib28 @Inject constructor(
    val fibM1: Fib27,
    val fibM2: Fib26,
)

@Binding
class Fib29 @Inject constructor(
    val fibM1: Fib28,
    val fibM2: Fib27,
)

@Binding
class Fib30 @Inject constructor(
    val fibM1: Fib29,
    val fibM2: Fib28,
)

@Binding
class Fib31 @Inject constructor(
    val fibM1: Fib30,
    val fibM2: Fib29,
)

@Binding
class Fib32 @Inject constructor(
    val fibM1: Fib31,
    val fibM2: Fib30,
)

@Binding
class Fib33 @Inject constructor(
    val fibM1: Fib32,
    val fibM2: Fib31,
)

@Binding
class Fib34 @Inject constructor(
    val fibM1: Fib33,
    val fibM2: Fib32,
)

@Binding
class Fib35 @Inject constructor(
    val fibM1: Fib34,
    val fibM2: Fib33,
)

@Binding
class Fib36 @Inject constructor(
    val fibM1: Fib35,
    val fibM2: Fib34,
)

@Binding
class Fib37 @Inject constructor(
    val fibM1: Fib36,
    val fibM2: Fib35,
)

@Binding
class Fib38 @Inject constructor(
    val fibM1: Fib37,
    val fibM2: Fib36,
)

@Binding
class Fib39 @Inject constructor(
    val fibM1: Fib38,
    val fibM2: Fib37,
)

@Binding
class Fib40 @Inject constructor(
    val fibM1: Fib39,
    val fibM2: Fib38,
)

@Binding
class Fib41 @Inject constructor(
    val fibM1: Fib40,
    val fibM2: Fib39,
)

@Binding
class Fib42 @Inject constructor(
    val fibM1: Fib41,
    val fibM2: Fib40,
)

@Binding
class Fib43 @Inject constructor(
    val fibM1: Fib42,
    val fibM2: Fib41,
)

@Binding
class Fib44 @Inject constructor(
    val fibM1: Fib43,
    val fibM2: Fib42,
)

@Binding
class Fib45 @Inject constructor(
    val fibM1: Fib44,
    val fibM2: Fib43,
)

@Binding
class Fib46 @Inject constructor(
    val fibM1: Fib45,
    val fibM2: Fib44,
)

@Binding
class Fib47 @Inject constructor(
    val fibM1: Fib46,
    val fibM2: Fib45,
)

@Binding
class Fib48 @Inject constructor(
    val fibM1: Fib47,
    val fibM2: Fib46,
)

@Binding
class Fib49 @Inject constructor(
    val fibM1: Fib48,
    val fibM2: Fib47,
)

@Binding
class Fib50 @Inject constructor(
    val fibM1: Fib49,
    val fibM2: Fib48,
)

@Binding
class Fib51 @Inject constructor(
    val fibM1: Fib50,
    val fibM2: Fib49,
)

@Binding
class Fib52 @Inject constructor(
    val fibM1: Fib51,
    val fibM2: Fib50,
)

@Binding
class Fib53 @Inject constructor(
    val fibM1: Fib52,
    val fibM2: Fib51,
)

@Binding
class Fib54 @Inject constructor(
    val fibM1: Fib53,
    val fibM2: Fib52,
)

@Binding
class Fib55 @Inject constructor(
    val fibM1: Fib54,
    val fibM2: Fib53,
)

@Binding
class Fib56 @Inject constructor(
    val fibM1: Fib55,
    val fibM2: Fib54,
)

@Binding
class Fib57 @Inject constructor(
    val fibM1: Fib56,
    val fibM2: Fib55,
)

@Binding
class Fib58 @Inject constructor(
    val fibM1: Fib57,
    val fibM2: Fib56,
)

@Binding
class Fib59 @Inject constructor(
    val fibM1: Fib58,
    val fibM2: Fib57,
)

@Binding
class Fib60 @Inject constructor(
    val fibM1: Fib59,
    val fibM2: Fib58,
)

@Binding
class Fib61 @Inject constructor(
    val fibM1: Fib60,
    val fibM2: Fib59,
)

@Binding
class Fib62 @Inject constructor(
    val fibM1: Fib61,
    val fibM2: Fib60,
)

@Binding
class Fib63 @Inject constructor(
    val fibM1: Fib62,
    val fibM2: Fib61,
)

@Binding
class Fib64 @Inject constructor(
    val fibM1: Fib63,
    val fibM2: Fib62,
)

@Binding
class Fib65 @Inject constructor(
    val fibM1: Fib64,
    val fibM2: Fib63,
)

@Binding
class Fib66 @Inject constructor(
    val fibM1: Fib65,
    val fibM2: Fib64,
)

@Binding
class Fib67 @Inject constructor(
    val fibM1: Fib66,
    val fibM2: Fib65,
)

@Binding
class Fib68 @Inject constructor(
    val fibM1: Fib67,
    val fibM2: Fib66,
)

@Binding
class Fib69 @Inject constructor(
    val fibM1: Fib68,
    val fibM2: Fib67,
)

@Binding
class Fib70 @Inject constructor(
    val fibM1: Fib69,
    val fibM2: Fib68,
)

@Binding
class Fib71 @Inject constructor(
    val fibM1: Fib70,
    val fibM2: Fib69,
)

@Binding
class Fib72 @Inject constructor(
    val fibM1: Fib71,
    val fibM2: Fib70,
)

@Binding
class Fib73 @Inject constructor(
    val fibM1: Fib72,
    val fibM2: Fib71,
)

@Binding
class Fib74 @Inject constructor(
    val fibM1: Fib73,
    val fibM2: Fib72,
)

@Binding
class Fib75 @Inject constructor(
    val fibM1: Fib74,
    val fibM2: Fib73,
)

@Binding
class Fib76 @Inject constructor(
    val fibM1: Fib75,
    val fibM2: Fib74,
)

@Binding
class Fib77 @Inject constructor(
    val fibM1: Fib76,
    val fibM2: Fib75,
)

@Binding
class Fib78 @Inject constructor(
    val fibM1: Fib77,
    val fibM2: Fib76,
)

@Binding
class Fib79 @Inject constructor(
    val fibM1: Fib78,
    val fibM2: Fib77,
)

@Binding
class Fib80 @Inject constructor(
    val fibM1: Fib79,
    val fibM2: Fib78,
)

@Binding
class Fib81 @Inject constructor(
    val fibM1: Fib80,
    val fibM2: Fib79,
)

@Binding
class Fib82 @Inject constructor(
    val fibM1: Fib81,
    val fibM2: Fib80,
)

@Binding
class Fib83 @Inject constructor(
    val fibM1: Fib82,
    val fibM2: Fib81,
)

@Binding
class Fib84 @Inject constructor(
    val fibM1: Fib83,
    val fibM2: Fib82,
)

@Binding
class Fib85 @Inject constructor(
    val fibM1: Fib84,
    val fibM2: Fib83,
)

@Binding
class Fib86 @Inject constructor(
    val fibM1: Fib85,
    val fibM2: Fib84,
)

@Binding
class Fib87 @Inject constructor(
    val fibM1: Fib86,
    val fibM2: Fib85,
)

@Binding
class Fib88 @Inject constructor(
    val fibM1: Fib87,
    val fibM2: Fib86,
)

@Binding
class Fib89 @Inject constructor(
    val fibM1: Fib88,
    val fibM2: Fib87,
)

@Binding
class Fib90 @Inject constructor(
    val fibM1: Fib89,
    val fibM2: Fib88,
)

@Binding
class Fib91 @Inject constructor(
    val fibM1: Fib90,
    val fibM2: Fib89,
)

@Binding
class Fib92 @Inject constructor(
    val fibM1: Fib91,
    val fibM2: Fib90,
)

@Binding
class Fib93 @Inject constructor(
    val fibM1: Fib92,
    val fibM2: Fib91,
)

@Binding
class Fib94 @Inject constructor(
    val fibM1: Fib93,
    val fibM2: Fib92,
)

@Binding
class Fib95 @Inject constructor(
    val fibM1: Fib94,
    val fibM2: Fib93,
)

@Binding
class Fib96 @Inject constructor(
    val fibM1: Fib95,
    val fibM2: Fib94,
)

@Binding
class Fib97 @Inject constructor(
    val fibM1: Fib96,
    val fibM2: Fib95,
)

@Binding
class Fib98 @Inject constructor(
    val fibM1: Fib97,
    val fibM2: Fib96,
)

@Binding
class Fib99 @Inject constructor(
    val fibM1: Fib98,
    val fibM2: Fib97,
)

@Binding
class Fib100 @Inject constructor(
    val fibM1: Fib99,
    val fibM2: Fib98,
)

@Binding
class Fib101 @Inject constructor(
    val fibM1: Fib100,
    val fibM2: Fib99,
)

@Binding
class Fib102 @Inject constructor(
    val fibM1: Fib101,
    val fibM2: Fib100,
)

@Binding
class Fib103 @Inject constructor(
    val fibM1: Fib102,
    val fibM2: Fib101,
)

@Binding
class Fib104 @Inject constructor(
    val fibM1: Fib103,
    val fibM2: Fib102,
)

@Binding
class Fib105 @Inject constructor(
    val fibM1: Fib104,
    val fibM2: Fib103,
)

@Binding
class Fib106 @Inject constructor(
    val fibM1: Fib105,
    val fibM2: Fib104,
)

@Binding
class Fib107 @Inject constructor(
    val fibM1: Fib106,
    val fibM2: Fib105,
)

@Binding
class Fib108 @Inject constructor(
    val fibM1: Fib107,
    val fibM2: Fib106,
)

@Binding
class Fib109 @Inject constructor(
    val fibM1: Fib108,
    val fibM2: Fib107,
)

@Binding
class Fib110 @Inject constructor(
    val fibM1: Fib109,
    val fibM2: Fib108,
)

@Binding
class Fib111 @Inject constructor(
    val fibM1: Fib110,
    val fibM2: Fib109,
)

@Binding
class Fib112 @Inject constructor(
    val fibM1: Fib111,
    val fibM2: Fib110,
)

@Binding
class Fib113 @Inject constructor(
    val fibM1: Fib112,
    val fibM2: Fib111,
)

@Binding
class Fib114 @Inject constructor(
    val fibM1: Fib113,
    val fibM2: Fib112,
)

@Binding
class Fib115 @Inject constructor(
    val fibM1: Fib114,
    val fibM2: Fib113,
)

@Binding
class Fib116 @Inject constructor(
    val fibM1: Fib115,
    val fibM2: Fib114,
)

@Binding
class Fib117 @Inject constructor(
    val fibM1: Fib116,
    val fibM2: Fib115,
)

@Binding
class Fib118 @Inject constructor(
    val fibM1: Fib117,
    val fibM2: Fib116,
)

@Binding
class Fib119 @Inject constructor(
    val fibM1: Fib118,
    val fibM2: Fib117,
)

@Binding
class Fib120 @Inject constructor(
    val fibM1: Fib119,
    val fibM2: Fib118,
)

@Binding
class Fib121 @Inject constructor(
    val fibM1: Fib120,
    val fibM2: Fib119,
)

@Binding
class Fib122 @Inject constructor(
    val fibM1: Fib121,
    val fibM2: Fib120,
)

@Binding
class Fib123 @Inject constructor(
    val fibM1: Fib122,
    val fibM2: Fib121,
)

@Binding
class Fib124 @Inject constructor(
    val fibM1: Fib123,
    val fibM2: Fib122,
)

@Binding
class Fib125 @Inject constructor(
    val fibM1: Fib124,
    val fibM2: Fib123,
)

@Binding
class Fib126 @Inject constructor(
    val fibM1: Fib125,
    val fibM2: Fib124,
)

@Binding
class Fib127 @Inject constructor(
    val fibM1: Fib126,
    val fibM2: Fib125,
)

@Binding
class Fib128 @Inject constructor(
    val fibM1: Fib127,
    val fibM2: Fib126,
)

@Binding
class Fib129 @Inject constructor(
    val fibM1: Fib128,
    val fibM2: Fib127,
)

@Binding
class Fib130 @Inject constructor(
    val fibM1: Fib129,
    val fibM2: Fib128,
)

@Binding
class Fib131 @Inject constructor(
    val fibM1: Fib130,
    val fibM2: Fib129,
)

@Binding
class Fib132 @Inject constructor(
    val fibM1: Fib131,
    val fibM2: Fib130,
)

@Binding
class Fib133 @Inject constructor(
    val fibM1: Fib132,
    val fibM2: Fib131,
)

@Binding
class Fib134 @Inject constructor(
    val fibM1: Fib133,
    val fibM2: Fib132,
)

@Binding
class Fib135 @Inject constructor(
    val fibM1: Fib134,
    val fibM2: Fib133,
)

@Binding
class Fib136 @Inject constructor(
    val fibM1: Fib135,
    val fibM2: Fib134,
)

@Binding
class Fib137 @Inject constructor(
    val fibM1: Fib136,
    val fibM2: Fib135,
)

@Binding
class Fib138 @Inject constructor(
    val fibM1: Fib137,
    val fibM2: Fib136,
)

@Binding
class Fib139 @Inject constructor(
    val fibM1: Fib138,
    val fibM2: Fib137,
)

@Binding
class Fib140 @Inject constructor(
    val fibM1: Fib139,
    val fibM2: Fib138,
)

@Binding
class Fib141 @Inject constructor(
    val fibM1: Fib140,
    val fibM2: Fib139,
)

@Binding
class Fib142 @Inject constructor(
    val fibM1: Fib141,
    val fibM2: Fib140,
)

@Binding
class Fib143 @Inject constructor(
    val fibM1: Fib142,
    val fibM2: Fib141,
)

@Binding
class Fib144 @Inject constructor(
    val fibM1: Fib143,
    val fibM2: Fib142,
)

@Binding
class Fib145 @Inject constructor(
    val fibM1: Fib144,
    val fibM2: Fib143,
)

@Binding
class Fib146 @Inject constructor(
    val fibM1: Fib145,
    val fibM2: Fib144,
)

@Binding
class Fib147 @Inject constructor(
    val fibM1: Fib146,
    val fibM2: Fib145,
)

@Binding
class Fib148 @Inject constructor(
    val fibM1: Fib147,
    val fibM2: Fib146,
)

@Binding
class Fib149 @Inject constructor(
    val fibM1: Fib148,
    val fibM2: Fib147,
)

@Binding
class Fib150 @Inject constructor(
    val fibM1: Fib149,
    val fibM2: Fib148,
)

@Binding
class Fib151 @Inject constructor(
    val fibM1: Fib150,
    val fibM2: Fib149,
)

@Binding
class Fib152 @Inject constructor(
    val fibM1: Fib151,
    val fibM2: Fib150,
)

@Binding
class Fib153 @Inject constructor(
    val fibM1: Fib152,
    val fibM2: Fib151,
)

@Binding
class Fib154 @Inject constructor(
    val fibM1: Fib153,
    val fibM2: Fib152,
)

@Binding
class Fib155 @Inject constructor(
    val fibM1: Fib154,
    val fibM2: Fib153,
)

@Binding
class Fib156 @Inject constructor(
    val fibM1: Fib155,
    val fibM2: Fib154,
)

@Binding
class Fib157 @Inject constructor(
    val fibM1: Fib156,
    val fibM2: Fib155,
)

@Binding
class Fib158 @Inject constructor(
    val fibM1: Fib157,
    val fibM2: Fib156,
)

@Binding
class Fib159 @Inject constructor(
    val fibM1: Fib158,
    val fibM2: Fib157,
)

@Binding
class Fib160 @Inject constructor(
    val fibM1: Fib159,
    val fibM2: Fib158,
)

@Binding
class Fib161 @Inject constructor(
    val fibM1: Fib160,
    val fibM2: Fib159,
)

@Binding
class Fib162 @Inject constructor(
    val fibM1: Fib161,
    val fibM2: Fib160,
)

@Binding
class Fib163 @Inject constructor(
    val fibM1: Fib162,
    val fibM2: Fib161,
)

@Binding
class Fib164 @Inject constructor(
    val fibM1: Fib163,
    val fibM2: Fib162,
)

@Binding
class Fib165 @Inject constructor(
    val fibM1: Fib164,
    val fibM2: Fib163,
)

@Binding
class Fib166 @Inject constructor(
    val fibM1: Fib165,
    val fibM2: Fib164,
)

@Binding
class Fib167 @Inject constructor(
    val fibM1: Fib166,
    val fibM2: Fib165,
)

@Binding
class Fib168 @Inject constructor(
    val fibM1: Fib167,
    val fibM2: Fib166,
)

@Binding
class Fib169 @Inject constructor(
    val fibM1: Fib168,
    val fibM2: Fib167,
)

@Binding
class Fib170 @Inject constructor(
    val fibM1: Fib169,
    val fibM2: Fib168,
)

@Binding
class Fib171 @Inject constructor(
    val fibM1: Fib170,
    val fibM2: Fib169,
)

@Binding
class Fib172 @Inject constructor(
    val fibM1: Fib171,
    val fibM2: Fib170,
)

@Binding
class Fib173 @Inject constructor(
    val fibM1: Fib172,
    val fibM2: Fib171,
)

@Binding
class Fib174 @Inject constructor(
    val fibM1: Fib173,
    val fibM2: Fib172,
)

@Binding
class Fib175 @Inject constructor(
    val fibM1: Fib174,
    val fibM2: Fib173,
)

@Binding
class Fib176 @Inject constructor(
    val fibM1: Fib175,
    val fibM2: Fib174,
)

@Binding
class Fib177 @Inject constructor(
    val fibM1: Fib176,
    val fibM2: Fib175,
)

@Binding
class Fib178 @Inject constructor(
    val fibM1: Fib177,
    val fibM2: Fib176,
)

@Binding
class Fib179 @Inject constructor(
    val fibM1: Fib178,
    val fibM2: Fib177,
)

@Binding
class Fib180 @Inject constructor(
    val fibM1: Fib179,
    val fibM2: Fib178,
)

@Binding
class Fib181 @Inject constructor(
    val fibM1: Fib180,
    val fibM2: Fib179,
)

@Binding
class Fib182 @Inject constructor(
    val fibM1: Fib181,
    val fibM2: Fib180,
)

@Binding
class Fib183 @Inject constructor(
    val fibM1: Fib182,
    val fibM2: Fib181,
)

@Binding
class Fib184 @Inject constructor(
    val fibM1: Fib183,
    val fibM2: Fib182,
)

@Binding
class Fib185 @Inject constructor(
    val fibM1: Fib184,
    val fibM2: Fib183,
)

@Binding
class Fib186 @Inject constructor(
    val fibM1: Fib185,
    val fibM2: Fib184,
)

@Binding
class Fib187 @Inject constructor(
    val fibM1: Fib186,
    val fibM2: Fib185,
)

@Binding
class Fib188 @Inject constructor(
    val fibM1: Fib187,
    val fibM2: Fib186,
)

@Binding
class Fib189 @Inject constructor(
    val fibM1: Fib188,
    val fibM2: Fib187,
)

@Binding
class Fib190 @Inject constructor(
    val fibM1: Fib189,
    val fibM2: Fib188,
)

@Binding
class Fib191 @Inject constructor(
    val fibM1: Fib190,
    val fibM2: Fib189,
)

@Binding
class Fib192 @Inject constructor(
    val fibM1: Fib191,
    val fibM2: Fib190,
)

@Binding
class Fib193 @Inject constructor(
    val fibM1: Fib192,
    val fibM2: Fib191,
)

@Binding
class Fib194 @Inject constructor(
    val fibM1: Fib193,
    val fibM2: Fib192,
)

@Binding
class Fib195 @Inject constructor(
    val fibM1: Fib194,
    val fibM2: Fib193,
)

@Binding
class Fib196 @Inject constructor(
    val fibM1: Fib195,
    val fibM2: Fib194,
)

@Binding
class Fib197 @Inject constructor(
    val fibM1: Fib196,
    val fibM2: Fib195,
)

@Binding
class Fib198 @Inject constructor(
    val fibM1: Fib197,
    val fibM2: Fib196,
)

@Binding
class Fib199 @Inject constructor(
    val fibM1: Fib198,
    val fibM2: Fib197,
)

@Binding
class Fib200 @Inject constructor(
    val fibM1: Fib199,
    val fibM2: Fib198,
)

@Binding
class Fib201 @Inject constructor(
    val fibM1: Fib200,
    val fibM2: Fib199,
)

@Binding
class Fib202 @Inject constructor(
    val fibM1: Fib201,
    val fibM2: Fib200,
)

@Binding
class Fib203 @Inject constructor(
    val fibM1: Fib202,
    val fibM2: Fib201,
)

@Binding
class Fib204 @Inject constructor(
    val fibM1: Fib203,
    val fibM2: Fib202,
)

@Binding
class Fib205 @Inject constructor(
    val fibM1: Fib204,
    val fibM2: Fib203,
)

@Binding
class Fib206 @Inject constructor(
    val fibM1: Fib205,
    val fibM2: Fib204,
)

@Binding
class Fib207 @Inject constructor(
    val fibM1: Fib206,
    val fibM2: Fib205,
)

@Binding
class Fib208 @Inject constructor(
    val fibM1: Fib207,
    val fibM2: Fib206,
)

@Binding
class Fib209 @Inject constructor(
    val fibM1: Fib208,
    val fibM2: Fib207,
)

@Binding
class Fib210 @Inject constructor(
    val fibM1: Fib209,
    val fibM2: Fib208,
)

@Binding
class Fib211 @Inject constructor(
    val fibM1: Fib210,
    val fibM2: Fib209,
)

@Binding
class Fib212 @Inject constructor(
    val fibM1: Fib211,
    val fibM2: Fib210,
)

@Binding
class Fib213 @Inject constructor(
    val fibM1: Fib212,
    val fibM2: Fib211,
)

@Binding
class Fib214 @Inject constructor(
    val fibM1: Fib213,
    val fibM2: Fib212,
)

@Binding
class Fib215 @Inject constructor(
    val fibM1: Fib214,
    val fibM2: Fib213,
)

@Binding
class Fib216 @Inject constructor(
    val fibM1: Fib215,
    val fibM2: Fib214,
)

@Binding
class Fib217 @Inject constructor(
    val fibM1: Fib216,
    val fibM2: Fib215,
)

@Binding
class Fib218 @Inject constructor(
    val fibM1: Fib217,
    val fibM2: Fib216,
)

@Binding
class Fib219 @Inject constructor(
    val fibM1: Fib218,
    val fibM2: Fib217,
)

@Binding
class Fib220 @Inject constructor(
    val fibM1: Fib219,
    val fibM2: Fib218,
)

@Binding
class Fib221 @Inject constructor(
    val fibM1: Fib220,
    val fibM2: Fib219,
)

@Binding
class Fib222 @Inject constructor(
    val fibM1: Fib221,
    val fibM2: Fib220,
)

@Binding
class Fib223 @Inject constructor(
    val fibM1: Fib222,
    val fibM2: Fib221,
)

@Binding
class Fib224 @Inject constructor(
    val fibM1: Fib223,
    val fibM2: Fib222,
)

@Binding
class Fib225 @Inject constructor(
    val fibM1: Fib224,
    val fibM2: Fib223,
)

@Binding
class Fib226 @Inject constructor(
    val fibM1: Fib225,
    val fibM2: Fib224,
)

@Binding
class Fib227 @Inject constructor(
    val fibM1: Fib226,
    val fibM2: Fib225,
)

@Binding
class Fib228 @Inject constructor(
    val fibM1: Fib227,
    val fibM2: Fib226,
)

@Binding
class Fib229 @Inject constructor(
    val fibM1: Fib228,
    val fibM2: Fib227,
)

@Binding
class Fib230 @Inject constructor(
    val fibM1: Fib229,
    val fibM2: Fib228,
)

@Binding
class Fib231 @Inject constructor(
    val fibM1: Fib230,
    val fibM2: Fib229,
)

@Binding
class Fib232 @Inject constructor(
    val fibM1: Fib231,
    val fibM2: Fib230,
)

@Binding
class Fib233 @Inject constructor(
    val fibM1: Fib232,
    val fibM2: Fib231,
)

@Binding
class Fib234 @Inject constructor(
    val fibM1: Fib233,
    val fibM2: Fib232,
)

@Binding
class Fib235 @Inject constructor(
    val fibM1: Fib234,
    val fibM2: Fib233,
)

@Binding
class Fib236 @Inject constructor(
    val fibM1: Fib235,
    val fibM2: Fib234,
)

@Binding
class Fib237 @Inject constructor(
    val fibM1: Fib236,
    val fibM2: Fib235,
)

@Binding
class Fib238 @Inject constructor(
    val fibM1: Fib237,
    val fibM2: Fib236,
)

@Binding
class Fib239 @Inject constructor(
    val fibM1: Fib238,
    val fibM2: Fib237,
)

@Binding
class Fib240 @Inject constructor(
    val fibM1: Fib239,
    val fibM2: Fib238,
)

@Binding
class Fib241 @Inject constructor(
    val fibM1: Fib240,
    val fibM2: Fib239,
)

@Binding
class Fib242 @Inject constructor(
    val fibM1: Fib241,
    val fibM2: Fib240,
)

@Binding
class Fib243 @Inject constructor(
    val fibM1: Fib242,
    val fibM2: Fib241,
)

@Binding
class Fib244 @Inject constructor(
    val fibM1: Fib243,
    val fibM2: Fib242,
)

@Binding
class Fib245 @Inject constructor(
    val fibM1: Fib244,
    val fibM2: Fib243,
)

@Binding
class Fib246 @Inject constructor(
    val fibM1: Fib245,
    val fibM2: Fib244,
)

@Binding
class Fib247 @Inject constructor(
    val fibM1: Fib246,
    val fibM2: Fib245,
)

@Binding
class Fib248 @Inject constructor(
    val fibM1: Fib247,
    val fibM2: Fib246,
)

@Binding
class Fib249 @Inject constructor(
    val fibM1: Fib248,
    val fibM2: Fib247,
)

@Binding
class Fib250 @Inject constructor(
    val fibM1: Fib249,
    val fibM2: Fib248,
)

@Binding
class Fib251 @Inject constructor(
    val fibM1: Fib250,
    val fibM2: Fib249,
)

@Binding
class Fib252 @Inject constructor(
    val fibM1: Fib251,
    val fibM2: Fib250,
)

@Binding
class Fib253 @Inject constructor(
    val fibM1: Fib252,
    val fibM2: Fib251,
)

@Binding
class Fib254 @Inject constructor(
    val fibM1: Fib253,
    val fibM2: Fib252,
)

@Binding
class Fib255 @Inject constructor(
    val fibM1: Fib254,
    val fibM2: Fib253,
)

@Binding
class Fib256 @Inject constructor(
    val fibM1: Fib255,
    val fibM2: Fib254,
)

@Binding
class Fib257 @Inject constructor(
    val fibM1: Fib256,
    val fibM2: Fib255,
)

@Binding
class Fib258 @Inject constructor(
    val fibM1: Fib257,
    val fibM2: Fib256,
)

@Binding
class Fib259 @Inject constructor(
    val fibM1: Fib258,
    val fibM2: Fib257,
)

@Binding
class Fib260 @Inject constructor(
    val fibM1: Fib259,
    val fibM2: Fib258,
)

@Binding
class Fib261 @Inject constructor(
    val fibM1: Fib260,
    val fibM2: Fib259,
)

@Binding
class Fib262 @Inject constructor(
    val fibM1: Fib261,
    val fibM2: Fib260,
)

@Binding
class Fib263 @Inject constructor(
    val fibM1: Fib262,
    val fibM2: Fib261,
)

@Binding
class Fib264 @Inject constructor(
    val fibM1: Fib263,
    val fibM2: Fib262,
)

@Binding
class Fib265 @Inject constructor(
    val fibM1: Fib264,
    val fibM2: Fib263,
)

@Binding
class Fib266 @Inject constructor(
    val fibM1: Fib265,
    val fibM2: Fib264,
)

@Binding
class Fib267 @Inject constructor(
    val fibM1: Fib266,
    val fibM2: Fib265,
)

@Binding
class Fib268 @Inject constructor(
    val fibM1: Fib267,
    val fibM2: Fib266,
)

@Binding
class Fib269 @Inject constructor(
    val fibM1: Fib268,
    val fibM2: Fib267,
)

@Binding
class Fib270 @Inject constructor(
    val fibM1: Fib269,
    val fibM2: Fib268,
)

@Binding
class Fib271 @Inject constructor(
    val fibM1: Fib270,
    val fibM2: Fib269,
)

@Binding
class Fib272 @Inject constructor(
    val fibM1: Fib271,
    val fibM2: Fib270,
)

@Binding
class Fib273 @Inject constructor(
    val fibM1: Fib272,
    val fibM2: Fib271,
)

@Binding
class Fib274 @Inject constructor(
    val fibM1: Fib273,
    val fibM2: Fib272,
)

@Binding
class Fib275 @Inject constructor(
    val fibM1: Fib274,
    val fibM2: Fib273,
)

@Binding
class Fib276 @Inject constructor(
    val fibM1: Fib275,
    val fibM2: Fib274,
)

@Binding
class Fib277 @Inject constructor(
    val fibM1: Fib276,
    val fibM2: Fib275,
)

@Binding
class Fib278 @Inject constructor(
    val fibM1: Fib277,
    val fibM2: Fib276,
)

@Binding
class Fib279 @Inject constructor(
    val fibM1: Fib278,
    val fibM2: Fib277,
)

@Binding
class Fib280 @Inject constructor(
    val fibM1: Fib279,
    val fibM2: Fib278,
)

@Binding
class Fib281 @Inject constructor(
    val fibM1: Fib280,
    val fibM2: Fib279,
)

@Binding
class Fib282 @Inject constructor(
    val fibM1: Fib281,
    val fibM2: Fib280,
)

@Binding
class Fib283 @Inject constructor(
    val fibM1: Fib282,
    val fibM2: Fib281,
)

@Binding
class Fib284 @Inject constructor(
    val fibM1: Fib283,
    val fibM2: Fib282,
)

@Binding
class Fib285 @Inject constructor(
    val fibM1: Fib284,
    val fibM2: Fib283,
)

@Binding
class Fib286 @Inject constructor(
    val fibM1: Fib285,
    val fibM2: Fib284,
)

@Binding
class Fib287 @Inject constructor(
    val fibM1: Fib286,
    val fibM2: Fib285,
)

@Binding
class Fib288 @Inject constructor(
    val fibM1: Fib287,
    val fibM2: Fib286,
)

@Binding
class Fib289 @Inject constructor(
    val fibM1: Fib288,
    val fibM2: Fib287,
)

@Binding
class Fib290 @Inject constructor(
    val fibM1: Fib289,
    val fibM2: Fib288,
)

@Binding
class Fib291 @Inject constructor(
    val fibM1: Fib290,
    val fibM2: Fib289,
)

@Binding
class Fib292 @Inject constructor(
    val fibM1: Fib291,
    val fibM2: Fib290,
)

@Binding
class Fib293 @Inject constructor(
    val fibM1: Fib292,
    val fibM2: Fib291,
)

@Binding
class Fib294 @Inject constructor(
    val fibM1: Fib293,
    val fibM2: Fib292,
)

@Binding
class Fib295 @Inject constructor(
    val fibM1: Fib294,
    val fibM2: Fib293,
)

@Binding
class Fib296 @Inject constructor(
    val fibM1: Fib295,
    val fibM2: Fib294,
)

@Binding
class Fib297 @Inject constructor(
    val fibM1: Fib296,
    val fibM2: Fib295,
)

@Binding
class Fib298 @Inject constructor(
    val fibM1: Fib297,
    val fibM2: Fib296,
)

@Binding
class Fib299 @Inject constructor(
    val fibM1: Fib298,
    val fibM2: Fib297,
)

@Binding
class Fib300 @Inject constructor(
    val fibM1: Fib299,
    val fibM2: Fib298,
)

@Binding
class Fib301 @Inject constructor(
    val fibM1: Fib300,
    val fibM2: Fib299,
)

@Binding
class Fib302 @Inject constructor(
    val fibM1: Fib301,
    val fibM2: Fib300,
)

@Binding
class Fib303 @Inject constructor(
    val fibM1: Fib302,
    val fibM2: Fib301,
)

@Binding
class Fib304 @Inject constructor(
    val fibM1: Fib303,
    val fibM2: Fib302,
)

@Binding
class Fib305 @Inject constructor(
    val fibM1: Fib304,
    val fibM2: Fib303,
)

@Binding
class Fib306 @Inject constructor(
    val fibM1: Fib305,
    val fibM2: Fib304,
)

@Binding
class Fib307 @Inject constructor(
    val fibM1: Fib306,
    val fibM2: Fib305,
)

@Binding
class Fib308 @Inject constructor(
    val fibM1: Fib307,
    val fibM2: Fib306,
)

@Binding
class Fib309 @Inject constructor(
    val fibM1: Fib308,
    val fibM2: Fib307,
)

@Binding
class Fib310 @Inject constructor(
    val fibM1: Fib309,
    val fibM2: Fib308,
)

@Binding
class Fib311 @Inject constructor(
    val fibM1: Fib310,
    val fibM2: Fib309,
)

@Binding
class Fib312 @Inject constructor(
    val fibM1: Fib311,
    val fibM2: Fib310,
)

@Binding
class Fib313 @Inject constructor(
    val fibM1: Fib312,
    val fibM2: Fib311,
)

@Binding
class Fib314 @Inject constructor(
    val fibM1: Fib313,
    val fibM2: Fib312,
)

@Binding
class Fib315 @Inject constructor(
    val fibM1: Fib314,
    val fibM2: Fib313,
)

@Binding
class Fib316 @Inject constructor(
    val fibM1: Fib315,
    val fibM2: Fib314,
)

@Binding
class Fib317 @Inject constructor(
    val fibM1: Fib316,
    val fibM2: Fib315,
)

@Binding
class Fib318 @Inject constructor(
    val fibM1: Fib317,
    val fibM2: Fib316,
)

@Binding
class Fib319 @Inject constructor(
    val fibM1: Fib318,
    val fibM2: Fib317,
)

@Binding
class Fib320 @Inject constructor(
    val fibM1: Fib319,
    val fibM2: Fib318,
)

@Binding
class Fib321 @Inject constructor(
    val fibM1: Fib320,
    val fibM2: Fib319,
)

@Binding
class Fib322 @Inject constructor(
    val fibM1: Fib321,
    val fibM2: Fib320,
)

@Binding
class Fib323 @Inject constructor(
    val fibM1: Fib322,
    val fibM2: Fib321,
)

@Binding
class Fib324 @Inject constructor(
    val fibM1: Fib323,
    val fibM2: Fib322,
)

@Binding
class Fib325 @Inject constructor(
    val fibM1: Fib324,
    val fibM2: Fib323,
)

@Binding
class Fib326 @Inject constructor(
    val fibM1: Fib325,
    val fibM2: Fib324,
)

@Binding
class Fib327 @Inject constructor(
    val fibM1: Fib326,
    val fibM2: Fib325,
)

@Binding
class Fib328 @Inject constructor(
    val fibM1: Fib327,
    val fibM2: Fib326,
)

@Binding
class Fib329 @Inject constructor(
    val fibM1: Fib328,
    val fibM2: Fib327,
)

@Binding
class Fib330 @Inject constructor(
    val fibM1: Fib329,
    val fibM2: Fib328,
)

@Binding
class Fib331 @Inject constructor(
    val fibM1: Fib330,
    val fibM2: Fib329,
)

@Binding
class Fib332 @Inject constructor(
    val fibM1: Fib331,
    val fibM2: Fib330,
)

@Binding
class Fib333 @Inject constructor(
    val fibM1: Fib332,
    val fibM2: Fib331,
)

@Binding
class Fib334 @Inject constructor(
    val fibM1: Fib333,
    val fibM2: Fib332,
)

@Binding
class Fib335 @Inject constructor(
    val fibM1: Fib334,
    val fibM2: Fib333,
)

@Binding
class Fib336 @Inject constructor(
    val fibM1: Fib335,
    val fibM2: Fib334,
)

@Binding
class Fib337 @Inject constructor(
    val fibM1: Fib336,
    val fibM2: Fib335,
)

@Binding
class Fib338 @Inject constructor(
    val fibM1: Fib337,
    val fibM2: Fib336,
)

@Binding
class Fib339 @Inject constructor(
    val fibM1: Fib338,
    val fibM2: Fib337,
)

@Binding
class Fib340 @Inject constructor(
    val fibM1: Fib339,
    val fibM2: Fib338,
)

@Binding
class Fib341 @Inject constructor(
    val fibM1: Fib340,
    val fibM2: Fib339,
)

@Binding
class Fib342 @Inject constructor(
    val fibM1: Fib341,
    val fibM2: Fib340,
)

@Binding
class Fib343 @Inject constructor(
    val fibM1: Fib342,
    val fibM2: Fib341,
)

@Binding
class Fib344 @Inject constructor(
    val fibM1: Fib343,
    val fibM2: Fib342,
)

@Binding
class Fib345 @Inject constructor(
    val fibM1: Fib344,
    val fibM2: Fib343,
)

@Binding
class Fib346 @Inject constructor(
    val fibM1: Fib345,
    val fibM2: Fib344,
)

@Binding
class Fib347 @Inject constructor(
    val fibM1: Fib346,
    val fibM2: Fib345,
)

@Binding
class Fib348 @Inject constructor(
    val fibM1: Fib347,
    val fibM2: Fib346,
)

@Binding
class Fib349 @Inject constructor(
    val fibM1: Fib348,
    val fibM2: Fib347,
)

@Binding
class Fib350 @Inject constructor(
    val fibM1: Fib349,
    val fibM2: Fib348,
)

@Binding
class Fib351 @Inject constructor(
    val fibM1: Fib350,
    val fibM2: Fib349,
)

@Binding
class Fib352 @Inject constructor(
    val fibM1: Fib351,
    val fibM2: Fib350,
)

@Binding
class Fib353 @Inject constructor(
    val fibM1: Fib352,
    val fibM2: Fib351,
)

@Binding
class Fib354 @Inject constructor(
    val fibM1: Fib353,
    val fibM2: Fib352,
)

@Binding
class Fib355 @Inject constructor(
    val fibM1: Fib354,
    val fibM2: Fib353,
)

@Binding
class Fib356 @Inject constructor(
    val fibM1: Fib355,
    val fibM2: Fib354,
)

@Binding
class Fib357 @Inject constructor(
    val fibM1: Fib356,
    val fibM2: Fib355,
)

@Binding
class Fib358 @Inject constructor(
    val fibM1: Fib357,
    val fibM2: Fib356,
)

@Binding
class Fib359 @Inject constructor(
    val fibM1: Fib358,
    val fibM2: Fib357,
)

@Binding
class Fib360 @Inject constructor(
    val fibM1: Fib359,
    val fibM2: Fib358,
)

@Binding
class Fib361 @Inject constructor(
    val fibM1: Fib360,
    val fibM2: Fib359,
)

@Binding
class Fib362 @Inject constructor(
    val fibM1: Fib361,
    val fibM2: Fib360,
)

@Binding
class Fib363 @Inject constructor(
    val fibM1: Fib362,
    val fibM2: Fib361,
)

@Binding
class Fib364 @Inject constructor(
    val fibM1: Fib363,
    val fibM2: Fib362,
)

@Binding
class Fib365 @Inject constructor(
    val fibM1: Fib364,
    val fibM2: Fib363,
)

@Binding
class Fib366 @Inject constructor(
    val fibM1: Fib365,
    val fibM2: Fib364,
)

@Binding
class Fib367 @Inject constructor(
    val fibM1: Fib366,
    val fibM2: Fib365,
)

@Binding
class Fib368 @Inject constructor(
    val fibM1: Fib367,
    val fibM2: Fib366,
)

@Binding
class Fib369 @Inject constructor(
    val fibM1: Fib368,
    val fibM2: Fib367,
)

@Binding
class Fib370 @Inject constructor(
    val fibM1: Fib369,
    val fibM2: Fib368,
)

@Binding
class Fib371 @Inject constructor(
    val fibM1: Fib370,
    val fibM2: Fib369,
)

@Binding
class Fib372 @Inject constructor(
    val fibM1: Fib371,
    val fibM2: Fib370,
)

@Binding
class Fib373 @Inject constructor(
    val fibM1: Fib372,
    val fibM2: Fib371,
)

@Binding
class Fib374 @Inject constructor(
    val fibM1: Fib373,
    val fibM2: Fib372,
)

@Binding
class Fib375 @Inject constructor(
    val fibM1: Fib374,
    val fibM2: Fib373,
)

@Binding
class Fib376 @Inject constructor(
    val fibM1: Fib375,
    val fibM2: Fib374,
)

@Binding
class Fib377 @Inject constructor(
    val fibM1: Fib376,
    val fibM2: Fib375,
)

@Binding
class Fib378 @Inject constructor(
    val fibM1: Fib377,
    val fibM2: Fib376,
)

@Binding
class Fib379 @Inject constructor(
    val fibM1: Fib378,
    val fibM2: Fib377,
)

@Binding
class Fib380 @Inject constructor(
    val fibM1: Fib379,
    val fibM2: Fib378,
)

@Binding
class Fib381 @Inject constructor(
    val fibM1: Fib380,
    val fibM2: Fib379,
)

@Binding
class Fib382 @Inject constructor(
    val fibM1: Fib381,
    val fibM2: Fib380,
)

@Binding
class Fib383 @Inject constructor(
    val fibM1: Fib382,
    val fibM2: Fib381,
)

@Binding
class Fib384 @Inject constructor(
    val fibM1: Fib383,
    val fibM2: Fib382,
)

@Binding
class Fib385 @Inject constructor(
    val fibM1: Fib384,
    val fibM2: Fib383,
)

@Binding
class Fib386 @Inject constructor(
    val fibM1: Fib385,
    val fibM2: Fib384,
)

@Binding
class Fib387 @Inject constructor(
    val fibM1: Fib386,
    val fibM2: Fib385,
)

@Binding
class Fib388 @Inject constructor(
    val fibM1: Fib387,
    val fibM2: Fib386,
)

@Binding
class Fib389 @Inject constructor(
    val fibM1: Fib388,
    val fibM2: Fib387,
)

@Binding
class Fib390 @Inject constructor(
    val fibM1: Fib389,
    val fibM2: Fib388,
)

@Binding
class Fib391 @Inject constructor(
    val fibM1: Fib390,
    val fibM2: Fib389,
)

@Binding
class Fib392 @Inject constructor(
    val fibM1: Fib391,
    val fibM2: Fib390,
)

@Binding
class Fib393 @Inject constructor(
    val fibM1: Fib392,
    val fibM2: Fib391,
)

@Binding
class Fib394 @Inject constructor(
    val fibM1: Fib393,
    val fibM2: Fib392,
)

@Binding
class Fib395 @Inject constructor(
    val fibM1: Fib394,
    val fibM2: Fib393,
)

@Binding
class Fib396 @Inject constructor(
    val fibM1: Fib395,
    val fibM2: Fib394,
)

@Binding
class Fib397 @Inject constructor(
    val fibM1: Fib396,
    val fibM2: Fib395,
)

@Binding
class Fib398 @Inject constructor(
    val fibM1: Fib397,
    val fibM2: Fib396,
)

@Binding
class Fib399 @Inject constructor(
    val fibM1: Fib398,
    val fibM2: Fib397,
)

@Binding
class Fib400 @Inject constructor(
    val fibM1: Fib399,
    val fibM2: Fib398
)
