package com.chibaminto.compactalarm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chibaminto.compactalarm.R
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime = LocalTime.now(),
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime)
                }
            ) {
                Text(
                    text = stringResource(R.string.set),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.select_time),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                MaterialTheme(
                    typography = MaterialTheme.typography.copy(
                        displayLarge = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
