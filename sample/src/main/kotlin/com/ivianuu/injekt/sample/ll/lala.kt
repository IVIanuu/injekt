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

package com.ivianuu.injekt.sample.ll

import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.internal.ReaderLambdaInvocation

val lambda: @Reader () -> Unit = {

}

val lambda_: @Reader () -> Unit = {
}

@ReaderLambda("com.ivianuu.injekt.sample.lambda")
interface lambdaDecl

@ReaderLambdaAssignment(
    "com.ivianuu.injekt.sample.main.lambdaVar",
    "com.ivianuu.injekt.sample.main.lambdaVar1"
)
interface lambdaAssignment

interface MyContext

fun higherOrder(
    block: @Reader () -> Unit
) {

}

@ReaderLambda("com.ivianuu.injekt.sample.higherOrder.block")
interface higherOrderBlockDecl

fun main() {
    var lambdaVar: @Reader () -> Unit = {
    }

    lambdaVar = {

    }

    higherOrder(lambdaVar)
}


@ReaderLambdaAssignment(
    "com.ivianuu.injekt.sample.higherOrder.block",
    [MyContext1::class]
)
interface higherOrderBlockDeclAssignment

@ReaderLambda("com.ivianuu.injekt.sample.main.lambdaVar")
interface mainLambdaVarDecl

@ReaderLambdaAssignment(
    "com.ivianuu.injekt.sample.main.lambdaVar",
    MyContext1::class
)
interface mainLambdaVarAssigment1

interface MyContext1

@ReaderLambdaAssignment(
    "com.ivianuu.injekt.sample.main.lambdaVar",
    MyContext2::class
)
interface mainLambdaVarAssigment2

interface MyContext2

@ReaderLambdaInvocation(
    "com.ivianuu.injekt.sample.SuperClass.func.block",
    [ApplicationComponent::class]
)
interface invoc1
