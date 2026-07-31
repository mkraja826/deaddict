package com.deaddict.app.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deaddict.app.ui.theme.DeAddictTheme

class StorePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = StorePreviewScreen.from(intent.getStringExtra(EXTRA_SCREEN))
        setContent {
            DeAddictTheme {
                StorePreviewApp(screen)
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
    }
}

private enum class StorePreviewScreen(
    val key: String,
    val label: String,
    val icon: String,
) {
    TODAY("today", "Today", "●"),
    TRACKS("tracks", "Tracks", "+"),
    RESCUE("rescue", "Tools", "◉"),
    INSIGHTS("insights", "Insights", "↗"),
    YOU("you", "You", "○");

    companion object {
        fun from(raw: String?): StorePreviewScreen =
            entries.firstOrNull { it.key == raw } ?: TODAY
    }
}

@Composable
private fun StorePreviewApp(screen: StorePreviewScreen) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                StorePreviewScreen.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == screen,
                        onClick = {},
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (screen) {
                StorePreviewScreen.TODAY -> TodayPreview()
                StorePreviewScreen.TRACKS -> TracksPreview()
                StorePreviewScreen.RESCUE -> RescuePreview()
                StorePreviewScreen.INSIGHTS -> InsightsPreview()
                StorePreviewScreen.YOU -> YouPreview()
            }
        }
    }
}

@Composable
private fun TodayPreview() {
    PreviewList(
        title = "Today",
        subtitle = "One honest check-in across every Recovery Track.",
    ) {
        item {
            RookPreview(
                title = "Rook · Facts first",
                body = "Social media met the goal. Protect the next decision instead of celebrating early.",
            )
        }
        item {
            ContextCard()
        }
        item {
            TrackOutcomeCard(
                title = "Social media",
                role = "Primary Recovery Track",
                outcome = "Goal met",
                detail = "42 minutes · peak urge 2 / 5",
                progress = 0.82f,
            )
        }
        item {
            TrackOutcomeCard(
                title = "Caffeine",
                role = "Supporting Recovery Track",
                outcome = "Partly met",
                detail = "2 cups · peak urge 3 / 5",
                progress = 0.56f,
            )
        }
        item {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save private check-in")
            }
        }
        item {
            Text(
                "Fictional preview data · private notes are excluded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TracksPreview() {
    PreviewList(
        title = "Recovery Tracks",
        subtitle = "Each addiction keeps its own goal, history and progress.",
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("Social media · Primary") })
                FilterChip(selected = false, onClick = {}, label = { Text("Caffeine") })
            }
        }
        item {
            RookPreview(
                title = "Rook · One track, one scoreboard",
                body = "Social media cannot borrow excuses from the other Recovery Tracks.",
            )
        }
        item {
            PreviewCard("Social media") {
                StatusLine("Active", "Primary")
                Text("Goal: Daily limit of 60 minutes")
                ProgressMetric("Goal adherence", "82%", 0.82f)
                StatusLine("Latest run", "6 days")
                StatusLine("Best run", "11 days")
            }
        }
        item {
            PreviewCard("Independent history") {
                Text("A slip here never changes Caffeine progress.")
                Text("Rescue sessions and goal changes stay attached to this Recovery Track.")
            }
        }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Add another Recovery Track")
            }
        }
    }
}

@Composable
private fun RescuePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Rescue for Social media",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Rook · Delay the decision",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "The urge has a sales pitch and terrible return policy. Give it two minutes of silence.",
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("01:24", style = MaterialTheme.typography.displayLarge)
        Text("Breathe out gently", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Continue when the pause ends")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Works offline · saved only to Social media",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InsightsPreview() {
    PreviewList(
        title = "Insights",
        subtitle = "Social media only · no combined recovery score.",
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = {}, label = { Text("7 days") })
                FilterChip(selected = true, onClick = {}, label = { Text("30 days") })
                FilterChip(selected = false, onClick = {}, label = { Text("90 days") })
            }
        }
        item {
            RookPreview(
                title = "Rook reads the pattern",
                body = "Social media is improving at 82%. Repeat the conditions behind the better days.",
            )
        }
        item {
            PreviewCard("Daily limit · current goal") {
                ProgressMetric("Adherence", "82%", 0.82f)
                StatusLine("Confirmed days", "24 / 30")
                StatusLine("Goal met", "18 days")
                StatusLine("Latest run", "6 days")
                StatusLine("Average peak urge", "2.4 / 5")
            }
        }
        item {
            PreviewCard("Across your Recovery Tracks") {
                Text("On 5 difficult days, Social media and Caffeine moved in the same direction.")
                Text(
                    "Association only—not proof that one caused the other.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PreviewCard("Replacement actions") {
                StatusLine("Take a short walk", "75% urge reduction")
                StatusLine("Put phone in another room", "67% urge reduction")
            }
        }
    }
}

@Composable
private fun YouPreview() {
    PreviewList(
        title = "You",
        subtitle = "Privacy, reminders, Rook and account controls.",
    ) {
        item {
            PreviewCard("Rook coach") {
                StatusLine("Default tone", "Direct")
                StatusLine("Social media override", "Brutal Banter")
                Text("Safety language always overrides tone.")
            }
        }
        item {
            PreviewCard("Privacy and security") {
                StatusLine("Biometric app lock", "On")
                StatusLine("Protect screenshots", "On")
                StatusLine("Usage monitoring", "Off")
                StatusLine("Anonymous analytics", "Off")
            }
        }
        item {
            PreviewCard("Your data") {
                Text("Private notes remain on this device and are excluded from sync and diagnostics.")
                StatusLine("Local deletion", "Available")
                StatusLine("Account deletion", "Available")
            }
        }
        item {
            PreviewCard("Support & legal") {
                Text("Privacy policy · Terms · Support · Account deletion help")
            }
        }
    }
}

@Composable
private fun PreviewList(
    title: String,
    subtitle: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ContextCard() {
    PreviewCard("Shared context") {
        StatusLine("Mood", "Okay")
        StatusLine("Stress", "2 / 5")
        StatusLine("Energy", "4 / 5")
        StatusLine("Sleep", "Good")
    }
}

@Composable
private fun TrackOutcomeCard(
    title: String,
    role: String,
    outcome: String,
    detail: String,
    progress: Float,
) {
    PreviewCard(title) {
        Text(role, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        StatusLine("Today", outcome)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RookPreview(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title. $body" },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body)
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    content: @Composable Column.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressMetric(label: String, value: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusLine(label, value)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
