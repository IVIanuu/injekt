/*
 * Copyright 2019 Manuel Wrage
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
        Fib3(Fib2Binding(), Fib1Binding())
}

object Fib4Binding : LinkedBinding<Fib4>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib4(Fib3Binding(), Fib2Binding())
}

object Fib5Binding : LinkedBinding<Fib5>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib5(Fib4Binding(), Fib3Binding())
}

object Fib6Binding : LinkedBinding<Fib6>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib6(Fib5Binding(), Fib4Binding())
}

object Fib7Binding : LinkedBinding<Fib7>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib7(Fib6Binding(), Fib5Binding())
}

object Fib8Binding : LinkedBinding<Fib8>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib8(Fib7Binding(), Fib6Binding())
}

object Fib9Binding : LinkedBinding<Fib9>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib9(Fib8Binding(), Fib7Binding())
}

object Fib10Binding : LinkedBinding<Fib10>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib10(Fib9Binding(), Fib8Binding())
}

object Fib11Binding : LinkedBinding<Fib11>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib11(Fib10Binding(), Fib9Binding())
}

object Fib12Binding : LinkedBinding<Fib12>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib12(Fib11Binding(), Fib10Binding())
}

object Fib13Binding : LinkedBinding<Fib13>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib13(Fib12Binding(), Fib11Binding())
}

object Fib14Binding : LinkedBinding<Fib14>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib14(Fib13Binding(), Fib12Binding())
}

object Fib15Binding : LinkedBinding<Fib15>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib15(Fib14Binding(), Fib13Binding())
}

object Fib16Binding : LinkedBinding<Fib16>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib16(Fib15Binding(), Fib14Binding())
}

object Fib17Binding : LinkedBinding<Fib17>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib17(Fib16Binding(), Fib15Binding())
}

object Fib18Binding : LinkedBinding<Fib18>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib18(Fib17Binding(), Fib16Binding())
}

object Fib19Binding : LinkedBinding<Fib19>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib19(Fib18Binding(), Fib17Binding())
}

object Fib20Binding : LinkedBinding<Fib20>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib20(Fib19Binding(), Fib18Binding())
}

object Fib21Binding : LinkedBinding<Fib21>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib21(Fib20Binding(), Fib19Binding())
}

object Fib22Binding : LinkedBinding<Fib22>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib22(Fib21Binding(), Fib20Binding())
}

object Fib23Binding : LinkedBinding<Fib23>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib23(Fib22Binding(), Fib21Binding())
}

object Fib24Binding : LinkedBinding<Fib24>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib24(Fib23Binding(), Fib22Binding())
}

object Fib25Binding : LinkedBinding<Fib25>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib25(Fib24Binding(), Fib23Binding())
}

object Fib26Binding : LinkedBinding<Fib26>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib26(Fib25Binding(), Fib24Binding())
}

object Fib27Binding : LinkedBinding<Fib27>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib27(Fib26Binding(), Fib25Binding())
}

object Fib28Binding : LinkedBinding<Fib28>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib28(Fib27Binding(), Fib26Binding())
}

object Fib29Binding : LinkedBinding<Fib29>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib29(Fib28Binding(), Fib27Binding())
}

object Fib30Binding : LinkedBinding<Fib30>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib30(Fib29Binding(), Fib28Binding())
}

object Fib31Binding : LinkedBinding<Fib31>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib31(Fib30Binding(), Fib29Binding())
}

object Fib32Binding : LinkedBinding<Fib32>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib32(Fib31Binding(), Fib30Binding())
}

object Fib33Binding : LinkedBinding<Fib33>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib33(Fib32Binding(), Fib31Binding())
}

object Fib34Binding : LinkedBinding<Fib34>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib34(Fib33Binding(), Fib32Binding())
}

object Fib35Binding : LinkedBinding<Fib35>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib35(Fib34Binding(), Fib33Binding())
}

object Fib36Binding : LinkedBinding<Fib36>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib36(Fib35Binding(), Fib34Binding())
}

object Fib37Binding : LinkedBinding<Fib37>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib37(Fib36Binding(), Fib35Binding())
}

object Fib38Binding : LinkedBinding<Fib38>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib38(Fib37Binding(), Fib36Binding())
}

object Fib39Binding : LinkedBinding<Fib39>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib39(Fib38Binding(), Fib37Binding())
}

object Fib40Binding : LinkedBinding<Fib40>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib40(Fib39Binding(), Fib38Binding())
}

object Fib41Binding : LinkedBinding<Fib41>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib41(Fib40Binding(), Fib39Binding())
}

object Fib42Binding : LinkedBinding<Fib42>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib42(Fib41Binding(), Fib40Binding())
}

object Fib43Binding : LinkedBinding<Fib43>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib43(Fib42Binding(), Fib41Binding())
}

object Fib44Binding : LinkedBinding<Fib44>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib44(Fib43Binding(), Fib42Binding())
}

object Fib45Binding : LinkedBinding<Fib45>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib45(Fib44Binding(), Fib43Binding())
}

object Fib46Binding : LinkedBinding<Fib46>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib46(Fib45Binding(), Fib44Binding())
}

object Fib47Binding : LinkedBinding<Fib47>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib47(Fib46Binding(), Fib45Binding())
}

object Fib48Binding : LinkedBinding<Fib48>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib48(Fib47Binding(), Fib46Binding())
}

object Fib49Binding : LinkedBinding<Fib49>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib49(Fib48Binding(), Fib47Binding())
}

object Fib50Binding : LinkedBinding<Fib50>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib50(Fib49Binding(), Fib48Binding())
}

object Fib51Binding : LinkedBinding<Fib51>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib51(Fib50Binding(), Fib49Binding())
}

object Fib52Binding : LinkedBinding<Fib52>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib52(Fib51Binding(), Fib50Binding())
}

object Fib53Binding : LinkedBinding<Fib53>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib53(Fib52Binding(), Fib51Binding())
}

object Fib54Binding : LinkedBinding<Fib54>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib54(Fib53Binding(), Fib52Binding())
}

object Fib55Binding : LinkedBinding<Fib55>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib55(Fib54Binding(), Fib53Binding())
}

object Fib56Binding : LinkedBinding<Fib56>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib56(Fib55Binding(), Fib54Binding())
}

object Fib57Binding : LinkedBinding<Fib57>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib57(Fib56Binding(), Fib55Binding())
}

object Fib58Binding : LinkedBinding<Fib58>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib58(Fib57Binding(), Fib56Binding())
}

object Fib59Binding : LinkedBinding<Fib59>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib59(Fib58Binding(), Fib57Binding())
}

object Fib60Binding : LinkedBinding<Fib60>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib60(Fib59Binding(), Fib58Binding())
}

object Fib61Binding : LinkedBinding<Fib61>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib61(Fib60Binding(), Fib59Binding())
}

object Fib62Binding : LinkedBinding<Fib62>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib62(Fib61Binding(), Fib60Binding())
}

object Fib63Binding : LinkedBinding<Fib63>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib63(Fib62Binding(), Fib61Binding())
}

object Fib64Binding : LinkedBinding<Fib64>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib64(Fib63Binding(), Fib62Binding())
}

object Fib65Binding : LinkedBinding<Fib65>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib65(Fib64Binding(), Fib63Binding())
}

object Fib66Binding : LinkedBinding<Fib66>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib66(Fib65Binding(), Fib64Binding())
}

object Fib67Binding : LinkedBinding<Fib67>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib67(Fib66Binding(), Fib65Binding())
}

object Fib68Binding : LinkedBinding<Fib68>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib68(Fib67Binding(), Fib66Binding())
}

object Fib69Binding : LinkedBinding<Fib69>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib69(Fib68Binding(), Fib67Binding())
}

object Fib70Binding : LinkedBinding<Fib70>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib70(Fib69Binding(), Fib68Binding())
}

object Fib71Binding : LinkedBinding<Fib71>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib71(Fib70Binding(), Fib69Binding())
}

object Fib72Binding : LinkedBinding<Fib72>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib72(Fib71Binding(), Fib70Binding())
}

object Fib73Binding : LinkedBinding<Fib73>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib73(Fib72Binding(), Fib71Binding())
}

object Fib74Binding : LinkedBinding<Fib74>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib74(Fib73Binding(), Fib72Binding())
}

object Fib75Binding : LinkedBinding<Fib75>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib75(Fib74Binding(), Fib73Binding())
}

object Fib76Binding : LinkedBinding<Fib76>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib76(Fib75Binding(), Fib74Binding())
}

object Fib77Binding : LinkedBinding<Fib77>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib77(Fib76Binding(), Fib75Binding())
}

object Fib78Binding : LinkedBinding<Fib78>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib78(Fib77Binding(), Fib76Binding())
}

object Fib79Binding : LinkedBinding<Fib79>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib79(Fib78Binding(), Fib77Binding())
}

object Fib80Binding : LinkedBinding<Fib80>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib80(Fib79Binding(), Fib78Binding())
}

object Fib81Binding : LinkedBinding<Fib81>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib81(Fib80Binding(), Fib79Binding())
}

object Fib82Binding : LinkedBinding<Fib82>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib82(Fib81Binding(), Fib80Binding())
}

object Fib83Binding : LinkedBinding<Fib83>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib83(Fib82Binding(), Fib81Binding())
}

object Fib84Binding : LinkedBinding<Fib84>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib84(Fib83Binding(), Fib82Binding())
}

object Fib85Binding : LinkedBinding<Fib85>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib85(Fib84Binding(), Fib83Binding())
}

object Fib86Binding : LinkedBinding<Fib86>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib86(Fib85Binding(), Fib84Binding())
}

object Fib87Binding : LinkedBinding<Fib87>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib87(Fib86Binding(), Fib85Binding())
}

object Fib88Binding : LinkedBinding<Fib88>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib88(Fib87Binding(), Fib86Binding())
}

object Fib89Binding : LinkedBinding<Fib89>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib89(Fib88Binding(), Fib87Binding())
}

object Fib90Binding : LinkedBinding<Fib90>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib90(Fib89Binding(), Fib88Binding())
}

object Fib91Binding : LinkedBinding<Fib91>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib91(Fib90Binding(), Fib89Binding())
}

object Fib92Binding : LinkedBinding<Fib92>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib92(Fib91Binding(), Fib90Binding())
}

object Fib93Binding : LinkedBinding<Fib93>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib93(Fib92Binding(), Fib91Binding())
}

object Fib94Binding : LinkedBinding<Fib94>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib94(Fib93Binding(), Fib92Binding())
}

object Fib95Binding : LinkedBinding<Fib95>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib95(Fib94Binding(), Fib93Binding())
}

object Fib96Binding : LinkedBinding<Fib96>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib96(Fib95Binding(), Fib94Binding())
}

object Fib97Binding : LinkedBinding<Fib97>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib97(Fib96Binding(), Fib95Binding())
}

object Fib98Binding : LinkedBinding<Fib98>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib98(Fib97Binding(), Fib96Binding())
}

object Fib99Binding : LinkedBinding<Fib99>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib99(Fib98Binding(), Fib97Binding())
}

object Fib100Binding : LinkedBinding<Fib100>() {
    override fun invoke(parameters: ParametersDefinition?) =
        Fib100(Fib99Binding(), Fib98Binding())
}