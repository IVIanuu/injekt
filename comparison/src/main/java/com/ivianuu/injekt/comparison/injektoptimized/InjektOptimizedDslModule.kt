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

import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.StateDefinition
import com.ivianuu.injekt.StateDefinitionContext
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
import com.ivianuu.injekt.module
import com.ivianuu.injekt.stateDefinitionBinding

inline fun <reified T> Module.factoryS(
    name: Any? = null,
    override: Boolean = false,
    noinline block: StateDefinitionContext.() -> StateDefinition<T>
): BindingContext<T> = bind(stateDefinitionBinding(block), name, override)


val injektOptimizedDslModule = createModule()

fun createModule() = module {
    factoryS { definition { Fib1() } }
    factoryS { definition { Fib2() } }
    factoryS {
        val fib2 = link<Fib2>()
        val fib1 = link<Fib1>()
        definition { Fib3(fib2(), fib1()) }
    }
    factoryS {
        val fib3 = link<Fib3>()
        val fib2 = link<Fib2>()
        definition { Fib4(fib3(), fib2()) }
    }
    factoryS {
        val fib4 = link<Fib4>()
        val fib3 = link<Fib3>()
        definition { Fib5(fib4(), fib3()) }
    }
    factoryS {
        val fib5 = link<Fib5>()
        val fib4 = link<Fib4>()
        definition { Fib6(fib5(), fib4()) }
    }
    factoryS {
        val fib6 = link<Fib6>()
        val fib5 = link<Fib5>()
        definition { Fib7(fib6(), fib5()) }
    }
    factoryS {
        val fib7 = link<Fib7>()
        val fib6 = link<Fib6>()
        definition { Fib8(fib7(), fib6()) }
    }
    factoryS {
        val fib8 = link<Fib8>()
        val fib7 = link<Fib7>()
        definition { Fib9(fib8(), fib7()) }
    }
    factoryS {
        val fib9 = link<Fib9>()
        val fib8 = link<Fib8>()
        definition { Fib10(fib9(), fib8()) }
    }
    factoryS {
        val fib10 = link<Fib10>()
        val fib9 = link<Fib9>()
        definition { Fib11(fib10(), fib9()) }
    }
    factoryS {
        val fib11 = link<Fib11>()
        val fib10 = link<Fib10>()
        definition { Fib12(fib11(), fib10()) }
    }
    factoryS {
        val fib12 = link<Fib12>()
        val fib11 = link<Fib11>()
        definition { Fib13(fib12(), fib11()) }
    }
    factoryS {
        val fib13 = link<Fib13>()
        val fib12 = link<Fib12>()
        definition { Fib14(fib13(), fib12()) }
    }
    factoryS {
        val fib14 = link<Fib14>()
        val fib13 = link<Fib13>()
        definition { Fib15(fib14(), fib13()) }
    }
    factoryS {
        val fib15 = link<Fib15>()
        val fib14 = link<Fib14>()
        definition { Fib16(fib15(), fib14()) }
    }
    factoryS {
        val fib16 = link<Fib16>()
        val fib15 = link<Fib15>()
        definition { Fib17(fib16(), fib15()) }
    }
    factoryS {
        val fib17 = link<Fib17>()
        val fib16 = link<Fib16>()
        definition { Fib18(fib17(), fib16()) }
    }
    factoryS {
        val fib18 = link<Fib18>()
        val fib17 = link<Fib17>()
        definition { Fib19(fib18(), fib17()) }
    }
    factoryS {
        val fib19 = link<Fib19>()
        val fib18 = link<Fib18>()
        definition { Fib20(fib19(), fib18()) }
    }
    factoryS {
        val fib20 = link<Fib20>()
        val fib19 = link<Fib19>()
        definition { Fib21(fib20(), fib19()) }
    }
    factoryS {
        val fib21 = link<Fib21>()
        val fib20 = link<Fib20>()
        definition { Fib22(fib21(), fib20()) }
    }
    factoryS {
        val fib22 = link<Fib22>()
        val fib21 = link<Fib21>()
        definition { Fib23(fib22(), fib21()) }
    }
    factoryS {
        val fib23 = link<Fib23>()
        val fib22 = link<Fib22>()
        definition { Fib24(fib23(), fib22()) }
    }
    factoryS {
        val fib24 = link<Fib24>()
        val fib23 = link<Fib23>()
        definition { Fib25(fib24(), fib23()) }
    }
    factoryS {
        val fib25 = link<Fib25>()
        val fib24 = link<Fib24>()
        definition { Fib26(fib25(), fib24()) }
    }
    factoryS {
        val fib26 = link<Fib26>()
        val fib25 = link<Fib25>()
        definition { Fib27(fib26(), fib25()) }
    }
    factoryS {
        val fib27 = link<Fib27>()
        val fib26 = link<Fib26>()
        definition { Fib28(fib27(), fib26()) }
    }
    factoryS {
        val fib28 = link<Fib28>()
        val fib27 = link<Fib27>()
        definition { Fib29(fib28(), fib27()) }
    }
    factoryS {
        val fib29 = link<Fib29>()
        val fib28 = link<Fib28>()
        definition { Fib30(fib29(), fib28()) }
    }
    factoryS {
        val fib30 = link<Fib30>()
        val fib29 = link<Fib29>()
        definition { Fib31(fib30(), fib29()) }
    }
    factoryS {
        val fib31 = link<Fib31>()
        val fib30 = link<Fib30>()
        definition { Fib32(fib31(), fib30()) }
    }
    factoryS {
        val fib32 = link<Fib32>()
        val fib31 = link<Fib31>()
        definition { Fib33(fib32(), fib31()) }
    }
    factoryS {
        val fib33 = link<Fib33>()
        val fib32 = link<Fib32>()
        definition { Fib34(fib33(), fib32()) }
    }
    factoryS {
        val fib34 = link<Fib34>()
        val fib33 = link<Fib33>()
        definition { Fib35(fib34(), fib33()) }
    }
    factoryS {
        val fib35 = link<Fib35>()
        val fib34 = link<Fib34>()
        definition { Fib36(fib35(), fib34()) }
    }
    factoryS {
        val fib36 = link<Fib36>()
        val fib35 = link<Fib35>()
        definition { Fib37(fib36(), fib35()) }
    }
    factoryS {
        val fib37 = link<Fib37>()
        val fib36 = link<Fib36>()
        definition { Fib38(fib37(), fib36()) }
    }
    factoryS {
        val fib38 = link<Fib38>()
        val fib37 = link<Fib37>()
        definition { Fib39(fib38(), fib37()) }
    }
    factoryS {
        val fib39 = link<Fib39>()
        val fib38 = link<Fib38>()
        definition { Fib40(fib39(), fib38()) }
    }
    factoryS {
        val fib40 = link<Fib40>()
        val fib39 = link<Fib39>()
        definition { Fib41(fib40(), fib39()) }
    }
    factoryS {
        val fib41 = link<Fib41>()
        val fib40 = link<Fib40>()
        definition { Fib42(fib41(), fib40()) }
    }
    factoryS {
        val fib42 = link<Fib42>()
        val fib41 = link<Fib41>()
        definition { Fib43(fib42(), fib41()) }
    }
    factoryS {
        val fib43 = link<Fib43>()
        val fib42 = link<Fib42>()
        definition { Fib44(fib43(), fib42()) }
    }
    factoryS {
        val fib44 = link<Fib44>()
        val fib43 = link<Fib43>()
        definition { Fib45(fib44(), fib43()) }
    }
    factoryS {
        val fib45 = link<Fib45>()
        val fib44 = link<Fib44>()
        definition { Fib46(fib45(), fib44()) }
    }
    factoryS {
        val fib46 = link<Fib46>()
        val fib45 = link<Fib45>()
        definition { Fib47(fib46(), fib45()) }
    }
    factoryS {
        val fib47 = link<Fib47>()
        val fib46 = link<Fib46>()
        definition { Fib48(fib47(), fib46()) }
    }
    factoryS {
        val fib48 = link<Fib48>()
        val fib47 = link<Fib47>()
        definition { Fib49(fib48(), fib47()) }
    }
    factoryS {
        val fib49 = link<Fib49>()
        val fib48 = link<Fib48>()
        definition { Fib50(fib49(), fib48()) }
    }
    factoryS {
        val fib50 = link<Fib50>()
        val fib49 = link<Fib49>()
        definition { Fib51(fib50(), fib49()) }
    }
    factoryS {
        val fib51 = link<Fib51>()
        val fib50 = link<Fib50>()
        definition { Fib52(fib51(), fib50()) }
    }
    factoryS {
        val fib52 = link<Fib52>()
        val fib51 = link<Fib51>()
        definition { Fib53(fib52(), fib51()) }
    }
    factoryS {
        val fib53 = link<Fib53>()
        val fib52 = link<Fib52>()
        definition { Fib54(fib53(), fib52()) }
    }
    factoryS {
        val fib54 = link<Fib54>()
        val fib53 = link<Fib53>()
        definition { Fib55(fib54(), fib53()) }
    }
    factoryS {
        val fib55 = link<Fib55>()
        val fib54 = link<Fib54>()
        definition { Fib56(fib55(), fib54()) }
    }
    factoryS {
        val fib56 = link<Fib56>()
        val fib55 = link<Fib55>()
        definition { Fib57(fib56(), fib55()) }
    }
    factoryS {
        val fib57 = link<Fib57>()
        val fib56 = link<Fib56>()
        definition { Fib58(fib57(), fib56()) }
    }
    factoryS {
        val fib58 = link<Fib58>()
        val fib57 = link<Fib57>()
        definition { Fib59(fib58(), fib57()) }
    }
    factoryS {
        val fib59 = link<Fib59>()
        val fib58 = link<Fib58>()
        definition { Fib60(fib59(), fib58()) }
    }
    factoryS {
        val fib60 = link<Fib60>()
        val fib59 = link<Fib59>()
        definition { Fib61(fib60(), fib59()) }
    }
    factoryS {
        val fib61 = link<Fib61>()
        val fib60 = link<Fib60>()
        definition { Fib62(fib61(), fib60()) }
    }
    factoryS {
        val fib62 = link<Fib62>()
        val fib61 = link<Fib61>()
        definition { Fib63(fib62(), fib61()) }
    }
    factoryS {
        val fib63 = link<Fib63>()
        val fib62 = link<Fib62>()
        definition { Fib64(fib63(), fib62()) }
    }
    factoryS {
        val fib64 = link<Fib64>()
        val fib63 = link<Fib63>()
        definition { Fib65(fib64(), fib63()) }
    }
    factoryS {
        val fib65 = link<Fib65>()
        val fib64 = link<Fib64>()
        definition { Fib66(fib65(), fib64()) }
    }
    factoryS {
        val fib66 = link<Fib66>()
        val fib65 = link<Fib65>()
        definition { Fib67(fib66(), fib65()) }
    }
    factoryS {
        val fib67 = link<Fib67>()
        val fib66 = link<Fib66>()
        definition { Fib68(fib67(), fib66()) }
    }
    factoryS {
        val fib68 = link<Fib68>()
        val fib67 = link<Fib67>()
        definition { Fib69(fib68(), fib67()) }
    }
    factoryS {
        val fib69 = link<Fib69>()
        val fib68 = link<Fib68>()
        definition { Fib70(fib69(), fib68()) }
    }
    factoryS {
        val fib70 = link<Fib70>()
        val fib69 = link<Fib69>()
        definition { Fib71(fib70(), fib69()) }
    }
    factoryS {
        val fib71 = link<Fib71>()
        val fib70 = link<Fib70>()
        definition { Fib72(fib71(), fib70()) }
    }
    factoryS {
        val fib72 = link<Fib72>()
        val fib71 = link<Fib71>()
        definition { Fib73(fib72(), fib71()) }
    }
    factoryS {
        val fib73 = link<Fib73>()
        val fib72 = link<Fib72>()
        definition { Fib74(fib73(), fib72()) }
    }
    factoryS {
        val fib74 = link<Fib74>()
        val fib73 = link<Fib73>()
        definition { Fib75(fib74(), fib73()) }
    }
    factoryS {
        val fib75 = link<Fib75>()
        val fib74 = link<Fib74>()
        definition { Fib76(fib75(), fib74()) }
    }
    factoryS {
        val fib76 = link<Fib76>()
        val fib75 = link<Fib75>()
        definition { Fib77(fib76(), fib75()) }
    }
    factoryS {
        val fib77 = link<Fib77>()
        val fib76 = link<Fib76>()
        definition { Fib78(fib77(), fib76()) }
    }
    factoryS {
        val fib78 = link<Fib78>()
        val fib77 = link<Fib77>()
        definition { Fib79(fib78(), fib77()) }
    }
    factoryS {
        val fib79 = link<Fib79>()
        val fib78 = link<Fib78>()
        definition { Fib80(fib79(), fib78()) }
    }
    factoryS {
        val fib80 = link<Fib80>()
        val fib79 = link<Fib79>()
        definition { Fib81(fib80(), fib79()) }
    }
    factoryS {
        val fib81 = link<Fib81>()
        val fib80 = link<Fib80>()
        definition { Fib82(fib81(), fib80()) }
    }
    factoryS {
        val fib82 = link<Fib82>()
        val fib81 = link<Fib81>()
        definition { Fib83(fib82(), fib81()) }
    }
    factoryS {
        val fib83 = link<Fib83>()
        val fib82 = link<Fib82>()
        definition { Fib84(fib83(), fib82()) }
    }
    factoryS {
        val fib84 = link<Fib84>()
        val fib83 = link<Fib83>()
        definition { Fib85(fib84(), fib83()) }
    }
    factoryS {
        val fib85 = link<Fib85>()
        val fib84 = link<Fib84>()
        definition { Fib86(fib85(), fib84()) }
    }
    factoryS {
        val fib86 = link<Fib86>()
        val fib85 = link<Fib85>()
        definition { Fib87(fib86(), fib85()) }
    }
    factoryS {
        val fib87 = link<Fib87>()
        val fib86 = link<Fib86>()
        definition { Fib88(fib87(), fib86()) }
    }
    factoryS {
        val fib88 = link<Fib88>()
        val fib87 = link<Fib87>()
        definition { Fib89(fib88(), fib87()) }
    }
    factoryS {
        val fib89 = link<Fib89>()
        val fib88 = link<Fib88>()
        definition { Fib90(fib89(), fib88()) }
    }
    factoryS {
        val fib90 = link<Fib90>()
        val fib89 = link<Fib89>()
        definition { Fib91(fib90(), fib89()) }
    }
    factoryS {
        val fib91 = link<Fib91>()
        val fib90 = link<Fib90>()
        definition { Fib92(fib91(), fib90()) }
    }
    factoryS {
        val fib92 = link<Fib92>()
        val fib91 = link<Fib91>()
        definition { Fib93(fib92(), fib91()) }
    }
    factoryS {
        val fib93 = link<Fib93>()
        val fib92 = link<Fib92>()
        definition { Fib94(fib93(), fib92()) }
    }
    factoryS {
        val fib94 = link<Fib94>()
        val fib93 = link<Fib93>()
        definition { Fib95(fib94(), fib93()) }
    }
    factoryS {
        val fib95 = link<Fib95>()
        val fib94 = link<Fib94>()
        definition { Fib96(fib95(), fib94()) }
    }
    factoryS {
        val fib96 = link<Fib96>()
        val fib95 = link<Fib95>()
        definition { Fib97(fib96(), fib95()) }
    }
    factoryS {
        val fib97 = link<Fib97>()
        val fib96 = link<Fib96>()
        definition { Fib98(fib97(), fib96()) }
    }
    factoryS {
        val fib98 = link<Fib98>()
        val fib97 = link<Fib97>()
        definition { Fib99(fib98(), fib97()) }
    }
    factoryS {
        val fib99 = link<Fib99>()
        val fib98 = link<Fib98>()
        definition { Fib100(fib99(), fib98()) }
    }
}