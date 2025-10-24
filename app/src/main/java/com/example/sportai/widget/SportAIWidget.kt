package com.example.sportai.widget

import android.content.Context
import android.content.Intent
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import com.example.sportai.MainActivity

class SportAIWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Row {
                Button(text = "Gemini", onClick = actionStartActivity(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("service", "gemini")
                }))
                Button(text = "OpenAI", onClick = actionStartActivity(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("service", "openai")
                }))
                Button(text = "Ollama", onClick = actionStartActivity(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("service", "ollama")
                }))
            }
        }
    }
}