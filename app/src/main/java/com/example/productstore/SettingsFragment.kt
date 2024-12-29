package com.example.productstore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import okhttp3.*
import java.io.IOException

class SettingsFragment : Fragment() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var themeSwitch: Switch

    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        newPasswordEditText = view.findViewById(R.id.newPasswordEditText)
        saveButton = view.findViewById(R.id.saveButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        themeSwitch = view.findViewById(R.id.themeSwitch)

        val sharedPref = activity?.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

        val isNightMode = sharedPref?.getBoolean("night_mode", false) ?: false
        themeSwitch.isChecked = isNightMode

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val previousMode = sharedPref?.getBoolean("night_mode", false) ?: false
            if (isChecked != previousMode) {
                sharedPref?.edit()?.putBoolean("night_mode", isChecked)?.apply()
                setAppTheme(isChecked)
            }
        }

        saveButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()

            if (newPassword.isBlank()) {
                Toast.makeText(activity, "Введите новый пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentPassword = sharedPref?.getString("password", "") ?: ""

            if (currentPassword.isEmpty()) {
                Toast.makeText(activity, "Не удалось получить текущий пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestBody = FormBody.Builder()
                .add("current_password", currentPassword)
                .add("new_password", newPassword)
                .build()
            val request = Request.Builder()
                .url("https://sagynbekovadi.pythonanywhere.com/change_password")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Toast.makeText(activity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    activity?.runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(activity, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                            newPasswordEditText.text.clear()
                            sharedPref?.edit()?.putString("password", newPassword)?.apply()
                        } else if (response.code == 403) {
                            Toast.makeText(activity, "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "Ошибка при смене пароля", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        logoutButton.setOnClickListener {
            sharedPref?.edit()?.clear()?.apply()

            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun setAppTheme(isNightMode: Boolean) {
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        activity?.recreate()
    }
}