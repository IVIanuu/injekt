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

import com.ivianuu.injekt.Given
import javax.inject.Inject

@Given class Fib1 @Inject constructor()

@Given class Fib2 @Inject constructor()


@Given class Fib3 @Inject constructor(
    @Given val fibM1: Fib2,
    @Given val fibM2: Fib1
)


@Given class Fib4 @Inject constructor(
    @Given val fibM1: Fib3,
    @Given val fibM2: Fib2
)


@Given class Fib5 @Inject constructor(
    @Given val fibM1: Fib4,
    @Given val fibM2: Fib3
)


@Given class Fib6 @Inject constructor(
    @Given val fibM1: Fib5,
    @Given val fibM2: Fib4
)


@Given class Fib7 @Inject constructor(
    @Given val fibM1: Fib6,
    @Given val fibM2: Fib5
)


@Given class Fib8 @Inject constructor(
    @Given val fibM1: Fib7,
    @Given val fibM2: Fib6
)


@Given class Fib9 @Inject constructor(
    @Given val fibM1: Fib8,
    @Given val fibM2: Fib7
)


@Given class Fib10 @Inject constructor(
    @Given val fibM1: Fib9,
    @Given val fibM2: Fib8
)


@Given class Fib11 @Inject constructor(
    @Given val fibM1: Fib10,
    @Given val fibM2: Fib9
)


@Given class Fib12 @Inject constructor(
    @Given val fibM1: Fib11,
    @Given val fibM2: Fib10
)


@Given class Fib13 @Inject constructor(
    @Given val fibM1: Fib12,
    @Given val fibM2: Fib11
)


@Given class Fib14 @Inject constructor(
    @Given val fibM1: Fib13,
    @Given val fibM2: Fib12
)


@Given class Fib15 @Inject constructor(
    @Given val fibM1: Fib14,
    @Given val fibM2: Fib13
)


@Given class Fib16 @Inject constructor(
    @Given val fibM1: Fib15,
    @Given val fibM2: Fib14
)


@Given class Fib17 @Inject constructor(
    @Given val fibM1: Fib16,
    @Given val fibM2: Fib15
)


@Given class Fib18 @Inject constructor(
    @Given val fibM1: Fib17,
    @Given val fibM2: Fib16
)


@Given class Fib19 @Inject constructor(
    @Given val fibM1: Fib18,
    @Given val fibM2: Fib17
)


@Given class Fib20 @Inject constructor(
    @Given val fibM1: Fib19,
    @Given val fibM2: Fib18
)


@Given class Fib21 @Inject constructor(
    @Given val fibM1: Fib20,
    @Given val fibM2: Fib19
)


@Given class Fib22 @Inject constructor(
    @Given val fibM1: Fib21,
    @Given val fibM2: Fib20
)


@Given class Fib23 @Inject constructor(
    @Given val fibM1: Fib22,
    @Given val fibM2: Fib21
)


@Given class Fib24 @Inject constructor(
    @Given val fibM1: Fib23,
    @Given val fibM2: Fib22
)


@Given class Fib25 @Inject constructor(
    @Given val fibM1: Fib24,
    @Given val fibM2: Fib23
)


@Given class Fib26 @Inject constructor(
    @Given val fibM1: Fib25,
    @Given val fibM2: Fib24
)


@Given class Fib27 @Inject constructor(
    @Given val fibM1: Fib26,
    @Given val fibM2: Fib25
)


@Given class Fib28 @Inject constructor(
    @Given val fibM1: Fib27,
    @Given val fibM2: Fib26
)


@Given class Fib29 @Inject constructor(
    @Given val fibM1: Fib28,
    @Given val fibM2: Fib27
)


@Given class Fib30 @Inject constructor(
    @Given val fibM1: Fib29,
    @Given val fibM2: Fib28
)


@Given class Fib31 @Inject constructor(
    @Given val fibM1: Fib30,
    @Given val fibM2: Fib29
)


@Given class Fib32 @Inject constructor(
    @Given val fibM1: Fib31,
    @Given val fibM2: Fib30
)


@Given class Fib33 @Inject constructor(
    @Given val fibM1: Fib32,
    @Given val fibM2: Fib31
)


@Given class Fib34 @Inject constructor(
    @Given val fibM1: Fib33,
    @Given val fibM2: Fib32
)


@Given class Fib35 @Inject constructor(
    @Given val fibM1: Fib34,
    @Given val fibM2: Fib33
)


@Given class Fib36 @Inject constructor(
    @Given val fibM1: Fib35,
    @Given val fibM2: Fib34
)


@Given class Fib37 @Inject constructor(
    @Given val fibM1: Fib36,
    @Given val fibM2: Fib35
)


@Given class Fib38 @Inject constructor(
    @Given val fibM1: Fib37,
    @Given val fibM2: Fib36
)


@Given class Fib39 @Inject constructor(
    @Given val fibM1: Fib38,
    @Given val fibM2: Fib37
)


@Given class Fib40 @Inject constructor(
    @Given val fibM1: Fib39,
    @Given val fibM2: Fib38
)


@Given class Fib41 @Inject constructor(
    @Given val fibM1: Fib40,
    @Given val fibM2: Fib39
)


@Given class Fib42 @Inject constructor(
    @Given val fibM1: Fib41,
    @Given val fibM2: Fib40
)


@Given class Fib43 @Inject constructor(
    @Given val fibM1: Fib42,
    @Given val fibM2: Fib41
)


@Given class Fib44 @Inject constructor(
    @Given val fibM1: Fib43,
    @Given val fibM2: Fib42
)


@Given class Fib45 @Inject constructor(
    @Given val fibM1: Fib44,
    @Given val fibM2: Fib43
)


@Given class Fib46 @Inject constructor(
    @Given val fibM1: Fib45,
    @Given val fibM2: Fib44
)


@Given class Fib47 @Inject constructor(
    @Given val fibM1: Fib46,
    @Given val fibM2: Fib45
)


@Given class Fib48 @Inject constructor(
    @Given val fibM1: Fib47,
    @Given val fibM2: Fib46
)


@Given class Fib49 @Inject constructor(
    @Given val fibM1: Fib48,
    @Given val fibM2: Fib47
)


@Given class Fib50 @Inject constructor(
    @Given val fibM1: Fib49,
    @Given val fibM2: Fib48
)


@Given class Fib51 @Inject constructor(
    @Given val fibM1: Fib50,
    @Given val fibM2: Fib49
)


@Given class Fib52 @Inject constructor(
    @Given val fibM1: Fib51,
    @Given val fibM2: Fib50
)


@Given class Fib53 @Inject constructor(
    @Given val fibM1: Fib52,
    @Given val fibM2: Fib51
)


@Given class Fib54 @Inject constructor(
    @Given val fibM1: Fib53,
    @Given val fibM2: Fib52
)


@Given class Fib55 @Inject constructor(
    @Given val fibM1: Fib54,
    @Given val fibM2: Fib53
)


@Given class Fib56 @Inject constructor(
    @Given val fibM1: Fib55,
    @Given val fibM2: Fib54
)


@Given class Fib57 @Inject constructor(
    @Given val fibM1: Fib56,
    @Given val fibM2: Fib55
)


@Given class Fib58 @Inject constructor(
    @Given val fibM1: Fib57,
    @Given val fibM2: Fib56
)


@Given class Fib59 @Inject constructor(
    @Given val fibM1: Fib58,
    @Given val fibM2: Fib57
)


@Given class Fib60 @Inject constructor(
    @Given val fibM1: Fib59,
    @Given val fibM2: Fib58
)


@Given class Fib61 @Inject constructor(
    @Given val fibM1: Fib60,
    @Given val fibM2: Fib59
)


@Given class Fib62 @Inject constructor(
    @Given val fibM1: Fib61,
    @Given val fibM2: Fib60
)


@Given class Fib63 @Inject constructor(
    @Given val fibM1: Fib62,
    @Given val fibM2: Fib61
)


@Given class Fib64 @Inject constructor(
    @Given val fibM1: Fib63,
    @Given val fibM2: Fib62
)


@Given class Fib65 @Inject constructor(
    @Given val fibM1: Fib64,
    @Given val fibM2: Fib63
)


@Given class Fib66 @Inject constructor(
    @Given val fibM1: Fib65,
    @Given val fibM2: Fib64
)


@Given class Fib67 @Inject constructor(
    @Given val fibM1: Fib66,
    @Given val fibM2: Fib65
)


@Given class Fib68 @Inject constructor(
    @Given val fibM1: Fib67,
    @Given val fibM2: Fib66
)


@Given class Fib69 @Inject constructor(
    @Given val fibM1: Fib68,
    @Given val fibM2: Fib67
)


@Given class Fib70 @Inject constructor(
    @Given val fibM1: Fib69,
    @Given val fibM2: Fib68
)


@Given class Fib71 @Inject constructor(
    @Given val fibM1: Fib70,
    @Given val fibM2: Fib69
)


@Given class Fib72 @Inject constructor(
    @Given val fibM1: Fib71,
    @Given val fibM2: Fib70
)


@Given class Fib73 @Inject constructor(
    @Given val fibM1: Fib72,
    @Given val fibM2: Fib71
)


@Given class Fib74 @Inject constructor(
    @Given val fibM1: Fib73,
    @Given val fibM2: Fib72
)


@Given class Fib75 @Inject constructor(
    @Given val fibM1: Fib74,
    @Given val fibM2: Fib73
)


@Given class Fib76 @Inject constructor(
    @Given val fibM1: Fib75,
    @Given val fibM2: Fib74
)


@Given class Fib77 @Inject constructor(
    @Given val fibM1: Fib76,
    @Given val fibM2: Fib75
)


@Given class Fib78 @Inject constructor(
    @Given val fibM1: Fib77,
    @Given val fibM2: Fib76
)


@Given class Fib79 @Inject constructor(
    @Given val fibM1: Fib78,
    @Given val fibM2: Fib77
)


@Given class Fib80 @Inject constructor(
    @Given val fibM1: Fib79,
    @Given val fibM2: Fib78
)


@Given class Fib81 @Inject constructor(
    @Given val fibM1: Fib80,
    @Given val fibM2: Fib79
)


@Given class Fib82 @Inject constructor(
    @Given val fibM1: Fib81,
    @Given val fibM2: Fib80
)


@Given class Fib83 @Inject constructor(
    @Given val fibM1: Fib82,
    @Given val fibM2: Fib81
)


@Given class Fib84 @Inject constructor(
    @Given val fibM1: Fib83,
    @Given val fibM2: Fib82
)


@Given class Fib85 @Inject constructor(
    @Given val fibM1: Fib84,
    @Given val fibM2: Fib83
)


@Given class Fib86 @Inject constructor(
    @Given val fibM1: Fib85,
    @Given val fibM2: Fib84
)


@Given class Fib87 @Inject constructor(
    @Given val fibM1: Fib86,
    @Given val fibM2: Fib85
)


@Given class Fib88 @Inject constructor(
    @Given val fibM1: Fib87,
    @Given val fibM2: Fib86
)


@Given class Fib89 @Inject constructor(
    @Given val fibM1: Fib88,
    @Given val fibM2: Fib87
)


@Given class Fib90 @Inject constructor(
    @Given val fibM1: Fib89,
    @Given val fibM2: Fib88
)


@Given class Fib91 @Inject constructor(
    @Given val fibM1: Fib90,
    @Given val fibM2: Fib89
)


@Given class Fib92 @Inject constructor(
    @Given val fibM1: Fib91,
    @Given val fibM2: Fib90
)


@Given class Fib93 @Inject constructor(
    @Given val fibM1: Fib92,
    @Given val fibM2: Fib91
)


@Given class Fib94 @Inject constructor(
    @Given val fibM1: Fib93,
    @Given val fibM2: Fib92
)


@Given class Fib95 @Inject constructor(
    @Given val fibM1: Fib94,
    @Given val fibM2: Fib93
)


@Given class Fib96 @Inject constructor(
    @Given val fibM1: Fib95,
    @Given val fibM2: Fib94
)


@Given class Fib97 @Inject constructor(
    @Given val fibM1: Fib96,
    @Given val fibM2: Fib95
)


@Given class Fib98 @Inject constructor(
    @Given val fibM1: Fib97,
    @Given val fibM2: Fib96
)


@Given class Fib99 @Inject constructor(
    @Given val fibM1: Fib98,
    @Given val fibM2: Fib97
)


@Given class Fib100 @Inject constructor(
    @Given val fibM1: Fib99,
    @Given val fibM2: Fib98
)


@Given class Fib101 @Inject constructor(
    @Given val fibM1: Fib100,
    @Given val fibM2: Fib99
)


@Given class Fib102 @Inject constructor(
    @Given val fibM1: Fib101,
    @Given val fibM2: Fib100
)


@Given class Fib103 @Inject constructor(
    @Given val fibM1: Fib102,
    @Given val fibM2: Fib101
)


@Given class Fib104 @Inject constructor(
    @Given val fibM1: Fib103,
    @Given val fibM2: Fib102
)


@Given class Fib105 @Inject constructor(
    @Given val fibM1: Fib104,
    @Given val fibM2: Fib103
)


@Given class Fib106 @Inject constructor(
    @Given val fibM1: Fib105,
    @Given val fibM2: Fib104
)


@Given class Fib107 @Inject constructor(
    @Given val fibM1: Fib106,
    @Given val fibM2: Fib105
)


@Given class Fib108 @Inject constructor(
    @Given val fibM1: Fib107,
    @Given val fibM2: Fib106
)


@Given class Fib109 @Inject constructor(
    @Given val fibM1: Fib108,
    @Given val fibM2: Fib107
)


@Given class Fib110 @Inject constructor(
    @Given val fibM1: Fib109,
    @Given val fibM2: Fib108
)


@Given class Fib111 @Inject constructor(
    @Given val fibM1: Fib110,
    @Given val fibM2: Fib109
)


@Given class Fib112 @Inject constructor(
    @Given val fibM1: Fib111,
    @Given val fibM2: Fib110
)


@Given class Fib113 @Inject constructor(
    @Given val fibM1: Fib112,
    @Given val fibM2: Fib111
)


@Given class Fib114 @Inject constructor(
    @Given val fibM1: Fib113,
    @Given val fibM2: Fib112
)


@Given class Fib115 @Inject constructor(
    @Given val fibM1: Fib114,
    @Given val fibM2: Fib113
)


@Given class Fib116 @Inject constructor(
    @Given val fibM1: Fib115,
    @Given val fibM2: Fib114
)


@Given class Fib117 @Inject constructor(
    @Given val fibM1: Fib116,
    @Given val fibM2: Fib115
)


@Given class Fib118 @Inject constructor(
    @Given val fibM1: Fib117,
    @Given val fibM2: Fib116
)


@Given class Fib119 @Inject constructor(
    @Given val fibM1: Fib118,
    @Given val fibM2: Fib117
)


@Given class Fib120 @Inject constructor(
    @Given val fibM1: Fib119,
    @Given val fibM2: Fib118
)


@Given class Fib121 @Inject constructor(
    @Given val fibM1: Fib120,
    @Given val fibM2: Fib119
)


@Given class Fib122 @Inject constructor(
    @Given val fibM1: Fib121,
    @Given val fibM2: Fib120
)


@Given class Fib123 @Inject constructor(
    @Given val fibM1: Fib122,
    @Given val fibM2: Fib121
)


@Given class Fib124 @Inject constructor(
    @Given val fibM1: Fib123,
    @Given val fibM2: Fib122
)


@Given class Fib125 @Inject constructor(
    @Given val fibM1: Fib124,
    @Given val fibM2: Fib123
)


@Given class Fib126 @Inject constructor(
    @Given val fibM1: Fib125,
    @Given val fibM2: Fib124
)


@Given class Fib127 @Inject constructor(
    @Given val fibM1: Fib126,
    @Given val fibM2: Fib125
)


@Given class Fib128 @Inject constructor(
    @Given val fibM1: Fib127,
    @Given val fibM2: Fib126
)


@Given class Fib129 @Inject constructor(
    @Given val fibM1: Fib128,
    @Given val fibM2: Fib127
)


@Given class Fib130 @Inject constructor(
    @Given val fibM1: Fib129,
    @Given val fibM2: Fib128
)


@Given class Fib131 @Inject constructor(
    @Given val fibM1: Fib130,
    @Given val fibM2: Fib129
)


@Given class Fib132 @Inject constructor(
    @Given val fibM1: Fib131,
    @Given val fibM2: Fib130
)


@Given class Fib133 @Inject constructor(
    @Given val fibM1: Fib132,
    @Given val fibM2: Fib131
)


@Given class Fib134 @Inject constructor(
    @Given val fibM1: Fib133,
    @Given val fibM2: Fib132
)


@Given class Fib135 @Inject constructor(
    @Given val fibM1: Fib134,
    @Given val fibM2: Fib133
)


@Given class Fib136 @Inject constructor(
    @Given val fibM1: Fib135,
    @Given val fibM2: Fib134
)


@Given class Fib137 @Inject constructor(
    @Given val fibM1: Fib136,
    @Given val fibM2: Fib135
)


@Given class Fib138 @Inject constructor(
    @Given val fibM1: Fib137,
    @Given val fibM2: Fib136
)


@Given class Fib139 @Inject constructor(
    @Given val fibM1: Fib138,
    @Given val fibM2: Fib137
)


@Given class Fib140 @Inject constructor(
    @Given val fibM1: Fib139,
    @Given val fibM2: Fib138
)


@Given class Fib141 @Inject constructor(
    @Given val fibM1: Fib140,
    @Given val fibM2: Fib139
)


@Given class Fib142 @Inject constructor(
    @Given val fibM1: Fib141,
    @Given val fibM2: Fib140
)


@Given class Fib143 @Inject constructor(
    @Given val fibM1: Fib142,
    @Given val fibM2: Fib141
)


@Given class Fib144 @Inject constructor(
    @Given val fibM1: Fib143,
    @Given val fibM2: Fib142
)


@Given class Fib145 @Inject constructor(
    @Given val fibM1: Fib144,
    @Given val fibM2: Fib143
)


@Given class Fib146 @Inject constructor(
    @Given val fibM1: Fib145,
    @Given val fibM2: Fib144
)


@Given class Fib147 @Inject constructor(
    @Given val fibM1: Fib146,
    @Given val fibM2: Fib145
)


@Given class Fib148 @Inject constructor(
    @Given val fibM1: Fib147,
    @Given val fibM2: Fib146
)


@Given class Fib149 @Inject constructor(
    @Given val fibM1: Fib148,
    @Given val fibM2: Fib147
)


@Given class Fib150 @Inject constructor(
    @Given val fibM1: Fib149,
    @Given val fibM2: Fib148
)


@Given class Fib151 @Inject constructor(
    @Given val fibM1: Fib150,
    @Given val fibM2: Fib149
)


@Given class Fib152 @Inject constructor(
    @Given val fibM1: Fib151,
    @Given val fibM2: Fib150
)


@Given class Fib153 @Inject constructor(
    @Given val fibM1: Fib152,
    @Given val fibM2: Fib151
)


@Given class Fib154 @Inject constructor(
    @Given val fibM1: Fib153,
    @Given val fibM2: Fib152
)


@Given class Fib155 @Inject constructor(
    @Given val fibM1: Fib154,
    @Given val fibM2: Fib153
)


@Given class Fib156 @Inject constructor(
    @Given val fibM1: Fib155,
    @Given val fibM2: Fib154
)


@Given class Fib157 @Inject constructor(
    @Given val fibM1: Fib156,
    @Given val fibM2: Fib155
)


@Given class Fib158 @Inject constructor(
    @Given val fibM1: Fib157,
    @Given val fibM2: Fib156
)


@Given class Fib159 @Inject constructor(
    @Given val fibM1: Fib158,
    @Given val fibM2: Fib157
)


@Given class Fib160 @Inject constructor(
    @Given val fibM1: Fib159,
    @Given val fibM2: Fib158
)


@Given class Fib161 @Inject constructor(
    @Given val fibM1: Fib160,
    @Given val fibM2: Fib159
)


@Given class Fib162 @Inject constructor(
    @Given val fibM1: Fib161,
    @Given val fibM2: Fib160
)


@Given class Fib163 @Inject constructor(
    @Given val fibM1: Fib162,
    @Given val fibM2: Fib161
)


@Given class Fib164 @Inject constructor(
    @Given val fibM1: Fib163,
    @Given val fibM2: Fib162
)


@Given class Fib165 @Inject constructor(
    @Given val fibM1: Fib164,
    @Given val fibM2: Fib163
)


@Given class Fib166 @Inject constructor(
    @Given val fibM1: Fib165,
    @Given val fibM2: Fib164
)


@Given class Fib167 @Inject constructor(
    @Given val fibM1: Fib166,
    @Given val fibM2: Fib165
)


@Given class Fib168 @Inject constructor(
    @Given val fibM1: Fib167,
    @Given val fibM2: Fib166
)


@Given class Fib169 @Inject constructor(
    @Given val fibM1: Fib168,
    @Given val fibM2: Fib167
)


@Given class Fib170 @Inject constructor(
    @Given val fibM1: Fib169,
    @Given val fibM2: Fib168
)


@Given class Fib171 @Inject constructor(
    @Given val fibM1: Fib170,
    @Given val fibM2: Fib169
)


@Given class Fib172 @Inject constructor(
    @Given val fibM1: Fib171,
    @Given val fibM2: Fib170
)


@Given class Fib173 @Inject constructor(
    @Given val fibM1: Fib172,
    @Given val fibM2: Fib171
)


@Given class Fib174 @Inject constructor(
    @Given val fibM1: Fib173,
    @Given val fibM2: Fib172
)


@Given class Fib175 @Inject constructor(
    @Given val fibM1: Fib174,
    @Given val fibM2: Fib173
)


@Given class Fib176 @Inject constructor(
    @Given val fibM1: Fib175,
    @Given val fibM2: Fib174
)


@Given class Fib177 @Inject constructor(
    @Given val fibM1: Fib176,
    @Given val fibM2: Fib175
)


@Given class Fib178 @Inject constructor(
    @Given val fibM1: Fib177,
    @Given val fibM2: Fib176
)


@Given class Fib179 @Inject constructor(
    @Given val fibM1: Fib178,
    @Given val fibM2: Fib177
)


@Given class Fib180 @Inject constructor(
    @Given val fibM1: Fib179,
    @Given val fibM2: Fib178
)


@Given class Fib181 @Inject constructor(
    @Given val fibM1: Fib180,
    @Given val fibM2: Fib179
)


@Given class Fib182 @Inject constructor(
    @Given val fibM1: Fib181,
    @Given val fibM2: Fib180
)


@Given class Fib183 @Inject constructor(
    @Given val fibM1: Fib182,
    @Given val fibM2: Fib181
)


@Given class Fib184 @Inject constructor(
    @Given val fibM1: Fib183,
    @Given val fibM2: Fib182
)


@Given class Fib185 @Inject constructor(
    @Given val fibM1: Fib184,
    @Given val fibM2: Fib183
)


@Given class Fib186 @Inject constructor(
    @Given val fibM1: Fib185,
    @Given val fibM2: Fib184
)


@Given class Fib187 @Inject constructor(
    @Given val fibM1: Fib186,
    @Given val fibM2: Fib185
)


@Given class Fib188 @Inject constructor(
    @Given val fibM1: Fib187,
    @Given val fibM2: Fib186
)


@Given class Fib189 @Inject constructor(
    @Given val fibM1: Fib188,
    @Given val fibM2: Fib187
)


@Given class Fib190 @Inject constructor(
    @Given val fibM1: Fib189,
    @Given val fibM2: Fib188
)


@Given class Fib191 @Inject constructor(
    @Given val fibM1: Fib190,
    @Given val fibM2: Fib189
)


@Given class Fib192 @Inject constructor(
    @Given val fibM1: Fib191,
    @Given val fibM2: Fib190
)


@Given class Fib193 @Inject constructor(
    @Given val fibM1: Fib192,
    @Given val fibM2: Fib191
)


@Given class Fib194 @Inject constructor(
    @Given val fibM1: Fib193,
    @Given val fibM2: Fib192
)


@Given class Fib195 @Inject constructor(
    @Given val fibM1: Fib194,
    @Given val fibM2: Fib193
)


@Given class Fib196 @Inject constructor(
    @Given val fibM1: Fib195,
    @Given val fibM2: Fib194
)


@Given class Fib197 @Inject constructor(
    @Given val fibM1: Fib196,
    @Given val fibM2: Fib195
)


@Given class Fib198 @Inject constructor(
    @Given val fibM1: Fib197,
    @Given val fibM2: Fib196
)


@Given class Fib199 @Inject constructor(
    @Given val fibM1: Fib198,
    @Given val fibM2: Fib197
)


@Given class Fib200 @Inject constructor(
    @Given val fibM1: Fib199,
    @Given val fibM2: Fib198
)


@Given class Fib201 @Inject constructor(
    @Given val fibM1: Fib200,
    @Given val fibM2: Fib199
)


@Given class Fib202 @Inject constructor(
    @Given val fibM1: Fib201,
    @Given val fibM2: Fib200
)


@Given class Fib203 @Inject constructor(
    @Given val fibM1: Fib202,
    @Given val fibM2: Fib201
)


@Given class Fib204 @Inject constructor(
    @Given val fibM1: Fib203,
    @Given val fibM2: Fib202
)


@Given class Fib205 @Inject constructor(
    @Given val fibM1: Fib204,
    @Given val fibM2: Fib203
)


@Given class Fib206 @Inject constructor(
    @Given val fibM1: Fib205,
    @Given val fibM2: Fib204
)


@Given class Fib207 @Inject constructor(
    @Given val fibM1: Fib206,
    @Given val fibM2: Fib205
)


@Given class Fib208 @Inject constructor(
    @Given val fibM1: Fib207,
    @Given val fibM2: Fib206
)


@Given class Fib209 @Inject constructor(
    @Given val fibM1: Fib208,
    @Given val fibM2: Fib207
)


@Given class Fib210 @Inject constructor(
    @Given val fibM1: Fib209,
    @Given val fibM2: Fib208
)


@Given class Fib211 @Inject constructor(
    @Given val fibM1: Fib210,
    @Given val fibM2: Fib209
)


@Given class Fib212 @Inject constructor(
    @Given val fibM1: Fib211,
    @Given val fibM2: Fib210
)


@Given class Fib213 @Inject constructor(
    @Given val fibM1: Fib212,
    @Given val fibM2: Fib211
)


@Given class Fib214 @Inject constructor(
    @Given val fibM1: Fib213,
    @Given val fibM2: Fib212
)


@Given class Fib215 @Inject constructor(
    @Given val fibM1: Fib214,
    @Given val fibM2: Fib213
)


@Given class Fib216 @Inject constructor(
    @Given val fibM1: Fib215,
    @Given val fibM2: Fib214
)


@Given class Fib217 @Inject constructor(
    @Given val fibM1: Fib216,
    @Given val fibM2: Fib215
)


@Given class Fib218 @Inject constructor(
    @Given val fibM1: Fib217,
    @Given val fibM2: Fib216
)


@Given class Fib219 @Inject constructor(
    @Given val fibM1: Fib218,
    @Given val fibM2: Fib217
)


@Given class Fib220 @Inject constructor(
    @Given val fibM1: Fib219,
    @Given val fibM2: Fib218
)


@Given class Fib221 @Inject constructor(
    @Given val fibM1: Fib220,
    @Given val fibM2: Fib219
)


@Given class Fib222 @Inject constructor(
    @Given val fibM1: Fib221,
    @Given val fibM2: Fib220
)


@Given class Fib223 @Inject constructor(
    @Given val fibM1: Fib222,
    @Given val fibM2: Fib221
)


@Given class Fib224 @Inject constructor(
    @Given val fibM1: Fib223,
    @Given val fibM2: Fib222
)


@Given class Fib225 @Inject constructor(
    @Given val fibM1: Fib224,
    @Given val fibM2: Fib223
)


@Given class Fib226 @Inject constructor(
    @Given val fibM1: Fib225,
    @Given val fibM2: Fib224
)


@Given class Fib227 @Inject constructor(
    @Given val fibM1: Fib226,
    @Given val fibM2: Fib225
)


@Given class Fib228 @Inject constructor(
    @Given val fibM1: Fib227,
    @Given val fibM2: Fib226
)


@Given class Fib229 @Inject constructor(
    @Given val fibM1: Fib228,
    @Given val fibM2: Fib227
)


@Given class Fib230 @Inject constructor(
    @Given val fibM1: Fib229,
    @Given val fibM2: Fib228
)


@Given class Fib231 @Inject constructor(
    @Given val fibM1: Fib230,
    @Given val fibM2: Fib229
)


@Given class Fib232 @Inject constructor(
    @Given val fibM1: Fib231,
    @Given val fibM2: Fib230
)


@Given class Fib233 @Inject constructor(
    @Given val fibM1: Fib232,
    @Given val fibM2: Fib231
)


@Given class Fib234 @Inject constructor(
    @Given val fibM1: Fib233,
    @Given val fibM2: Fib232
)


@Given class Fib235 @Inject constructor(
    @Given val fibM1: Fib234,
    @Given val fibM2: Fib233
)


@Given class Fib236 @Inject constructor(
    @Given val fibM1: Fib235,
    @Given val fibM2: Fib234
)


@Given class Fib237 @Inject constructor(
    @Given val fibM1: Fib236,
    @Given val fibM2: Fib235
)


@Given class Fib238 @Inject constructor(
    @Given val fibM1: Fib237,
    @Given val fibM2: Fib236
)


@Given class Fib239 @Inject constructor(
    @Given val fibM1: Fib238,
    @Given val fibM2: Fib237
)


@Given class Fib240 @Inject constructor(
    @Given val fibM1: Fib239,
    @Given val fibM2: Fib238
)


@Given class Fib241 @Inject constructor(
    @Given val fibM1: Fib240,
    @Given val fibM2: Fib239
)


@Given class Fib242 @Inject constructor(
    @Given val fibM1: Fib241,
    @Given val fibM2: Fib240
)


@Given class Fib243 @Inject constructor(
    @Given val fibM1: Fib242,
    @Given val fibM2: Fib241
)


@Given class Fib244 @Inject constructor(
    @Given val fibM1: Fib243,
    @Given val fibM2: Fib242
)


@Given class Fib245 @Inject constructor(
    @Given val fibM1: Fib244,
    @Given val fibM2: Fib243
)


@Given class Fib246 @Inject constructor(
    @Given val fibM1: Fib245,
    @Given val fibM2: Fib244
)


@Given class Fib247 @Inject constructor(
    @Given val fibM1: Fib246,
    @Given val fibM2: Fib245
)


@Given class Fib248 @Inject constructor(
    @Given val fibM1: Fib247,
    @Given val fibM2: Fib246
)


@Given class Fib249 @Inject constructor(
    @Given val fibM1: Fib248,
    @Given val fibM2: Fib247
)


@Given class Fib250 @Inject constructor(
    @Given val fibM1: Fib249,
    @Given val fibM2: Fib248
)


@Given class Fib251 @Inject constructor(
    @Given val fibM1: Fib250,
    @Given val fibM2: Fib249
)


@Given class Fib252 @Inject constructor(
    @Given val fibM1: Fib251,
    @Given val fibM2: Fib250
)


@Given class Fib253 @Inject constructor(
    @Given val fibM1: Fib252,
    @Given val fibM2: Fib251
)


@Given class Fib254 @Inject constructor(
    @Given val fibM1: Fib253,
    @Given val fibM2: Fib252
)


@Given class Fib255 @Inject constructor(
    @Given val fibM1: Fib254,
    @Given val fibM2: Fib253
)


@Given class Fib256 @Inject constructor(
    @Given val fibM1: Fib255,
    @Given val fibM2: Fib254
)


@Given class Fib257 @Inject constructor(
    @Given val fibM1: Fib256,
    @Given val fibM2: Fib255
)


@Given class Fib258 @Inject constructor(
    @Given val fibM1: Fib257,
    @Given val fibM2: Fib256
)


@Given class Fib259 @Inject constructor(
    @Given val fibM1: Fib258,
    @Given val fibM2: Fib257
)


@Given class Fib260 @Inject constructor(
    @Given val fibM1: Fib259,
    @Given val fibM2: Fib258
)


@Given class Fib261 @Inject constructor(
    @Given val fibM1: Fib260,
    @Given val fibM2: Fib259
)


@Given class Fib262 @Inject constructor(
    @Given val fibM1: Fib261,
    @Given val fibM2: Fib260
)


@Given class Fib263 @Inject constructor(
    @Given val fibM1: Fib262,
    @Given val fibM2: Fib261
)


@Given class Fib264 @Inject constructor(
    @Given val fibM1: Fib263,
    @Given val fibM2: Fib262
)


@Given class Fib265 @Inject constructor(
    @Given val fibM1: Fib264,
    @Given val fibM2: Fib263
)


@Given class Fib266 @Inject constructor(
    @Given val fibM1: Fib265,
    @Given val fibM2: Fib264
)


@Given class Fib267 @Inject constructor(
    @Given val fibM1: Fib266,
    @Given val fibM2: Fib265
)


@Given class Fib268 @Inject constructor(
    @Given val fibM1: Fib267,
    @Given val fibM2: Fib266
)


@Given class Fib269 @Inject constructor(
    @Given val fibM1: Fib268,
    @Given val fibM2: Fib267
)


@Given class Fib270 @Inject constructor(
    @Given val fibM1: Fib269,
    @Given val fibM2: Fib268
)


@Given class Fib271 @Inject constructor(
    @Given val fibM1: Fib270,
    @Given val fibM2: Fib269
)


@Given class Fib272 @Inject constructor(
    @Given val fibM1: Fib271,
    @Given val fibM2: Fib270
)


@Given class Fib273 @Inject constructor(
    @Given val fibM1: Fib272,
    @Given val fibM2: Fib271
)


@Given class Fib274 @Inject constructor(
    @Given val fibM1: Fib273,
    @Given val fibM2: Fib272
)


@Given class Fib275 @Inject constructor(
    @Given val fibM1: Fib274,
    @Given val fibM2: Fib273
)


@Given class Fib276 @Inject constructor(
    @Given val fibM1: Fib275,
    @Given val fibM2: Fib274
)


@Given class Fib277 @Inject constructor(
    @Given val fibM1: Fib276,
    @Given val fibM2: Fib275
)


@Given class Fib278 @Inject constructor(
    @Given val fibM1: Fib277,
    @Given val fibM2: Fib276
)


@Given class Fib279 @Inject constructor(
    @Given val fibM1: Fib278,
    @Given val fibM2: Fib277
)


@Given class Fib280 @Inject constructor(
    @Given val fibM1: Fib279,
    @Given val fibM2: Fib278
)


@Given class Fib281 @Inject constructor(
    @Given val fibM1: Fib280,
    @Given val fibM2: Fib279
)


@Given class Fib282 @Inject constructor(
    @Given val fibM1: Fib281,
    @Given val fibM2: Fib280
)


@Given class Fib283 @Inject constructor(
    @Given val fibM1: Fib282,
    @Given val fibM2: Fib281
)


@Given class Fib284 @Inject constructor(
    @Given val fibM1: Fib283,
    @Given val fibM2: Fib282
)


@Given class Fib285 @Inject constructor(
    @Given val fibM1: Fib284,
    @Given val fibM2: Fib283
)


@Given class Fib286 @Inject constructor(
    @Given val fibM1: Fib285,
    @Given val fibM2: Fib284
)


@Given class Fib287 @Inject constructor(
    @Given val fibM1: Fib286,
    @Given val fibM2: Fib285
)


@Given class Fib288 @Inject constructor(
    @Given val fibM1: Fib287,
    @Given val fibM2: Fib286
)


@Given class Fib289 @Inject constructor(
    @Given val fibM1: Fib288,
    @Given val fibM2: Fib287
)


@Given class Fib290 @Inject constructor(
    @Given val fibM1: Fib289,
    @Given val fibM2: Fib288
)


@Given class Fib291 @Inject constructor(
    @Given val fibM1: Fib290,
    @Given val fibM2: Fib289
)


@Given class Fib292 @Inject constructor(
    @Given val fibM1: Fib291,
    @Given val fibM2: Fib290
)


@Given class Fib293 @Inject constructor(
    @Given val fibM1: Fib292,
    @Given val fibM2: Fib291
)


@Given class Fib294 @Inject constructor(
    @Given val fibM1: Fib293,
    @Given val fibM2: Fib292
)


@Given class Fib295 @Inject constructor(
    @Given val fibM1: Fib294,
    @Given val fibM2: Fib293
)


@Given class Fib296 @Inject constructor(
    @Given val fibM1: Fib295,
    @Given val fibM2: Fib294
)


@Given class Fib297 @Inject constructor(
    @Given val fibM1: Fib296,
    @Given val fibM2: Fib295
)


@Given class Fib298 @Inject constructor(
    @Given val fibM1: Fib297,
    @Given val fibM2: Fib296
)


@Given class Fib299 @Inject constructor(
    @Given val fibM1: Fib298,
    @Given val fibM2: Fib297
)


@Given class Fib300 @Inject constructor(
    @Given val fibM1: Fib299,
    @Given val fibM2: Fib298
)


@Given class Fib301 @Inject constructor(
    @Given val fibM1: Fib300,
    @Given val fibM2: Fib299
)


@Given class Fib302 @Inject constructor(
    @Given val fibM1: Fib301,
    @Given val fibM2: Fib300
)


@Given class Fib303 @Inject constructor(
    @Given val fibM1: Fib302,
    @Given val fibM2: Fib301
)


@Given class Fib304 @Inject constructor(
    @Given val fibM1: Fib303,
    @Given val fibM2: Fib302
)


@Given class Fib305 @Inject constructor(
    @Given val fibM1: Fib304,
    @Given val fibM2: Fib303
)


@Given class Fib306 @Inject constructor(
    @Given val fibM1: Fib305,
    @Given val fibM2: Fib304
)


@Given class Fib307 @Inject constructor(
    @Given val fibM1: Fib306,
    @Given val fibM2: Fib305
)


@Given class Fib308 @Inject constructor(
    @Given val fibM1: Fib307,
    @Given val fibM2: Fib306
)


@Given class Fib309 @Inject constructor(
    @Given val fibM1: Fib308,
    @Given val fibM2: Fib307
)


@Given class Fib310 @Inject constructor(
    @Given val fibM1: Fib309,
    @Given val fibM2: Fib308
)


@Given class Fib311 @Inject constructor(
    @Given val fibM1: Fib310,
    @Given val fibM2: Fib309
)


@Given class Fib312 @Inject constructor(
    @Given val fibM1: Fib311,
    @Given val fibM2: Fib310
)


@Given class Fib313 @Inject constructor(
    @Given val fibM1: Fib312,
    @Given val fibM2: Fib311
)


@Given class Fib314 @Inject constructor(
    @Given val fibM1: Fib313,
    @Given val fibM2: Fib312
)


@Given class Fib315 @Inject constructor(
    @Given val fibM1: Fib314,
    @Given val fibM2: Fib313
)


@Given class Fib316 @Inject constructor(
    @Given val fibM1: Fib315,
    @Given val fibM2: Fib314
)


@Given class Fib317 @Inject constructor(
    @Given val fibM1: Fib316,
    @Given val fibM2: Fib315
)


@Given class Fib318 @Inject constructor(
    @Given val fibM1: Fib317,
    @Given val fibM2: Fib316
)


@Given class Fib319 @Inject constructor(
    @Given val fibM1: Fib318,
    @Given val fibM2: Fib317
)


@Given class Fib320 @Inject constructor(
    @Given val fibM1: Fib319,
    @Given val fibM2: Fib318
)


@Given class Fib321 @Inject constructor(
    @Given val fibM1: Fib320,
    @Given val fibM2: Fib319
)


@Given class Fib322 @Inject constructor(
    @Given val fibM1: Fib321,
    @Given val fibM2: Fib320
)


@Given class Fib323 @Inject constructor(
    @Given val fibM1: Fib322,
    @Given val fibM2: Fib321
)


@Given class Fib324 @Inject constructor(
    @Given val fibM1: Fib323,
    @Given val fibM2: Fib322
)


@Given class Fib325 @Inject constructor(
    @Given val fibM1: Fib324,
    @Given val fibM2: Fib323
)


@Given class Fib326 @Inject constructor(
    @Given val fibM1: Fib325,
    @Given val fibM2: Fib324
)


@Given class Fib327 @Inject constructor(
    @Given val fibM1: Fib326,
    @Given val fibM2: Fib325
)


@Given class Fib328 @Inject constructor(
    @Given val fibM1: Fib327,
    @Given val fibM2: Fib326
)


@Given class Fib329 @Inject constructor(
    @Given val fibM1: Fib328,
    @Given val fibM2: Fib327
)


@Given class Fib330 @Inject constructor(
    @Given val fibM1: Fib329,
    @Given val fibM2: Fib328
)


@Given class Fib331 @Inject constructor(
    @Given val fibM1: Fib330,
    @Given val fibM2: Fib329
)


@Given class Fib332 @Inject constructor(
    @Given val fibM1: Fib331,
    @Given val fibM2: Fib330
)


@Given class Fib333 @Inject constructor(
    @Given val fibM1: Fib332,
    @Given val fibM2: Fib331
)


@Given class Fib334 @Inject constructor(
    @Given val fibM1: Fib333,
    @Given val fibM2: Fib332
)


@Given class Fib335 @Inject constructor(
    @Given val fibM1: Fib334,
    @Given val fibM2: Fib333
)


@Given class Fib336 @Inject constructor(
    @Given val fibM1: Fib335,
    @Given val fibM2: Fib334
)


@Given class Fib337 @Inject constructor(
    @Given val fibM1: Fib336,
    @Given val fibM2: Fib335
)


@Given class Fib338 @Inject constructor(
    @Given val fibM1: Fib337,
    @Given val fibM2: Fib336
)


@Given class Fib339 @Inject constructor(
    @Given val fibM1: Fib338,
    @Given val fibM2: Fib337
)


@Given class Fib340 @Inject constructor(
    @Given val fibM1: Fib339,
    @Given val fibM2: Fib338
)


@Given class Fib341 @Inject constructor(
    @Given val fibM1: Fib340,
    @Given val fibM2: Fib339
)


@Given class Fib342 @Inject constructor(
    @Given val fibM1: Fib341,
    @Given val fibM2: Fib340
)


@Given class Fib343 @Inject constructor(
    @Given val fibM1: Fib342,
    @Given val fibM2: Fib341
)


@Given class Fib344 @Inject constructor(
    @Given val fibM1: Fib343,
    @Given val fibM2: Fib342
)


@Given class Fib345 @Inject constructor(
    @Given val fibM1: Fib344,
    @Given val fibM2: Fib343
)


@Given class Fib346 @Inject constructor(
    @Given val fibM1: Fib345,
    @Given val fibM2: Fib344
)


@Given class Fib347 @Inject constructor(
    @Given val fibM1: Fib346,
    @Given val fibM2: Fib345
)


@Given class Fib348 @Inject constructor(
    @Given val fibM1: Fib347,
    @Given val fibM2: Fib346
)


@Given class Fib349 @Inject constructor(
    @Given val fibM1: Fib348,
    @Given val fibM2: Fib347
)


@Given class Fib350 @Inject constructor(
    @Given val fibM1: Fib349,
    @Given val fibM2: Fib348
)


@Given class Fib351 @Inject constructor(
    @Given val fibM1: Fib350,
    @Given val fibM2: Fib349
)


@Given class Fib352 @Inject constructor(
    @Given val fibM1: Fib351,
    @Given val fibM2: Fib350
)


@Given class Fib353 @Inject constructor(
    @Given val fibM1: Fib352,
    @Given val fibM2: Fib351
)


@Given class Fib354 @Inject constructor(
    @Given val fibM1: Fib353,
    @Given val fibM2: Fib352
)


@Given class Fib355 @Inject constructor(
    @Given val fibM1: Fib354,
    @Given val fibM2: Fib353
)


@Given class Fib356 @Inject constructor(
    @Given val fibM1: Fib355,
    @Given val fibM2: Fib354
)


@Given class Fib357 @Inject constructor(
    @Given val fibM1: Fib356,
    @Given val fibM2: Fib355
)


@Given class Fib358 @Inject constructor(
    @Given val fibM1: Fib357,
    @Given val fibM2: Fib356
)


@Given class Fib359 @Inject constructor(
    @Given val fibM1: Fib358,
    @Given val fibM2: Fib357
)


@Given class Fib360 @Inject constructor(
    @Given val fibM1: Fib359,
    @Given val fibM2: Fib358
)


@Given class Fib361 @Inject constructor(
    @Given val fibM1: Fib360,
    @Given val fibM2: Fib359
)


@Given class Fib362 @Inject constructor(
    @Given val fibM1: Fib361,
    @Given val fibM2: Fib360
)


@Given class Fib363 @Inject constructor(
    @Given val fibM1: Fib362,
    @Given val fibM2: Fib361
)


@Given class Fib364 @Inject constructor(
    @Given val fibM1: Fib363,
    @Given val fibM2: Fib362
)


@Given class Fib365 @Inject constructor(
    @Given val fibM1: Fib364,
    @Given val fibM2: Fib363
)


@Given class Fib366 @Inject constructor(
    @Given val fibM1: Fib365,
    @Given val fibM2: Fib364
)


@Given class Fib367 @Inject constructor(
    @Given val fibM1: Fib366,
    @Given val fibM2: Fib365
)


@Given class Fib368 @Inject constructor(
    @Given val fibM1: Fib367,
    @Given val fibM2: Fib366
)


@Given class Fib369 @Inject constructor(
    @Given val fibM1: Fib368,
    @Given val fibM2: Fib367
)


@Given class Fib370 @Inject constructor(
    @Given val fibM1: Fib369,
    @Given val fibM2: Fib368
)


@Given class Fib371 @Inject constructor(
    @Given val fibM1: Fib370,
    @Given val fibM2: Fib369
)


@Given class Fib372 @Inject constructor(
    @Given val fibM1: Fib371,
    @Given val fibM2: Fib370
)


@Given class Fib373 @Inject constructor(
    @Given val fibM1: Fib372,
    @Given val fibM2: Fib371
)


@Given class Fib374 @Inject constructor(
    @Given val fibM1: Fib373,
    @Given val fibM2: Fib372
)


@Given class Fib375 @Inject constructor(
    @Given val fibM1: Fib374,
    @Given val fibM2: Fib373
)


@Given class Fib376 @Inject constructor(
    @Given val fibM1: Fib375,
    @Given val fibM2: Fib374
)


@Given class Fib377 @Inject constructor(
    @Given val fibM1: Fib376,
    @Given val fibM2: Fib375
)


@Given class Fib378 @Inject constructor(
    @Given val fibM1: Fib377,
    @Given val fibM2: Fib376
)


@Given class Fib379 @Inject constructor(
    @Given val fibM1: Fib378,
    @Given val fibM2: Fib377
)


@Given class Fib380 @Inject constructor(
    @Given val fibM1: Fib379,
    @Given val fibM2: Fib378
)


@Given class Fib381 @Inject constructor(
    @Given val fibM1: Fib380,
    @Given val fibM2: Fib379
)


@Given class Fib382 @Inject constructor(
    @Given val fibM1: Fib381,
    @Given val fibM2: Fib380
)


@Given class Fib383 @Inject constructor(
    @Given val fibM1: Fib382,
    @Given val fibM2: Fib381
)


@Given class Fib384 @Inject constructor(
    @Given val fibM1: Fib383,
    @Given val fibM2: Fib382
)


@Given class Fib385 @Inject constructor(
    @Given val fibM1: Fib384,
    @Given val fibM2: Fib383
)


@Given class Fib386 @Inject constructor(
    @Given val fibM1: Fib385,
    @Given val fibM2: Fib384
)


@Given class Fib387 @Inject constructor(
    @Given val fibM1: Fib386,
    @Given val fibM2: Fib385
)


@Given class Fib388 @Inject constructor(
    @Given val fibM1: Fib387,
    @Given val fibM2: Fib386
)


@Given class Fib389 @Inject constructor(
    @Given val fibM1: Fib388,
    @Given val fibM2: Fib387
)


@Given class Fib390 @Inject constructor(
    @Given val fibM1: Fib389,
    @Given val fibM2: Fib388
)


@Given class Fib391 @Inject constructor(
    @Given val fibM1: Fib390,
    @Given val fibM2: Fib389
)


@Given class Fib392 @Inject constructor(
    @Given val fibM1: Fib391,
    @Given val fibM2: Fib390
)


@Given class Fib393 @Inject constructor(
    @Given val fibM1: Fib392,
    @Given val fibM2: Fib391
)


@Given class Fib394 @Inject constructor(
    @Given val fibM1: Fib393,
    @Given val fibM2: Fib392
)


@Given class Fib395 @Inject constructor(
    @Given val fibM1: Fib394,
    @Given val fibM2: Fib393
)


@Given class Fib396 @Inject constructor(
    @Given val fibM1: Fib395,
    @Given val fibM2: Fib394
)


@Given class Fib397 @Inject constructor(
    @Given val fibM1: Fib396,
    @Given val fibM2: Fib395
)


@Given class Fib398 @Inject constructor(
    @Given val fibM1: Fib397,
    @Given val fibM2: Fib396
)


@Given class Fib399 @Inject constructor(
    @Given val fibM1: Fib398,
    @Given val fibM2: Fib397
)


@Given class Fib400 @Inject constructor(
    @Given val fibM1: Fib399,
    @Given val fibM2: Fib398
)
