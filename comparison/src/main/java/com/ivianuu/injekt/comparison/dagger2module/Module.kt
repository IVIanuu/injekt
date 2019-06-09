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

package com.ivianuu.injekt.comparison.dagger2module

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
import dagger.Module
import dagger.Provides

@Module
class Dagger2Module {
    @Provides
    fun provideFib1() = Fib1()

    @Provides
    fun provideFib2() = Fib2()

    @Provides
    fun provideFib3(fib2: Fib2, fib1: Fib1) = Fib3(fib2, fib1)

    @Provides
    fun provideFib4(fib3: Fib3, fib2: Fib2) = Fib4(fib3, fib2)

    @Provides
    fun provideFib5(fib4: Fib4, fib3: Fib3) = Fib5(fib4, fib3)

    @Provides
    fun provideFib6(fib5: Fib5, fib4: Fib4) = Fib6(fib5, fib4)

    @Provides
    fun provideFib7(fib6: Fib6, fib5: Fib5) = Fib7(fib6, fib5)

    @Provides
    fun provideFib8(fib7: Fib7, fib6: Fib6) = Fib8(fib7, fib6)

    @Provides
    fun provideFib9(fib8: Fib8, fib7: Fib7) = Fib9(fib8, fib7)

    @Provides
    fun provideFib10(fib9: Fib9, fib8: Fib8) = Fib10(fib9, fib8)

    @Provides
    fun provideFib11(fib10: Fib10, fib9: Fib9) = Fib11(fib10, fib9)

    @Provides
    fun provideFib12(fib11: Fib11, fib10: Fib10) = Fib12(fib11, fib10)

    @Provides
    fun provideFib13(fib12: Fib12, fib11: Fib11) = Fib13(fib12, fib11)

    @Provides
    fun provideFib14(fib13: Fib13, fib12: Fib12) = Fib14(fib13, fib12)

    @Provides
    fun provideFib15(fib14: Fib14, fib13: Fib13) = Fib15(fib14, fib13)

    @Provides
    fun provideFib16(fib15: Fib15, fib14: Fib14) = Fib16(fib15, fib14)

    @Provides
    fun provideFib17(fib16: Fib16, fib15: Fib15) = Fib17(fib16, fib15)

    @Provides
    fun provideFib18(fib17: Fib17, fib16: Fib16) = Fib18(fib17, fib16)

    @Provides
    fun provideFib19(fib18: Fib18, fib17: Fib17) = Fib19(fib18, fib17)

    @Provides
    fun provideFib20(fib19: Fib19, fib18: Fib18) = Fib20(fib19, fib18)

    @Provides
    fun provideFib21(fib20: Fib20, fib19: Fib19) = Fib21(fib20, fib19)

    @Provides
    fun provideFib22(fib21: Fib21, fib20: Fib20) = Fib22(fib21, fib20)

    @Provides
    fun provideFib23(fib22: Fib22, fib21: Fib21) = Fib23(fib22, fib21)

    @Provides
    fun provideFib24(fib23: Fib23, fib22: Fib22) = Fib24(fib23, fib22)

    @Provides
    fun provideFib25(fib24: Fib24, fib23: Fib23) = Fib25(fib24, fib23)

    @Provides
    fun provideFib26(fib25: Fib25, fib24: Fib24) = Fib26(fib25, fib24)

    @Provides
    fun provideFib27(fib26: Fib26, fib25: Fib25) = Fib27(fib26, fib25)

    @Provides
    fun provideFib28(fib27: Fib27, fib26: Fib26) = Fib28(fib27, fib26)

    @Provides
    fun provideFib29(fib28: Fib28, fib27: Fib27) = Fib29(fib28, fib27)

    @Provides
    fun provideFib30(fib29: Fib29, fib28: Fib28) = Fib30(fib29, fib28)

    @Provides
    fun provideFib31(fib30: Fib30, fib29: Fib29) = Fib31(fib30, fib29)

    @Provides
    fun provideFib32(fib31: Fib31, fib30: Fib30) = Fib32(fib31, fib30)

    @Provides
    fun provideFib33(fib32: Fib32, fib31: Fib31) = Fib33(fib32, fib31)

    @Provides
    fun provideFib34(fib33: Fib33, fib32: Fib32) = Fib34(fib33, fib32)

    @Provides
    fun provideFib35(fib34: Fib34, fib33: Fib33) = Fib35(fib34, fib33)

    @Provides
    fun provideFib36(fib35: Fib35, fib34: Fib34) = Fib36(fib35, fib34)

    @Provides
    fun provideFib37(fib36: Fib36, fib35: Fib35) = Fib37(fib36, fib35)

    @Provides
    fun provideFib38(fib37: Fib37, fib36: Fib36) = Fib38(fib37, fib36)

    @Provides
    fun provideFib39(fib38: Fib38, fib37: Fib37) = Fib39(fib38, fib37)

    @Provides
    fun provideFib40(fib39: Fib39, fib38: Fib38) = Fib40(fib39, fib38)

    @Provides
    fun provideFib41(fib40: Fib40, fib39: Fib39) = Fib41(fib40, fib39)

    @Provides
    fun provideFib42(fib41: Fib41, fib40: Fib40) = Fib42(fib41, fib40)

    @Provides
    fun provideFib43(fib42: Fib42, fib41: Fib41) = Fib43(fib42, fib41)

    @Provides
    fun provideFib44(fib43: Fib43, fib42: Fib42) = Fib44(fib43, fib42)

    @Provides
    fun provideFib45(fib44: Fib44, fib43: Fib43) = Fib45(fib44, fib43)

    @Provides
    fun provideFib46(fib45: Fib45, fib44: Fib44) = Fib46(fib45, fib44)

    @Provides
    fun provideFib47(fib46: Fib46, fib45: Fib45) = Fib47(fib46, fib45)

    @Provides
    fun provideFib48(fib47: Fib47, fib46: Fib46) = Fib48(fib47, fib46)

    @Provides
    fun provideFib49(fib48: Fib48, fib47: Fib47) = Fib49(fib48, fib47)

    @Provides
    fun provideFib50(fib49: Fib49, fib48: Fib48) = Fib50(fib49, fib48)

    @Provides
    fun provideFib51(fib50: Fib50, fib49: Fib49) = Fib51(fib50, fib49)

    @Provides
    fun provideFib52(fib51: Fib51, fib50: Fib50) = Fib52(fib51, fib50)

    @Provides
    fun provideFib53(fib52: Fib52, fib51: Fib51) = Fib53(fib52, fib51)

    @Provides
    fun provideFib54(fib53: Fib53, fib52: Fib52) = Fib54(fib53, fib52)

    @Provides
    fun provideFib55(fib54: Fib54, fib53: Fib53) = Fib55(fib54, fib53)

    @Provides
    fun provideFib56(fib55: Fib55, fib54: Fib54) = Fib56(fib55, fib54)

    @Provides
    fun provideFib57(fib56: Fib56, fib55: Fib55) = Fib57(fib56, fib55)

    @Provides
    fun provideFib58(fib57: Fib57, fib56: Fib56) = Fib58(fib57, fib56)

    @Provides
    fun provideFib59(fib58: Fib58, fib57: Fib57) = Fib59(fib58, fib57)

    @Provides
    fun provideFib60(fib59: Fib59, fib58: Fib58) = Fib60(fib59, fib58)

    @Provides
    fun provideFib61(fib60: Fib60, fib59: Fib59) = Fib61(fib60, fib59)

    @Provides
    fun provideFib62(fib61: Fib61, fib60: Fib60) = Fib62(fib61, fib60)

    @Provides
    fun provideFib63(fib62: Fib62, fib61: Fib61) = Fib63(fib62, fib61)

    @Provides
    fun provideFib64(fib63: Fib63, fib62: Fib62) = Fib64(fib63, fib62)

    @Provides
    fun provideFib65(fib64: Fib64, fib63: Fib63) = Fib65(fib64, fib63)

    @Provides
    fun provideFib66(fib65: Fib65, fib64: Fib64) = Fib66(fib65, fib64)

    @Provides
    fun provideFib67(fib66: Fib66, fib65: Fib65) = Fib67(fib66, fib65)

    @Provides
    fun provideFib68(fib67: Fib67, fib66: Fib66) = Fib68(fib67, fib66)

    @Provides
    fun provideFib69(fib68: Fib68, fib67: Fib67) = Fib69(fib68, fib67)

    @Provides
    fun provideFib70(fib69: Fib69, fib68: Fib68) = Fib70(fib69, fib68)

    @Provides
    fun provideFib71(fib70: Fib70, fib69: Fib69) = Fib71(fib70, fib69)

    @Provides
    fun provideFib72(fib71: Fib71, fib70: Fib70) = Fib72(fib71, fib70)

    @Provides
    fun provideFib73(fib72: Fib72, fib71: Fib71) = Fib73(fib72, fib71)

    @Provides
    fun provideFib74(fib73: Fib73, fib72: Fib72) = Fib74(fib73, fib72)

    @Provides
    fun provideFib75(fib74: Fib74, fib73: Fib73) = Fib75(fib74, fib73)

    @Provides
    fun provideFib76(fib75: Fib75, fib74: Fib74) = Fib76(fib75, fib74)

    @Provides
    fun provideFib77(fib76: Fib76, fib75: Fib75) = Fib77(fib76, fib75)

    @Provides
    fun provideFib78(fib77: Fib77, fib76: Fib76) = Fib78(fib77, fib76)

    @Provides
    fun provideFib79(fib78: Fib78, fib77: Fib77) = Fib79(fib78, fib77)

    @Provides
    fun provideFib80(fib79: Fib79, fib78: Fib78) = Fib80(fib79, fib78)

    @Provides
    fun provideFib81(fib80: Fib80, fib79: Fib79) = Fib81(fib80, fib79)

    @Provides
    fun provideFib82(fib81: Fib81, fib80: Fib80) = Fib82(fib81, fib80)

    @Provides
    fun provideFib83(fib82: Fib82, fib81: Fib81) = Fib83(fib82, fib81)

    @Provides
    fun provideFib84(fib83: Fib83, fib82: Fib82) = Fib84(fib83, fib82)

    @Provides
    fun provideFib85(fib84: Fib84, fib83: Fib83) = Fib85(fib84, fib83)

    @Provides
    fun provideFib86(fib85: Fib85, fib84: Fib84) = Fib86(fib85, fib84)

    @Provides
    fun provideFib87(fib86: Fib86, fib85: Fib85) = Fib87(fib86, fib85)

    @Provides
    fun provideFib88(fib87: Fib87, fib86: Fib86) = Fib88(fib87, fib86)

    @Provides
    fun provideFib89(fib88: Fib88, fib87: Fib87) = Fib89(fib88, fib87)

    @Provides
    fun provideFib90(fib89: Fib89, fib88: Fib88) = Fib90(fib89, fib88)

    @Provides
    fun provideFib91(fib90: Fib90, fib89: Fib89) = Fib91(fib90, fib89)

    @Provides
    fun provideFib92(fib91: Fib91, fib90: Fib90) = Fib92(fib91, fib90)

    @Provides
    fun provideFib93(fib92: Fib92, fib91: Fib91) = Fib93(fib92, fib91)

    @Provides
    fun provideFib94(fib93: Fib93, fib92: Fib92) = Fib94(fib93, fib92)

    @Provides
    fun provideFib95(fib94: Fib94, fib93: Fib93) = Fib95(fib94, fib93)

    @Provides
    fun provideFib96(fib95: Fib95, fib94: Fib94) = Fib96(fib95, fib94)

    @Provides
    fun provideFib97(fib96: Fib96, fib95: Fib95) = Fib97(fib96, fib95)

    @Provides
    fun provideFib98(fib97: Fib97, fib96: Fib96) = Fib98(fib97, fib96)

    @Provides
    fun provideFib99(fib98: Fib98, fib97: Fib97) = Fib99(fib98, fib97)

    @Provides
    fun provideFib100(fib99: Fib99, fib98: Fib98) = Fib100(fib99, fib98)
}