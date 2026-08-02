package com.surveyyemen.aerialmap.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.surveyyemen.aerialmap.LocaleHelper
import com.surveyyemen.aerialmap.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnArabic.setOnClickListener { switchLanguage("ar") }
        binding.btnEnglish.setOnClickListener { switchLanguage("en") }
    }

    private fun switchLanguage(lang: String) {
        LocaleHelper.setLocale(this, lang)
        // إعادة تشغيل التطبيق بالكامل لتطبيق اللغة الجديدة على كل الشاشات
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finishAffinity()
    }
}
