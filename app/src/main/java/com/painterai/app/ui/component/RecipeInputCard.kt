package com.painterai.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.painterai.app.domain.model.Toner

@Composable
fun RecipeInputCard(
    title: String,
    toners: List<Toner>,
    onTonersChanged: (List<Toner>) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalGrams = toners.sumOf { it.grams }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("토너코드", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("g 수", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(40.dp))
            }

            // Toner rows
            toners.forEachIndexed { index, toner ->
                TonerRow(
                    toner = toner,
                    onTonerChanged = { updated ->
                        val newList = toners.toMutableList()
                        newList[index] = updated
                        onTonersChanged(newList)
                    },
                    onDelete = {
                        val newList = toners.toMutableList()
                        newList.removeAt(index)
                        onTonersChanged(newList)
                    }
                )
            }

            // Add button
            TextButton(
                onClick = {
                    onTonersChanged(toners + Toner())
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("토너 추가")
            }

            // Total
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "합계: ${String.format("%.2f", totalGrams)}g",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun TonerRow(
    toner: Toner,
    onTonerChanged: (Toner) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = toner.code,
            onValueChange = { onTonerChanged(toner.copy(code = it.uppercase())) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("K9001") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = if (toner.grams == 0.0) "" else toner.grams.toString(),
            onValueChange = { value ->
                val grams = value.toDoubleOrNull() ?: 0.0
                onTonerChanged(toner.copy(grams = grams))
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(18.dp))
        }
    }
}
