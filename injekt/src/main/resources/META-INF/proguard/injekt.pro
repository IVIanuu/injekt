-keepclassmembers,allowobfuscation class * { @com.ivianuu.injekt.** *; }
-keepclassmembers class * extends com.ivianuu.injekt.LinkedBinding {
    public static ** INSTANCE;
}
-keepclassmembers class * extends com.ivianuu.injekt.UnlinkedBinding {
    public static ** INSTANCE;
}