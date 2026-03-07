# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================== ОБЩИЕ НАСТРОЙКИ ====================
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-verbose
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

# ==================== ОБФУСКАЦИЯ SECURITY КЛАССОВ ====================
# Максимальная обфускация security пакета
-keep class com.tutozz.blespam.security.NativeLib {
    native <methods>;
}

-keepclassmembers class com.tutozz.blespam.security.NativeLib {
    native <methods>;
}

# Обфусцировать всё остальное в security
-repackageclasses 'a'
-flattenpackagehierarchy 'a'

# Переименование классов security в нечитаемые имена
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# ==================== УДАЛЕНИЕ DEBUG INFO ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Удалить println
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ==================== ЗАЩИТА ОТ ДЕКОМПИЛЯЦИИ ====================
# Шифрование строковых литералов (если используется R8 full mode)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Оптимизации
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ==================== KEEP RULES ДЛЯ ПРИЛОЖЕНИЯ ====================
# Сохранить Activity
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Сохранить Application
-keep public class * extends android.app.Application

# JSON модели (если используются)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== OKHTTP ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==================== AGGRESSIVE OBFUSCATION ====================
# Инлайнинг
-allowaccessmodification
-mergeinterfacesaggressively

# Удаление неиспользуемого кода
-dontnote **
-dontwarn **