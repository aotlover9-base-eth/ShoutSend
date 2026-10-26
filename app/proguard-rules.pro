# ShoutSend R8 / ProGuard Optimization Rules

# 1. Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# 2. Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class app.shoutsend.navigation.** { *; }

# 3. Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class app.shoutsend.core.data.vault.db.** { *; }
-keepclassmembers class app.shoutsend.core.data.vault.db.** { *; }

# 4. Pure JVM Modem & Models
-keep class app.shoutsend.modem.payload.** { *; }
-keep class app.shoutsend.modem.packet.** { *; }
-keep class app.shoutsend.modem.profile.** { *; }
-keep class app.shoutsend.modem.tuning.** { *; }

# 5. Security & Keystore
-dontwarn javax.crypto.**
-keep class javax.crypto.** { *; }
