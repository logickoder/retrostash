package dev.logickoder.retrostash.example.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logickoder.retrostash.example.DemoState
import dev.logickoder.retrostash.example.domain.Transport
import dev.logickoder.retrostash.example.platformName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportSwitcher(
    availableTransports: List<Transport>,
    selectedTransport: Transport,
    modifier: Modifier = Modifier,
    onSelectTransport: (Transport) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = {
                    Text(
                        "Transport",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Transport.entries.forEachIndexed { index, transport ->
                                val enabled = transport in availableTransports
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = Transport.entries.size,
                                    ),
                                    selected = selectedTransport == transport,
                                    enabled = enabled,
                                    onClick = { onSelectTransport(transport) },
                                    label = { Text(transport.label) },
                                )
                            }
                        }
                    )
                    if (Transport.OkHttp !in availableTransports) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "OkHttp adapter unavailable on ${platformName}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            )
        }
    )
}