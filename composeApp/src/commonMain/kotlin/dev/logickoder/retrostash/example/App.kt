package dev.logickoder.retrostash.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.logickoder.retrostash.example.domain.DemoEvent
import dev.logickoder.retrostash.example.model.DemoResult
import dev.logickoder.retrostash.example.presentation.ControlsCard
import dev.logickoder.retrostash.example.presentation.EventLogCard
import dev.logickoder.retrostash.example.presentation.ResultCard
import dev.logickoder.retrostash.example.presentation.RetrostashTheme
import dev.logickoder.retrostash.example.presentation.TransportSwitcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() = RetrostashTheme {
    val state = rememberDemoState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Retrostash Playground", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Annotation-driven cache · ${Platform.name}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    TransportSwitcher(
                        availableTransports = state.availableTransports(),
                        selectedTransport = state.transport,
                        onSelectTransport = { state.transport = it },
                    )
                    ControlsCard(state)
                    state.lastResult?.let {
                        ResultCard(it)
                    }
                    EventLogCard(state.events, modifier = Modifier.fillMaxSize())
                }
            )
        }
    )
}
