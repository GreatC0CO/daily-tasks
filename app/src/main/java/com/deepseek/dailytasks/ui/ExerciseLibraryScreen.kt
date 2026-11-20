package com.deepseek.dailytasks.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.ExerciseNote
import com.deepseek.dailytasks.data.variantMark

/**
 * 动作库（动作仓库）：所有计划里出现过的动作，按 2 列卡片展示。
 *
 * 和以前「点一下就加进今天」不同，这里最大的用处是**存资料**：
 * 每个动作可以配一张示范图（从相册选）和一段文字说明（怎么做、注意什么）。
 * 卡片上的 ①② 表示同名动作有几种练法。
 */
@Composable
fun ExerciseLibraryScreen(
    items: List<Exercise>,
    notes: List<ExerciseNote>,
    onSaveNote: (ExerciseNote) -> Unit,
    onBack: () -> Unit,
    onPick: (Exercise) -> Unit,
) {
    var detailName by remember { mutableStateOf<String?>(null) }

    val groups = remember(items) {
        items.groupBy { it.name.trim().lowercase() }
    }
    val noteMap = remember(notes) {
        notes.associateBy { it.name.trim().lowercase() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("动作库", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "${items.size} 个动作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "这里是你所有动作的仓库：点卡片看/写说明和示范图，长按或用卡片右下角＋把它加进今天。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "动作库还是空的。\n先在「今日计划 → 加动作」里建几个动作。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.key }) { exercise ->
                val variants = groups[exercise.name.trim().lowercase()].orEmpty()
                val index = variants.indexOf(exercise).coerceAtLeast(0)
                val note = noteMap[exercise.name.trim().lowercase()]
                LibraryCard(
                    exercise = exercise,
                    note = note,
                    variantLabel = if (variants.size > 1) variantMark(index) else "",
                    onClick = { detailName = exercise.name },
                    onAdd = { onPick(exercise) },
                )
            }
        }
    }

    detailName?.let { name ->
        val variants = groups[name.trim().lowercase()].orEmpty()
        LibraryDetailDialog(
            name = name,
            variants = variants,
            note = noteMap[name.trim().lowercase()] ?: ExerciseNote(name),
            onDismiss = { detailName = null },
            onSaveNote = onSaveNote,
            onAdd = { exercise ->
                detailName = null
                onPick(exercise)
            },
        )
    }
}

@Composable
private fun LibraryCard(
    exercise: Exercise,
    note: ExerciseNote?,
    variantLabel: String,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column {
            val bitmap = rememberImage(note?.imageUri.orEmpty())
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = exercise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        exercise.name.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!note?.hasImage.orFalse()) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .width(14.dp),
                    )
                }
            }
            Row(
                Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (variantLabel.isNotEmpty()) {
                            Text(
                                variantLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        exercise.detailText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        note?.summary ?: "还没有写说明",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (note?.detail.isNullOrBlank()) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onAdd, modifier = Modifier.width(34.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "加入今日", modifier = Modifier.width(18.dp))
                }
            }
        }
    }
}

/** 动作详情：示范图 + 文字说明 + 同名动作的几种练法。 */
@Composable
private fun LibraryDetailDialog(
    name: String,
    variants: List<Exercise>,
    note: ExerciseNote,
    onDismiss: () -> Unit,
    onSaveNote: (ExerciseNote) -> Unit,
    onAdd: (Exercise) -> Unit,
) {
    val context = LocalContext.current
    var detail by remember(name) { mutableStateOf(note.detail) }
    var imageUri by remember(name) { mutableStateOf(note.imageUri) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val bitmap = rememberImage(imageUri)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            "还没有示范图",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (imageUri.isBlank()) "选一张图" else "换一张图", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                    }
                    if (imageUri.isNotBlank()) {
                        OutlinedButton(onClick = { imageUri = "" }, modifier = Modifier.weight(1f)) {
                            Text("去掉图片")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("怎么练 / 注意什么") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
                if (variants.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "名字相同的练法（在「训练模板」里改）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    variants.forEachIndexed { index, exercise ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(variantMark(index), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            Text(exercise.detailText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onAdd(exercise) }) {
                                Text("加进今天", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onAdd(variants.firstOrNull() ?: Exercise(name = name, sets = 3, reps = "10"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("加进今天的清单")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveNote(ExerciseNote(name = name, detail = detail, imageUri = imageUri))
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/** 把持久化 URI 解码成一张小图（选完图立刻能看到）；失败返回 null。 */
@Composable
internal fun rememberImage(uri: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(uri) {
        if (uri.isBlank()) {
            null
        } else {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
                    if (input == null) null else BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}

private fun Boolean?.orFalse(): Boolean = this == true
