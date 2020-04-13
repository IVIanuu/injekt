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

import com.ivianuu.injekt.synthetic.Factory
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
