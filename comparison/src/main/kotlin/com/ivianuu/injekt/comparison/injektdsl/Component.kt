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

package com.ivianuu.injekt.comparison.injektdsl

import com.ivianuu.injekt.Component
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

fun createComponent() = Component {
    factory { Fib1() }
    factory { Fib2() }
    factory { Fib3(get(), get()) }
    factory { Fib4(get(), get()) }
    factory { Fib5(get(), get()) }
    factory { Fib6(get(), get()) }
    factory { Fib7(get(), get()) }
    factory { Fib8(get(), get()) }
    factory { Fib9(get(), get()) }
    factory { Fib10(get(), get()) }
    factory { Fib11(get(), get()) }
    factory { Fib12(get(), get()) }
    factory { Fib13(get(), get()) }
    factory { Fib14(get(), get()) }
    factory { Fib15(get(), get()) }
    factory { Fib16(get(), get()) }
    factory { Fib17(get(), get()) }
    factory { Fib18(get(), get()) }
    factory { Fib19(get(), get()) }
    factory { Fib20(get(), get()) }
    factory { Fib21(get(), get()) }
    factory { Fib22(get(), get()) }
    factory { Fib23(get(), get()) }
    factory { Fib24(get(), get()) }
    factory { Fib25(get(), get()) }
    factory { Fib26(get(), get()) }
    factory { Fib27(get(), get()) }
    factory { Fib28(get(), get()) }
    factory { Fib29(get(), get()) }
    factory { Fib30(get(), get()) }
    factory { Fib31(get(), get()) }
    factory { Fib32(get(), get()) }
    factory { Fib33(get(), get()) }
    factory { Fib34(get(), get()) }
    factory { Fib35(get(), get()) }
    factory { Fib36(get(), get()) }
    factory { Fib37(get(), get()) }
    factory { Fib38(get(), get()) }
    factory { Fib39(get(), get()) }
    factory { Fib40(get(), get()) }
    factory { Fib41(get(), get()) }
    factory { Fib42(get(), get()) }
    factory { Fib43(get(), get()) }
    factory { Fib44(get(), get()) }
    factory { Fib45(get(), get()) }
    factory { Fib46(get(), get()) }
    factory { Fib47(get(), get()) }
    factory { Fib48(get(), get()) }
    factory { Fib49(get(), get()) }
    factory { Fib50(get(), get()) }
    factory { Fib51(get(), get()) }
    factory { Fib52(get(), get()) }
    factory { Fib53(get(), get()) }
    factory { Fib54(get(), get()) }
    factory { Fib55(get(), get()) }
    factory { Fib56(get(), get()) }
    factory { Fib57(get(), get()) }
    factory { Fib58(get(), get()) }
    factory { Fib59(get(), get()) }
    factory { Fib60(get(), get()) }
    factory { Fib61(get(), get()) }
    factory { Fib62(get(), get()) }
    factory { Fib63(get(), get()) }
    factory { Fib64(get(), get()) }
    factory { Fib65(get(), get()) }
    factory { Fib66(get(), get()) }
    factory { Fib67(get(), get()) }
    factory { Fib68(get(), get()) }
    factory { Fib69(get(), get()) }
    factory { Fib70(get(), get()) }
    factory { Fib71(get(), get()) }
    factory { Fib72(get(), get()) }
    factory { Fib73(get(), get()) }
    factory { Fib74(get(), get()) }
    factory { Fib75(get(), get()) }
    factory { Fib76(get(), get()) }
    factory { Fib77(get(), get()) }
    factory { Fib78(get(), get()) }
    factory { Fib79(get(), get()) }
    factory { Fib80(get(), get()) }
    factory { Fib81(get(), get()) }
    factory { Fib82(get(), get()) }
    factory { Fib83(get(), get()) }
    factory { Fib84(get(), get()) }
    factory { Fib85(get(), get()) }
    factory { Fib86(get(), get()) }
    factory { Fib87(get(), get()) }
    factory { Fib88(get(), get()) }
    factory { Fib89(get(), get()) }
    factory { Fib90(get(), get()) }
    factory { Fib91(get(), get()) }
    factory { Fib92(get(), get()) }
    factory { Fib93(get(), get()) }
    factory { Fib94(get(), get()) }
    factory { Fib95(get(), get()) }
    factory { Fib96(get(), get()) }
    factory { Fib97(get(), get()) }
    factory { Fib98(get(), get()) }
    factory { Fib99(get(), get()) }
    factory { Fib100(get(), get()) }
}
