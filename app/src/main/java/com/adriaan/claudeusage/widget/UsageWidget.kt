package com.adriaan.claudeusage.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adriaan.claudeusage.MainActivity
import com.adriaan.claudeusage.R
import com.adriaan.claudeusage.data.local.SessionManager
import com.adriaan.claudeusage.data.model.UsageData
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit

class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SessionManager(context).usageData.first()
        provideContent { WidgetContent(data) }
    }

    @Composable
    private fun WidgetContent(data: UsageData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1A1917))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.Top
            ) {
                // Header: "Claude · Pro" + refresh button
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Claude",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFD97757)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (!data.planName.isNullOrEmpty()) {
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = "· ${formatPlanName(data.planName)}",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF9E9B96)),
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier
                            .size(18.dp)
                            .clickable(actionRunCallback<RefreshWidgetAction>())
                    )
                }

                Spacer(GlanceModifier.height(10.dp))

                val rows = listOfNotNull(
                    data.sessionLimit?.let { "Session" to it },
                    data.weeklyLimits.firstOrNull()?.let { "Weekly" to it }
                )

                if (rows.isEmpty()) {
                    Text(
                        text = "Tap refresh to load",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF6B6863)),
                            fontSize = 11.sp
                        )
                    )
                } else {
                    rows.forEachIndexed { index, (label, limit) ->
                        LimitBlock(label, limit)
                        if (index < rows.lastIndex) Spacer(GlanceModifier.height(8.dp))
                    }

                    // Countdown for the session window (the one that resets soonest).
                    val countdown = formatCountdown(data.sessionLimit?.resetsAt)
                    if (countdown.isNotEmpty()) {
                        Spacer(GlanceModifier.height(8.dp))
                        Text(
                            text = countdown,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF6B6863)),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LimitBlock(label: String, limit: com.adriaan.claudeusage.data.model.UsageLimit) {
        val color = when {
            limit.percent >= 90 -> Color(0xFFE05252)
            limit.percent >= 70 -> Color(0xFFE8A045)
            else -> Color(0xFFD97757)
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(Color(0xFF9E9B96)), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "${limit.percent}%",
                style = TextStyle(
                    color = ColorProvider(color),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = limit.fraction,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .cornerRadius(4.dp),
            color = ColorProvider(color),
            backgroundColor = ColorProvider(Color(0xFF2E2C2A))
        )
    }

    private fun formatPlanName(raw: String): String =
        raw.replace("_", " ").split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

    private fun formatCountdown(iso: String?): String {
        if (iso.isNullOrEmpty()) return ""
        return try {
            val now = Instant.now()
            val reset = Instant.parse(iso)
            if (reset.isBefore(now)) return "Resetting soon"
            val hours = ChronoUnit.HOURS.between(now, reset)
            val mins = ChronoUnit.MINUTES.between(now, reset) % 60
            val timeLeft = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            "Session resets in $timeLeft"
        } catch (_: Exception) {
            ""
        }
    }
}
