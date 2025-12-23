package com.example.llamadroid

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.example.llamadroid.data.AppContainer
import com.example.llamadroid.data.DefaultAppContainer
import com.example.llamadroid.service.UnifiedNotificationManager
import java.util.Locale

class LlamaApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        UnifiedNotificationManager.init(this)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateLocale(base))
    }
    
    companion object {
        fun updateLocale(context: Context): Context {
            val prefs = context.getSharedPreferences("llamadroid_settings", Context.MODE_PRIVATE)
            val languageCode = prefs.getString("selected_language", "system") ?: "system"
            
            val locale = when (languageCode) {
                "system" -> Locale.getDefault()
                "en" -> Locale.ENGLISH
                "es" -> Locale("es")
                else -> Locale(languageCode)
            }
            
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            
            return context.createConfigurationContext(config)
        }
    }
}
