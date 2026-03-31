package com.painterai.app.ui.screen.newjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJobScreen(
    onJobCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NewJobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdJobId) {
        uiState.createdJobId?.let { onJobCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 작업") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 목업 데이터 생성 버튼
            OutlinedButton(
                onClick = viewModel::fillMockData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("테스트 데이터 채우기")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.vehicleModel,
                onValueChange = viewModel::onVehicleModelChanged,
                label = { Text("차량 모델 *") },
                placeholder = { Text("예: 아이오닉6") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.vehicleYear,
                onValueChange = viewModel::onVehicleYearChanged,
                label = { Text("연식") },
                placeholder = { Text("예: 2023") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.colorCode,
                onValueChange = viewModel::onColorCodeChanged,
                label = { Text("컬러코드 *") },
                placeholder = { Text("예: T2G, D9B") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Paint brand dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.brandExpanded,
                onExpandedChange = viewModel::onBrandExpandedChanged
            ) {
                OutlinedTextField(
                    value = uiState.paintBrand,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("도료사") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.brandExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = uiState.brandExpanded,
                    onDismissRequest = { viewModel.onBrandExpandedChanged(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("KCC 수믹스") },
                        onClick = {
                            viewModel.onPaintBrandChanged("KCC 수믹스")
                            viewModel.onBrandExpandedChanged(false)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.workArea,
                onValueChange = viewModel::onWorkAreaChanged,
                label = { Text("작업 부위") },
                placeholder = { Text("예: 본넷, 휀다") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("메모") },
                placeholder = { Text("추가 메모") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::createJob,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("작업 시작하기")
                }
            }
        }
    }
}
