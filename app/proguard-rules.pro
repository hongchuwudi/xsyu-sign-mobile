# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.xsyusign.mobile.network.dto.** { *; }
-keepclassmembers class com.xsyusign.mobile.network.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
