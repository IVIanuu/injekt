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
import javax.inject.Inject

@Factory
class Fib1 @Inject constructor()

@Factory
class Fib2 @Inject constructor()

@Factory
class Fib3 @Inject constructor(
    val fibM1: Fib2,
    val fibM2: Fib1
)

@Factory
class Fib4 @Inject constructor(
    val fibM1: Fib3,
    val fibM2: Fib2
)

@Factory
class Fib5 @Inject constructor(
    val fibM1: Fib4,
    val fibM2: Fib3
)

@Factory
class Fib6 @Inject constructor(
    val fibM1: Fib5,
    val fibM2: Fib4
)

@Factory
class Fib7 @Inject constructor(
    val fibM1: Fib6,
    val fibM2: Fib5
)

@Factory
class Fib8 @Inject constructor(
    val fibM1: Fib7,
    val fibM2: Fib6
)

@Factory
class Fib9 @Inject constructor(
    val fibM1: Fib8,
    val fibM2: Fib7
)

@Factory
class Fib10 @Inject constructor(
    val fibM1: Fib9,
    val fibM2: Fib8
)

@Factory
class Fib11 @Inject constructor(
    val fibM1: Fib10,
    val fibM2: Fib9
)

@Factory
class Fib12 @Inject constructor(
    val fibM1: Fib11,
    val fibM2: Fib10
)

@Factory
class Fib13 @Inject constructor(
    val fibM1: Fib12,
    val fibM2: Fib11
)

@Factory
class Fib14 @Inject constructor(
    val fibM1: Fib13,
    val fibM2: Fib12
)

@Factory
class Fib15 @Inject constructor(
    val fibM1: Fib14,
    val fibM2: Fib13
)

@Factory
class Fib16 @Inject constructor(
    val fibM1: Fib15,
    val fibM2: Fib14
)

@Factory
class Fib17 @Inject constructor(
    val fibM1: Fib16,
    val fibM2: Fib15
)

@Factory
class Fib18 @Inject constructor(
    val fibM1: Fib17,
    val fibM2: Fib16
)

@Factory
class Fib19 @Inject constructor(
    val fibM1: Fib18,
    val fibM2: Fib17
)

@Factory
class Fib20 @Inject constructor(
    val fibM1: Fib19,
    val fibM2: Fib18
)

@Factory
class Fib21 @Inject constructor(
    val fibM1: Fib20,
    val fibM2: Fib19
)

@Factory
class Fib22 @Inject constructor(
    val fibM1: Fib21,
    val fibM2: Fib20
)

@Factory
class Fib23 @Inject constructor(
    val fibM1: Fib22,
    val fibM2: Fib21
)

@Factory
class Fib24 @Inject constructor(
    val fibM1: Fib23,
    val fibM2: Fib22
)

@Factory
class Fib25 @Inject constructor(
    val fibM1: Fib24,
    val fibM2: Fib23
)

@Factory
class Fib26 @Inject constructor(
    val fibM1: Fib25,
    val fibM2: Fib24
)

@Factory
class Fib27 @Inject constructor(
    val fibM1: Fib26,
    val fibM2: Fib25
)

@Factory
class Fib28 @Inject constructor(
    val fibM1: Fib27,
    val fibM2: Fib26
)

@Factory
class Fib29 @Inject constructor(
    val fibM1: Fib28,
    val fibM2: Fib27
)

@Factory
class Fib30 @Inject constructor(
    val fibM1: Fib29,
    val fibM2: Fib28
)

@Factory
class Fib31 @Inject constructor(
    val fibM1: Fib30,
    val fibM2: Fib29
)

@Factory
class Fib32 @Inject constructor(
    val fibM1: Fib31,
    val fibM2: Fib30
)

@Factory
class Fib33 @Inject constructor(
    val fibM1: Fib32,
    val fibM2: Fib31
)

@Factory
class Fib34 @Inject constructor(
    val fibM1: Fib33,
    val fibM2: Fib32
)

@Factory
class Fib35 @Inject constructor(
    val fibM1: Fib34,
    val fibM2: Fib33
)

@Factory
class Fib36 @Inject constructor(
    val fibM1: Fib35,
    val fibM2: Fib34
)

@Factory
class Fib37 @Inject constructor(
    val fibM1: Fib36,
    val fibM2: Fib35
)

@Factory
class Fib38 @Inject constructor(
    val fibM1: Fib37,
    val fibM2: Fib36
)

@Factory
class Fib39 @Inject constructor(
    val fibM1: Fib38,
    val fibM2: Fib37
)

@Factory
class Fib40 @Inject constructor(
    val fibM1: Fib39,
    val fibM2: Fib38
)

@Factory
class Fib41 @Inject constructor(
    val fibM1: Fib40,
    val fibM2: Fib39
)

@Factory
class Fib42 @Inject constructor(
    val fibM1: Fib41,
    val fibM2: Fib40
)

@Factory
class Fib43 @Inject constructor(
    val fibM1: Fib42,
    val fibM2: Fib41
)

@Factory
class Fib44 @Inject constructor(
    val fibM1: Fib43,
    val fibM2: Fib42
)

@Factory
class Fib45 @Inject constructor(
    val fibM1: Fib44,
    val fibM2: Fib43
)

@Factory
class Fib46 @Inject constructor(
    val fibM1: Fib45,
    val fibM2: Fib44
)

@Factory
class Fib47 @Inject constructor(
    val fibM1: Fib46,
    val fibM2: Fib45
)

@Factory
class Fib48 @Inject constructor(
    val fibM1: Fib47,
    val fibM2: Fib46
)

@Factory
class Fib49 @Inject constructor(
    val fibM1: Fib48,
    val fibM2: Fib47
)

@Factory
class Fib50 @Inject constructor(
    val fibM1: Fib49,
    val fibM2: Fib48
)

@Factory
class Fib51 @Inject constructor(
    val fibM1: Fib50,
    val fibM2: Fib49
)

@Factory
class Fib52 @Inject constructor(
    val fibM1: Fib51,
    val fibM2: Fib50
)

@Factory
class Fib53 @Inject constructor(
    val fibM1: Fib52,
    val fibM2: Fib51
)

@Factory
class Fib54 @Inject constructor(
    val fibM1: Fib53,
    val fibM2: Fib52
)

@Factory
class Fib55 @Inject constructor(
    val fibM1: Fib54,
    val fibM2: Fib53
)

@Factory
class Fib56 @Inject constructor(
    val fibM1: Fib55,
    val fibM2: Fib54
)

@Factory
class Fib57 @Inject constructor(
    val fibM1: Fib56,
    val fibM2: Fib55
)

@Factory
class Fib58 @Inject constructor(
    val fibM1: Fib57,
    val fibM2: Fib56
)

@Factory
class Fib59 @Inject constructor(
    val fibM1: Fib58,
    val fibM2: Fib57
)

@Factory
class Fib60 @Inject constructor(
    val fibM1: Fib59,
    val fibM2: Fib58
)

@Factory
class Fib61 @Inject constructor(
    val fibM1: Fib60,
    val fibM2: Fib59
)

@Factory
class Fib62 @Inject constructor(
    val fibM1: Fib61,
    val fibM2: Fib60
)

@Factory
class Fib63 @Inject constructor(
    val fibM1: Fib62,
    val fibM2: Fib61
)

@Factory
class Fib64 @Inject constructor(
    val fibM1: Fib63,
    val fibM2: Fib62
)

@Factory
class Fib65 @Inject constructor(
    val fibM1: Fib64,
    val fibM2: Fib63
)

@Factory
class Fib66 @Inject constructor(
    val fibM1: Fib65,
    val fibM2: Fib64
)

@Factory
class Fib67 @Inject constructor(
    val fibM1: Fib66,
    val fibM2: Fib65
)

@Factory
class Fib68 @Inject constructor(
    val fibM1: Fib67,
    val fibM2: Fib66
)

@Factory
class Fib69 @Inject constructor(
    val fibM1: Fib68,
    val fibM2: Fib67
)

@Factory
class Fib70 @Inject constructor(
    val fibM1: Fib69,
    val fibM2: Fib68
)

@Factory
class Fib71 @Inject constructor(
    val fibM1: Fib70,
    val fibM2: Fib69
)

@Factory
class Fib72 @Inject constructor(
    val fibM1: Fib71,
    val fibM2: Fib70
)

@Factory
class Fib73 @Inject constructor(
    val fibM1: Fib72,
    val fibM2: Fib71
)

@Factory
class Fib74 @Inject constructor(
    val fibM1: Fib73,
    val fibM2: Fib72
)

@Factory
class Fib75 @Inject constructor(
    val fibM1: Fib74,
    val fibM2: Fib73
)

@Factory
class Fib76 @Inject constructor(
    val fibM1: Fib75,
    val fibM2: Fib74
)

@Factory
class Fib77 @Inject constructor(
    val fibM1: Fib76,
    val fibM2: Fib75
)

@Factory
class Fib78 @Inject constructor(
    val fibM1: Fib77,
    val fibM2: Fib76
)

@Factory
class Fib79 @Inject constructor(
    val fibM1: Fib78,
    val fibM2: Fib77
)

@Factory
class Fib80 @Inject constructor(
    val fibM1: Fib79,
    val fibM2: Fib78
)

@Factory
class Fib81 @Inject constructor(
    val fibM1: Fib80,
    val fibM2: Fib79
)

@Factory
class Fib82 @Inject constructor(
    val fibM1: Fib81,
    val fibM2: Fib80
)

@Factory
class Fib83 @Inject constructor(
    val fibM1: Fib82,
    val fibM2: Fib81
)

@Factory
class Fib84 @Inject constructor(
    val fibM1: Fib83,
    val fibM2: Fib82
)

@Factory
class Fib85 @Inject constructor(
    val fibM1: Fib84,
    val fibM2: Fib83
)

@Factory
class Fib86 @Inject constructor(
    val fibM1: Fib85,
    val fibM2: Fib84
)

@Factory
class Fib87 @Inject constructor(
    val fibM1: Fib86,
    val fibM2: Fib85
)

@Factory
class Fib88 @Inject constructor(
    val fibM1: Fib87,
    val fibM2: Fib86
)

@Factory
class Fib89 @Inject constructor(
    val fibM1: Fib88,
    val fibM2: Fib87
)

@Factory
class Fib90 @Inject constructor(
    val fibM1: Fib89,
    val fibM2: Fib88
)

@Factory
class Fib91 @Inject constructor(
    val fibM1: Fib90,
    val fibM2: Fib89
)

@Factory
class Fib92 @Inject constructor(
    val fibM1: Fib91,
    val fibM2: Fib90
)

@Factory
class Fib93 @Inject constructor(
    val fibM1: Fib92,
    val fibM2: Fib91
)

@Factory
class Fib94 @Inject constructor(
    val fibM1: Fib93,
    val fibM2: Fib92
)

@Factory
class Fib95 @Inject constructor(
    val fibM1: Fib94,
    val fibM2: Fib93
)

@Factory
class Fib96 @Inject constructor(
    val fibM1: Fib95,
    val fibM2: Fib94
)

@Factory
class Fib97 @Inject constructor(
    val fibM1: Fib96,
    val fibM2: Fib95
)

@Factory
class Fib98 @Inject constructor(
    val fibM1: Fib97,
    val fibM2: Fib96
)

@Factory
class Fib99 @Inject constructor(
    val fibM1: Fib98,
    val fibM2: Fib97
)

@Factory
class Fib100 @Inject constructor(
    val fibM1: Fib99,
    val fibM2: Fib98
)

@Factory
class Fib101 @Inject constructor(
    val fibM1: Fib100,
    val fibM2: Fib99
)

@Factory
class Fib102 @Inject constructor(
    val fibM1: Fib101,
    val fibM2: Fib100
)

@Factory
class Fib103 @Inject constructor(
    val fibM1: Fib102,
    val fibM2: Fib101
)

@Factory
class Fib104 @Inject constructor(
    val fibM1: Fib103,
    val fibM2: Fib102
)

@Factory
class Fib105 @Inject constructor(
    val fibM1: Fib104,
    val fibM2: Fib103
)

@Factory
class Fib106 @Inject constructor(
    val fibM1: Fib105,
    val fibM2: Fib104
)

@Factory
class Fib107 @Inject constructor(
    val fibM1: Fib106,
    val fibM2: Fib105
)

@Factory
class Fib108 @Inject constructor(
    val fibM1: Fib107,
    val fibM2: Fib106
)

@Factory
class Fib109 @Inject constructor(
    val fibM1: Fib108,
    val fibM2: Fib107
)

@Factory
class Fib110 @Inject constructor(
    val fibM1: Fib109,
    val fibM2: Fib108
)

@Factory
class Fib111 @Inject constructor(
    val fibM1: Fib110,
    val fibM2: Fib109
)

@Factory
class Fib112 @Inject constructor(
    val fibM1: Fib111,
    val fibM2: Fib110
)

@Factory
class Fib113 @Inject constructor(
    val fibM1: Fib112,
    val fibM2: Fib111
)

@Factory
class Fib114 @Inject constructor(
    val fibM1: Fib113,
    val fibM2: Fib112
)

@Factory
class Fib115 @Inject constructor(
    val fibM1: Fib114,
    val fibM2: Fib113
)

@Factory
class Fib116 @Inject constructor(
    val fibM1: Fib115,
    val fibM2: Fib114
)

@Factory
class Fib117 @Inject constructor(
    val fibM1: Fib116,
    val fibM2: Fib115
)

@Factory
class Fib118 @Inject constructor(
    val fibM1: Fib117,
    val fibM2: Fib116
)

@Factory
class Fib119 @Inject constructor(
    val fibM1: Fib118,
    val fibM2: Fib117
)

@Factory
class Fib120 @Inject constructor(
    val fibM1: Fib119,
    val fibM2: Fib118
)

@Factory
class Fib121 @Inject constructor(
    val fibM1: Fib120,
    val fibM2: Fib119
)

@Factory
class Fib122 @Inject constructor(
    val fibM1: Fib121,
    val fibM2: Fib120
)

@Factory
class Fib123 @Inject constructor(
    val fibM1: Fib122,
    val fibM2: Fib121
)

@Factory
class Fib124 @Inject constructor(
    val fibM1: Fib123,
    val fibM2: Fib122
)

@Factory
class Fib125 @Inject constructor(
    val fibM1: Fib124,
    val fibM2: Fib123
)

@Factory
class Fib126 @Inject constructor(
    val fibM1: Fib125,
    val fibM2: Fib124
)

@Factory
class Fib127 @Inject constructor(
    val fibM1: Fib126,
    val fibM2: Fib125
)

@Factory
class Fib128 @Inject constructor(
    val fibM1: Fib127,
    val fibM2: Fib126
)

@Factory
class Fib129 @Inject constructor(
    val fibM1: Fib128,
    val fibM2: Fib127
)

@Factory
class Fib130 @Inject constructor(
    val fibM1: Fib129,
    val fibM2: Fib128
)

@Factory
class Fib131 @Inject constructor(
    val fibM1: Fib130,
    val fibM2: Fib129
)

@Factory
class Fib132 @Inject constructor(
    val fibM1: Fib131,
    val fibM2: Fib130
)

@Factory
class Fib133 @Inject constructor(
    val fibM1: Fib132,
    val fibM2: Fib131
)

@Factory
class Fib134 @Inject constructor(
    val fibM1: Fib133,
    val fibM2: Fib132
)

@Factory
class Fib135 @Inject constructor(
    val fibM1: Fib134,
    val fibM2: Fib133
)

@Factory
class Fib136 @Inject constructor(
    val fibM1: Fib135,
    val fibM2: Fib134
)

@Factory
class Fib137 @Inject constructor(
    val fibM1: Fib136,
    val fibM2: Fib135
)

@Factory
class Fib138 @Inject constructor(
    val fibM1: Fib137,
    val fibM2: Fib136
)

@Factory
class Fib139 @Inject constructor(
    val fibM1: Fib138,
    val fibM2: Fib137
)

@Factory
class Fib140 @Inject constructor(
    val fibM1: Fib139,
    val fibM2: Fib138
)

@Factory
class Fib141 @Inject constructor(
    val fibM1: Fib140,
    val fibM2: Fib139
)

@Factory
class Fib142 @Inject constructor(
    val fibM1: Fib141,
    val fibM2: Fib140
)

@Factory
class Fib143 @Inject constructor(
    val fibM1: Fib142,
    val fibM2: Fib141
)

@Factory
class Fib144 @Inject constructor(
    val fibM1: Fib143,
    val fibM2: Fib142
)

@Factory
class Fib145 @Inject constructor(
    val fibM1: Fib144,
    val fibM2: Fib143
)

@Factory
class Fib146 @Inject constructor(
    val fibM1: Fib145,
    val fibM2: Fib144
)

@Factory
class Fib147 @Inject constructor(
    val fibM1: Fib146,
    val fibM2: Fib145
)

@Factory
class Fib148 @Inject constructor(
    val fibM1: Fib147,
    val fibM2: Fib146
)

@Factory
class Fib149 @Inject constructor(
    val fibM1: Fib148,
    val fibM2: Fib147
)

@Factory
class Fib150 @Inject constructor(
    val fibM1: Fib149,
    val fibM2: Fib148
)

@Factory
class Fib151 @Inject constructor(
    val fibM1: Fib150,
    val fibM2: Fib149
)

@Factory
class Fib152 @Inject constructor(
    val fibM1: Fib151,
    val fibM2: Fib150
)

@Factory
class Fib153 @Inject constructor(
    val fibM1: Fib152,
    val fibM2: Fib151
)

@Factory
class Fib154 @Inject constructor(
    val fibM1: Fib153,
    val fibM2: Fib152
)

@Factory
class Fib155 @Inject constructor(
    val fibM1: Fib154,
    val fibM2: Fib153
)

@Factory
class Fib156 @Inject constructor(
    val fibM1: Fib155,
    val fibM2: Fib154
)

@Factory
class Fib157 @Inject constructor(
    val fibM1: Fib156,
    val fibM2: Fib155
)

@Factory
class Fib158 @Inject constructor(
    val fibM1: Fib157,
    val fibM2: Fib156
)

@Factory
class Fib159 @Inject constructor(
    val fibM1: Fib158,
    val fibM2: Fib157
)

@Factory
class Fib160 @Inject constructor(
    val fibM1: Fib159,
    val fibM2: Fib158
)

@Factory
class Fib161 @Inject constructor(
    val fibM1: Fib160,
    val fibM2: Fib159
)

@Factory
class Fib162 @Inject constructor(
    val fibM1: Fib161,
    val fibM2: Fib160
)

@Factory
class Fib163 @Inject constructor(
    val fibM1: Fib162,
    val fibM2: Fib161
)

@Factory
class Fib164 @Inject constructor(
    val fibM1: Fib163,
    val fibM2: Fib162
)

@Factory
class Fib165 @Inject constructor(
    val fibM1: Fib164,
    val fibM2: Fib163
)

@Factory
class Fib166 @Inject constructor(
    val fibM1: Fib165,
    val fibM2: Fib164
)

@Factory
class Fib167 @Inject constructor(
    val fibM1: Fib166,
    val fibM2: Fib165
)

@Factory
class Fib168 @Inject constructor(
    val fibM1: Fib167,
    val fibM2: Fib166
)

@Factory
class Fib169 @Inject constructor(
    val fibM1: Fib168,
    val fibM2: Fib167
)

@Factory
class Fib170 @Inject constructor(
    val fibM1: Fib169,
    val fibM2: Fib168
)

@Factory
class Fib171 @Inject constructor(
    val fibM1: Fib170,
    val fibM2: Fib169
)

@Factory
class Fib172 @Inject constructor(
    val fibM1: Fib171,
    val fibM2: Fib170
)

@Factory
class Fib173 @Inject constructor(
    val fibM1: Fib172,
    val fibM2: Fib171
)

@Factory
class Fib174 @Inject constructor(
    val fibM1: Fib173,
    val fibM2: Fib172
)

@Factory
class Fib175 @Inject constructor(
    val fibM1: Fib174,
    val fibM2: Fib173
)

@Factory
class Fib176 @Inject constructor(
    val fibM1: Fib175,
    val fibM2: Fib174
)

@Factory
class Fib177 @Inject constructor(
    val fibM1: Fib176,
    val fibM2: Fib175
)

@Factory
class Fib178 @Inject constructor(
    val fibM1: Fib177,
    val fibM2: Fib176
)

@Factory
class Fib179 @Inject constructor(
    val fibM1: Fib178,
    val fibM2: Fib177
)

@Factory
class Fib180 @Inject constructor(
    val fibM1: Fib179,
    val fibM2: Fib178
)

@Factory
class Fib181 @Inject constructor(
    val fibM1: Fib180,
    val fibM2: Fib179
)

@Factory
class Fib182 @Inject constructor(
    val fibM1: Fib181,
    val fibM2: Fib180
)

@Factory
class Fib183 @Inject constructor(
    val fibM1: Fib182,
    val fibM2: Fib181
)

@Factory
class Fib184 @Inject constructor(
    val fibM1: Fib183,
    val fibM2: Fib182
)

@Factory
class Fib185 @Inject constructor(
    val fibM1: Fib184,
    val fibM2: Fib183
)

@Factory
class Fib186 @Inject constructor(
    val fibM1: Fib185,
    val fibM2: Fib184
)

@Factory
class Fib187 @Inject constructor(
    val fibM1: Fib186,
    val fibM2: Fib185
)

@Factory
class Fib188 @Inject constructor(
    val fibM1: Fib187,
    val fibM2: Fib186
)

@Factory
class Fib189 @Inject constructor(
    val fibM1: Fib188,
    val fibM2: Fib187
)

@Factory
class Fib190 @Inject constructor(
    val fibM1: Fib189,
    val fibM2: Fib188
)

@Factory
class Fib191 @Inject constructor(
    val fibM1: Fib190,
    val fibM2: Fib189
)

@Factory
class Fib192 @Inject constructor(
    val fibM1: Fib191,
    val fibM2: Fib190
)

@Factory
class Fib193 @Inject constructor(
    val fibM1: Fib192,
    val fibM2: Fib191
)

@Factory
class Fib194 @Inject constructor(
    val fibM1: Fib193,
    val fibM2: Fib192
)

@Factory
class Fib195 @Inject constructor(
    val fibM1: Fib194,
    val fibM2: Fib193
)

@Factory
class Fib196 @Inject constructor(
    val fibM1: Fib195,
    val fibM2: Fib194
)

@Factory
class Fib197 @Inject constructor(
    val fibM1: Fib196,
    val fibM2: Fib195
)

@Factory
class Fib198 @Inject constructor(
    val fibM1: Fib197,
    val fibM2: Fib196
)

@Factory
class Fib199 @Inject constructor(
    val fibM1: Fib198,
    val fibM2: Fib197
)

@Factory
class Fib200 @Inject constructor(
    val fibM1: Fib199,
    val fibM2: Fib198
)

@Factory
class Fib201 @Inject constructor(
    val fibM1: Fib200,
    val fibM2: Fib199
)

@Factory
class Fib202 @Inject constructor(
    val fibM1: Fib201,
    val fibM2: Fib200
)

@Factory
class Fib203 @Inject constructor(
    val fibM1: Fib202,
    val fibM2: Fib201
)

@Factory
class Fib204 @Inject constructor(
    val fibM1: Fib203,
    val fibM2: Fib202
)

@Factory
class Fib205 @Inject constructor(
    val fibM1: Fib204,
    val fibM2: Fib203
)

@Factory
class Fib206 @Inject constructor(
    val fibM1: Fib205,
    val fibM2: Fib204
)

@Factory
class Fib207 @Inject constructor(
    val fibM1: Fib206,
    val fibM2: Fib205
)

@Factory
class Fib208 @Inject constructor(
    val fibM1: Fib207,
    val fibM2: Fib206
)

@Factory
class Fib209 @Inject constructor(
    val fibM1: Fib208,
    val fibM2: Fib207
)

@Factory
class Fib210 @Inject constructor(
    val fibM1: Fib209,
    val fibM2: Fib208
)

@Factory
class Fib211 @Inject constructor(
    val fibM1: Fib210,
    val fibM2: Fib209
)

@Factory
class Fib212 @Inject constructor(
    val fibM1: Fib211,
    val fibM2: Fib210
)

@Factory
class Fib213 @Inject constructor(
    val fibM1: Fib212,
    val fibM2: Fib211
)

@Factory
class Fib214 @Inject constructor(
    val fibM1: Fib213,
    val fibM2: Fib212
)

@Factory
class Fib215 @Inject constructor(
    val fibM1: Fib214,
    val fibM2: Fib213
)

@Factory
class Fib216 @Inject constructor(
    val fibM1: Fib215,
    val fibM2: Fib214
)

@Factory
class Fib217 @Inject constructor(
    val fibM1: Fib216,
    val fibM2: Fib215
)

@Factory
class Fib218 @Inject constructor(
    val fibM1: Fib217,
    val fibM2: Fib216
)

@Factory
class Fib219 @Inject constructor(
    val fibM1: Fib218,
    val fibM2: Fib217
)

@Factory
class Fib220 @Inject constructor(
    val fibM1: Fib219,
    val fibM2: Fib218
)

@Factory
class Fib221 @Inject constructor(
    val fibM1: Fib220,
    val fibM2: Fib219
)

@Factory
class Fib222 @Inject constructor(
    val fibM1: Fib221,
    val fibM2: Fib220
)

@Factory
class Fib223 @Inject constructor(
    val fibM1: Fib222,
    val fibM2: Fib221
)

@Factory
class Fib224 @Inject constructor(
    val fibM1: Fib223,
    val fibM2: Fib222
)

@Factory
class Fib225 @Inject constructor(
    val fibM1: Fib224,
    val fibM2: Fib223
)

@Factory
class Fib226 @Inject constructor(
    val fibM1: Fib225,
    val fibM2: Fib224
)

@Factory
class Fib227 @Inject constructor(
    val fibM1: Fib226,
    val fibM2: Fib225
)

@Factory
class Fib228 @Inject constructor(
    val fibM1: Fib227,
    val fibM2: Fib226
)

@Factory
class Fib229 @Inject constructor(
    val fibM1: Fib228,
    val fibM2: Fib227
)

@Factory
class Fib230 @Inject constructor(
    val fibM1: Fib229,
    val fibM2: Fib228
)

@Factory
class Fib231 @Inject constructor(
    val fibM1: Fib230,
    val fibM2: Fib229
)

@Factory
class Fib232 @Inject constructor(
    val fibM1: Fib231,
    val fibM2: Fib230
)

@Factory
class Fib233 @Inject constructor(
    val fibM1: Fib232,
    val fibM2: Fib231
)

@Factory
class Fib234 @Inject constructor(
    val fibM1: Fib233,
    val fibM2: Fib232
)

@Factory
class Fib235 @Inject constructor(
    val fibM1: Fib234,
    val fibM2: Fib233
)

@Factory
class Fib236 @Inject constructor(
    val fibM1: Fib235,
    val fibM2: Fib234
)

@Factory
class Fib237 @Inject constructor(
    val fibM1: Fib236,
    val fibM2: Fib235
)

@Factory
class Fib238 @Inject constructor(
    val fibM1: Fib237,
    val fibM2: Fib236
)

@Factory
class Fib239 @Inject constructor(
    val fibM1: Fib238,
    val fibM2: Fib237
)

@Factory
class Fib240 @Inject constructor(
    val fibM1: Fib239,
    val fibM2: Fib238
)

@Factory
class Fib241 @Inject constructor(
    val fibM1: Fib240,
    val fibM2: Fib239
)

@Factory
class Fib242 @Inject constructor(
    val fibM1: Fib241,
    val fibM2: Fib240
)

@Factory
class Fib243 @Inject constructor(
    val fibM1: Fib242,
    val fibM2: Fib241
)

@Factory
class Fib244 @Inject constructor(
    val fibM1: Fib243,
    val fibM2: Fib242
)

@Factory
class Fib245 @Inject constructor(
    val fibM1: Fib244,
    val fibM2: Fib243
)

@Factory
class Fib246 @Inject constructor(
    val fibM1: Fib245,
    val fibM2: Fib244
)

@Factory
class Fib247 @Inject constructor(
    val fibM1: Fib246,
    val fibM2: Fib245
)

@Factory
class Fib248 @Inject constructor(
    val fibM1: Fib247,
    val fibM2: Fib246
)

@Factory
class Fib249 @Inject constructor(
    val fibM1: Fib248,
    val fibM2: Fib247
)

@Factory
class Fib250 @Inject constructor(
    val fibM1: Fib249,
    val fibM2: Fib248
)

@Factory
class Fib251 @Inject constructor(
    val fibM1: Fib250,
    val fibM2: Fib249
)

@Factory
class Fib252 @Inject constructor(
    val fibM1: Fib251,
    val fibM2: Fib250
)

@Factory
class Fib253 @Inject constructor(
    val fibM1: Fib252,
    val fibM2: Fib251
)

@Factory
class Fib254 @Inject constructor(
    val fibM1: Fib253,
    val fibM2: Fib252
)

@Factory
class Fib255 @Inject constructor(
    val fibM1: Fib254,
    val fibM2: Fib253
)

@Factory
class Fib256 @Inject constructor(
    val fibM1: Fib255,
    val fibM2: Fib254
)

@Factory
class Fib257 @Inject constructor(
    val fibM1: Fib256,
    val fibM2: Fib255
)

@Factory
class Fib258 @Inject constructor(
    val fibM1: Fib257,
    val fibM2: Fib256
)

@Factory
class Fib259 @Inject constructor(
    val fibM1: Fib258,
    val fibM2: Fib257
)

@Factory
class Fib260 @Inject constructor(
    val fibM1: Fib259,
    val fibM2: Fib258
)

@Factory
class Fib261 @Inject constructor(
    val fibM1: Fib260,
    val fibM2: Fib259
)

@Factory
class Fib262 @Inject constructor(
    val fibM1: Fib261,
    val fibM2: Fib260
)

@Factory
class Fib263 @Inject constructor(
    val fibM1: Fib262,
    val fibM2: Fib261
)

@Factory
class Fib264 @Inject constructor(
    val fibM1: Fib263,
    val fibM2: Fib262
)

@Factory
class Fib265 @Inject constructor(
    val fibM1: Fib264,
    val fibM2: Fib263
)

@Factory
class Fib266 @Inject constructor(
    val fibM1: Fib265,
    val fibM2: Fib264
)

@Factory
class Fib267 @Inject constructor(
    val fibM1: Fib266,
    val fibM2: Fib265
)

@Factory
class Fib268 @Inject constructor(
    val fibM1: Fib267,
    val fibM2: Fib266
)

@Factory
class Fib269 @Inject constructor(
    val fibM1: Fib268,
    val fibM2: Fib267
)

@Factory
class Fib270 @Inject constructor(
    val fibM1: Fib269,
    val fibM2: Fib268
)

@Factory
class Fib271 @Inject constructor(
    val fibM1: Fib270,
    val fibM2: Fib269
)

@Factory
class Fib272 @Inject constructor(
    val fibM1: Fib271,
    val fibM2: Fib270
)

@Factory
class Fib273 @Inject constructor(
    val fibM1: Fib272,
    val fibM2: Fib271
)

@Factory
class Fib274 @Inject constructor(
    val fibM1: Fib273,
    val fibM2: Fib272
)

@Factory
class Fib275 @Inject constructor(
    val fibM1: Fib274,
    val fibM2: Fib273
)

@Factory
class Fib276 @Inject constructor(
    val fibM1: Fib275,
    val fibM2: Fib274
)

@Factory
class Fib277 @Inject constructor(
    val fibM1: Fib276,
    val fibM2: Fib275
)

@Factory
class Fib278 @Inject constructor(
    val fibM1: Fib277,
    val fibM2: Fib276
)

@Factory
class Fib279 @Inject constructor(
    val fibM1: Fib278,
    val fibM2: Fib277
)

@Factory
class Fib280 @Inject constructor(
    val fibM1: Fib279,
    val fibM2: Fib278
)

@Factory
class Fib281 @Inject constructor(
    val fibM1: Fib280,
    val fibM2: Fib279
)

@Factory
class Fib282 @Inject constructor(
    val fibM1: Fib281,
    val fibM2: Fib280
)

@Factory
class Fib283 @Inject constructor(
    val fibM1: Fib282,
    val fibM2: Fib281
)

@Factory
class Fib284 @Inject constructor(
    val fibM1: Fib283,
    val fibM2: Fib282
)

@Factory
class Fib285 @Inject constructor(
    val fibM1: Fib284,
    val fibM2: Fib283
)

@Factory
class Fib286 @Inject constructor(
    val fibM1: Fib285,
    val fibM2: Fib284
)

@Factory
class Fib287 @Inject constructor(
    val fibM1: Fib286,
    val fibM2: Fib285
)

@Factory
class Fib288 @Inject constructor(
    val fibM1: Fib287,
    val fibM2: Fib286
)

@Factory
class Fib289 @Inject constructor(
    val fibM1: Fib288,
    val fibM2: Fib287
)

@Factory
class Fib290 @Inject constructor(
    val fibM1: Fib289,
    val fibM2: Fib288
)

@Factory
class Fib291 @Inject constructor(
    val fibM1: Fib290,
    val fibM2: Fib289
)

@Factory
class Fib292 @Inject constructor(
    val fibM1: Fib291,
    val fibM2: Fib290
)

@Factory
class Fib293 @Inject constructor(
    val fibM1: Fib292,
    val fibM2: Fib291
)

@Factory
class Fib294 @Inject constructor(
    val fibM1: Fib293,
    val fibM2: Fib292
)

@Factory
class Fib295 @Inject constructor(
    val fibM1: Fib294,
    val fibM2: Fib293
)

@Factory
class Fib296 @Inject constructor(
    val fibM1: Fib295,
    val fibM2: Fib294
)

@Factory
class Fib297 @Inject constructor(
    val fibM1: Fib296,
    val fibM2: Fib295
)

@Factory
class Fib298 @Inject constructor(
    val fibM1: Fib297,
    val fibM2: Fib296
)

@Factory
class Fib299 @Inject constructor(
    val fibM1: Fib298,
    val fibM2: Fib297
)

@Factory
class Fib300 @Inject constructor(
    val fibM1: Fib299,
    val fibM2: Fib298
)

@Factory
class Fib301 @Inject constructor(
    val fibM1: Fib300,
    val fibM2: Fib299
)

@Factory
class Fib302 @Inject constructor(
    val fibM1: Fib301,
    val fibM2: Fib300
)

@Factory
class Fib303 @Inject constructor(
    val fibM1: Fib302,
    val fibM2: Fib301
)

@Factory
class Fib304 @Inject constructor(
    val fibM1: Fib303,
    val fibM2: Fib302
)

@Factory
class Fib305 @Inject constructor(
    val fibM1: Fib304,
    val fibM2: Fib303
)

@Factory
class Fib306 @Inject constructor(
    val fibM1: Fib305,
    val fibM2: Fib304
)

@Factory
class Fib307 @Inject constructor(
    val fibM1: Fib306,
    val fibM2: Fib305
)

@Factory
class Fib308 @Inject constructor(
    val fibM1: Fib307,
    val fibM2: Fib306
)

@Factory
class Fib309 @Inject constructor(
    val fibM1: Fib308,
    val fibM2: Fib307
)

@Factory
class Fib310 @Inject constructor(
    val fibM1: Fib309,
    val fibM2: Fib308
)

@Factory
class Fib311 @Inject constructor(
    val fibM1: Fib310,
    val fibM2: Fib309
)

@Factory
class Fib312 @Inject constructor(
    val fibM1: Fib311,
    val fibM2: Fib310
)

@Factory
class Fib313 @Inject constructor(
    val fibM1: Fib312,
    val fibM2: Fib311
)

@Factory
class Fib314 @Inject constructor(
    val fibM1: Fib313,
    val fibM2: Fib312
)

@Factory
class Fib315 @Inject constructor(
    val fibM1: Fib314,
    val fibM2: Fib313
)

@Factory
class Fib316 @Inject constructor(
    val fibM1: Fib315,
    val fibM2: Fib314
)

@Factory
class Fib317 @Inject constructor(
    val fibM1: Fib316,
    val fibM2: Fib315
)

@Factory
class Fib318 @Inject constructor(
    val fibM1: Fib317,
    val fibM2: Fib316
)

@Factory
class Fib319 @Inject constructor(
    val fibM1: Fib318,
    val fibM2: Fib317
)

@Factory
class Fib320 @Inject constructor(
    val fibM1: Fib319,
    val fibM2: Fib318
)

@Factory
class Fib321 @Inject constructor(
    val fibM1: Fib320,
    val fibM2: Fib319
)

@Factory
class Fib322 @Inject constructor(
    val fibM1: Fib321,
    val fibM2: Fib320
)

@Factory
class Fib323 @Inject constructor(
    val fibM1: Fib322,
    val fibM2: Fib321
)

@Factory
class Fib324 @Inject constructor(
    val fibM1: Fib323,
    val fibM2: Fib322
)

@Factory
class Fib325 @Inject constructor(
    val fibM1: Fib324,
    val fibM2: Fib323
)

@Factory
class Fib326 @Inject constructor(
    val fibM1: Fib325,
    val fibM2: Fib324
)

@Factory
class Fib327 @Inject constructor(
    val fibM1: Fib326,
    val fibM2: Fib325
)

@Factory
class Fib328 @Inject constructor(
    val fibM1: Fib327,
    val fibM2: Fib326
)

@Factory
class Fib329 @Inject constructor(
    val fibM1: Fib328,
    val fibM2: Fib327
)

@Factory
class Fib330 @Inject constructor(
    val fibM1: Fib329,
    val fibM2: Fib328
)

@Factory
class Fib331 @Inject constructor(
    val fibM1: Fib330,
    val fibM2: Fib329
)

@Factory
class Fib332 @Inject constructor(
    val fibM1: Fib331,
    val fibM2: Fib330
)

@Factory
class Fib333 @Inject constructor(
    val fibM1: Fib332,
    val fibM2: Fib331
)

@Factory
class Fib334 @Inject constructor(
    val fibM1: Fib333,
    val fibM2: Fib332
)

@Factory
class Fib335 @Inject constructor(
    val fibM1: Fib334,
    val fibM2: Fib333
)

@Factory
class Fib336 @Inject constructor(
    val fibM1: Fib335,
    val fibM2: Fib334
)

@Factory
class Fib337 @Inject constructor(
    val fibM1: Fib336,
    val fibM2: Fib335
)

@Factory
class Fib338 @Inject constructor(
    val fibM1: Fib337,
    val fibM2: Fib336
)

@Factory
class Fib339 @Inject constructor(
    val fibM1: Fib338,
    val fibM2: Fib337
)

@Factory
class Fib340 @Inject constructor(
    val fibM1: Fib339,
    val fibM2: Fib338
)

@Factory
class Fib341 @Inject constructor(
    val fibM1: Fib340,
    val fibM2: Fib339
)

@Factory
class Fib342 @Inject constructor(
    val fibM1: Fib341,
    val fibM2: Fib340
)

@Factory
class Fib343 @Inject constructor(
    val fibM1: Fib342,
    val fibM2: Fib341
)

@Factory
class Fib344 @Inject constructor(
    val fibM1: Fib343,
    val fibM2: Fib342
)

@Factory
class Fib345 @Inject constructor(
    val fibM1: Fib344,
    val fibM2: Fib343
)

@Factory
class Fib346 @Inject constructor(
    val fibM1: Fib345,
    val fibM2: Fib344
)

@Factory
class Fib347 @Inject constructor(
    val fibM1: Fib346,
    val fibM2: Fib345
)

@Factory
class Fib348 @Inject constructor(
    val fibM1: Fib347,
    val fibM2: Fib346
)

@Factory
class Fib349 @Inject constructor(
    val fibM1: Fib348,
    val fibM2: Fib347
)

@Factory
class Fib350 @Inject constructor(
    val fibM1: Fib349,
    val fibM2: Fib348
)

@Factory
class Fib351 @Inject constructor(
    val fibM1: Fib350,
    val fibM2: Fib349
)

@Factory
class Fib352 @Inject constructor(
    val fibM1: Fib351,
    val fibM2: Fib350
)

@Factory
class Fib353 @Inject constructor(
    val fibM1: Fib352,
    val fibM2: Fib351
)

@Factory
class Fib354 @Inject constructor(
    val fibM1: Fib353,
    val fibM2: Fib352
)

@Factory
class Fib355 @Inject constructor(
    val fibM1: Fib354,
    val fibM2: Fib353
)

@Factory
class Fib356 @Inject constructor(
    val fibM1: Fib355,
    val fibM2: Fib354
)

@Factory
class Fib357 @Inject constructor(
    val fibM1: Fib356,
    val fibM2: Fib355
)

@Factory
class Fib358 @Inject constructor(
    val fibM1: Fib357,
    val fibM2: Fib356
)

@Factory
class Fib359 @Inject constructor(
    val fibM1: Fib358,
    val fibM2: Fib357
)

@Factory
class Fib360 @Inject constructor(
    val fibM1: Fib359,
    val fibM2: Fib358
)

@Factory
class Fib361 @Inject constructor(
    val fibM1: Fib360,
    val fibM2: Fib359
)

@Factory
class Fib362 @Inject constructor(
    val fibM1: Fib361,
    val fibM2: Fib360
)

@Factory
class Fib363 @Inject constructor(
    val fibM1: Fib362,
    val fibM2: Fib361
)

@Factory
class Fib364 @Inject constructor(
    val fibM1: Fib363,
    val fibM2: Fib362
)

@Factory
class Fib365 @Inject constructor(
    val fibM1: Fib364,
    val fibM2: Fib363
)

@Factory
class Fib366 @Inject constructor(
    val fibM1: Fib365,
    val fibM2: Fib364
)

@Factory
class Fib367 @Inject constructor(
    val fibM1: Fib366,
    val fibM2: Fib365
)

@Factory
class Fib368 @Inject constructor(
    val fibM1: Fib367,
    val fibM2: Fib366
)

@Factory
class Fib369 @Inject constructor(
    val fibM1: Fib368,
    val fibM2: Fib367
)

@Factory
class Fib370 @Inject constructor(
    val fibM1: Fib369,
    val fibM2: Fib368
)

@Factory
class Fib371 @Inject constructor(
    val fibM1: Fib370,
    val fibM2: Fib369
)

@Factory
class Fib372 @Inject constructor(
    val fibM1: Fib371,
    val fibM2: Fib370
)

@Factory
class Fib373 @Inject constructor(
    val fibM1: Fib372,
    val fibM2: Fib371
)

@Factory
class Fib374 @Inject constructor(
    val fibM1: Fib373,
    val fibM2: Fib372
)

@Factory
class Fib375 @Inject constructor(
    val fibM1: Fib374,
    val fibM2: Fib373
)

@Factory
class Fib376 @Inject constructor(
    val fibM1: Fib375,
    val fibM2: Fib374
)

@Factory
class Fib377 @Inject constructor(
    val fibM1: Fib376,
    val fibM2: Fib375
)

@Factory
class Fib378 @Inject constructor(
    val fibM1: Fib377,
    val fibM2: Fib376
)

@Factory
class Fib379 @Inject constructor(
    val fibM1: Fib378,
    val fibM2: Fib377
)

@Factory
class Fib380 @Inject constructor(
    val fibM1: Fib379,
    val fibM2: Fib378
)

@Factory
class Fib381 @Inject constructor(
    val fibM1: Fib380,
    val fibM2: Fib379
)

@Factory
class Fib382 @Inject constructor(
    val fibM1: Fib381,
    val fibM2: Fib380
)

@Factory
class Fib383 @Inject constructor(
    val fibM1: Fib382,
    val fibM2: Fib381
)

@Factory
class Fib384 @Inject constructor(
    val fibM1: Fib383,
    val fibM2: Fib382
)

@Factory
class Fib385 @Inject constructor(
    val fibM1: Fib384,
    val fibM2: Fib383
)

@Factory
class Fib386 @Inject constructor(
    val fibM1: Fib385,
    val fibM2: Fib384
)

@Factory
class Fib387 @Inject constructor(
    val fibM1: Fib386,
    val fibM2: Fib385
)

@Factory
class Fib388 @Inject constructor(
    val fibM1: Fib387,
    val fibM2: Fib386
)

@Factory
class Fib389 @Inject constructor(
    val fibM1: Fib388,
    val fibM2: Fib387
)

@Factory
class Fib390 @Inject constructor(
    val fibM1: Fib389,
    val fibM2: Fib388
)

@Factory
class Fib391 @Inject constructor(
    val fibM1: Fib390,
    val fibM2: Fib389
)

@Factory
class Fib392 @Inject constructor(
    val fibM1: Fib391,
    val fibM2: Fib390
)

@Factory
class Fib393 @Inject constructor(
    val fibM1: Fib392,
    val fibM2: Fib391
)

@Factory
class Fib394 @Inject constructor(
    val fibM1: Fib393,
    val fibM2: Fib392
)

@Factory
class Fib395 @Inject constructor(
    val fibM1: Fib394,
    val fibM2: Fib393
)

@Factory
class Fib396 @Inject constructor(
    val fibM1: Fib395,
    val fibM2: Fib394
)

@Factory
class Fib397 @Inject constructor(
    val fibM1: Fib396,
    val fibM2: Fib395
)

@Factory
class Fib398 @Inject constructor(
    val fibM1: Fib397,
    val fibM2: Fib396
)

@Factory
class Fib399 @Inject constructor(
    val fibM1: Fib398,
    val fibM2: Fib397
)

@Factory
class Fib400 @Inject constructor(
    val fibM1: Fib399,
    val fibM2: Fib398
)
