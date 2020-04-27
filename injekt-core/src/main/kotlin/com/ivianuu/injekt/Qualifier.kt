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

package com.ivianuu.injekt

/**
 *
 * A qualifier can help to distinct between bindings of the same type
 *
 * For example:
 *
 * ´´´
 * val component = Component {
 *     factory<CreditCardHandler>(qualifier = Paypal::class) { ... }
 *     factory<CreditCardHandler>(qualifier = Amazon::class) { ... }
 * }
 *
 * val creditCardHandler: CreditCardHandler = if (usePaypal) {
 *     component.get(qualifier = Paypal::class)
 * } else {
 *     component.get(qualifier = Amazon::class)
 * }
 * ´´´
 *
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Qualifier
