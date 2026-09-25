# Proguard / R8 rules for BizliPay

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Moshi & Data Models
-keep class com.plusemon.bizlipay.data.model.** { *; }
-keep class com.plusemon.bizlipay.data.api.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Errorprone annotations (Tink / Security Crypto)
-dontwarn com.google.errorprone.annotations.**

# Coroutines
-dontwarn kotlinx.coroutines.**

