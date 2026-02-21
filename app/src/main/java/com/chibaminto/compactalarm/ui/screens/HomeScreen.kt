package com.chibaminto.compactalarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibaminto.compactalarm.R
import com.chibaminto.compactalarm.data.AlarmViewModel
import com.chibaminto.compactalarm.ui.components.AlarmCard
import com.chibaminto.compactalarm.ui.components.TimePickerDialog
import com.chibaminto.compactalarm.utils.PermissionUtils
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val context = LocalContext.current

    val cards by viewModel.cards.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val parentCards = remember(cards) {
        cards.filter { it.isParent }.sortedBy { it.alarmTime }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf<String?>(null) }

    var editingCardId by remember { mutableStateOf<String?>(null) }
    var editingInitialTime by remember { mutableStateOf<LocalTime?>(null) }

    // 権限不足ダイアログ用
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 権限チェック（両方必須：ブロック）
    val checkPermissionsAndProceed: (() -> Unit) -> Unit = remember(context) {
        { onPermissionsOk ->
            when {
                !PermissionUtils.hasExactAlarmPermission(context) -> {
                    showAlarmPermissionDialog = true
                }
                PermissionUtils.requiresNotificationPermission() &&
                        !PermissionUtils.hasNotificationPermission(context) -> {
                    showNotificationPermissionDialog = true
                }
                else -> onPermissionsOk()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { messageResId ->
            val result = snackbarHostState.showSnackbar(
                message = context.getString(messageResId),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF333333),
                    contentColor = Color.White,
                    actionColor = Color(0xFF00E5FF)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    checkPermissionsAndProceed {
                        selectedParentId = null
                        showTimePicker = true
                    }
                },
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                    ),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    focusedElevation = 8.dp,
                    hoveredElevation = 10.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_alarm),
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF121212),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }

            AnimatedVisibility(
                visible = !isLoading && parentCards.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = stringResource(R.string.no_alarms),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            AnimatedVisibility(
                visible = !isLoading && parentCards.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = parentCards,
                        key = { it.id }
                    ) { card ->
                        val childAlarms = remember(cards, card.id) {
                            viewModel.getChildAlarms(card.id)
                        }

                        AlarmCard(
                            cardData = card,
                            childAlarms = childAlarms,
                            onToggleEnabled = { id, enabled ->
                                viewModel.toggleCardEnabled(id, enabled)
                            },
                            onAddTime = { parentId ->
                                checkPermissionsAndProceed {
                                    selectedParentId = parentId
                                    showTimePicker = true
                                }
                            },
                            onDelete = { id ->
                                viewModel.removeCard(id)
                            },
                            onWeekdaysChanged = { id, weekdays ->
                                viewModel.updateWeekdays(id, weekdays)
                            },
                            onEditTime = { id, currentTime ->
                                checkPermissionsAndProceed {
                                    editingCardId = id
                                    editingInitialTime = currentTime
                                }
                            },
                            onEditLabel = { id, label ->
                                viewModel.updateLabel(id, label)
                            },
                            onToggleVibrationOnly = { id, vibOnly ->
                                viewModel.updateVibrationOnly(id, vibOnly)
                            }
                        )
                    }
                }
            }
        }
    }

    // 新規作成用TimePicker
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = LocalTime.now().plusMinutes(1),
            onTimeSelected = { time ->
                if (selectedParentId != null) {
                    viewModel.addChildCard(selectedParentId!!, time)
                } else {
                    viewModel.addParentCard(time)
                }
                showTimePicker = false
                selectedParentId = null
            },
            onDismiss = {
                showTimePicker = false
                selectedParentId = null
            }
        )
    }

    // 編集用TimePicker
    if (editingCardId != null && editingInitialTime != null) {
        TimePickerDialog(
            initialTime = editingInitialTime!!,
            onTimeSelected = { time ->
                viewModel.updateAlarmTime(editingCardId!!, time)
                editingCardId = null
                editingInitialTime = null
            },
            onDismiss = {
                editingCardId = null
                editingInitialTime = null
            }
        )
    }

    // アラーム権限ダイアログ（必須）
    if (showAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionDialog = false },
            title = {
                Text(text = stringResource(R.string.permission_required_title))
            },
            text = {
                Text(text = stringResource(R.string.alarm_permission_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlarmPermissionDialog = false
                        context.startActivity(
                            PermissionUtils.getExactAlarmSettingsIntent()
                        )
                    }
                ) {
                    Text(text = stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAlarmPermissionDialog = false }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    // 通知権限ダイアログ（必須）
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = {
                Text(text = stringResource(R.string.permission_required_title))
            },
            text = {
                Text(text = stringResource(R.string.notification_permission_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationPermissionDialog = false }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}