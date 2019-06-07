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

import com.ivianuu.injekt.bind
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

val injektModule = createModule()

fun createModule() = module {
    bind { Fib1() }
    bind { Fib2() }
    bind { Fib3(get(), get()) }
    bind { Fib4(get(), get()) }
    bind { Fib5(get(), get()) }
    bind { Fib6(get(), get()) }
    bind { Fib7(get(), get()) }
    bind { Fib8(get(), get()) }
    bind { Fib9(get(), get()) }
    bind { Fib10(get(), get()) }
    bind { Fib11(get(), get()) }
    bind { Fib12(get(), get()) }
    bind { Fib13(get(), get()) }
    bind { Fib14(get(), get()) }
    bind { Fib15(get(), get()) }
    bind { Fib16(get(), get()) }
    bind { Fib17(get(), get()) }
    bind { Fib18(get(), get()) }
    bind { Fib19(get(), get()) }
    bind { Fib20(get(), get()) }
    bind { Fib21(get(), get()) }
    bind { Fib22(get(), get()) }
    bind { Fib23(get(), get()) }
    bind { Fib24(get(), get()) }
    bind { Fib25(get(), get()) }
    bind { Fib26(get(), get()) }
    bind { Fib27(get(), get()) }
    bind { Fib28(get(), get()) }
    bind { Fib29(get(), get()) }
    bind { Fib30(get(), get()) }
    bind { Fib31(get(), get()) }
    bind { Fib32(get(), get()) }
    bind { Fib33(get(), get()) }
    bind { Fib34(get(), get()) }
    bind { Fib35(get(), get()) }
    bind { Fib36(get(), get()) }
    bind { Fib37(get(), get()) }
    bind { Fib38(get(), get()) }
    bind { Fib39(get(), get()) }
    bind { Fib40(get(), get()) }
    bind { Fib41(get(), get()) }
    bind { Fib42(get(), get()) }
    bind { Fib43(get(), get()) }
    bind { Fib44(get(), get()) }
    bind { Fib45(get(), get()) }
    bind { Fib46(get(), get()) }
    bind { Fib47(get(), get()) }
    bind { Fib48(get(), get()) }
    bind { Fib49(get(), get()) }
    bind { Fib50(get(), get()) }
    bind { Fib51(get(), get()) }
    bind { Fib52(get(), get()) }
    bind { Fib53(get(), get()) }
    bind { Fib54(get(), get()) }
    bind { Fib55(get(), get()) }
    bind { Fib56(get(), get()) }
    bind { Fib57(get(), get()) }
    bind { Fib58(get(), get()) }
    bind { Fib59(get(), get()) }
    bind { Fib60(get(), get()) }
    bind { Fib61(get(), get()) }
    bind { Fib62(get(), get()) }
    bind { Fib63(get(), get()) }
    bind { Fib64(get(), get()) }
    bind { Fib65(get(), get()) }
    bind { Fib66(get(), get()) }
    bind { Fib67(get(), get()) }
    bind { Fib68(get(), get()) }
    bind { Fib69(get(), get()) }
    bind { Fib70(get(), get()) }
    bind { Fib71(get(), get()) }
    bind { Fib72(get(), get()) }
    bind { Fib73(get(), get()) }
    bind { Fib74(get(), get()) }
    bind { Fib75(get(), get()) }
    bind { Fib76(get(), get()) }
    bind { Fib77(get(), get()) }
    bind { Fib78(get(), get()) }
    bind { Fib79(get(), get()) }
    bind { Fib80(get(), get()) }
    bind { Fib81(get(), get()) }
    bind { Fib82(get(), get()) }
    bind { Fib83(get(), get()) }
    bind { Fib84(get(), get()) }
    bind { Fib85(get(), get()) }
    bind { Fib86(get(), get()) }
    bind { Fib87(get(), get()) }
    bind { Fib88(get(), get()) }
    bind { Fib89(get(), get()) }
    bind { Fib90(get(), get()) }
    bind { Fib91(get(), get()) }
    bind { Fib92(get(), get()) }
    bind { Fib93(get(), get()) }
    bind { Fib94(get(), get()) }
    bind { Fib95(get(), get()) }
    bind { Fib96(get(), get()) }
    bind { Fib97(get(), get()) }
    bind { Fib98(get(), get()) }
    bind { Fib99(get(), get()) }
    bind { Fib100(get(), get()) }
}
