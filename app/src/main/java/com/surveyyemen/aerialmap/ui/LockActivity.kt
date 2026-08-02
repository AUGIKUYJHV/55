package com.surveyyemen.aerialmap.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.surveyyemen.aerialmap.R
import com.surveyyemen.aerialmap.databinding.ActivityLockBinding
import java.security.MessageDigest

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val prefs by lazy { getSharedPreferences("survey_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hasPin = prefs.contains("app_pin_hash")

        if (!hasPin) {
            // أول مرة: نطلب من المستخدم تعيين كلمة سر احتياطية
            binding.tvLockTitle.text = getString(R.string.set_new_pin)
            binding.btnUnlock.text = getString(R.string.save_pin)
            binding.btnUnlock.setOnClickListener { savePin() }
        } else {
            binding.tvLockTitle.text = getString(R.string.enter_pin)
            binding.btnUnlock.setOnClickListener { checkPin() }
            tryBiometricUnlock()
        }

        binding.btnUseFingerprint.setOnClickListener { tryBiometricUnlock() }
    }

    private fun savePin() {
        val pin = binding.etPin.text.toString()
        if (pin.length < 4) {
            Toast.makeText(this, getString(R.string.pin_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().putString("app_pin_hash", hash(pin)).apply()
        goToMain()
    }

    private fun checkPin() {
        val pin = binding.etPin.text.toString()
        val savedHash = prefs.getString("app_pin_hash", null)
        if (savedHash != null && savedHash == hash(pin)) {
            goToMain()
        } else {
            Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun tryBiometricUnlock() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    goToMain()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // المستخدم يستطيع اللجوء لكلمة السر بدلًا من ذلك
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.use_pin_instead))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
