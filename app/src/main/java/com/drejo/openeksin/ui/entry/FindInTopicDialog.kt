package com.drejo.openeksin.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FindInTopicDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onQuickFilter: (QuickFindFilter) -> Unit,
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("başlıkta ara") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickFindFilter.entries.forEach { filter ->
                        OutlinedButton(onClick = { onQuickFilter(filter) }) {
                            Text(filter.label)
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    placeholder = { Text("kelime veya @yazar") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSearch(query) }) { Text("ara") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("vazgeç") }
        },
    )
}

enum class QuickFindFilter(val label: String) {
    BuddyEntries("badi entry'leri"),
    Today("bugün"),
    Nice("şükela"),
    Links("linkler"),
}
