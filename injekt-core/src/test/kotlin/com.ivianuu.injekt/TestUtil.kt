package com.ivianuu.injekt

class Foo

class Bar(val foo: Foo)

class Baz(val bar: Bar, val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

class CommandC : Command
