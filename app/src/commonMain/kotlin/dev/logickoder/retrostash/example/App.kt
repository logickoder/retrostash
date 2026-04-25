package dev.logickoder.retrostash.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Brand = Color(0xFF6750A4)
private val BrandSecondary = Color(0xFF625B71)

private val LightScheme = lightColorScheme(
    primary = Brand,
    secondary = BrandSecondary,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
)

@Composable
fun RetrostashTheme(useDark: Boolean = false, content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = if (useDark) DarkScheme else LightScheme,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    RetrostashTheme {
        val scope = rememberCoroutineScope()
        val state = remember { DemoState(scope) }
        DisposableEffect(state) {
            onDispose { state.shutdown() }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Retrostash Playground", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Annotation-driven cache · ${platformName}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransportSwitcher(state)
                ControlsCard(state)
                ResultCard(state.lastResult)
                EventLogCard(state, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportSwitcher(state: DemoState) {
    val available = state.availableTransports()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Transport",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Transport.entries.forEachIndexed { index, transport ->
                    val enabled = transport in available
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Transport.entries.size,
                        ),
                        selected = state.transport == transport,
                        enabled = enabled,
                        onClick = { state.transport = transport },
                        label = { Text(transport.label) },
                    )
                }
            }
            if (Transport.OkHttp !in available) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "OkHttp adapter unavailable on $platformName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlsCard(state: DemoState) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Post ID", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(1, 2, 3, 7).forEach { id ->
                        FilterChip(
                            selected = state.postId == id,
                            onClick = { state.postId = id },
                            label = { Text("#$id") },
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { state.runQuery() },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Query")
                }
                Button(
                    onClick = { state.runMutation() },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PublishedWithChanges, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mutate")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { state.clearCache() },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear cache")
                }
                OutlinedButton(
                    onClick = { state.clearLog() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear log")
                }
            }

            if (state.busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Working…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: DemoResult?) {
    if (result == null) return
    val cacheHit = result.source == "retrostash-cache"
    val accent =
        if (cacheHit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        }
    }
}

@Composable
private fun EventLogCard(state: DemoState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Event log", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.events.size} events",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (state.events.isEmpty()) {
                Text(
                    "No events yet. Run a query.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.events) { event ->
                        EventRow(event)
                    }
                }
            }
        }
    }
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
