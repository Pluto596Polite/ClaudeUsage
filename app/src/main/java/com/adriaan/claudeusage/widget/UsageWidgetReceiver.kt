package com.adriaan.claudeusage.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = UsageWidget()
}
