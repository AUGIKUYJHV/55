package com.surveyyemen.aerialmap

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration.getInstance
import java.util.Locale

class SurveyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // إعداد osmdroid: مجلد التخزين المحلي للخرائط والصور المخزنة (Cache Offline)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        getInstance().osmdroidBasePath = getExternalFilesDir("osmdroid")
        getInstance().osmdroidTileCache = getExternalFilesDir("osmdroid/tiles")
        getInstance().userAgentValue = packageName
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}

/**
 * أداة مساعدة لتبديل لغة التطبيق بين العربية والإنجليزية فورًا دون إعادة تثبيت
 */
object LocaleHelper {
    const val PREF_LANG = "app_language"

    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString(PREF_LANG, "ar") ?: "ar"
        return updateLocale(context, lang)
    }

    fun setLocale(context: Context, lang: String) {
        val prefs = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LANG, lang).apply()
    }

    private fun updateLocale(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
