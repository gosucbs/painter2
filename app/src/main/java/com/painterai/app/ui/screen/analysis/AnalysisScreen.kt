package com.painterai.app.ui.screen.analysis

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.rememberAsyncImagePainter
import com.painterai.app.domain.model.JobResult
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    jobId: String,
    onOpenChat: () -> Unit,
    onBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var activeSlot by remember { mutableIntStateOf(0) }
    var showPickerDialog by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun setPhotoForSlot(slot: Int, uri: Uri) {
        when (slot) {
            0 -> viewModel.onVehiclePhotoSelected(uri)
            1 -> viewModel.onSampleSpecimenSelected(uri)
            2 -> viewModel.onSampleRecipePhotoSelected(uri)
        }
    }

    fun createTempUri(): Uri {
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { setPhotoForSlot(activeSlot, it) } }

    // 갤러리에 사진 저장
    fun saveToGallery(uri: Uri) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "PainterAI_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PainterAI")
            }
            val resolver = context.contentResolver
            val galleryUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (galleryUri != null) {
                resolver.openInputStream(uri)?.use { input ->
                    resolver.openOutputStream(galleryUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            // 갤러리 저장 실패해도 앱 동작에는 영향 없음
        }
    }

    // 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) cameraUri?.let {
            saveToGallery(it)
            setPhotoForSlot(activeSlot, it)
        }
    }

    // 카메라 권한 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val uri = createTempUri()
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val uri = createTempUri()
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            // 긴 변이 1024px로 리사이즈 (API 전송 크기 줄이기)
            val maxSize = 1024
            val ratio = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height, 1f)
            val resized = if (ratio < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    original,
                    (original.width * ratio).toInt(),
                    (original.height * ratio).toInt(),
                    true
                )
            } else {
                original
            }

            // 항상 JPEG로 변환
            val outputStream = ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val jpegBytes = outputStream.toByteArray()

            // JPEG 매직바이트 확인 (FF D8)
            if (jpegBytes.size < 2 || jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) {
                return null
            }

            Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    // 카메라/갤러리 선택 다이얼로그
    if (showPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPickerDialog = false },
            title = { Text("사진 가져오기") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showPickerDialog = false
                            launchCamera()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("카메라로 촬영", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showPickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("갤러리에서 선택", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPickerDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${uiState.job?.colorCode ?: ""} ${uiState.job?.vehicleModel ?: ""}",
                        maxLines = 1
                    )
                },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhotoSlot(
                    label = "차량사진",
                    uri = uiState.vehiclePhotoUri,
                    onClick = { activeSlot = 0; showPickerDialog = true },
                    modifier = Modifier.weight(1f)
                )
                PhotoSlot(
                    label = "조색시편사진",
                    uri = uiState.sampleSpecimenUri,
                    onClick = { activeSlot = 1; showPickerDialog = true },
                    modifier = Modifier.weight(1f)
                )
                PhotoSlot(
                    label = "조색시편배합",
                    uri = uiState.sampleRecipeUri,
                    onClick = { activeSlot = 2; showPickerDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val images = listOfNotNull(
                        uiState.vehiclePhotoUri?.let { uriToBase64(it) },
                        uiState.sampleSpecimenUri?.let { uriToBase64(it) },
                        uiState.sampleRecipeUri?.let { uriToBase64(it) }
                    )
                    viewModel.analyzeWithPhotos(images)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isAnalyzing
                    && uiState.vehiclePhotoUri != null
                    && uiState.sampleSpecimenUri != null
                    && uiState.sampleRecipeUri != null
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 분석 중...")
                } else {
                    Text("AI 분석 요청")
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.analysisResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "AI 분석 결과",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnalysisResultText(uiState.analysisResult!!)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenChat,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("추가 질문")
                    }

                    if (uiState.isSaved) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("저장 완료 - 돌아가기")
                        }
                    } else {
                        // 성공 저장
                        Button(
                            onClick = { viewModel.saveJobResult(JobResult.SUCCESS) },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text("성공 저장")
                        }
                        // 실패 저장
                        OutlinedButton(
                            onClick = { viewModel.saveJobResult(JobResult.FAIL) },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text("실패 저장")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PhotoSlot(
    label: String,
    uri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("촬영", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AnalysisResultText(text: String) {
    val lines = text.split("\n")
    Column(modifier = Modifier.fillMaxWidth()) {
        for (line in lines) {
            when {
                // ### 제목 (h3)
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                // ## 제목 (h2)
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                // 구분선
                line.startsWith("---") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // 불릿 항목 (- 텍스트)
                line.trimStart().startsWith("- ") -> {
                    val indent = line.length - line.trimStart().length
                    val content = line.trimStart().removePrefix("- ")
                    Row(
                        modifier = Modifier.padding(start = (indent * 4 + 8).dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text("•", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 6.dp))
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
                // 볼드 텍스트 (**text**)
                line.contains("**") -> {
                    Spacer(modifier = Modifier.height(2.dp))
                    val parts = line.split("**")
                    androidx.compose.foundation.text.BasicText(
                        text = buildAnnotatedString(parts),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                // 빈 줄
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                // 일반 텍스트
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedString(parts: List<String>): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // 볼드
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
}
