package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.VideoModel

/**
 * شاشة المجلدات الرئيسية (FoldersScreen) لتطبيق "نوفا بلاير" (Nova Player).
 * منظمة ومطابقة تماماً لتصميم xPlayer:
 * 1. شريط علوي بعنوان Video مع أيقونات البث (Cast)، البحث، والمزيد.
 * 2. أشرطة التصفية السريعة (Privacy, All Videos, Downloader).
 * 3. معاينة أفقية لأحدث مقاطع الفيديو مع الصور المصغرة والمدة.
 * 4. قائمة المجلدات مع عدد الفيديوهات بداخلها، تفتح الفيديوهات الداخلية فور الضغط على أي مجلد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: Map<String, List<VideoModel>>,
    isLoading: Boolean,
    onFolderClick: (folderName: String) -> Unit,
    onVideoClick: (VideoModel) -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onAllVideosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // جميع الفيديوهات لاستخراج أحدث المقاطع للعرض الأفقي
    val allVideos = folders.values.flatten().sortedByDescending { it.dateAdded }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.testTag("folders_cast_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast"
                        )
                    }
                    IconButton(
                        onClick = onAllVideosClick,
                        modifier = Modifier.testTag("folders_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.testTag("folders_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("folders_top_app_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("folders_loading_indicator"),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري مسح مقاطع الفيديو...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                folders.isEmpty() -> {
                    EmptyFoldersView(
                        modifier = Modifier.testTag("empty_folders_view")
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("folders_list")
                    ) {
                        // 1. شريط الخيارات السريعة (Chips)
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = onPrivacyClick,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        label = { Text("Privacy") },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                        modifier = Modifier.testTag("chip_privacy")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = onAllVideosClick,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        label = { Text("All Videos") },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                        modifier = Modifier.testTag("chip_all_videos")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = { },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        label = { Text("Downloader") },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                        modifier = Modifier.testTag("chip_downloader")
                                    )
                                }
                            }
                        }

                        // 2. معاينة أحدث مقاطع الفيديو أفقياً
                        if (allVideos.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("recent_videos_row")
                                ) {
                                    items(allVideos.take(10), key = { it.id }) { video ->
                                        RecentVideoPreviewCard(
                                            video = video,
                                            onClick = { onVideoClick(video) }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. عنوان قسم المجلدات
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${folders.size} FOLDERS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 4. قائمة المجلدات (الضغط على أي مجلد يعرض فيديوهاته الداخلية فوراً)
                        items(
                            items = folders.entries.toList(),
                            key = { entry -> entry.key }
                        ) { (folderName, videoList) ->
                            FolderRowItem(
                                folderName = folderName,
                                videoCount = videoList.size,
                                onClick = { onFolderClick(folderName) },
                                modifier = Modifier.testTag("folder_item_$folderName")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * بطاقة المعاينة الأفقية للفيديوهات الحديثة
 */
@Composable
fun RecentVideoPreviewCard(
    video: VideoModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .width(135.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.parsedThumbnailUri,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // شارة المدة بالزاوية السفلية اليمنى
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = video.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * صف عرض المجلد الواحد مطابق تماماً لـ xPlayer
 */
@Composable
fun FolderRowItem(
    folderName: String,
    videoCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // أيقونة المجلد بلون محايد مطابق لتصميم xPlayer
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // اسم المجلد وعدد الفيديوهات
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$videoCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

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
 * واجهة الحالة الفارغة عند عدم العثور على أي مجلدات
 */
@Composable
fun EmptyFoldersView(modifier: Modifier = Modifier) {
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
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "لا توجد مجلدات فيديوهات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "لم يتم العثور على مقاطع فيديو في الذاكرة التخزينية لجهازك.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
