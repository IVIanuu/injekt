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
