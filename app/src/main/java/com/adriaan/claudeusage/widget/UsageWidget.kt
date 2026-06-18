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
import androidx.glance.layout.wrapContentWidth
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
        val percent = data.percentUsed
        val progressColor = when {
            percent >= 0.9f -> Color(0xFFE05252)
            percent >= 0.7f -> Color(0xFFE8A045)
            else -> Color(0xFFD97757)
        }

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
                // Header: "Claude · Pro"  +  refresh button aligned right
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
                    // Push refresh button to the right
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

                // Main count
                if (data.messagesLimit > 0) {
                    Text(
                        text = "${data.messagesUsed} / ${data.messagesLimit}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF0EDE8)),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    Text(
                        text = if (data.messagesUsed > 0) "${data.messagesUsed}" else "—",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF0EDE8)),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "messages used",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9E9B96)),
                        fontSize = 11.sp
                    )
                )

                Spacer(GlanceModifier.height(10.dp))

                // Progress bar
                if (data.messagesLimit > 0) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF2E2C2A))
                            .cornerRadius(3.dp)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth(percent.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(progressColor)
                                .cornerRadius(3.dp)
                        ) {}
                    }
                    Spacer(GlanceModifier.height(6.dp))
                }

                // Reset info
                val resetText = when {
                    !data.resetAtIso.isNullOrEmpty() -> "Resets ${formatResetShort(data.resetAtIso)}"
                    !data.hasData -> "Tap refresh to load"
                    else -> ""
                }
                if (resetText.isNotEmpty()) {
                    Text(
                        text = resetText,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF9E9B96)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }

    private fun formatPlanName(raw: String): String =
        raw.replace("_", " ").split(" ").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

    private fun formatResetShort(iso: String): String = try {
        val now = Instant.now()
        val reset = Instant.parse(iso)
        if (reset.isBefore(now)) return "soon"
        val hours = ChronoUnit.HOURS.between(now, reset)
        val mins = ChronoUnit.MINUTES.between(now, reset) % 60
        if (hours > 0) "in ${hours}h ${mins}m" else "in ${mins}m"
    } catch (e: Exception) {
        ""
    }
}
