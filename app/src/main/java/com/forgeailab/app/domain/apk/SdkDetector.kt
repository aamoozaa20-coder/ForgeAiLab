package com.forgeailab.app.domain.apk

import com.forgeailab.app.data.model.DetectedSdk
import com.forgeailab.app.data.model.SdkCategory

class SdkDetector {

    private data class Rule(
        val name: String,
        val category: SdkCategory,
        val patterns: List<String>,
        val weight: Float = 0.7f
    )

    private val rules = listOf(
        Rule("Google AdMob", SdkCategory.ADS, listOf("com.google.android.gms.ads", "com.google.ads", "admob")),
        Rule("Facebook Audience Network", SdkCategory.ADS, listOf("com.facebook.ads", "audience_network")),
        Rule("Unity Ads", SdkCategory.ADS, listOf("com.unity3d.ads", "unityads")),
        Rule("AppLovin", SdkCategory.ADS, listOf("com.applovin", "applovin")),
        Rule("ironSource", SdkCategory.ADS, listOf("com.ironsource", "ironsource")),
        Rule("Firebase Analytics", SdkCategory.ANALYTICS, listOf("com.google.firebase.analytics", "firebase.analytics")),
        Rule("Firebase Crashlytics", SdkCategory.CRASH, listOf("com.google.firebase.crashlytics", "crashlytics")),
        Rule("AppsFlyer", SdkCategory.ANALYTICS, listOf("com.appsflyer")),
        Rule("Adjust", SdkCategory.ANALYTICS, listOf("com.adjust.sdk")),
        Rule("OkHttp", SdkCategory.NETWORK, listOf("okhttp3", "com.squareup.okhttp")),
        Rule("Retrofit", SdkCategory.NETWORK, listOf("retrofit2", "com.squareup.retrofit")),
        Rule("Facebook SDK", SdkCategory.SOCIAL, listOf("com.facebook.FacebookSdk", "com.facebook.login")),
        Rule("Billing / Play", SdkCategory.PAYMENT, listOf("com.android.billingclient", "com.android.vending.billing")),
        Rule("FCM / Push", SdkCategory.PUSH, listOf("com.google.firebase.messaging", "com.google.android.gms.gcm")),
        Rule("OneSignal", SdkCategory.PUSH, listOf("com.onesignal")),
        Rule("Google Maps", SdkCategory.MAP, listOf("com.google.android.gms.maps")),
    )

    fun detect(
        packageName: String,
        permissions: List<String>,
        components: List<String>,
        strings: List<String>,
        resources: List<String>,
        nativeLibs: List<String>
    ): List<DetectedSdk> {
        val haystack = (listOf(packageName) + permissions + components + strings.take(300) +
                resources + nativeLibs).joinToString("\n").lowercase()
        val found = mutableListOf<DetectedSdk>()
        for (rule in rules) {
            val hits = rule.patterns.filter { haystack.contains(it.lowercase()) }
            if (hits.isNotEmpty()) {
                val confidence = (rule.weight + 0.1f * hits.size).coerceAtMost(0.98f)
                found.add(
                    DetectedSdk(
                        name = rule.name,
                        category = rule.category,
                        evidence = hits.map { "pattern: $it" },
                        confidence = confidence
                    )
                )
            }
        }
        return found.sortedByDescending { it.confidence }
    }
}
