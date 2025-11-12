package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM

@Composable
fun GameScreen(vm: GameViewModel, onNavigateHome: () -> Unit = {}) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()

    val totalEvents = vm.totalEvents
    val eventNumberText = if (gameState.eventIndex >= 0) {
        "Event ${gameState.eventIndex + 1}/$totalEvents"
    } else {
        "Event -/-"
    }


    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "N-back: ${vm.nBack}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = eventNumberText,
                    style = MaterialTheme.typography.titleMedium
                )

            }

            if(gameState.gameType != GameType.Audio) {
                // 3x3 Grid in the middle
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val cellIndex = row * 3 + col
                                val isHighlighted = gameState.eventValue == cellIndex

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            when {
                                                isHighlighted && gameState.useAlternateColor ->
                                                    MaterialTheme.colorScheme.secondary
                                                isHighlighted ->
                                                    MaterialTheme.colorScheme.primary
                                                else ->
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .border(2.dp, MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                }
            } else {
                // Show audio version
                Text(
                    text = "Listen to the sounds",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 100.dp)
                )
            }

            // Match button at bottom
            Button(
                onClick = { vm.checkMatch() },
                enabled = gameState.eventValue != -1, // Only enabled during game
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(60.dp)
            ) {
                Text(
                    text = "MATCH",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (gameState.eventIndex == -1) {
                Button(
                    onClick = {
                        onNavigateHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(50.dp)
                ) {
                    Text("Back to Home")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    GameScreen(vm = FakeVM())
}