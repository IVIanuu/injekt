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

package com.ivianuu.injekt.comparison.dagger

import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib10
import com.ivianuu.injekt.comparison.fibonacci.Fib100
import com.ivianuu.injekt.comparison.fibonacci.Fib11
import com.ivianuu.injekt.comparison.fibonacci.Fib12
import com.ivianuu.injekt.comparison.fibonacci.Fib13
import com.ivianuu.injekt.comparison.fibonacci.Fib14
import com.ivianuu.injekt.comparison.fibonacci.Fib15
import com.ivianuu.injekt.comparison.fibonacci.Fib16
import com.ivianuu.injekt.comparison.fibonacci.Fib17
import com.ivianuu.injekt.comparison.fibonacci.Fib18
import com.ivianuu.injekt.comparison.fibonacci.Fib19
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib20
import com.ivianuu.injekt.comparison.fibonacci.Fib21
import com.ivianuu.injekt.comparison.fibonacci.Fib22
import com.ivianuu.injekt.comparison.fibonacci.Fib23
import com.ivianuu.injekt.comparison.fibonacci.Fib24
import com.ivianuu.injekt.comparison.fibonacci.Fib25
import com.ivianuu.injekt.comparison.fibonacci.Fib26
import com.ivianuu.injekt.comparison.fibonacci.Fib27
import com.ivianuu.injekt.comparison.fibonacci.Fib28
import com.ivianuu.injekt.comparison.fibonacci.Fib29
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib30
import com.ivianuu.injekt.comparison.fibonacci.Fib31
import com.ivianuu.injekt.comparison.fibonacci.Fib32
import com.ivianuu.injekt.comparison.fibonacci.Fib33
import com.ivianuu.injekt.comparison.fibonacci.Fib34
import com.ivianuu.injekt.comparison.fibonacci.Fib35
import com.ivianuu.injekt.comparison.fibonacci.Fib36
import com.ivianuu.injekt.comparison.fibonacci.Fib37
import com.ivianuu.injekt.comparison.fibonacci.Fib38
import com.ivianuu.injekt.comparison.fibonacci.Fib39
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib40
import com.ivianuu.injekt.comparison.fibonacci.Fib41
import com.ivianuu.injekt.comparison.fibonacci.Fib42
import com.ivianuu.injekt.comparison.fibonacci.Fib43
import com.ivianuu.injekt.comparison.fibonacci.Fib44
import com.ivianuu.injekt.comparison.fibonacci.Fib45
import com.ivianuu.injekt.comparison.fibonacci.Fib46
import com.ivianuu.injekt.comparison.fibonacci.Fib47
import com.ivianuu.injekt.comparison.fibonacci.Fib48
import com.ivianuu.injekt.comparison.fibonacci.Fib49
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib50
import com.ivianuu.injekt.comparison.fibonacci.Fib51
import com.ivianuu.injekt.comparison.fibonacci.Fib52
import com.ivianuu.injekt.comparison.fibonacci.Fib53
import com.ivianuu.injekt.comparison.fibonacci.Fib54
import com.ivianuu.injekt.comparison.fibonacci.Fib55
import com.ivianuu.injekt.comparison.fibonacci.Fib56
import com.ivianuu.injekt.comparison.fibonacci.Fib57
import com.ivianuu.injekt.comparison.fibonacci.Fib58
import com.ivianuu.injekt.comparison.fibonacci.Fib59
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib60
import com.ivianuu.injekt.comparison.fibonacci.Fib61
import com.ivianuu.injekt.comparison.fibonacci.Fib62
import com.ivianuu.injekt.comparison.fibonacci.Fib63
import com.ivianuu.injekt.comparison.fibonacci.Fib64
import com.ivianuu.injekt.comparison.fibonacci.Fib65
import com.ivianuu.injekt.comparison.fibonacci.Fib66
import com.ivianuu.injekt.comparison.fibonacci.Fib67
import com.ivianuu.injekt.comparison.fibonacci.Fib68
import com.ivianuu.injekt.comparison.fibonacci.Fib69
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib70
import com.ivianuu.injekt.comparison.fibonacci.Fib71
import com.ivianuu.injekt.comparison.fibonacci.Fib72
import com.ivianuu.injekt.comparison.fibonacci.Fib73
import com.ivianuu.injekt.comparison.fibonacci.Fib74
import com.ivianuu.injekt.comparison.fibonacci.Fib75
import com.ivianuu.injekt.comparison.fibonacci.Fib76
import com.ivianuu.injekt.comparison.fibonacci.Fib77
import com.ivianuu.injekt.comparison.fibonacci.Fib78
import com.ivianuu.injekt.comparison.fibonacci.Fib79
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.comparison.fibonacci.Fib80
import com.ivianuu.injekt.comparison.fibonacci.Fib81
import com.ivianuu.injekt.comparison.fibonacci.Fib82
import com.ivianuu.injekt.comparison.fibonacci.Fib83
import com.ivianuu.injekt.comparison.fibonacci.Fib84
import com.ivianuu.injekt.comparison.fibonacci.Fib85
import com.ivianuu.injekt.comparison.fibonacci.Fib86
import com.ivianuu.injekt.comparison.fibonacci.Fib87
import com.ivianuu.injekt.comparison.fibonacci.Fib88
import com.ivianuu.injekt.comparison.fibonacci.Fib89
import com.ivianuu.injekt.comparison.fibonacci.Fib9
import com.ivianuu.injekt.comparison.fibonacci.Fib90
import com.ivianuu.injekt.comparison.fibonacci.Fib91
import com.ivianuu.injekt.comparison.fibonacci.Fib92
import com.ivianuu.injekt.comparison.fibonacci.Fib93
import com.ivianuu.injekt.comparison.fibonacci.Fib94
import com.ivianuu.injekt.comparison.fibonacci.Fib95
import com.ivianuu.injekt.comparison.fibonacci.Fib96
import com.ivianuu.injekt.comparison.fibonacci.Fib97
import com.ivianuu.injekt.comparison.fibonacci.Fib98
import com.ivianuu.injekt.comparison.fibonacci.Fib99
import daggerone.Module

@Module(
    injects = [
        Fib1::class,
        Fib2::class,
        Fib3::class,
        Fib4::class,
        Fib5::class,
        Fib6::class,
        Fib7::class,
        Fib8::class,
        Fib9::class,
        Fib10::class,
        Fib11::class,
        Fib12::class,
        Fib13::class,
        Fib14::class,
        Fib15::class,
        Fib16::class,
        Fib17::class,
        Fib18::class,
        Fib19::class,
        Fib20::class,
        Fib21::class,
        Fib22::class,
        Fib23::class,
        Fib24::class,
        Fib25::class,
        Fib26::class,
        Fib27::class,
        Fib28::class,
        Fib29::class,
        Fib30::class,
        Fib31::class,
        Fib32::class,
        Fib33::class,
        Fib34::class,
        Fib35::class,
        Fib36::class,
        Fib37::class,
        Fib38::class,
        Fib39::class,
        Fib40::class,
        Fib41::class,
        Fib42::class,
        Fib43::class,
        Fib44::class,
        Fib45::class,
        Fib46::class,
        Fib47::class,
        Fib48::class,
        Fib49::class,
        Fib50::class,
        Fib51::class,
        Fib52::class,
        Fib53::class,
        Fib54::class,
        Fib55::class,
        Fib56::class,
        Fib57::class,
        Fib58::class,
        Fib59::class,
        Fib60::class,
        Fib61::class,
        Fib62::class,
        Fib63::class,
        Fib64::class,
        Fib65::class,
        Fib66::class,
        Fib67::class,
        Fib68::class,
        Fib69::class,
        Fib70::class,
        Fib71::class,
        Fib72::class,
        Fib73::class,
        Fib74::class,
        Fib75::class,
        Fib76::class,
        Fib77::class,
        Fib78::class,
        Fib79::class,
        Fib80::class,
        Fib81::class,
        Fib82::class,
        Fib83::class,
        Fib84::class,
        Fib85::class,
        Fib86::class,
        Fib87::class,
        Fib88::class,
        Fib89::class,
        Fib90::class,
        Fib91::class,
        Fib92::class,
        Fib93::class,
        Fib94::class,
        Fib95::class,
        Fib96::class,
        Fib97::class,
        Fib98::class,
        Fib99::class,
        Fib100::class
    ]
)
class DaggerModule
