/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.comparison.injekt

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

import com.ivianuu.injekt.get
import com.ivianuu.injekt.module
import com.ivianuu.injekt.provide

val injektModule = createModule()

fun createModule() = module {
    provide { Fib1() }
    provide { Fib2() }
    provide { Fib3(get(), get()) }
    provide { Fib4(get(), get()) }
    provide { Fib5(get(), get()) }
    provide { Fib6(get(), get()) }
    provide { Fib7(get(), get()) }
    provide { Fib8(get(), get()) }
    provide { Fib9(get(), get()) }
    provide { Fib10(get(), get()) }
    provide { Fib11(get(), get()) }
    provide { Fib12(get(), get()) }
    provide { Fib13(get(), get()) }
    provide { Fib14(get(), get()) }
    provide { Fib15(get(), get()) }
    provide { Fib16(get(), get()) }
    provide { Fib17(get(), get()) }
    provide { Fib18(get(), get()) }
    provide { Fib19(get(), get()) }
    provide { Fib20(get(), get()) }
    provide { Fib21(get(), get()) }
    provide { Fib22(get(), get()) }
    provide { Fib23(get(), get()) }
    provide { Fib24(get(), get()) }
    provide { Fib25(get(), get()) }
    provide { Fib26(get(), get()) }
    provide { Fib27(get(), get()) }
    provide { Fib28(get(), get()) }
    provide { Fib29(get(), get()) }
    provide { Fib30(get(), get()) }
    provide { Fib31(get(), get()) }
    provide { Fib32(get(), get()) }
    provide { Fib33(get(), get()) }
    provide { Fib34(get(), get()) }
    provide { Fib35(get(), get()) }
    provide { Fib36(get(), get()) }
    provide { Fib37(get(), get()) }
    provide { Fib38(get(), get()) }
    provide { Fib39(get(), get()) }
    provide { Fib40(get(), get()) }
    provide { Fib41(get(), get()) }
    provide { Fib42(get(), get()) }
    provide { Fib43(get(), get()) }
    provide { Fib44(get(), get()) }
    provide { Fib45(get(), get()) }
    provide { Fib46(get(), get()) }
    provide { Fib47(get(), get()) }
    provide { Fib48(get(), get()) }
    provide { Fib49(get(), get()) }
    provide { Fib50(get(), get()) }
    provide { Fib51(get(), get()) }
    provide { Fib52(get(), get()) }
    provide { Fib53(get(), get()) }
    provide { Fib54(get(), get()) }
    provide { Fib55(get(), get()) }
    provide { Fib56(get(), get()) }
    provide { Fib57(get(), get()) }
    provide { Fib58(get(), get()) }
    provide { Fib59(get(), get()) }
    provide { Fib60(get(), get()) }
    provide { Fib61(get(), get()) }
    provide { Fib62(get(), get()) }
    provide { Fib63(get(), get()) }
    provide { Fib64(get(), get()) }
    provide { Fib65(get(), get()) }
    provide { Fib66(get(), get()) }
    provide { Fib67(get(), get()) }
    provide { Fib68(get(), get()) }
    provide { Fib69(get(), get()) }
    provide { Fib70(get(), get()) }
    provide { Fib71(get(), get()) }
    provide { Fib72(get(), get()) }
    provide { Fib73(get(), get()) }
    provide { Fib74(get(), get()) }
    provide { Fib75(get(), get()) }
    provide { Fib76(get(), get()) }
    provide { Fib77(get(), get()) }
    provide { Fib78(get(), get()) }
    provide { Fib79(get(), get()) }
    provide { Fib80(get(), get()) }
    provide { Fib81(get(), get()) }
    provide { Fib82(get(), get()) }
    provide { Fib83(get(), get()) }
    provide { Fib84(get(), get()) }
    provide { Fib85(get(), get()) }
    provide { Fib86(get(), get()) }
    provide { Fib87(get(), get()) }
    provide { Fib88(get(), get()) }
    provide { Fib89(get(), get()) }
    provide { Fib90(get(), get()) }
    provide { Fib91(get(), get()) }
    provide { Fib92(get(), get()) }
    provide { Fib93(get(), get()) }
    provide { Fib94(get(), get()) }
    provide { Fib95(get(), get()) }
    provide { Fib96(get(), get()) }
    provide { Fib97(get(), get()) }
    provide { Fib98(get(), get()) }
    provide { Fib99(get(), get()) }
    provide { Fib100(get(), get()) }
}
