package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.VideoModel
import com.example.viewmodel.SortBy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة قائمة الفيديوهات (VideoListScreen) لمجلد معين أو لكافة الفيديوهات،
 * مطابقة تماماً لتصميم xPlayer:
 * 1. زر تبديل العرض بين وضع القائمة (List View) وعرض الشبكة (Grid View) في الشريط العلوي مع تحول فوري وسلس.
 * 2. إظهار الصور المصغرة للفيديوهات بوضوح ودقة عبر مكتبة Coil.
 * 3. عرض شريط المدة مع النسبة المئوية/شريط التقدم الأخضر وتاريخ الإضافة (MM-dd) وقائمة الخيارات (3-dots).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    folderName: String?,
    videos: List<VideoModel>,
    isLoading: Boolean,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSortByChange: (SortBy) -> Unit = {},
    onBackClick: () -> Unit,
    onVideoClick: (VideoModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // حالة التبديل الفوري بين وضع الشبكة ووضع القائمة
    var isGridView by remember { mutableStateOf(false) }
    // حالة إظهار شريط البحث
    var isSearchActive by remember { mutableStateOf(false) }
    // قائمة خيارات الفرز المنسدلة
    var showSortMenu by remember { mutableStateOf(false) }

    // حساب الحجم الإجمالي لكافة الفيديوهات
    val totalBytes = remember(videos) { videos.sumOf { it.size } }
    val formattedTotalSize = remember(totalBytes) { formatTotalSize(totalBytes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("بحث عن فيديو...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_search_input")
                        )
                    } else {
                        Text(
                            text = folderName ?: "Recent Added",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                onSearchQueryChange("")
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("video_list_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // أيقونة البث (Cast)
                    IconButton(
                        onClick = { },
                        modifier = Modifier.testTag("video_cast_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast"
                        )
                    }

                    // 1. زر تبديل العرض الفوري بين List View و Grid View
                    IconButton(
                        onClick = {
                            isGridView = !isGridView
                            val modeMsg = if (isGridView) "Grid View" else "List View"
                            Toast.makeText(context, modeMsg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("video_view_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "عرض كقائمة" else "عرض كشبكة"
                        )
                    }

                    // زر الخيارات الإضافية (3-dots) مع خيارات الفرز
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("video_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "المزيد")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("الأحدث أولاً (تاريخ الإضافة)") },
                                onClick = {
                                    onSortByChange(SortBy.DATE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("الاسم أبجدياً") },
                                onClick = {
                                    onSortByChange(SortBy.NAME)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حجم الملف") },
                                onClick = {
                                    onSortByChange(SortBy.SIZE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("المدة الزمنية") },
                                onClick = {
                                    onSortByChange(SortBy.DURATION)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("video_list_top_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // شريط إحصائيات المجلد (320 VIDEOS  10.3 GB) مطابق لـ xPlayer
            if (videos.isNotEmpty() && !isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${videos.size} VIDEOS   $formattedTotalSize",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("video_list_loading"),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "جاري تحميل مقاطع الفيديو...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    videos.isEmpty() -> {
                        EmptyVideosView(
                            isSearching = searchQuery.isNotEmpty(),
                            modifier = Modifier.testTag("video_list_empty_view")
                        )
                    }
                    isGridView -> {
                        // وضع العرض الشبكي (Grid View) - 3 أعمدة مطابقة لتصميم xPlayer
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("video_grid_list")
                        ) {
                            items(items = videos, key = { it.id }) { video ->
                                VideoGridItem(
                                    video = video,
                                    onClick = { onVideoClick(video) },
                                    modifier = Modifier.testTag("video_grid_item_${video.id}")
                                )
                            }
                        }
                    }
                    else -> {
                        // وضع العرض بالقائمة (List View) - مطابق لتصميم xPlayer
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("video_linear_list")
                        ) {
                            items(items = videos, key = { it.id }) { video ->
                                VideoListItem(
                                    video = video,
                                    onClick = { onVideoClick(video) },
                                    modifier = Modifier.testTag("video_list_item_${video.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * عنصر الفيديو في وضع القائمة (List View) مطابق تماماً لتصميم xPlayer
 */
@Composable
fun VideoListItem(
    video: VideoModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = remember(video.dateAdded) { formatDateShort(video.dateAdded) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // صورة المصغرة بواسطة Coil مع المدة وشريط التقدم الأخضر
        Box(
            modifier = Modifier
                .width(124.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.parsedThumbnailUri)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // شارة المدة بالزاوية السفلية اليمنى
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // شريط استئناف المشاهدة الأخضر في أسفل المصغرة
            if (video.playbackProgressRatio > 0f) {
                LinearProgressIndicator(
                    progress = { video.playbackProgressRatio },
                    color = Color(0xFF00E676),
                    trackColor = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // بيانات الفيديو: الاسم والتاريخ
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }

        // أيقونة الخيارات (3-dots)
        IconButton(
            onClick = { },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * عنصر الفيديو في وضع الشبكة (Grid View) - 3 أعمدة مطابق تماماً لتصميم xPlayer
 */
@Composable
fun VideoGridItem(
    video: VideoModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = remember(video.dateAdded) { formatDateShort(video.dateAdded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        // صورة المصغرة بنسبة 16:9 عبر مكتبة Coil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.parsedThumbnailUri)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // شارة المدة بالزاوية السفلية اليمنى
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }

            // شريط التقدم الأخضر في أسفل المصغرة
            if (video.playbackProgressRatio > 0f) {
                LinearProgressIndicator(
                    progress = { video.playbackProgressRatio },
                    color = Color(0xFF00E676),
                    trackColor = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // اسم الفيديو وأيقونة الخيارات
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        // تاريخ الإضافة (MM-dd)
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

/**
 * شاشة الحالة الفارغة للفيديوهات
 */
@Composable
fun EmptyVideosView(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isSearching) "لا توجد نتائج بحث مطابقة" else "المجلد فارغ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) "تأكد من كتابة اسم الفيديو بشكل صحيح" else "لا توجد أي مقاطع فيديو متاحة في هذا المجلد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDateShort(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

private fun formatTotalSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}
