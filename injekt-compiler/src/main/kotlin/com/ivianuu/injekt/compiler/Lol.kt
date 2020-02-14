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

package com.ivianuu.injekt.compiler

/*
block: BLOCK type=com.ivianuu.injekt.sample.invoking.<no name provided> origin=OBJECT_LITERAL
CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[<unbound IrClassSymbolImpl><<unbound IrClassSymbolImpl>, <unbound IrClassSymbolImpl>, <unbound IrClassSymbolImpl>>]
$this: VALUE_PARAMETER INSTANCE_RECEIVER name:<this> type:com.ivianuu.injekt.sample.invoking.<no name provided>
CONSTRUCTOR visibility:public <> () returnType:com.ivianuu.injekt.sample.invoking.<no name provided> [primary]
BLOCK_BODY
DELEGATING_CONSTRUCTOR_CALL 'UNBOUND IrConstructorSymbolImpl'
INSTANCE_INITIALIZER_CALL classDescriptor='CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[<unbound IrClassSymbolImpl><<unbound IrClassSymbolImpl>, <unbound IrClassSymbolImpl>, <unbound IrClassSymbolImpl>>]'
FUN name:invoke visibility:public modality:OPEN <> ($this:com.ivianuu.injekt.sample.invoking.<no name provided>, p1:<unbound IrClassSymbolImpl>, p2:<unbound IrClassSymbolImpl>) returnType:<unbound IrClassSymbolImpl> [operator]
overridden:
UNBOUND IrSimpleFunctionSymbolImpl
$this: VALUE_PARAMETER name:<this> type:com.ivianuu.injekt.sample.invoking.<no name provided>
VALUE_PARAMETER name:p1 index:0 type:<unbound IrClassSymbolImpl>
VALUE_PARAMETER name:p2 index:1 type:<unbound IrClassSymbolImpl>
BLOCK_BODY
CALL 'UNBOUND IrSimpleFunctionSymbolImpl' type=<unbound IrClassSymbolImpl> origin=null
FUN FAKE_OVERRIDE name:equals visibility:public modality:OPEN <> ($this:<unbound IrClassSymbolImpl>, other:<unbound IrClassSymbolImpl>?) returnType:<unbound IrClassSymbolImpl> [fake_override,operator]
overridden:
UNBOUND IrSimpleFunctionSymbolImpl
$this: VALUE_PARAMETER name:<this> type:<unbound IrClassSymbolImpl>
VALUE_PARAMETER name:other index:0 type:<unbound IrClassSymbolImpl>?
FUN FAKE_OVERRIDE name:hashCode visibility:public modality:OPEN <> ($this:<unbound IrClassSymbolImpl>) returnType:<unbound IrClassSymbolImpl> [fake_override]
overridden:
UNBOUND IrSimpleFunctionSymbolImpl
$this: VALUE_PARAMETER name:<this> type:<unbound IrClassSymbolImpl>
FUN FAKE_OVERRIDE name:toString visibility:public modality:OPEN <> ($this:<unbound IrClassSymbolImpl>) returnType:<unbound IrClassSymbolImpl> [fake_override]
overridden:
UNBOUND IrSimpleFunctionSymbolImpl
$this: VALUE_PARAMETER name:<this> type:<unbound IrClassSymbolImpl>
CONSTRUCTOR_CALL 'public constructor <init> () [primary] declared in com.ivianuu.injekt.sample.invoking.<no name provided>' type=com.ivianuu.injekt.sample.invoking.<no name provided> origin=OBJECT_LITERAL
 */