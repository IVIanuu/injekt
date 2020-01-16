-keepclassmembers,allowobfuscation class * { @com.ivianuu.injekt.** *; }
-keep class **__Binding
-if class **__Binding
-keep class <1>

-keepclassmembers class * extends com.ivianuu.injekt.LinkedBinding {
    public static ** INSTANCE;
}

-keepclassmembers class * extends com.ivianuu.injekt.UnlinkedBinding {
    public static ** INSTANCE;
}