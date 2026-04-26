package dev.logickoder.retrostash.example.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logickoder.retrostash.example.model.DemoResult


@Composable
fun ResultCard(result: DemoResult) {
    val cacheHit = result.source == "retrostash-cache"
    val accent = if (cacheHit) {
        MaterialTheme.colorScheme.tertiary
    } else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = accent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${result.transport.label} · ${result.operation}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        content = {
                            AssistChip(
                                onClick = {},
                                label = { Text("HTTP ${result.statusCode}") },
                                colors = AssistChipDefaults.assistChipColors(),
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(result.source) },
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("${result.sizeBytes}B") },
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("${result.durationMs}ms") },
                            )
                        }
                    )
                }
            )
        }
    )
}