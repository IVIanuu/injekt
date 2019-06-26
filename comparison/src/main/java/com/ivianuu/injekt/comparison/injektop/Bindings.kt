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

package com.ivianuu.injekt.comparison.injektop

import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.ParametersDefinition
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

object Fib1Binding : LinkedBinding<Fib1>() {
    override fun invoke(parameters: ParametersDefinition?): Fib1 = Fib1()
}

object Fib2Binding : LinkedBinding<Fib2>() {
    override fun invoke(parameters: ParametersDefinition?): Fib2 = Fib2()
}

object Fib3Binding : LinkedBinding<Fib3>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib3(Fib2Binding.get(), Fib1Binding.get())
}

object Fib4Binding : LinkedBinding<Fib4>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib4(Fib3Binding.get(), Fib2Binding.get())
}

object Fib5Binding : LinkedBinding<Fib5>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib5(Fib4Binding.get(), Fib3Binding.get())
}

object Fib6Binding : LinkedBinding<Fib6>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib6(Fib5Binding.get(), Fib4Binding.get())
}

object Fib7Binding : LinkedBinding<Fib7>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib7(Fib6Binding.get(), Fib5Binding.get())
}

object Fib8Binding : LinkedBinding<Fib8>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib8(Fib7Binding.get(), Fib6Binding.get())
}

object Fib9Binding : LinkedBinding<Fib9>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib9(Fib8Binding.get(), Fib7Binding.get())
}

object Fib10Binding : LinkedBinding<Fib10>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib10(Fib9Binding.get(), Fib8Binding.get())
}

object Fib11Binding : LinkedBinding<Fib11>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib11(Fib10Binding.get(), Fib9Binding.get())
}

object Fib12Binding : LinkedBinding<Fib12>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib12(Fib11Binding.get(), Fib10Binding.get())
}

object Fib13Binding : LinkedBinding<Fib13>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib13(Fib12Binding.get(), Fib11Binding.get())
}

object Fib14Binding : LinkedBinding<Fib14>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib14(Fib13Binding.get(), Fib12Binding.get())
}

object Fib15Binding : LinkedBinding<Fib15>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib15(Fib14Binding.get(), Fib13Binding.get())
}

object Fib16Binding : LinkedBinding<Fib16>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib16(Fib15Binding.get(), Fib14Binding.get())
}

object Fib17Binding : LinkedBinding<Fib17>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib17(Fib16Binding.get(), Fib15Binding.get())
}

object Fib18Binding : LinkedBinding<Fib18>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib18(Fib17Binding.get(), Fib16Binding.get())
}

object Fib19Binding : LinkedBinding<Fib19>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib19(Fib18Binding.get(), Fib17Binding.get())
}

object Fib20Binding : LinkedBinding<Fib20>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib20(Fib19Binding.get(), Fib18Binding.get())
}

object Fib21Binding : LinkedBinding<Fib21>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib21(Fib20Binding.get(), Fib19Binding.get())
}

object Fib22Binding : LinkedBinding<Fib22>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib22(Fib21Binding.get(), Fib20Binding.get())
}

object Fib23Binding : LinkedBinding<Fib23>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib23(Fib22Binding.get(), Fib21Binding.get())
}

object Fib24Binding : LinkedBinding<Fib24>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib24(Fib23Binding.get(), Fib22Binding.get())
}

object Fib25Binding : LinkedBinding<Fib25>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib25(Fib24Binding.get(), Fib23Binding.get())
}

object Fib26Binding : LinkedBinding<Fib26>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib26(Fib25Binding.get(), Fib24Binding.get())
}

object Fib27Binding : LinkedBinding<Fib27>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib27(Fib26Binding.get(), Fib25Binding.get())
}

object Fib28Binding : LinkedBinding<Fib28>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib28(Fib27Binding.get(), Fib26Binding.get())
}

object Fib29Binding : LinkedBinding<Fib29>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib29(Fib28Binding.get(), Fib27Binding.get())
}

object Fib30Binding : LinkedBinding<Fib30>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib30(Fib29Binding.get(), Fib28Binding.get())
}

object Fib31Binding : LinkedBinding<Fib31>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib31(Fib30Binding.get(), Fib29Binding.get())
}

object Fib32Binding : LinkedBinding<Fib32>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib32(Fib31Binding.get(), Fib30Binding.get())
}

object Fib33Binding : LinkedBinding<Fib33>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib33(Fib32Binding.get(), Fib31Binding.get())
}

object Fib34Binding : LinkedBinding<Fib34>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib34(Fib33Binding.get(), Fib32Binding.get())
}

object Fib35Binding : LinkedBinding<Fib35>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib35(Fib34Binding.get(), Fib33Binding.get())
}

object Fib36Binding : LinkedBinding<Fib36>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib36(Fib35Binding.get(), Fib34Binding.get())
}

object Fib37Binding : LinkedBinding<Fib37>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib37(Fib36Binding.get(), Fib35Binding.get())
}

object Fib38Binding : LinkedBinding<Fib38>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib38(Fib37Binding.get(), Fib36Binding.get())
}

object Fib39Binding : LinkedBinding<Fib39>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib39(Fib38Binding.get(), Fib37Binding.get())
}

object Fib40Binding : LinkedBinding<Fib40>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib40(Fib39Binding.get(), Fib38Binding.get())
}

object Fib41Binding : LinkedBinding<Fib41>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib41(Fib40Binding.get(), Fib39Binding.get())
}

object Fib42Binding : LinkedBinding<Fib42>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib42(Fib41Binding.get(), Fib40Binding.get())
}

object Fib43Binding : LinkedBinding<Fib43>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib43(Fib42Binding.get(), Fib41Binding.get())
}

object Fib44Binding : LinkedBinding<Fib44>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib44(Fib43Binding.get(), Fib42Binding.get())
}

object Fib45Binding : LinkedBinding<Fib45>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib45(Fib44Binding.get(), Fib43Binding.get())
}

object Fib46Binding : LinkedBinding<Fib46>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib46(Fib45Binding.get(), Fib44Binding.get())
}

object Fib47Binding : LinkedBinding<Fib47>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib47(Fib46Binding.get(), Fib45Binding.get())
}

object Fib48Binding : LinkedBinding<Fib48>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib48(Fib47Binding.get(), Fib46Binding.get())
}

object Fib49Binding : LinkedBinding<Fib49>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib49(Fib48Binding.get(), Fib47Binding.get())
}

object Fib50Binding : LinkedBinding<Fib50>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib50(Fib49Binding.get(), Fib48Binding.get())
}

object Fib51Binding : LinkedBinding<Fib51>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib51(Fib50Binding.get(), Fib49Binding.get())
}

object Fib52Binding : LinkedBinding<Fib52>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib52(Fib51Binding.get(), Fib50Binding.get())
}

object Fib53Binding : LinkedBinding<Fib53>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib53(Fib52Binding.get(), Fib51Binding.get())
}

object Fib54Binding : LinkedBinding<Fib54>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib54(Fib53Binding.get(), Fib52Binding.get())
}

object Fib55Binding : LinkedBinding<Fib55>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib55(Fib54Binding.get(), Fib53Binding.get())
}

object Fib56Binding : LinkedBinding<Fib56>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib56(Fib55Binding.get(), Fib54Binding.get())
}

object Fib57Binding : LinkedBinding<Fib57>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib57(Fib56Binding.get(), Fib55Binding.get())
}

object Fib58Binding : LinkedBinding<Fib58>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib58(Fib57Binding.get(), Fib56Binding.get())
}

object Fib59Binding : LinkedBinding<Fib59>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib59(Fib58Binding.get(), Fib57Binding.get())
}

object Fib60Binding : LinkedBinding<Fib60>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib60(Fib59Binding.get(), Fib58Binding.get())
}

object Fib61Binding : LinkedBinding<Fib61>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib61(Fib60Binding.get(), Fib59Binding.get())
}

object Fib62Binding : LinkedBinding<Fib62>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib62(Fib61Binding.get(), Fib60Binding.get())
}

object Fib63Binding : LinkedBinding<Fib63>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib63(Fib62Binding.get(), Fib61Binding.get())
}

object Fib64Binding : LinkedBinding<Fib64>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib64(Fib63Binding.get(), Fib62Binding.get())
}

object Fib65Binding : LinkedBinding<Fib65>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib65(Fib64Binding.get(), Fib63Binding.get())
}

object Fib66Binding : LinkedBinding<Fib66>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib66(Fib65Binding.get(), Fib64Binding.get())
}

object Fib67Binding : LinkedBinding<Fib67>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib67(Fib66Binding.get(), Fib65Binding.get())
}

object Fib68Binding : LinkedBinding<Fib68>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib68(Fib67Binding.get(), Fib66Binding.get())
}

object Fib69Binding : LinkedBinding<Fib69>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib69(Fib68Binding.get(), Fib67Binding.get())
}

object Fib70Binding : LinkedBinding<Fib70>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib70(Fib69Binding.get(), Fib68Binding.get())
}

object Fib71Binding : LinkedBinding<Fib71>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib71(Fib70Binding.get(), Fib69Binding.get())
}

object Fib72Binding : LinkedBinding<Fib72>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib72(Fib71Binding.get(), Fib70Binding.get())
}

object Fib73Binding : LinkedBinding<Fib73>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib73(Fib72Binding.get(), Fib71Binding.get())
}

object Fib74Binding : LinkedBinding<Fib74>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib74(Fib73Binding.get(), Fib72Binding.get())
}

object Fib75Binding : LinkedBinding<Fib75>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib75(Fib74Binding.get(), Fib73Binding.get())
}

object Fib76Binding : LinkedBinding<Fib76>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib76(Fib75Binding.get(), Fib74Binding.get())
}

object Fib77Binding : LinkedBinding<Fib77>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib77(Fib76Binding.get(), Fib75Binding.get())
}

object Fib78Binding : LinkedBinding<Fib78>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib78(Fib77Binding.get(), Fib76Binding.get())
}

object Fib79Binding : LinkedBinding<Fib79>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib79(Fib78Binding.get(), Fib77Binding.get())
}

object Fib80Binding : LinkedBinding<Fib80>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib80(Fib79Binding.get(), Fib78Binding.get())
}

object Fib81Binding : LinkedBinding<Fib81>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib81(Fib80Binding.get(), Fib79Binding.get())
}

object Fib82Binding : LinkedBinding<Fib82>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib82(Fib81Binding.get(), Fib80Binding.get())
}

object Fib83Binding : LinkedBinding<Fib83>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib83(Fib82Binding.get(), Fib81Binding.get())
}

object Fib84Binding : LinkedBinding<Fib84>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib84(Fib83Binding.get(), Fib82Binding.get())
}

object Fib85Binding : LinkedBinding<Fib85>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib85(Fib84Binding.get(), Fib83Binding.get())
}

object Fib86Binding : LinkedBinding<Fib86>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib86(Fib85Binding.get(), Fib84Binding.get())
}

object Fib87Binding : LinkedBinding<Fib87>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib87(Fib86Binding.get(), Fib85Binding.get())
}

object Fib88Binding : LinkedBinding<Fib88>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib88(Fib87Binding.get(), Fib86Binding.get())
}

object Fib89Binding : LinkedBinding<Fib89>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib89(Fib88Binding.get(), Fib87Binding.get())
}

object Fib90Binding : LinkedBinding<Fib90>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib90(Fib89Binding.get(), Fib88Binding.get())
}

object Fib91Binding : LinkedBinding<Fib91>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib91(Fib90Binding.get(), Fib89Binding.get())
}

object Fib92Binding : LinkedBinding<Fib92>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib92(Fib91Binding.get(), Fib90Binding.get())
}

object Fib93Binding : LinkedBinding<Fib93>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib93(Fib92Binding.get(), Fib91Binding.get())
}

object Fib94Binding : LinkedBinding<Fib94>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib94(Fib93Binding.get(), Fib92Binding.get())
}

object Fib95Binding : LinkedBinding<Fib95>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib95(Fib94Binding.get(), Fib93Binding.get())
}

object Fib96Binding : LinkedBinding<Fib96>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib96(Fib95Binding.get(), Fib94Binding.get())
}

object Fib97Binding : LinkedBinding<Fib97>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib97(Fib96Binding.get(), Fib95Binding.get())
}

object Fib98Binding : LinkedBinding<Fib98>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib98(Fib97Binding.get(), Fib96Binding.get())
}

object Fib99Binding : LinkedBinding<Fib99>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib99(Fib98Binding.get(), Fib97Binding.get())
}

object Fib100Binding : LinkedBinding<Fib100>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib100(Fib99Binding.get(), Fib98Binding.get())
}