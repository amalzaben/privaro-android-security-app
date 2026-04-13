package com.example.privaro.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.data.model.UsageLog
import com.example.privaro.ui.theme.PrivaroTeal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogEntryCard(log: UsageLog) {
    val isSafe = (log.peopleDetected ?: 0) == 0
    val title = if (isSafe) "Safe Scan" else "Person Detected"
    val description = if (isSafe) "No people detected" else "TalkBack was paused"
    val formattedTime = formatTimestamp(log.timestamp)

    val accessibilityDescription = buildString {
        append(title)
        append(". ")
        append(formattedTime)
        append(". ")
        append(description)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibilityDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSafe) PrivaroTeal.copy(alpha = 0.15f)
                        else Color(0xFFFF9800).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSafe) "\u2713" else "\u26A0",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSafe) PrivaroTeal else Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSafe) PrivaroTeal else Color(0xFFFF9800)
                )

                // Timestamp
                Text(
                    text = formattedTime,
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )

                // Description
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }

            // People count badge if detected
            if (!isSafe && log.peopleDetected != null) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${log.peopleDetected}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant): String {
    val logDate = timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val dayPart = when {
        logDate == today -> "Today"
        logDate == yesterday -> "Yesterday"
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
            dateFormatter.format(logDate)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        .withZone(ZoneId.systemDefault())
    val timePart = timeFormatter.format(timestamp)

    return "$dayPart \u2022 $timePart"
}
