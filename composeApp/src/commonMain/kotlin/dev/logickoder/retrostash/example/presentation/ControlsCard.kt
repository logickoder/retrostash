package dev.logickoder.retrostash.example.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logickoder.retrostash.example.DemoState

@Composable
fun ControlsCard(state: DemoState) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Text("Post ID", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                content = {
                                    listOf(1, 2, 3, 7).forEach { id ->
                                        FilterChip(
                                            selected = state.postId == id,
                                            onClick = { state.postId = id },
                                            label = { Text("#$id") },
                                        )
                                    }
                                }
                            )
                        }
                    )

                    HorizontalDivider()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Button(
                                onClick = state::runQuery,
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                                content = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Query")
                                }
                            )
                            Button(
                                onClick = state::runMutation,
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                                content = {
                                    Icon(
                                        Icons.Default.PublishedWithChanges,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mutate")
                                }
                            )
                        }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            OutlinedButton(
                                onClick = state::clearCache,
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                                content = {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Clear cache")
                                }
                            )
                            OutlinedButton(
                                onClick = state::clearLog,
                                modifier = Modifier.weight(1f),
                                content = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Clear log")
                                }
                            )
                        }
                    )

                    if (state.busy) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            content = {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Working…", style = MaterialTheme.typography.bodySmall)
                            }
                        )
                    }
                }
            )
        }
    )
}