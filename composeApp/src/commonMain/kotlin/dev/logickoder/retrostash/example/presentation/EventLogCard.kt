package dev.logickoder.retrostash.example.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.logickoder.retrostash.example.domain.DemoEvent

@Composable
fun EventLogCard(
    events: List<DemoEvent>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Text("Event log", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${events.size} events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        events.isEmpty() -> Text(
                            "No events yet. Run a query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            content = {
                                items(events) { event ->
                                    EventRow(event)
                                }
                            }
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun EventRow(event: DemoEvent) {
    val color =
        if (event.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Text(
        text = event.message,
        color = color,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
    )
}