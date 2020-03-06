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

package com.ivianuu.injekt.comparison.container

import com.ivianuu.injekt.comparison.Fib1
import com.ivianuu.injekt.comparison.Fib10
import com.ivianuu.injekt.comparison.Fib100
import com.ivianuu.injekt.comparison.Fib11
import com.ivianuu.injekt.comparison.Fib12
import com.ivianuu.injekt.comparison.Fib13
import com.ivianuu.injekt.comparison.Fib14
import com.ivianuu.injekt.comparison.Fib15
import com.ivianuu.injekt.comparison.Fib16
import com.ivianuu.injekt.comparison.Fib17
import com.ivianuu.injekt.comparison.Fib18
import com.ivianuu.injekt.comparison.Fib19
import com.ivianuu.injekt.comparison.Fib2
import com.ivianuu.injekt.comparison.Fib20
import com.ivianuu.injekt.comparison.Fib21
import com.ivianuu.injekt.comparison.Fib22
import com.ivianuu.injekt.comparison.Fib23
import com.ivianuu.injekt.comparison.Fib24
import com.ivianuu.injekt.comparison.Fib25
import com.ivianuu.injekt.comparison.Fib26
import com.ivianuu.injekt.comparison.Fib27
import com.ivianuu.injekt.comparison.Fib28
import com.ivianuu.injekt.comparison.Fib29
import com.ivianuu.injekt.comparison.Fib3
import com.ivianuu.injekt.comparison.Fib30
import com.ivianuu.injekt.comparison.Fib31
import com.ivianuu.injekt.comparison.Fib32
import com.ivianuu.injekt.comparison.Fib33
import com.ivianuu.injekt.comparison.Fib34
import com.ivianuu.injekt.comparison.Fib35
import com.ivianuu.injekt.comparison.Fib36
import com.ivianuu.injekt.comparison.Fib37
import com.ivianuu.injekt.comparison.Fib38
import com.ivianuu.injekt.comparison.Fib39
import com.ivianuu.injekt.comparison.Fib4
import com.ivianuu.injekt.comparison.Fib40
import com.ivianuu.injekt.comparison.Fib41
import com.ivianuu.injekt.comparison.Fib42
import com.ivianuu.injekt.comparison.Fib43
import com.ivianuu.injekt.comparison.Fib44
import com.ivianuu.injekt.comparison.Fib45
import com.ivianuu.injekt.comparison.Fib46
import com.ivianuu.injekt.comparison.Fib47
import com.ivianuu.injekt.comparison.Fib48
import com.ivianuu.injekt.comparison.Fib49
import com.ivianuu.injekt.comparison.Fib5
import com.ivianuu.injekt.comparison.Fib50
import com.ivianuu.injekt.comparison.Fib51
import com.ivianuu.injekt.comparison.Fib52
import com.ivianuu.injekt.comparison.Fib53
import com.ivianuu.injekt.comparison.Fib54
import com.ivianuu.injekt.comparison.Fib55
import com.ivianuu.injekt.comparison.Fib56
import com.ivianuu.injekt.comparison.Fib57
import com.ivianuu.injekt.comparison.Fib58
import com.ivianuu.injekt.comparison.Fib59
import com.ivianuu.injekt.comparison.Fib6
import com.ivianuu.injekt.comparison.Fib60
import com.ivianuu.injekt.comparison.Fib61
import com.ivianuu.injekt.comparison.Fib62
import com.ivianuu.injekt.comparison.Fib63
import com.ivianuu.injekt.comparison.Fib64
import com.ivianuu.injekt.comparison.Fib65
import com.ivianuu.injekt.comparison.Fib66
import com.ivianuu.injekt.comparison.Fib67
import com.ivianuu.injekt.comparison.Fib68
import com.ivianuu.injekt.comparison.Fib69
import com.ivianuu.injekt.comparison.Fib7
import com.ivianuu.injekt.comparison.Fib70
import com.ivianuu.injekt.comparison.Fib71
import com.ivianuu.injekt.comparison.Fib72
import com.ivianuu.injekt.comparison.Fib73
import com.ivianuu.injekt.comparison.Fib74
import com.ivianuu.injekt.comparison.Fib75
import com.ivianuu.injekt.comparison.Fib76
import com.ivianuu.injekt.comparison.Fib77
import com.ivianuu.injekt.comparison.Fib78
import com.ivianuu.injekt.comparison.Fib79
import com.ivianuu.injekt.comparison.Fib8
import com.ivianuu.injekt.comparison.Fib80
import com.ivianuu.injekt.comparison.Fib81
import com.ivianuu.injekt.comparison.Fib82
import com.ivianuu.injekt.comparison.Fib83
import com.ivianuu.injekt.comparison.Fib84
import com.ivianuu.injekt.comparison.Fib85
import com.ivianuu.injekt.comparison.Fib86
import com.ivianuu.injekt.comparison.Fib87
import com.ivianuu.injekt.comparison.Fib88
import com.ivianuu.injekt.comparison.Fib89
import com.ivianuu.injekt.comparison.Fib9
import com.ivianuu.injekt.comparison.Fib90
import com.ivianuu.injekt.comparison.Fib91
import com.ivianuu.injekt.comparison.Fib92
import com.ivianuu.injekt.comparison.Fib93
import com.ivianuu.injekt.comparison.Fib94
import com.ivianuu.injekt.comparison.Fib95
import com.ivianuu.injekt.comparison.Fib96
import com.ivianuu.injekt.comparison.Fib97
import com.ivianuu.injekt.comparison.Fib98
import com.ivianuu.injekt.comparison.Fib99
import com.ivianuu.injekt.comparison.container.impl.Container
import com.ivianuu.injekt.comparison.container.impl.factory

fun createContainer2() = Container {
    factory<Fib1>()
    factory<Fib2>()
    factory<Fib3>()
    factory<Fib4>()
    factory<Fib5>()
    factory<Fib6>()
    factory<Fib7>()
    factory<Fib8>()
    factory<Fib9>()
    factory<Fib10>()
    factory<Fib11>()
    factory<Fib12>()
    factory<Fib13>()
    factory<Fib14>()
    factory<Fib15>()
    factory<Fib16>()
    factory<Fib17>()
    factory<Fib18>()
    factory<Fib19>()
    factory<Fib20>()
    factory<Fib21>()
    factory<Fib22>()
    factory<Fib23>()
    factory<Fib24>()
    factory<Fib25>()
    factory<Fib26>()
    factory<Fib27>()
    factory<Fib28>()
    factory<Fib29>()
    factory<Fib30>()
    factory<Fib31>()
    factory<Fib32>()
    factory<Fib33>()
    factory<Fib34>()
    factory<Fib35>()
    factory<Fib36>()
    factory<Fib37>()
    factory<Fib38>()
    factory<Fib39>()
    factory<Fib40>()
    factory<Fib41>()
    factory<Fib42>()
    factory<Fib43>()
    factory<Fib44>()
    factory<Fib45>()
    factory<Fib46>()
    factory<Fib47>()
    factory<Fib48>()
    factory<Fib49>()
    factory<Fib50>()
    factory<Fib51>()
    factory<Fib52>()
    factory<Fib53>()
    factory<Fib54>()
    factory<Fib55>()
    factory<Fib56>()
    factory<Fib57>()
    factory<Fib58>()
    factory<Fib59>()
    factory<Fib60>()
    factory<Fib61>()
    factory<Fib62>()
    factory<Fib63>()
    factory<Fib64>()
    factory<Fib65>()
    factory<Fib66>()
    factory<Fib67>()
    factory<Fib68>()
    factory<Fib69>()
    factory<Fib70>()
    factory<Fib71>()
    factory<Fib72>()
    factory<Fib73>()
    factory<Fib74>()
    factory<Fib75>()
    factory<Fib76>()
    factory<Fib77>()
    factory<Fib78>()
    factory<Fib79>()
    factory<Fib80>()
    factory<Fib81>()
    factory<Fib82>()
    factory<Fib83>()
    factory<Fib84>()
    factory<Fib85>()
    factory<Fib86>()
    factory<Fib87>()
    factory<Fib88>()
    factory<Fib89>()
    factory<Fib90>()
    factory<Fib91>()
    factory<Fib92>()
    factory<Fib93>()
    factory<Fib94>()
    factory<Fib95>()
    factory<Fib96>()
    factory<Fib97>()
    factory<Fib98>()
    factory<Fib99>()
    factory<Fib100>()
}
