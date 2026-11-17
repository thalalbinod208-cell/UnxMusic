package com.maxrave.simpmusic.selfheal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.BugTicketEntity
import com.maxrave.domain.repository.SelfHealRepository
import com.maxrave.simpmusic.selfheal.ai.GeminiBugSolver
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SelfHealConsoleScreen(navController: NavController) {
    val repository: SelfHealRepository = koinInject()
    val solver: GeminiBugSolver = koinInject()
    val tickets by repository.tickets.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<BugTicketEntity?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val visible = tickets.filter { it.title.contains(query, true) || it.category.contains(query, true) }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Developer · AI Self-Heal") }, navigationIcon = {
            TextButton(onClick = { navController.navigateUp() }) { Text("Back") }
        }, actions = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val result = repository.syncGitHubIssues("thalalbinod208-cell/UnxMusic")
                    message = result.fold({ "Synced $it GitHub issues" }, { it.message ?: "GitHub sync failed" })
                    busy = false
                }
            }) { Text("Sync") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            item {
                val runtime = tickets.count { it.source != "GITHUB_ISSUE" }
                val healed = tickets.sumOf { it.selfHealSucceeded }
                val ai = tickets.count { it.aiRootCause != null }
                Text("${tickets.size} tickets  ·  $runtime runtime  ·  $healed auto-healed  ·  $ai AI analyzed", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search tickets") }, singleLine = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
            items(visible, key = { it.id }) { ticket ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { selected = ticket }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ticket.title, style = MaterialTheme.typography.titleMedium)
                        Text("${ticket.source} · ${ticket.category} · ${ticket.severity} · ${ticket.status}", style = MaterialTheme.typography.bodySmall)
                        Text("${ticket.occurrenceCount} occurrence(s) · ${ticket.selfHealSucceeded}/${ticket.selfHealAttempts} recovered", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    selected?.let { ticket ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(ticket.title) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${ticket.category} · ${ticket.severity} · ${ticket.status}")
                ticket.aiRootCause?.let { Text("Root cause: $it") }
                ticket.aiStepByStepFix?.let { Text("Suggested fix: $it") }
                ticket.aiKotlinPatch?.takeIf { it.isNotBlank() }?.let { patch ->
                    Text(patch, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(patch)) }) { Text("Copy patch") }
                }
                if (ticket.stackTrace.isNotBlank()) Text(ticket.stackTrace.take(2_500), style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    message = solver.solve(ticket).fold({ "AI analysis saved" }, { it.message ?: "AI analysis failed" })
                    busy = false
                }
            }) { Text(if (busy) "Working…" else "AI analyze") }
        }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Close") } })
    }
}
