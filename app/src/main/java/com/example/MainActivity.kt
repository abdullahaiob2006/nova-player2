package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.example.model.VideoModel
import com.example.repository.AudioRepository
import com.example.repository.VideoRepository
import com.example.ui.screens.AudioListScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VideoListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.vault.VaultManager
import com.example.viewmodel.VideoViewModel
import kotlinx.coroutines.launch

/**
 * تبويبات التنقل الرئيسية الثلاثة للتطبيق (Video, Music, Playlist) مطابقة لتصميم xPlayer
 */
enum class MainNavTab(val title: String) {
    VIDEO("Video"),
    MUSIC("Music"),
    PLAYLIST("Playlist")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // تهيئة محمل الصور Coil مع دعم فك تشفير إطارات الفيديو (Video Thumbnails)
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        val videoRepository = VideoRepository(this)
        val vaultManager = VaultManager(this)
        val videoViewModel = VideoViewModel(videoRepository)

        setContent {
            MyApplicationTheme {
                NovaPlayerApp(
                    videoViewModel = videoViewModel,
                    videoRepository = videoRepository,
                    vaultManager = vaultManager
                )
            }
        }
    }
}

/**
 * المكون الجذري للتطبيق (Nova Player Root App)
 */
@Composable
fun NovaPlayerApp(
    videoViewModel: VideoViewModel,
    videoRepository: VideoRepository,
    vaultManager: VaultManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioRepository = remember { AudioRepository(context) }
    val audiosList by audioRepository.getAllAudios().collectAsState(initial = emptyList())

    // فحص إذن الوصول لملفات الوسائط (فيديو وصوت)
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isGranted = permissionsMap.values.any { it }
        hasPermission = isGranted
        if (isGranted) {
            videoViewModel.loadVideos()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // حالات التنقل والشاشات
    var selectedTab by remember { mutableStateOf(MainNavTab.VIDEO) }
    var viewingFolder by remember { mutableStateOf<String?>(null) }
    var isVaultOpen by remember { mutableStateOf(false) }
    var activePlayingVideo by remember { mutableStateOf<VideoModel?>(null) }
    var activePlayingPlaylist by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var currentPlayingIndex by remember { mutableIntStateOf(0) }

    val uiState by videoViewModel.uiState.collectAsState()

    // إذا كان هناك فيديو أو مقطع قيد التشغيل، نعرض شاشة PlayerScreen في وضع ملء الشاشة
    if (activePlayingVideo != null) {
        val videoToPlay = activePlayingVideo!!
        BackHandler {
            scope.launch {
                val lastPos = videoRepository.getLastPosition(videoToPlay.id)
                videoRepository.savePlaybackPosition(videoToPlay.id, lastPos, videoToPlay.duration)
            }
            activePlayingVideo = null
        }

        PlayerScreen(
            video = videoToPlay,
            playlist = activePlayingPlaylist,
            initialIndex = currentPlayingIndex,
            initialPosition = videoToPlay.lastPlayedPosition,
            onVideoChanged = { newVideo ->
                activePlayingVideo = newVideo
                currentPlayingIndex = activePlayingPlaylist.indexOfFirst { it.id == newVideo.id }.coerceAtLeast(0)
            },
            onBackClick = { lastPosition ->
                scope.launch {
                    videoRepository.savePlaybackPosition(videoToPlay.id, lastPosition, videoToPlay.duration)
                    videoViewModel.loadVideos()
                }
                activePlayingVideo = null
            }
        )
        return
    }

    // إدارة زر الرجوع
    BackHandler(enabled = isVaultOpen || viewingFolder != null || selectedTab != MainNavTab.VIDEO) {
        if (isVaultOpen) {
            isVaultOpen = false
        } else if (viewingFolder != null) {
            viewingFolder = null
        } else if (selectedTab != MainNavTab.VIDEO) {
            selectedTab = MainNavTab.VIDEO
        }
    }

    // إذا لم يتم منح إذن الوصول للوسائط، نعرض شاشة طلب الإذن
    if (!hasPermission) {
        PermissionRequiredScreen(
            onRequestPermission = {
                permissionLauncher.launch(permissionsToRequest)
            }
        )
        return
    }

    // واجهة التطبيق الرئيسية مع شريط التنقل السفلي بثلاثة تبويبات رئيسية (Video, Music, Playlist) مطابقة لـ xPlayer
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                // 1. تبويب Video
                NavigationBarItem(
                    selected = selectedTab == MainNavTab.VIDEO && !isVaultOpen,
                    onClick = {
                        isVaultOpen = false
                        viewingFolder = null
                        selectedTab = MainNavTab.VIDEO
                    },
                    icon = {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "Video")
                    },
                    label = { Text("Video") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_video")
                )

                // 2. تبويب Music
                NavigationBarItem(
                    selected = selectedTab == MainNavTab.MUSIC,
                    onClick = {
                        isVaultOpen = false
                        viewingFolder = null
                        selectedTab = MainNavTab.MUSIC
                    },
                    icon = {
                        Icon(Icons.Default.MusicNote, contentDescription = "Music")
                    },
                    label = { Text("Music") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_music")
                )

                // 3. تبويب Playlist
                NavigationBarItem(
                    selected = selectedTab == MainNavTab.PLAYLIST,
                    onClick = {
                        isVaultOpen = false
                        viewingFolder = null
                        selectedTab = MainNavTab.PLAYLIST
                    },
                    icon = {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlist")
                    },
                    label = { Text("Playlist") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_playlist")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = Triple(selectedTab, viewingFolder, isVaultOpen),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main_screen_transition"
            ) { (currentTab, currentFolder, vaultOpen) ->
                when {
                    // 1. الخزنة الخاصة المشفرة
                    vaultOpen -> {
                        VaultScreen(
                            vaultManager = vaultManager,
                            onBackClick = { isVaultOpen = false },
                            onPlayVideo = { video ->
                                activePlayingPlaylist = listOf(video)
                                currentPlayingIndex = 0
                                activePlayingVideo = video
                            }
                        )
                    }

                    // 2. عرض فيديوهات مجلد معين فور الضغط عليه في شاشة المجلدات
                    currentFolder != null -> {
                        val folderVideos = if (currentFolder == "All Videos") {
                            uiState.videos
                        } else {
                            uiState.folders[currentFolder] ?: emptyList()
                        }
                        VideoListScreen(
                            folderName = currentFolder,
                            videos = folderVideos,
                            isLoading = uiState.isLoading,
                            onBackClick = { viewingFolder = null },
                            onVideoClick = { video ->
                                activePlayingPlaylist = folderVideos
                                currentPlayingIndex = folderVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                activePlayingVideo = video
                            }
                        )
                    }

                    // 3. تبويب Video الرئيسي: يعرض شاشة المجلدات المنظمة المطابقة لـ xPlayer
                    currentTab == MainNavTab.VIDEO -> {
                        FoldersScreen(
                            folders = uiState.folders,
                            isLoading = uiState.isLoading,
                            onFolderClick = { folderName ->
                                // فتح الفيديوهات الداخلية للمجلد فور الضغط عليه
                                viewingFolder = folderName
                            },
                            onVideoClick = { video ->
                                val allVideos = uiState.videos
                                activePlayingPlaylist = allVideos
                                currentPlayingIndex = allVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                activePlayingVideo = video
                            },
                            onPrivacyClick = {
                                isVaultOpen = true
                            },
                            onAllVideosClick = {
                                viewingFolder = "All Videos"
                            }
                        )
                    }

                    // 4. تبويب Music: مكتبة المقاطع الصوتية
                    currentTab == MainNavTab.MUSIC -> {
                        AudioListScreen(
                            audios = audiosList,
                            isLoading = false,
                            onAudioClick = { audio ->
                                val audioVideos = audiosList.map { it.toVideoModel() }
                                activePlayingPlaylist = audioVideos
                                currentPlayingIndex = audioVideos.indexOfFirst { it.id == audio.id }.coerceAtLeast(0)
                                activePlayingVideo = audio.toVideoModel()
                            }
                        )
                    }

                    // 5. تبويب Playlist: قوائم التشغيل المطابقة لتصميم xPlayer
                    currentTab == MainNavTab.PLAYLIST -> {
                        PlaylistsScreen(
                            onPlaylistClick = { playlist ->
                                Toast.makeText(context, "قائمة: ${playlist.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * شاشة توضيحية لطلب الإذن لقراءة ملفات الوسائط
 */
@Composable
fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit,
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
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "مرحباً بك في نوفا بلاير",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "يحتاج التطبيق إلى إذن الوصول لملفات الوسائط حتى يتمكن من مسح وتشغيل مقاطع الفيديو المخزنة على هاتفك.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .testTag("grant_permission_button")
        ) {
            Text(
                text = "منح الإذن الآن",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
