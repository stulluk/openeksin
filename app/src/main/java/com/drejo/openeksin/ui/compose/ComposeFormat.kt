package com.drejo.openeksin.ui.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** Inserts eksisozluk markup around the current selection, matching Ekşin. */
fun applyComposeFormat(kind: ComposeFormatKind, value: TextFieldValue): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
    val end = maxOf(value.selection.start, value.selection.end)
    val selected = if (start != end) text.substring(start, end) else ""
    val replacement = when (kind) {
        ComposeFormatKind.Bkz -> "(bkz: $selected)"
        ComposeFormatKind.Hede -> "`$selected`"
        ComposeFormatKind.Smart -> "`:$selected`"
        ComposeFormatKind.Spoiler -> "-s!-\n$selected\n-s!-"
        ComposeFormatKind.Http -> "[http:// $selected]"
    }
    val newText = text.replaceRange(start, end, replacement)
    val cursor = start + replacement.length
    return TextFieldValue(newText, TextRange(cursor))
}

enum class ComposeFormatKind {
    Bkz,
    Hede,
    Smart,
    Spoiler,
    Http,
}

/**
 * Horizontal row of formatting shortcuts from [fragment_entrycompose.xml].
 */
@Composable
fun ComposeFormatBar(
    onFormat: (ComposeFormatKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FormatButton("(bkz: hede)") { onFormat(ComposeFormatKind.Bkz) }
        FormatButton("hede") { onFormat(ComposeFormatKind.Hede) }
        FormatButton("*") { onFormat(ComposeFormatKind.Smart) }
        FormatButton("spoiler") { onFormat(ComposeFormatKind.Spoiler) }
        FormatButton("http") { onFormat(ComposeFormatKind.Http) }
    }
}

@Composable
private fun FormatButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
