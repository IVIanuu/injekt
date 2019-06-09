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

package com.ivianuu.injekt.comparison.injektunoptimizeddsl

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
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.module

val injektUnoptimizedModule = createModule()

fun createModule() = module {
    factory(optimizing = false) { Fib1() }
    factory(optimizing = false) { Fib2() }
    factory(optimizing = false) { Fib3(get(), get()) }
    factory(optimizing = false) { Fib4(get(), get()) }
    factory(optimizing = false) { Fib5(get(), get()) }
    factory(optimizing = false) { Fib6(get(), get()) }
    factory(optimizing = false) { Fib7(get(), get()) }
    factory(optimizing = false) { Fib8(get(), get()) }
    factory(optimizing = false) { Fib9(get(), get()) }
    factory(optimizing = false) { Fib10(get(), get()) }
    factory(optimizing = false) { Fib11(get(), get()) }
    factory(optimizing = false) { Fib12(get(), get()) }
    factory(optimizing = false) { Fib13(get(), get()) }
    factory(optimizing = false) { Fib14(get(), get()) }
    factory(optimizing = false) { Fib15(get(), get()) }
    factory(optimizing = false) { Fib16(get(), get()) }
    factory(optimizing = false) { Fib17(get(), get()) }
    factory(optimizing = false) { Fib18(get(), get()) }
    factory(optimizing = false) { Fib19(get(), get()) }
    factory(optimizing = false) { Fib20(get(), get()) }
    factory(optimizing = false) { Fib21(get(), get()) }
    factory(optimizing = false) { Fib22(get(), get()) }
    factory(optimizing = false) { Fib23(get(), get()) }
    factory(optimizing = false) { Fib24(get(), get()) }
    factory(optimizing = false) { Fib25(get(), get()) }
    factory(optimizing = false) { Fib26(get(), get()) }
    factory(optimizing = false) { Fib27(get(), get()) }
    factory(optimizing = false) { Fib28(get(), get()) }
    factory(optimizing = false) { Fib29(get(), get()) }
    factory(optimizing = false) { Fib30(get(), get()) }
    factory(optimizing = false) { Fib31(get(), get()) }
    factory(optimizing = false) { Fib32(get(), get()) }
    factory(optimizing = false) { Fib33(get(), get()) }
    factory(optimizing = false) { Fib34(get(), get()) }
    factory(optimizing = false) { Fib35(get(), get()) }
    factory(optimizing = false) { Fib36(get(), get()) }
    factory(optimizing = false) { Fib37(get(), get()) }
    factory(optimizing = false) { Fib38(get(), get()) }
    factory(optimizing = false) { Fib39(get(), get()) }
    factory(optimizing = false) { Fib40(get(), get()) }
    factory(optimizing = false) { Fib41(get(), get()) }
    factory(optimizing = false) { Fib42(get(), get()) }
    factory(optimizing = false) { Fib43(get(), get()) }
    factory(optimizing = false) { Fib44(get(), get()) }
    factory(optimizing = false) { Fib45(get(), get()) }
    factory(optimizing = false) { Fib46(get(), get()) }
    factory(optimizing = false) { Fib47(get(), get()) }
    factory(optimizing = false) { Fib48(get(), get()) }
    factory(optimizing = false) { Fib49(get(), get()) }
    factory(optimizing = false) { Fib50(get(), get()) }
    factory(optimizing = false) { Fib51(get(), get()) }
    factory(optimizing = false) { Fib52(get(), get()) }
    factory(optimizing = false) { Fib53(get(), get()) }
    factory(optimizing = false) { Fib54(get(), get()) }
    factory(optimizing = false) { Fib55(get(), get()) }
    factory(optimizing = false) { Fib56(get(), get()) }
    factory(optimizing = false) { Fib57(get(), get()) }
    factory(optimizing = false) { Fib58(get(), get()) }
    factory(optimizing = false) { Fib59(get(), get()) }
    factory(optimizing = false) { Fib60(get(), get()) }
    factory(optimizing = false) { Fib61(get(), get()) }
    factory(optimizing = false) { Fib62(get(), get()) }
    factory(optimizing = false) { Fib63(get(), get()) }
    factory(optimizing = false) { Fib64(get(), get()) }
    factory(optimizing = false) { Fib65(get(), get()) }
    factory(optimizing = false) { Fib66(get(), get()) }
    factory(optimizing = false) { Fib67(get(), get()) }
    factory(optimizing = false) { Fib68(get(), get()) }
    factory(optimizing = false) { Fib69(get(), get()) }
    factory(optimizing = false) { Fib70(get(), get()) }
    factory(optimizing = false) { Fib71(get(), get()) }
    factory(optimizing = false) { Fib72(get(), get()) }
    factory(optimizing = false) { Fib73(get(), get()) }
    factory(optimizing = false) { Fib74(get(), get()) }
    factory(optimizing = false) { Fib75(get(), get()) }
    factory(optimizing = false) { Fib76(get(), get()) }
    factory(optimizing = false) { Fib77(get(), get()) }
    factory(optimizing = false) { Fib78(get(), get()) }
    factory(optimizing = false) { Fib79(get(), get()) }
    factory(optimizing = false) { Fib80(get(), get()) }
    factory(optimizing = false) { Fib81(get(), get()) }
    factory(optimizing = false) { Fib82(get(), get()) }
    factory(optimizing = false) { Fib83(get(), get()) }
    factory(optimizing = false) { Fib84(get(), get()) }
    factory(optimizing = false) { Fib85(get(), get()) }
    factory(optimizing = false) { Fib86(get(), get()) }
    factory(optimizing = false) { Fib87(get(), get()) }
    factory(optimizing = false) { Fib88(get(), get()) }
    factory(optimizing = false) { Fib89(get(), get()) }
    factory(optimizing = false) { Fib90(get(), get()) }
    factory(optimizing = false) { Fib91(get(), get()) }
    factory(optimizing = false) { Fib92(get(), get()) }
    factory(optimizing = false) { Fib93(get(), get()) }
    factory(optimizing = false) { Fib94(get(), get()) }
    factory(optimizing = false) { Fib95(get(), get()) }
    factory(optimizing = false) { Fib96(get(), get()) }
    factory(optimizing = false) { Fib97(get(), get()) }
    factory(optimizing = false) { Fib98(get(), get()) }
    factory(optimizing = false) { Fib99(get(), get()) }
    factory(optimizing = false) { Fib100(get(), get()) }
}