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

package com.ivianuu.injekt.comparison.injektoptimized

import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.UnlinkedBinding
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

class Fib1Binding : LinkedBinding<Fib1>() {
    override fun get(parameters: ParametersDefinition?): Fib1 = Fib1()
}

class Fib2Binding : LinkedBinding<Fib2>() {
    override fun get(parameters: ParametersDefinition?): Fib2 = Fib2()
}

class UnlinkedFib3Binding : UnlinkedBinding<Fib3>() {
    override fun link(linker: Linker) =
        LinkedFib3Binding(linker.get(), linker.get())
}

class LinkedFib3Binding(
    private val fib2Binding: LinkedBinding<Fib2>,
    private val fib1Binding: LinkedBinding<Fib1>
) : LinkedBinding<Fib3>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib3(fib2Binding(), fib1Binding())
}

class UnlinkedFib4Binding : UnlinkedBinding<Fib4>() {
    override fun link(linker: Linker) =
        LinkedFib4Binding(linker.get(), linker.get())
}

class LinkedFib4Binding(
    private val fib3Binding: LinkedBinding<Fib3>,
    private val fib2Binding: LinkedBinding<Fib2>
) : LinkedBinding<Fib4>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib4(fib3Binding(), fib2Binding())
}

class UnlinkedFib5Binding : UnlinkedBinding<Fib5>() {
    override fun link(linker: Linker) =
        LinkedFib5Binding(linker.get(), linker.get())
}

class LinkedFib5Binding(
    private val fib4Binding: LinkedBinding<Fib4>,
    private val fib3Binding: LinkedBinding<Fib3>
) : LinkedBinding<Fib5>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib5(fib4Binding(), fib3Binding())
}

class UnlinkedFib6Binding : UnlinkedBinding<Fib6>() {
    override fun link(linker: Linker) =
        LinkedFib6Binding(linker.get(), linker.get())
}

class LinkedFib6Binding(
    private val fib5Binding: LinkedBinding<Fib5>,
    private val fib4Binding: LinkedBinding<Fib4>
) : LinkedBinding<Fib6>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib6(fib5Binding(), fib4Binding())
}

class UnlinkedFib7Binding : UnlinkedBinding<Fib7>() {
    override fun link(linker: Linker) =
        LinkedFib7Binding(linker.get(), linker.get())
}

class LinkedFib7Binding(
    private val fib6Binding: LinkedBinding<Fib6>,
    private val fib5Binding: LinkedBinding<Fib5>
) : LinkedBinding<Fib7>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib7(fib6Binding(), fib5Binding())
}

class UnlinkedFib8Binding : UnlinkedBinding<Fib8>() {
    override fun link(linker: Linker) =
        LinkedFib8Binding(linker.get(), linker.get())
}

class LinkedFib8Binding(
    private val fib7Binding: LinkedBinding<Fib7>,
    private val fib6Binding: LinkedBinding<Fib6>
) : LinkedBinding<Fib8>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib8(fib7Binding(), fib6Binding())
}

class UnlinkedFib9Binding : UnlinkedBinding<Fib9>() {
    override fun link(linker: Linker) =
        LinkedFib9Binding(linker.get(), linker.get())
}

class LinkedFib9Binding(
    private val fib8Binding: LinkedBinding<Fib8>,
    private val fib7Binding: LinkedBinding<Fib7>
) : LinkedBinding<Fib9>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib9(fib8Binding(), fib7Binding())
}

class UnlinkedFib10Binding : UnlinkedBinding<Fib10>() {
    override fun link(linker: Linker) =
        LinkedFib10Binding(linker.get(), linker.get())
}

class LinkedFib10Binding(
    private val fib9Binding: LinkedBinding<Fib9>,
    private val fib8Binding: LinkedBinding<Fib8>
) : LinkedBinding<Fib10>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib10(fib9Binding(), fib8Binding())
}

class UnlinkedFib11Binding : UnlinkedBinding<Fib11>() {
    override fun link(linker: Linker) =
        LinkedFib11Binding(linker.get(), linker.get())
}

class LinkedFib11Binding(
    private val fib10Binding: LinkedBinding<Fib10>,
    private val fib9Binding: LinkedBinding<Fib9>
) : LinkedBinding<Fib11>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib11(fib10Binding(), fib9Binding())
}

class UnlinkedFib12Binding : UnlinkedBinding<Fib12>() {
    override fun link(linker: Linker) =
        LinkedFib12Binding(linker.get(), linker.get())
}

class LinkedFib12Binding(
    private val fib11Binding: LinkedBinding<Fib11>,
    private val fib10Binding: LinkedBinding<Fib10>
) : LinkedBinding<Fib12>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib12(fib11Binding(), fib10Binding())
}

class UnlinkedFib13Binding : UnlinkedBinding<Fib13>() {
    override fun link(linker: Linker) =
        LinkedFib13Binding(linker.get(), linker.get())
}

class LinkedFib13Binding(
    private val fib12Binding: LinkedBinding<Fib12>,
    private val fib11Binding: LinkedBinding<Fib11>
) : LinkedBinding<Fib13>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib13(fib12Binding(), fib11Binding())
}

class UnlinkedFib14Binding : UnlinkedBinding<Fib14>() {
    override fun link(linker: Linker) =
        LinkedFib14Binding(linker.get(), linker.get())
}

class LinkedFib14Binding(
    private val fib13Binding: LinkedBinding<Fib13>,
    private val fib12Binding: LinkedBinding<Fib12>
) : LinkedBinding<Fib14>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib14(fib13Binding(), fib12Binding())
}

class UnlinkedFib15Binding : UnlinkedBinding<Fib15>() {
    override fun link(linker: Linker) =
        LinkedFib15Binding(linker.get(), linker.get())
}

class LinkedFib15Binding(
    private val fib14Binding: LinkedBinding<Fib14>,
    private val fib13Binding: LinkedBinding<Fib13>
) : LinkedBinding<Fib15>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib15(fib14Binding(), fib13Binding())
}

class UnlinkedFib16Binding : UnlinkedBinding<Fib16>() {
    override fun link(linker: Linker) =
        LinkedFib16Binding(linker.get(), linker.get())
}

class LinkedFib16Binding(
    private val fib15Binding: LinkedBinding<Fib15>,
    private val fib14Binding: LinkedBinding<Fib14>
) : LinkedBinding<Fib16>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib16(fib15Binding(), fib14Binding())
}

class UnlinkedFib17Binding : UnlinkedBinding<Fib17>() {
    override fun link(linker: Linker) =
        LinkedFib17Binding(linker.get(), linker.get())
}

class LinkedFib17Binding(
    private val fib16Binding: LinkedBinding<Fib16>,
    private val fib15Binding: LinkedBinding<Fib15>
) : LinkedBinding<Fib17>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib17(fib16Binding(), fib15Binding())
}

class UnlinkedFib18Binding : UnlinkedBinding<Fib18>() {
    override fun link(linker: Linker) =
        LinkedFib18Binding(linker.get(), linker.get())
}

class LinkedFib18Binding(
    private val fib17Binding: LinkedBinding<Fib17>,
    private val fib16Binding: LinkedBinding<Fib16>
) : LinkedBinding<Fib18>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib18(fib17Binding(), fib16Binding())
}

class UnlinkedFib19Binding : UnlinkedBinding<Fib19>() {
    override fun link(linker: Linker) =
        LinkedFib19Binding(linker.get(), linker.get())
}

class LinkedFib19Binding(
    private val fib18Binding: LinkedBinding<Fib18>,
    private val fib17Binding: LinkedBinding<Fib17>
) : LinkedBinding<Fib19>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib19(fib18Binding(), fib17Binding())
}

class UnlinkedFib20Binding : UnlinkedBinding<Fib20>() {
    override fun link(linker: Linker) =
        LinkedFib20Binding(linker.get(), linker.get())
}

class LinkedFib20Binding(
    private val fib19Binding: LinkedBinding<Fib19>,
    private val fib18Binding: LinkedBinding<Fib18>
) : LinkedBinding<Fib20>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib20(fib19Binding(), fib18Binding())
}

class UnlinkedFib21Binding : UnlinkedBinding<Fib21>() {
    override fun link(linker: Linker) =
        LinkedFib21Binding(linker.get(), linker.get())
}

class LinkedFib21Binding(
    private val fib20Binding: LinkedBinding<Fib20>,
    private val fib19Binding: LinkedBinding<Fib19>
) : LinkedBinding<Fib21>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib21(fib20Binding(), fib19Binding())
}

class UnlinkedFib22Binding : UnlinkedBinding<Fib22>() {
    override fun link(linker: Linker) =
        LinkedFib22Binding(linker.get(), linker.get())
}

class LinkedFib22Binding(
    private val fib21Binding: LinkedBinding<Fib21>,
    private val fib20Binding: LinkedBinding<Fib20>
) : LinkedBinding<Fib22>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib22(fib21Binding(), fib20Binding())
}

class UnlinkedFib23Binding : UnlinkedBinding<Fib23>() {
    override fun link(linker: Linker) =
        LinkedFib23Binding(linker.get(), linker.get())
}

class LinkedFib23Binding(
    private val fib22Binding: LinkedBinding<Fib22>,
    private val fib21Binding: LinkedBinding<Fib21>
) : LinkedBinding<Fib23>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib23(fib22Binding(), fib21Binding())
}

class UnlinkedFib24Binding : UnlinkedBinding<Fib24>() {
    override fun link(linker: Linker) =
        LinkedFib24Binding(linker.get(), linker.get())
}

class LinkedFib24Binding(
    private val fib23Binding: LinkedBinding<Fib23>,
    private val fib22Binding: LinkedBinding<Fib22>
) : LinkedBinding<Fib24>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib24(fib23Binding(), fib22Binding())
}

class UnlinkedFib25Binding : UnlinkedBinding<Fib25>() {
    override fun link(linker: Linker) =
        LinkedFib25Binding(linker.get(), linker.get())
}

class LinkedFib25Binding(
    private val fib24Binding: LinkedBinding<Fib24>,
    private val fib23Binding: LinkedBinding<Fib23>
) : LinkedBinding<Fib25>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib25(fib24Binding(), fib23Binding())
}

class UnlinkedFib26Binding : UnlinkedBinding<Fib26>() {
    override fun link(linker: Linker) =
        LinkedFib26Binding(linker.get(), linker.get())
}

class LinkedFib26Binding(
    private val fib25Binding: LinkedBinding<Fib25>,
    private val fib24Binding: LinkedBinding<Fib24>
) : LinkedBinding<Fib26>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib26(fib25Binding(), fib24Binding())
}

class UnlinkedFib27Binding : UnlinkedBinding<Fib27>() {
    override fun link(linker: Linker) =
        LinkedFib27Binding(linker.get(), linker.get())
}

class LinkedFib27Binding(
    private val fib26Binding: LinkedBinding<Fib26>,
    private val fib25Binding: LinkedBinding<Fib25>
) : LinkedBinding<Fib27>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib27(fib26Binding(), fib25Binding())
}

class UnlinkedFib28Binding : UnlinkedBinding<Fib28>() {
    override fun link(linker: Linker) =
        LinkedFib28Binding(linker.get(), linker.get())
}

class LinkedFib28Binding(
    private val fib27Binding: LinkedBinding<Fib27>,
    private val fib26Binding: LinkedBinding<Fib26>
) : LinkedBinding<Fib28>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib28(fib27Binding(), fib26Binding())
}

class UnlinkedFib29Binding : UnlinkedBinding<Fib29>() {
    override fun link(linker: Linker) =
        LinkedFib29Binding(linker.get(), linker.get())
}

class LinkedFib29Binding(
    private val fib28Binding: LinkedBinding<Fib28>,
    private val fib27Binding: LinkedBinding<Fib27>
) : LinkedBinding<Fib29>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib29(fib28Binding(), fib27Binding())
}

class UnlinkedFib30Binding : UnlinkedBinding<Fib30>() {
    override fun link(linker: Linker) =
        LinkedFib30Binding(linker.get(), linker.get())
}

class LinkedFib30Binding(
    private val fib29Binding: LinkedBinding<Fib29>,
    private val fib28Binding: LinkedBinding<Fib28>
) : LinkedBinding<Fib30>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib30(fib29Binding(), fib28Binding())
}

class UnlinkedFib31Binding : UnlinkedBinding<Fib31>() {
    override fun link(linker: Linker) =
        LinkedFib31Binding(linker.get(), linker.get())
}

class LinkedFib31Binding(
    private val fib30Binding: LinkedBinding<Fib30>,
    private val fib29Binding: LinkedBinding<Fib29>
) : LinkedBinding<Fib31>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib31(fib30Binding(), fib29Binding())
}

class UnlinkedFib32Binding : UnlinkedBinding<Fib32>() {
    override fun link(linker: Linker) =
        LinkedFib32Binding(linker.get(), linker.get())
}

class LinkedFib32Binding(
    private val fib31Binding: LinkedBinding<Fib31>,
    private val fib30Binding: LinkedBinding<Fib30>
) : LinkedBinding<Fib32>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib32(fib31Binding(), fib30Binding())
}

class UnlinkedFib33Binding : UnlinkedBinding<Fib33>() {
    override fun link(linker: Linker) =
        LinkedFib33Binding(linker.get(), linker.get())
}

class LinkedFib33Binding(
    private val fib32Binding: LinkedBinding<Fib32>,
    private val fib31Binding: LinkedBinding<Fib31>
) : LinkedBinding<Fib33>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib33(fib32Binding(), fib31Binding())
}

class UnlinkedFib34Binding : UnlinkedBinding<Fib34>() {
    override fun link(linker: Linker) =
        LinkedFib34Binding(linker.get(), linker.get())
}

class LinkedFib34Binding(
    private val fib33Binding: LinkedBinding<Fib33>,
    private val fib32Binding: LinkedBinding<Fib32>
) : LinkedBinding<Fib34>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib34(fib33Binding(), fib32Binding())
}

class UnlinkedFib35Binding : UnlinkedBinding<Fib35>() {
    override fun link(linker: Linker) =
        LinkedFib35Binding(linker.get(), linker.get())
}

class LinkedFib35Binding(
    private val fib34Binding: LinkedBinding<Fib34>,
    private val fib33Binding: LinkedBinding<Fib33>
) : LinkedBinding<Fib35>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib35(fib34Binding(), fib33Binding())
}

class UnlinkedFib36Binding : UnlinkedBinding<Fib36>() {
    override fun link(linker: Linker) =
        LinkedFib36Binding(linker.get(), linker.get())
}

class LinkedFib36Binding(
    private val fib35Binding: LinkedBinding<Fib35>,
    private val fib34Binding: LinkedBinding<Fib34>
) : LinkedBinding<Fib36>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib36(fib35Binding(), fib34Binding())
}

class UnlinkedFib37Binding : UnlinkedBinding<Fib37>() {
    override fun link(linker: Linker) =
        LinkedFib37Binding(linker.get(), linker.get())
}

class LinkedFib37Binding(
    private val fib36Binding: LinkedBinding<Fib36>,
    private val fib35Binding: LinkedBinding<Fib35>
) : LinkedBinding<Fib37>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib37(fib36Binding(), fib35Binding())
}

class UnlinkedFib38Binding : UnlinkedBinding<Fib38>() {
    override fun link(linker: Linker) =
        LinkedFib38Binding(linker.get(), linker.get())
}

class LinkedFib38Binding(
    private val fib37Binding: LinkedBinding<Fib37>,
    private val fib36Binding: LinkedBinding<Fib36>
) : LinkedBinding<Fib38>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib38(fib37Binding(), fib36Binding())
}

class UnlinkedFib39Binding : UnlinkedBinding<Fib39>() {
    override fun link(linker: Linker) =
        LinkedFib39Binding(linker.get(), linker.get())
}

class LinkedFib39Binding(
    private val fib38Binding: LinkedBinding<Fib38>,
    private val fib37Binding: LinkedBinding<Fib37>
) : LinkedBinding<Fib39>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib39(fib38Binding(), fib37Binding())
}

class UnlinkedFib40Binding : UnlinkedBinding<Fib40>() {
    override fun link(linker: Linker) =
        LinkedFib40Binding(linker.get(), linker.get())
}

class LinkedFib40Binding(
    private val fib39Binding: LinkedBinding<Fib39>,
    private val fib38Binding: LinkedBinding<Fib38>
) : LinkedBinding<Fib40>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib40(fib39Binding(), fib38Binding())
}

class UnlinkedFib41Binding : UnlinkedBinding<Fib41>() {
    override fun link(linker: Linker) =
        LinkedFib41Binding(linker.get(), linker.get())
}

class LinkedFib41Binding(
    private val fib40Binding: LinkedBinding<Fib40>,
    private val fib39Binding: LinkedBinding<Fib39>
) : LinkedBinding<Fib41>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib41(fib40Binding(), fib39Binding())
}

class UnlinkedFib42Binding : UnlinkedBinding<Fib42>() {
    override fun link(linker: Linker) =
        LinkedFib42Binding(linker.get(), linker.get())
}

class LinkedFib42Binding(
    private val fib41Binding: LinkedBinding<Fib41>,
    private val fib40Binding: LinkedBinding<Fib40>
) : LinkedBinding<Fib42>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib42(fib41Binding(), fib40Binding())
}

class UnlinkedFib43Binding : UnlinkedBinding<Fib43>() {
    override fun link(linker: Linker) =
        LinkedFib43Binding(linker.get(), linker.get())
}

class LinkedFib43Binding(
    private val fib42Binding: LinkedBinding<Fib42>,
    private val fib41Binding: LinkedBinding<Fib41>
) : LinkedBinding<Fib43>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib43(fib42Binding(), fib41Binding())
}

class UnlinkedFib44Binding : UnlinkedBinding<Fib44>() {
    override fun link(linker: Linker) =
        LinkedFib44Binding(linker.get(), linker.get())
}

class LinkedFib44Binding(
    private val fib43Binding: LinkedBinding<Fib43>,
    private val fib42Binding: LinkedBinding<Fib42>
) : LinkedBinding<Fib44>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib44(fib43Binding(), fib42Binding())
}

class UnlinkedFib45Binding : UnlinkedBinding<Fib45>() {
    override fun link(linker: Linker) =
        LinkedFib45Binding(linker.get(), linker.get())
}

class LinkedFib45Binding(
    private val fib44Binding: LinkedBinding<Fib44>,
    private val fib43Binding: LinkedBinding<Fib43>
) : LinkedBinding<Fib45>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib45(fib44Binding(), fib43Binding())
}

class UnlinkedFib46Binding : UnlinkedBinding<Fib46>() {
    override fun link(linker: Linker) =
        LinkedFib46Binding(linker.get(), linker.get())
}

class LinkedFib46Binding(
    private val fib45Binding: LinkedBinding<Fib45>,
    private val fib44Binding: LinkedBinding<Fib44>
) : LinkedBinding<Fib46>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib46(fib45Binding(), fib44Binding())
}

class UnlinkedFib47Binding : UnlinkedBinding<Fib47>() {
    override fun link(linker: Linker) =
        LinkedFib47Binding(linker.get(), linker.get())
}

class LinkedFib47Binding(
    private val fib46Binding: LinkedBinding<Fib46>,
    private val fib45Binding: LinkedBinding<Fib45>
) : LinkedBinding<Fib47>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib47(fib46Binding(), fib45Binding())
}

class UnlinkedFib48Binding : UnlinkedBinding<Fib48>() {
    override fun link(linker: Linker) =
        LinkedFib48Binding(linker.get(), linker.get())
}

class LinkedFib48Binding(
    private val fib47Binding: LinkedBinding<Fib47>,
    private val fib46Binding: LinkedBinding<Fib46>
) : LinkedBinding<Fib48>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib48(fib47Binding(), fib46Binding())
}

class UnlinkedFib49Binding : UnlinkedBinding<Fib49>() {
    override fun link(linker: Linker) =
        LinkedFib49Binding(linker.get(), linker.get())
}

class LinkedFib49Binding(
    private val fib48Binding: LinkedBinding<Fib48>,
    private val fib47Binding: LinkedBinding<Fib47>
) : LinkedBinding<Fib49>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib49(fib48Binding(), fib47Binding())
}

class UnlinkedFib50Binding : UnlinkedBinding<Fib50>() {
    override fun link(linker: Linker) =
        LinkedFib50Binding(linker.get(), linker.get())
}

class LinkedFib50Binding(
    private val fib49Binding: LinkedBinding<Fib49>,
    private val fib48Binding: LinkedBinding<Fib48>
) : LinkedBinding<Fib50>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib50(fib49Binding(), fib48Binding())
}

class UnlinkedFib51Binding : UnlinkedBinding<Fib51>() {
    override fun link(linker: Linker) =
        LinkedFib51Binding(linker.get(), linker.get())
}

class LinkedFib51Binding(
    private val fib50Binding: LinkedBinding<Fib50>,
    private val fib49Binding: LinkedBinding<Fib49>
) : LinkedBinding<Fib51>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib51(fib50Binding(), fib49Binding())
}

class UnlinkedFib52Binding : UnlinkedBinding<Fib52>() {
    override fun link(linker: Linker) =
        LinkedFib52Binding(linker.get(), linker.get())
}

class LinkedFib52Binding(
    private val fib51Binding: LinkedBinding<Fib51>,
    private val fib50Binding: LinkedBinding<Fib50>
) : LinkedBinding<Fib52>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib52(fib51Binding(), fib50Binding())
}

class UnlinkedFib53Binding : UnlinkedBinding<Fib53>() {
    override fun link(linker: Linker) =
        LinkedFib53Binding(linker.get(), linker.get())
}

class LinkedFib53Binding(
    private val fib52Binding: LinkedBinding<Fib52>,
    private val fib51Binding: LinkedBinding<Fib51>
) : LinkedBinding<Fib53>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib53(fib52Binding(), fib51Binding())
}

class UnlinkedFib54Binding : UnlinkedBinding<Fib54>() {
    override fun link(linker: Linker) =
        LinkedFib54Binding(linker.get(), linker.get())
}

class LinkedFib54Binding(
    private val fib53Binding: LinkedBinding<Fib53>,
    private val fib52Binding: LinkedBinding<Fib52>
) : LinkedBinding<Fib54>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib54(fib53Binding(), fib52Binding())
}

class UnlinkedFib55Binding : UnlinkedBinding<Fib55>() {
    override fun link(linker: Linker) =
        LinkedFib55Binding(linker.get(), linker.get())
}

class LinkedFib55Binding(
    private val fib54Binding: LinkedBinding<Fib54>,
    private val fib53Binding: LinkedBinding<Fib53>
) : LinkedBinding<Fib55>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib55(fib54Binding(), fib53Binding())
}

class UnlinkedFib56Binding : UnlinkedBinding<Fib56>() {
    override fun link(linker: Linker) =
        LinkedFib56Binding(linker.get(), linker.get())
}

class LinkedFib56Binding(
    private val fib55Binding: LinkedBinding<Fib55>,
    private val fib54Binding: LinkedBinding<Fib54>
) : LinkedBinding<Fib56>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib56(fib55Binding(), fib54Binding())
}

class UnlinkedFib57Binding : UnlinkedBinding<Fib57>() {
    override fun link(linker: Linker) =
        LinkedFib57Binding(linker.get(), linker.get())
}

class LinkedFib57Binding(
    private val fib56Binding: LinkedBinding<Fib56>,
    private val fib55Binding: LinkedBinding<Fib55>
) : LinkedBinding<Fib57>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib57(fib56Binding(), fib55Binding())
}

class UnlinkedFib58Binding : UnlinkedBinding<Fib58>() {
    override fun link(linker: Linker) =
        LinkedFib58Binding(linker.get(), linker.get())
}

class LinkedFib58Binding(
    private val fib57Binding: LinkedBinding<Fib57>,
    private val fib56Binding: LinkedBinding<Fib56>
) : LinkedBinding<Fib58>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib58(fib57Binding(), fib56Binding())
}

class UnlinkedFib59Binding : UnlinkedBinding<Fib59>() {
    override fun link(linker: Linker) =
        LinkedFib59Binding(linker.get(), linker.get())
}

class LinkedFib59Binding(
    private val fib58Binding: LinkedBinding<Fib58>,
    private val fib57Binding: LinkedBinding<Fib57>
) : LinkedBinding<Fib59>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib59(fib58Binding(), fib57Binding())
}

class UnlinkedFib60Binding : UnlinkedBinding<Fib60>() {
    override fun link(linker: Linker) =
        LinkedFib60Binding(linker.get(), linker.get())
}

class LinkedFib60Binding(
    private val fib59Binding: LinkedBinding<Fib59>,
    private val fib58Binding: LinkedBinding<Fib58>
) : LinkedBinding<Fib60>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib60(fib59Binding(), fib58Binding())
}

class UnlinkedFib61Binding : UnlinkedBinding<Fib61>() {
    override fun link(linker: Linker) =
        LinkedFib61Binding(linker.get(), linker.get())
}

class LinkedFib61Binding(
    private val fib60Binding: LinkedBinding<Fib60>,
    private val fib59Binding: LinkedBinding<Fib59>
) : LinkedBinding<Fib61>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib61(fib60Binding(), fib59Binding())
}

class UnlinkedFib62Binding : UnlinkedBinding<Fib62>() {
    override fun link(linker: Linker) =
        LinkedFib62Binding(linker.get(), linker.get())
}

class LinkedFib62Binding(
    private val fib61Binding: LinkedBinding<Fib61>,
    private val fib60Binding: LinkedBinding<Fib60>
) : LinkedBinding<Fib62>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib62(fib61Binding(), fib60Binding())
}

class UnlinkedFib63Binding : UnlinkedBinding<Fib63>() {
    override fun link(linker: Linker) =
        LinkedFib63Binding(linker.get(), linker.get())
}

class LinkedFib63Binding(
    private val fib62Binding: LinkedBinding<Fib62>,
    private val fib61Binding: LinkedBinding<Fib61>
) : LinkedBinding<Fib63>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib63(fib62Binding(), fib61Binding())
}

class UnlinkedFib64Binding : UnlinkedBinding<Fib64>() {
    override fun link(linker: Linker) =
        LinkedFib64Binding(linker.get(), linker.get())
}

class LinkedFib64Binding(
    private val fib63Binding: LinkedBinding<Fib63>,
    private val fib62Binding: LinkedBinding<Fib62>
) : LinkedBinding<Fib64>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib64(fib63Binding(), fib62Binding())
}

class UnlinkedFib65Binding : UnlinkedBinding<Fib65>() {
    override fun link(linker: Linker) =
        LinkedFib65Binding(linker.get(), linker.get())
}

class LinkedFib65Binding(
    private val fib64Binding: LinkedBinding<Fib64>,
    private val fib63Binding: LinkedBinding<Fib63>
) : LinkedBinding<Fib65>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib65(fib64Binding(), fib63Binding())
}

class UnlinkedFib66Binding : UnlinkedBinding<Fib66>() {
    override fun link(linker: Linker) =
        LinkedFib66Binding(linker.get(), linker.get())
}

class LinkedFib66Binding(
    private val fib65Binding: LinkedBinding<Fib65>,
    private val fib64Binding: LinkedBinding<Fib64>
) : LinkedBinding<Fib66>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib66(fib65Binding(), fib64Binding())
}

class UnlinkedFib67Binding : UnlinkedBinding<Fib67>() {
    override fun link(linker: Linker) =
        LinkedFib67Binding(linker.get(), linker.get())
}

class LinkedFib67Binding(
    private val fib66Binding: LinkedBinding<Fib66>,
    private val fib65Binding: LinkedBinding<Fib65>
) : LinkedBinding<Fib67>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib67(fib66Binding(), fib65Binding())
}

class UnlinkedFib68Binding : UnlinkedBinding<Fib68>() {
    override fun link(linker: Linker) =
        LinkedFib68Binding(linker.get(), linker.get())
}

class LinkedFib68Binding(
    private val fib67Binding: LinkedBinding<Fib67>,
    private val fib66Binding: LinkedBinding<Fib66>
) : LinkedBinding<Fib68>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib68(fib67Binding(), fib66Binding())
}

class UnlinkedFib69Binding : UnlinkedBinding<Fib69>() {
    override fun link(linker: Linker) =
        LinkedFib69Binding(linker.get(), linker.get())
}

class LinkedFib69Binding(
    private val fib68Binding: LinkedBinding<Fib68>,
    private val fib67Binding: LinkedBinding<Fib67>
) : LinkedBinding<Fib69>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib69(fib68Binding(), fib67Binding())
}

class UnlinkedFib70Binding : UnlinkedBinding<Fib70>() {
    override fun link(linker: Linker) =
        LinkedFib70Binding(linker.get(), linker.get())
}

class LinkedFib70Binding(
    private val fib69Binding: LinkedBinding<Fib69>,
    private val fib68Binding: LinkedBinding<Fib68>
) : LinkedBinding<Fib70>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib70(fib69Binding(), fib68Binding())
}

class UnlinkedFib71Binding : UnlinkedBinding<Fib71>() {
    override fun link(linker: Linker) =
        LinkedFib71Binding(linker.get(), linker.get())
}

class LinkedFib71Binding(
    private val fib70Binding: LinkedBinding<Fib70>,
    private val fib69Binding: LinkedBinding<Fib69>
) : LinkedBinding<Fib71>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib71(fib70Binding(), fib69Binding())
}

class UnlinkedFib72Binding : UnlinkedBinding<Fib72>() {
    override fun link(linker: Linker) =
        LinkedFib72Binding(linker.get(), linker.get())
}

class LinkedFib72Binding(
    private val fib71Binding: LinkedBinding<Fib71>,
    private val fib70Binding: LinkedBinding<Fib70>
) : LinkedBinding<Fib72>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib72(fib71Binding(), fib70Binding())
}

class UnlinkedFib73Binding : UnlinkedBinding<Fib73>() {
    override fun link(linker: Linker) =
        LinkedFib73Binding(linker.get(), linker.get())
}

class LinkedFib73Binding(
    private val fib72Binding: LinkedBinding<Fib72>,
    private val fib71Binding: LinkedBinding<Fib71>
) : LinkedBinding<Fib73>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib73(fib72Binding(), fib71Binding())
}

class UnlinkedFib74Binding : UnlinkedBinding<Fib74>() {
    override fun link(linker: Linker) =
        LinkedFib74Binding(linker.get(), linker.get())
}

class LinkedFib74Binding(
    private val fib73Binding: LinkedBinding<Fib73>,
    private val fib72Binding: LinkedBinding<Fib72>
) : LinkedBinding<Fib74>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib74(fib73Binding(), fib72Binding())
}

class UnlinkedFib75Binding : UnlinkedBinding<Fib75>() {
    override fun link(linker: Linker) =
        LinkedFib75Binding(linker.get(), linker.get())
}

class LinkedFib75Binding(
    private val fib74Binding: LinkedBinding<Fib74>,
    private val fib73Binding: LinkedBinding<Fib73>
) : LinkedBinding<Fib75>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib75(fib74Binding(), fib73Binding())
}

class UnlinkedFib76Binding : UnlinkedBinding<Fib76>() {
    override fun link(linker: Linker) =
        LinkedFib76Binding(linker.get(), linker.get())
}

class LinkedFib76Binding(
    private val fib75Binding: LinkedBinding<Fib75>,
    private val fib74Binding: LinkedBinding<Fib74>
) : LinkedBinding<Fib76>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib76(fib75Binding(), fib74Binding())
}

class UnlinkedFib77Binding : UnlinkedBinding<Fib77>() {
    override fun link(linker: Linker) =
        LinkedFib77Binding(linker.get(), linker.get())
}

class LinkedFib77Binding(
    private val fib76Binding: LinkedBinding<Fib76>,
    private val fib75Binding: LinkedBinding<Fib75>
) : LinkedBinding<Fib77>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib77(fib76Binding(), fib75Binding())
}

class UnlinkedFib78Binding : UnlinkedBinding<Fib78>() {
    override fun link(linker: Linker) =
        LinkedFib78Binding(linker.get(), linker.get())
}

class LinkedFib78Binding(
    private val fib77Binding: LinkedBinding<Fib77>,
    private val fib76Binding: LinkedBinding<Fib76>
) : LinkedBinding<Fib78>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib78(fib77Binding(), fib76Binding())
}

class UnlinkedFib79Binding : UnlinkedBinding<Fib79>() {
    override fun link(linker: Linker) =
        LinkedFib79Binding(linker.get(), linker.get())
}

class LinkedFib79Binding(
    private val fib78Binding: LinkedBinding<Fib78>,
    private val fib77Binding: LinkedBinding<Fib77>
) : LinkedBinding<Fib79>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib79(fib78Binding(), fib77Binding())
}

class UnlinkedFib80Binding : UnlinkedBinding<Fib80>() {
    override fun link(linker: Linker) =
        LinkedFib80Binding(linker.get(), linker.get())
}

class LinkedFib80Binding(
    private val fib79Binding: LinkedBinding<Fib79>,
    private val fib78Binding: LinkedBinding<Fib78>
) : LinkedBinding<Fib80>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib80(fib79Binding(), fib78Binding())
}

class UnlinkedFib81Binding : UnlinkedBinding<Fib81>() {
    override fun link(linker: Linker) =
        LinkedFib81Binding(linker.get(), linker.get())
}

class LinkedFib81Binding(
    private val fib80Binding: LinkedBinding<Fib80>,
    private val fib79Binding: LinkedBinding<Fib79>
) : LinkedBinding<Fib81>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib81(fib80Binding(), fib79Binding())
}

class UnlinkedFib82Binding : UnlinkedBinding<Fib82>() {
    override fun link(linker: Linker) =
        LinkedFib82Binding(linker.get(), linker.get())
}

class LinkedFib82Binding(
    private val fib81Binding: LinkedBinding<Fib81>,
    private val fib80Binding: LinkedBinding<Fib80>
) : LinkedBinding<Fib82>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib82(fib81Binding(), fib80Binding())
}

class UnlinkedFib83Binding : UnlinkedBinding<Fib83>() {
    override fun link(linker: Linker) =
        LinkedFib83Binding(linker.get(), linker.get())
}

class LinkedFib83Binding(
    private val fib82Binding: LinkedBinding<Fib82>,
    private val fib81Binding: LinkedBinding<Fib81>
) : LinkedBinding<Fib83>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib83(fib82Binding(), fib81Binding())
}

class UnlinkedFib84Binding : UnlinkedBinding<Fib84>() {
    override fun link(linker: Linker) =
        LinkedFib84Binding(linker.get(), linker.get())
}

class LinkedFib84Binding(
    private val fib83Binding: LinkedBinding<Fib83>,
    private val fib82Binding: LinkedBinding<Fib82>
) : LinkedBinding<Fib84>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib84(fib83Binding(), fib82Binding())
}

class UnlinkedFib85Binding : UnlinkedBinding<Fib85>() {
    override fun link(linker: Linker) =
        LinkedFib85Binding(linker.get(), linker.get())
}

class LinkedFib85Binding(
    private val fib84Binding: LinkedBinding<Fib84>,
    private val fib83Binding: LinkedBinding<Fib83>
) : LinkedBinding<Fib85>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib85(fib84Binding(), fib83Binding())
}

class UnlinkedFib86Binding : UnlinkedBinding<Fib86>() {
    override fun link(linker: Linker) =
        LinkedFib86Binding(linker.get(), linker.get())
}

class LinkedFib86Binding(
    private val fib85Binding: LinkedBinding<Fib85>,
    private val fib84Binding: LinkedBinding<Fib84>
) : LinkedBinding<Fib86>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib86(fib85Binding(), fib84Binding())
}

class UnlinkedFib87Binding : UnlinkedBinding<Fib87>() {
    override fun link(linker: Linker) =
        LinkedFib87Binding(linker.get(), linker.get())
}

class LinkedFib87Binding(
    private val fib86Binding: LinkedBinding<Fib86>,
    private val fib85Binding: LinkedBinding<Fib85>
) : LinkedBinding<Fib87>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib87(fib86Binding(), fib85Binding())
}

class UnlinkedFib88Binding : UnlinkedBinding<Fib88>() {
    override fun link(linker: Linker) =
        LinkedFib88Binding(linker.get(), linker.get())
}

class LinkedFib88Binding(
    private val fib87Binding: LinkedBinding<Fib87>,
    private val fib86Binding: LinkedBinding<Fib86>
) : LinkedBinding<Fib88>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib88(fib87Binding(), fib86Binding())
}

class UnlinkedFib89Binding : UnlinkedBinding<Fib89>() {
    override fun link(linker: Linker) =
        LinkedFib89Binding(linker.get(), linker.get())
}

class LinkedFib89Binding(
    private val fib88Binding: LinkedBinding<Fib88>,
    private val fib87Binding: LinkedBinding<Fib87>
) : LinkedBinding<Fib89>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib89(fib88Binding(), fib87Binding())
}

class UnlinkedFib90Binding : UnlinkedBinding<Fib90>() {
    override fun link(linker: Linker) =
        LinkedFib90Binding(linker.get(), linker.get())
}

class LinkedFib90Binding(
    private val fib89Binding: LinkedBinding<Fib89>,
    private val fib88Binding: LinkedBinding<Fib88>
) : LinkedBinding<Fib90>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib90(fib89Binding(), fib88Binding())
}

class UnlinkedFib91Binding : UnlinkedBinding<Fib91>() {
    override fun link(linker: Linker) =
        LinkedFib91Binding(linker.get(), linker.get())
}

class LinkedFib91Binding(
    private val fib90Binding: LinkedBinding<Fib90>,
    private val fib89Binding: LinkedBinding<Fib89>
) : LinkedBinding<Fib91>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib91(fib90Binding(), fib89Binding())
}

class UnlinkedFib92Binding : UnlinkedBinding<Fib92>() {
    override fun link(linker: Linker) =
        LinkedFib92Binding(linker.get(), linker.get())
}

class LinkedFib92Binding(
    private val fib91Binding: LinkedBinding<Fib91>,
    private val fib90Binding: LinkedBinding<Fib90>
) : LinkedBinding<Fib92>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib92(fib91Binding(), fib90Binding())
}

class UnlinkedFib93Binding : UnlinkedBinding<Fib93>() {
    override fun link(linker: Linker) =
        LinkedFib93Binding(linker.get(), linker.get())
}

class LinkedFib93Binding(
    private val fib92Binding: LinkedBinding<Fib92>,
    private val fib91Binding: LinkedBinding<Fib91>
) : LinkedBinding<Fib93>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib93(fib92Binding(), fib91Binding())
}

class UnlinkedFib94Binding : UnlinkedBinding<Fib94>() {
    override fun link(linker: Linker) =
        LinkedFib94Binding(linker.get(), linker.get())
}

class LinkedFib94Binding(
    private val fib93Binding: LinkedBinding<Fib93>,
    private val fib92Binding: LinkedBinding<Fib92>
) : LinkedBinding<Fib94>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib94(fib93Binding(), fib92Binding())
}

class UnlinkedFib95Binding : UnlinkedBinding<Fib95>() {
    override fun link(linker: Linker) =
        LinkedFib95Binding(linker.get(), linker.get())
}

class LinkedFib95Binding(
    private val fib94Binding: LinkedBinding<Fib94>,
    private val fib93Binding: LinkedBinding<Fib93>
) : LinkedBinding<Fib95>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib95(fib94Binding(), fib93Binding())
}

class UnlinkedFib96Binding : UnlinkedBinding<Fib96>() {
    override fun link(linker: Linker) =
        LinkedFib96Binding(linker.get(), linker.get())
}

class LinkedFib96Binding(
    private val fib95Binding: LinkedBinding<Fib95>,
    private val fib94Binding: LinkedBinding<Fib94>
) : LinkedBinding<Fib96>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib96(fib95Binding(), fib94Binding())
}

class UnlinkedFib97Binding : UnlinkedBinding<Fib97>() {
    override fun link(linker: Linker) =
        LinkedFib97Binding(linker.get(), linker.get())
}

class LinkedFib97Binding(
    private val fib96Binding: LinkedBinding<Fib96>,
    private val fib95Binding: LinkedBinding<Fib95>
) : LinkedBinding<Fib97>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib97(fib96Binding(), fib95Binding())
}

class UnlinkedFib98Binding : UnlinkedBinding<Fib98>() {
    override fun link(linker: Linker) =
        LinkedFib98Binding(linker.get(), linker.get())
}

class LinkedFib98Binding(
    private val fib97Binding: LinkedBinding<Fib97>,
    private val fib96Binding: LinkedBinding<Fib96>
) : LinkedBinding<Fib98>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib98(fib97Binding(), fib96Binding())
}

class UnlinkedFib99Binding : UnlinkedBinding<Fib99>() {
    override fun link(linker: Linker) =
        LinkedFib99Binding(linker.get(), linker.get())
}

class LinkedFib99Binding(
    private val fib98Binding: LinkedBinding<Fib98>,
    private val fib97Binding: LinkedBinding<Fib97>
) : LinkedBinding<Fib99>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib99(fib98Binding(), fib97Binding())
}

class UnlinkedFib100Binding : UnlinkedBinding<Fib100>() {
    override fun link(linker: Linker) =
        LinkedFib100Binding(linker.get(), linker.get())
}

class LinkedFib100Binding(
    private val fib99Binding: LinkedBinding<Fib99>,
    private val fib98Binding: LinkedBinding<Fib98>
) : LinkedBinding<Fib100>() {
    override fun get(parameters: ParametersDefinition?) =
        Fib100(fib99Binding(), fib98Binding())
}