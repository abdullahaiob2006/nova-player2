@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.VideoModel
import com.example.player.AspectRatioItem
import com.example.player.DecoderMode
import com.example.player.EqualizerBottomSheet
import com.example.player.NovaAudioEffects
import com.example.player.NovaRatioPreferences
import com.example.player.PlayOptionsMenuBottomSheet
import com.example.player.RatioBottomSheet
import com.example.player.ScreenOrientationMode
import com.example.player.TrackManager
import com.example.player.TracksSelectionBottomSheet
import com.example.player.VideoScreenshotHelper
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * خيارات التحكم بنسبة عرض الشاشة (Aspect Ratio Modes)
 */
enum class VideoAspectRatio(val title: String, val resizeMode: Int) {
    FIT("ملاءمة (Fit)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("ملء الشاشة (Fill)", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("تكبير (Zoom)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("أبعاد 16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

/**
 * خيارات مؤقت النوم (Sleep Timer)
 */
enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("إيقاف المؤقت", 0),
    MIN_10("10 دقائق", 10),
    MIN_15("15 دقيقة", 15),
    MIN_30("30 دقيقة", 30),
    MIN_45("45 دقيقة", 45),
    MIN_60("60 دقيقة", 60),
    END_OF_VIDEO("عند نهاية الفيديو", -1)
}

/**
 * نوع المؤشر البصري المعروض نتيجة إيماءات اللمس
 */
enum class GestureIndicatorType {
    VOLUME,
    BRIGHTNESS
}

private enum class DragDirection {
    NONE,
    HORIZONTAL,
    VERTICAL
}

/**
 * شاشة مشغل الفيديو والصوت المتقدمة والشاملة لتطبيق "نوفا بلاير" (Nova Player).
 * تتضمن الميزات الاحترافية الكاملة:
 * 1. تشغيل MP4, MKV, AVI, WebM, MP3, AAC, FLAC عبر Jetpack Media3 ExoPlayer.
 * 2. التحكم بسرعة التشغيل (Playback Speed) من 0.25x حتى 4.0x مع تصحيح طبقة الصوت (Pitch Correction).
 * 3. ميزة تكرار المقطع (A-B Repeat) لتكرار جزء معين بحلقة لا نهائية.
 * 4. أدوات مزامنة التأخير (Delay Offset) لضبط توقيت الصوت والترجمة بالمللي ثانية.
 * 5. المعادل الصوتي والمؤثرات (Equalizer & Bass Boost).
 * 6. ميزة التشغيل في الخلفية (Background Playback) مع دعم إشعارات الوسائط.
 * 7. استئناف المشاهدة (Resume Playback) ومؤقت النوم (Sleep Timer).
 * 8. النافذة العائمة (PiP)، قفل الشاشة، والتحكم بنسب العرض، وإيماءات اللمس.
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoModel,
    playlist: List<VideoModel> = emptyList(),
    initialIndex: Int = 0,
    initialPosition: Long = 0L,
    onVideoChanged: ((VideoModel) -> Unit)? = null,
    onBackClick: (lastPosition: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // إدارة قائمة التشغيل والفهرس الحالي بدقة
    val effectivePlaylist = remember(playlist, video) {
        if (playlist.isNotEmpty()) playlist else listOf(video)
    }
    var currentIndex by remember(effectivePlaylist, initialIndex) {
        mutableIntStateOf(
            if (effectivePlaylist.isNotEmpty()) {
                val foundIndex = effectivePlaylist.indexOfFirst { it.id == video.id }
                if (foundIndex >= 0) foundIndex else initialIndex.coerceIn(0, effectivePlaylist.size - 1)
            } else 0
        )
    }
    var currentVideo by remember(currentIndex, effectivePlaylist) {
        mutableStateOf(effectivePlaylist.getOrElse(currentIndex) { video })
    }
    var lastPreviousClickTime by remember { mutableLongStateOf(0L) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    // تهيئة مشغل ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(currentVideo.contentUri))
            prepare()
            if (initialPosition > 0) {
                seekTo(initialPosition)
            }
            playWhenReady = true
        }
    }

    // مدير المسارات الصوتية والترجمة
    val trackManager = remember(exoPlayer) { TrackManager(exoPlayer) }
    var showTracksSheet by remember { mutableStateOf(false) }

    // المعادل الصوتي وتضخيم الصوت (Equalizer & Bass Boost)
    val audioEffects = remember(exoPlayer) { NovaAudioEffects(exoPlayer) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val eqSheetState = rememberModalBottomSheetState()

    // حالات تحكم المشغل الأساسية
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(initialPosition) }
    var totalDuration by remember { mutableLongStateOf(currentVideo.duration) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    // دالة الانتقال الفوري لفيديو معين بالقائمة مع التحديث والتجهيز والتشغيل التلقائي من البداية
    val playVideoAtIndex: (Int) -> Unit = { newIndex ->
        if (newIndex in effectivePlaylist.indices) {
            currentIndex = newIndex
            val targetVideo = effectivePlaylist[newIndex]
            currentVideo = targetVideo
            onVideoChanged?.invoke(targetVideo)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(targetVideo.contentUri))
            exoPlayer.prepare()
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            currentPosition = 0L
            totalDuration = targetVideo.duration.coerceAtLeast(0L)
            isPlaying = true
        }
    }

    // سرعة التشغيل (Playback Speed)
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // خيارات المشغل الاحترافية (xPlayer Professional Suite)
    var orientationMode by remember { mutableStateOf(ScreenOrientationMode.AUTO) }
    var showOrientationBanner by remember { mutableStateOf(false) }
    var decoderMode by remember { mutableStateOf(DecoderMode.HW) }
    var isMuted by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableFloatStateOf(1.0f) }
    var isNightMode by remember { mutableStateOf(false) }
    var nightDimLevel by remember { mutableFloatStateOf(0.4f) }
    var isMirrored by remember { mutableStateOf(false) }
    var isVisualEnhancer by remember { mutableStateOf(false) }
    var showPlayOptionsMenu by remember { mutableStateOf(false) }
    var showScreenshotFlash by remember { mutableStateOf(false) }
    val playOptionsSheetState = rememberModalBottomSheetState()

    // ميزة تكرار المقطع (A-B Repeat)
    var abRepeatPointA by remember { mutableStateOf<Long?>(null) }
    var abRepeatPointB by remember { mutableStateOf<Long?>(null) }
    var isAbRepeatActive by remember { mutableStateOf(false) }

    // أدوات مزامنة التأخير (Audio & Subtitle Delay)
    var audioDelayMs by remember { mutableLongStateOf(0L) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var showDelayDialog by remember { mutableStateOf(false) }

    // التشغيل في الخلفية (Background Playback)
    var isBackgroundPlaybackEnabled by remember { mutableStateOf(true) }

    // قفل الشاشة (Screen Lock)
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }

    // نسبة أبعاد العرض وإعدادات xPlayer (Aspect Ratio)
    var rememberRatio by remember {
        mutableStateOf(NovaRatioPreferences.isRememberRatioEnabled(context))
    }
    var tapToSwitchDirectly by remember {
        mutableStateOf(NovaRatioPreferences.isTapToSwitchDirectlyEnabled(context))
    }
    var selectedRatioItem by remember {
        val initialId = if (rememberRatio) NovaRatioPreferences.getSavedRatioId(context) else AspectRatioItem.FIT.id
        mutableStateOf(AspectRatioItem.fromId(initialId))
    }
    var showRatioBottomSheet by remember { mutableStateOf(false) }
    val ratioSheetState = rememberModalBottomSheetState()
    var showAspectRatioBanner by remember { mutableStateOf(false) }

    // النافذة العائمة (Picture-in-Picture)
    var isInPipMode by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.isInPictureInPictureMode == true
            } else false
        )
    }

    // مؤقت النوم (Sleep Timer)
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingSeconds by remember { mutableLongStateOf(0L) }
    var sleepTimerActive by remember { mutableStateOf(false) }
    var sleepAtEndOfVideo by remember { mutableStateOf(false) }

    // شارة استئناف المشاهدة
    var showResumeBanner by remember { mutableStateOf(initialPosition > 5_000L) }

    // حالات السحب
    var isHorizontalSeeking by remember { mutableStateOf(false) }
    var horizontalSeekTargetMs by remember { mutableLongStateOf(0L) }
    var horizontalSeekDeltaSeconds by remember { mutableLongStateOf(0L) }

    var currentVolumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var currentBrightnessLevel by remember {
        val initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (initialBrightness >= 0f) initialBrightness else 0.5f)
    }
    var activeIndicator by remember { mutableStateOf<GestureIndicatorType?>(null) }
    var indicatorPercent by remember { mutableIntStateOf(0) }
    var hideIndicatorTrigger by remember { mutableLongStateOf(0L) }

    // التحكم الفوري بكتم الصوت (Instant Mute / Unmute)
    val toggleMute: () -> Unit = {
        if (isMuted) {
            exoPlayer.volume = previousVolume.coerceIn(0.1f, 1.0f)
            isMuted = false
            Toast.makeText(context, "تم تشغيل الصوت", Toast.LENGTH_SHORT).show()
        } else {
            previousVolume = if (exoPlayer.volume > 0f) exoPlayer.volume else 1.0f
            exoPlayer.volume = 0f
            isMuted = true
            Toast.makeText(context, "تم كتم الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    // التقاط صورة الشاشة الفورية (Screenshot)
    val takeScreenshot: () -> Unit = {
        activity?.let { act ->
            showScreenshotFlash = true
            VideoScreenshotHelper.captureScreenshot(
                activity = act,
                onSuccess = { uri ->
                    Toast.makeText(context, "تم حفظ لقطة الشاشة في المعرض (Pictures/NovaPlayer)", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // إدارة اتجاه الشاشة (Screen Orientation)
    DisposableEffect(orientationMode, activity) {
        activity?.requestedOrientation = orientationMode.orientationCode
        onDispose {
            // إعادة الضبط للوضع الطبيعي غير المقيد عند الخروج
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // تأثير وميض لقطة الشاشة
    LaunchedEffect(showScreenshotFlash) {
        if (showScreenshotFlash) {
            delay(120)
            showScreenshotFlash = false
        }
    }

    // مؤقت إخفاء شارة اتجاه الشاشة
    LaunchedEffect(showOrientationBanner) {
        if (showOrientationBanner) {
            delay(1500)
            showOrientationBanner = false
        }
    }

    // حلقة تكرار المقطع A-B Repeat
    LaunchedEffect(isAbRepeatActive, currentPosition, abRepeatPointA, abRepeatPointB) {
        if (isAbRepeatActive && abRepeatPointA != null && abRepeatPointB != null) {
            val endPoint = abRepeatPointB!!
            val startPoint = abRepeatPointA!!
            if (currentPosition >= endPoint) {
                exoPlayer.seekTo(startPoint)
            }
        }
    }

    // إعداد الدخول التلقائي في PiP
    DisposableEffect(activity, isPlaying) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(isPlaying)
                .build()
            activity?.setPictureInPictureParams(params)
        }
        onDispose { }
    }

    // رصد تغييرات وضع PiP
    DisposableEffect(context) {
        val callback = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isInPipMode = activity?.isInPictureInPictureMode == true
                }
            }
            override fun onLowMemory() {}
        }
        context.registerComponentCallbacks(callback)
        onDispose {
            context.unregisterComponentCallbacks(callback)
        }
    }

    // عداد مؤقت النوم الحي
    LaunchedEffect(sleepTimerActive, sleepTimerRemainingSeconds) {
        if (sleepTimerActive && sleepTimerRemainingSeconds > 0) {
            delay(1000)
            sleepTimerRemainingSeconds -= 1
            if (sleepTimerRemainingSeconds <= 0L) {
                exoPlayer.pause()
                sleepTimerActive = false
                Toast.makeText(context, "تم إيقاف التشغيل بواسطة مؤقت النوم", Toast.LENGTH_LONG).show()
            }
        }
    }

    // إخفاء شارة الاستئناف بعد 4.5 ثوانٍ
    LaunchedEffect(showResumeBanner) {
        if (showResumeBanner) {
            delay(4500)
            showResumeBanner = false
        }
    }

    // مؤقت إخفاء شارة نسبة العرض
    LaunchedEffect(showAspectRatioBanner) {
        if (showAspectRatioBanner) {
            delay(1500)
            showAspectRatioBanner = false
        }
    }

    // مؤقت إخفاء مؤشرات السحب
    LaunchedEffect(hideIndicatorTrigger) {
        if (hideIndicatorTrigger > 0L) {
            delay(1200)
            activeIndicator = null
        }
    }

    // مؤقت إخفاء زر فتح القفل
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3000)
            showUnlockButton = false
        }
    }

    // مزامنة حالة المشغل وتحرير الموارد
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (sleepAtEndOfVideo) {
                        exoPlayer.pause()
                        sleepAtEndOfVideo = false
                        Toast.makeText(context, "اكتمل تشغيل الفيديو ومؤقت النوم", Toast.LENGTH_SHORT).show()
                    } else if (currentIndex < effectivePlaylist.size - 1) {
                        playVideoAtIndex(currentIndex + 1)
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            audioEffects.release()
            exoPlayer.release()
        }
    }

    // ربط دورة الحياة مع دعم التشغيل في الخلفية
    DisposableEffect(lifecycleOwner, isBackgroundPlaybackEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // إذا كان التشغيل في الخلفية مفعلاً أو التطبيق في وضع PiP، يستمر التشغيل
                    if (isBackgroundPlaybackEnabled) {
                        // استمرار تشغيل الصوت بالخلفية
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity?.isInPictureInPictureMode == true) {
                        // استمرار التشغيل في PiP
                    } else {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer.play()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        isInPipMode = activity?.isInPictureInPictureMode == true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // تحديث موضع التشغيل
    LaunchedEffect(isPlaying, isSeeking, isHorizontalSeeking) {
        while (isPlaying && !isSeeking && !isHorizontalSeeking) {
            currentPosition = exoPlayer.currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // إخفاء أدوات التحكم تلقائياً
    LaunchedEffect(showControls, isPlaying, isScreenLocked, isInPipMode) {
        if (showControls && isPlaying && !isScreenLocked && !isInPipMode) {
            delay(3500)
            showControls = false
        }
    }

    // الحاوية الأساسية للشاشة
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_container")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isInPipMode) {
                        Modifier.pointerInput(isScreenLocked) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isRightSide = down.position.x >= (size.width / 2f)
                                var totalDragX = 0f
                                var totalDragY = 0f
                                var dragDirection = DragDirection.NONE
                                var dragStartPlaybackPos = exoPlayer.currentPosition
                                var isDragging = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (change.changedToUp()) {
                                        if (isScreenLocked) {
                                            showUnlockButton = !showUnlockButton
                                        } else if (!isDragging) {
                                            showControls = !showControls
                                        } else {
                                            if (dragDirection == DragDirection.HORIZONTAL) {
                                                exoPlayer.seekTo(horizontalSeekTargetMs)
                                                currentPosition = horizontalSeekTargetMs
                                                isHorizontalSeeking = false
                                            } else if (dragDirection == DragDirection.VERTICAL) {
                                                hideIndicatorTrigger = System.currentTimeMillis()
                                            }
                                        }
                                        break
                                    }

                                    if (isScreenLocked) continue

                                    val deltaX = change.position.x - change.previousPosition.x
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalDragX += abs(deltaX)
                                    totalDragY += abs(deltaY)

                                    if (dragDirection == DragDirection.NONE && (totalDragX > 20f || totalDragY > 20f)) {
                                        dragDirection = if (totalDragX > totalDragY) {
                                            dragStartPlaybackPos = exoPlayer.currentPosition
                                            DragDirection.HORIZONTAL
                                        } else {
                                            DragDirection.VERTICAL
                                        }
                                    }

                                    if (dragDirection != DragDirection.NONE) {
                                        isDragging = true
                                        change.consume()

                                        if (dragDirection == DragDirection.HORIZONTAL) {
                                            isHorizontalSeeking = true
                                            val screenWidthPx = size.width.toFloat().coerceAtLeast(1f)
                                            val seekRangeMs = (totalDuration.coerceAtLeast(60_000L) * 0.25f).coerceIn(30_000f, 180_000f)
                                            val deltaMs = (deltaX / screenWidthPx) * seekRangeMs

                                            val newTarget = (horizontalSeekTargetMs.takeIf { isHorizontalSeeking && it > 0 } ?: dragStartPlaybackPos) + deltaMs.toLong()
                                            horizontalSeekTargetMs = newTarget.coerceIn(0L, totalDuration.coerceAtLeast(0L))
                                            horizontalSeekDeltaSeconds = (horizontalSeekTargetMs - dragStartPlaybackPos) / 1000L
                                        } else {
                                            val screenHeightPx = size.height.toFloat().coerceAtLeast(1f)
                                            val deltaRatio = -deltaY / (screenHeightPx * 0.75f)

                                            if (isRightSide) {
                                                val deltaVolume = deltaRatio * maxVolume
                                                currentVolumeLevel = (currentVolumeLevel + deltaVolume).coerceIn(0f, maxVolume.toFloat())
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolumeLevel.toInt(), 0)
                                                indicatorPercent = ((currentVolumeLevel / maxVolume) * 100).toInt().coerceIn(0, 100)
                                                activeIndicator = GestureIndicatorType.VOLUME
                                            } else {
                                                val newBrightness = (currentBrightnessLevel + deltaRatio).coerceIn(0.01f, 1.0f)
                                                currentBrightnessLevel = newBrightness
                                                activity?.let { act ->
                                                    val lp = act.window.attributes
                                                    lp.screenBrightness = newBrightness
                                                    act.window.attributes = lp
                                                }
                                                indicatorPercent = (newBrightness * 100).toInt().coerceIn(1, 100)
                                                activeIndicator = GestureIndicatorType.BRIGHTNESS
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = selectedRatioItem.exoResizeMode
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        playerView.resizeMode = selectedRatioItem.exoResizeMode
                    },
                    modifier = run {
                        val targetRatio = selectedRatioItem.floatRatio
                        if (targetRatio != null) {
                            Modifier
                                .aspectRatio(targetRatio, matchHeightConstraintsFirst = false)
                                .graphicsLayer {
                                    scaleX = if (isMirrored) -1f else 1f
                                }
                        } else {
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = if (isMirrored) -1f else 1f
                                }
                        }
                    }
                )

            // 1. فلتر محسن العرض البصري (Visual Enhancer)
            if (isVisualEnhancer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1500D0FF))
                )
            }

            // 2. فلتر الوضع الليلي لحماية العين (Night Mode Dim Filter)
            if (isNightMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = nightDimLevel))
                )
            }
            }
        }

        // وميض التقاط لقطة الشاشة (Screenshot Flash Effect)
        if (showScreenshotFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        if (!isInPipMode) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.Center)
                        .testTag("player_buffering_indicator")
                )
            }

            // شارة استئناف المشاهدة
            AnimatedVisibility(
                visible = showResumeBanner,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE61E1E2C),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "تم استئناف التشغيل من ${formatTime(initialPosition)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    exoPlayer.seekTo(0L)
                                    currentPosition = 0L
                                    showResumeBanner = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "من البداية",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // شارة تكرار المقطع A-B إذا كان مفعلاً
            if (isAbRepeatActive && abRepeatPointA != null && abRepeatPointB != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 74.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RepeatOne,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تكرار A-B: ${formatTime(abRepeatPointA!!)} ⇄ ${formatTime(abRepeatPointB!!)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    isAbRepeatActive = false
                                    abRepeatPointA = null
                                    abRepeatPointB = null
                                }
                        )
                    }
                }
            }

            // شارة نسبة العرض المختارة
            AnimatedVisibility(
                visible = showAspectRatioBanner,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE61E1E2C),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selectedRatioItem.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // شارة اتجاه الشاشة عند التبديل
            AnimatedVisibility(
                visible = showOrientationBanner,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE61E1E2C),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = orientationMode.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // المؤشر البصري للسحب الأفقي
            AnimatedVisibility(
                visible = isHorizontalSeeking,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                HorizontalSeekFeedbackIndicator(
                    targetPosition = horizontalSeekTargetMs,
                    totalDuration = totalDuration,
                    deltaSeconds = horizontalSeekDeltaSeconds,
                    modifier = Modifier.testTag("horizontal_seek_indicator")
                )
            }

            // المؤشر البصري للصوت / السطوع
            AnimatedVisibility(
                visible = activeIndicator != null && !isHorizontalSeeking,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(
                    if (activeIndicator == GestureIndicatorType.VOLUME) Alignment.CenterEnd else Alignment.CenterStart
                )
            ) {
                activeIndicator?.let { type ->
                    GestureFeedbackIndicator(
                        type = type,
                        percent = indicatorPercent,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .testTag("gesture_feedback_indicator")
                    )
                }
            }

            // زر فتح القفل العائم
            AnimatedVisibility(
                visible = isScreenLocked && showUnlockButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable {
                            isScreenLocked = false
                            showControls = true
                            showUnlockButton = false
                        }
                        .testTag("player_unlock_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "فتح قفل الشاشة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // واجهة عناصر التحكم المخصصة (Overlay Controls)
            AnimatedVisibility(
                visible = showControls && !isScreenLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                ) {
                    // 1. الشريط العلوي: زر الرجوع، العنوان، والميزات الاحترافية
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onBackClick(exoPlayer.currentPosition) },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = currentVideo.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // شارة فك التشفير (HW vs SW Decoder Badge)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (decoderMode == DecoderMode.HW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    decoderMode = if (decoderMode == DecoderMode.HW) DecoderMode.SW else DecoderMode.HW
                                    Toast.makeText(context, "تم تحويل فك التشفير إلى: ${decoderMode.title}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = decoderMode.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        // زر كتم الصوت الفوري (Instant Mute / Unmute)
                        IconButton(
                            onClick = toggleMute,
                            modifier = Modifier.testTag("player_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "إلغاء كتم الصوت" else "كتم الصوت",
                                tint = if (isMuted) MaterialTheme.colorScheme.error else Color.White
                            )
                        }

                        // زر تبديل اتجاه الشاشة (Screen Orientation)
                        IconButton(
                            onClick = {
                                orientationMode = when (orientationMode) {
                                    ScreenOrientationMode.AUTO -> ScreenOrientationMode.LANDSCAPE
                                    ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
                                    ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.AUTO
                                }
                                showOrientationBanner = true
                            },
                            modifier = Modifier.testTag("player_orientation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "تبديل اتجاه الشاشة",
                                tint = if (orientationMode != ScreenOrientationMode.AUTO) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // زر التقاط صورة الشاشة المباشرة (Screenshot)
                        IconButton(
                            onClick = takeScreenshot,
                            modifier = Modifier.testTag("player_screenshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "التقاط صورة",
                                tint = Color.White
                            )
                        }

                        // زر سرعة التشغيل (Playback Speed)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSpeedDialog = true }
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "${currentSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.onPrimary else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        // زر المعادل الصوتي (Equalizer)
                        IconButton(
                            onClick = { showEqualizerSheet = true },
                            modifier = Modifier.testTag("player_equalizer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "المعادل الصوتي",
                                tint = if (audioEffects.isEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // زر PiP
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                },
                                modifier = Modifier.testTag("player_pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "نافذة عائمة (PiP)",
                                    tint = Color.White
                                )
                            }
                        }

                        // زر قائمة الخيارات المتقدمة (Play Options Menu)
                        IconButton(
                            onClick = { showPlayOptionsMenu = true },
                            modifier = Modifier.testTag("player_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات التشغيل المتقدمة",
                                tint = Color.White
                            )
                        }
                    }

                    // شارة مؤقت النوم
                    if (sleepTimerActive && sleepTimerRemainingSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 62.dp, end = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatTime(sleepTimerRemainingSeconds * 1000L),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // 2. زر لقطة الشاشة العائم على الحافة اليمنى (مطابق لواجهة xPlayer في الصورة الأولى)
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    ) {
                        IconButton(
                            onClick = takeScreenshot,
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("player_floating_screenshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "التقاط صورة",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // 3. الشريط السفلي للتحكم (Bottom Controls Bar - مطابق لـ xPlayer)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        // أ. شريط التمرير الزمني: التوقيت الحالي يساراً، شريط التقدم، التوقيت الإجمالي يميناً (فرض اتجاه LTR حصرياً)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTime(if (isSeeking) (seekSliderPosition * totalDuration).toLong() else currentPosition),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.widthIn(min = 44.dp)
                                )

                                Slider(
                                    value = if (isSeeking) seekSliderPosition else {
                                        if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                                    },
                                    onValueChange = { newRatio ->
                                        isSeeking = true
                                        seekSliderPosition = newRatio
                                    },
                                    onValueChangeFinished = {
                                        val targetPosition = (seekSliderPosition * totalDuration).toLong()
                                        exoPlayer.seekTo(targetPosition)
                                        currentPosition = targetPosition
                                        isSeeking = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF00E676),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .testTag("player_timeline_slider")
                                )

                                Text(
                                    text = formatTime(totalDuration),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.widthIn(min = 44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ب. أزرار التحكم السفلية: Lock -> Next -> Play/Pause Circle -> Previous -> Ratio
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. زر قفل الشاشة (Lock)
                            IconButton(
                                onClick = {
                                    isScreenLocked = true
                                    showControls = false
                                    showUnlockButton = true
                                },
                                modifier = Modifier.testTag("player_bottom_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "قفل الشاشة",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // 2. زر الفيديو التالي (Next - بجانب زر القفل)
                            IconButton(
                                onClick = {
                                    if (currentIndex < effectivePlaylist.size - 1) {
                                        playVideoAtIndex(currentIndex + 1)
                                    } else {
                                        Toast.makeText(context, "وصلت إلى نهاية قائمة التشغيل", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("player_bottom_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "التالي",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 3. زر التشغيل / الإيقاف المؤقت الدائري البارز مع حلقة بيضاء (Play/Pause)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(width = 1.8.dp, color = Color.White, shape = CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            exoPlayer.play()
                                        }
                                    }
                                    .testTag("player_bottom_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 4. زر الفيديو السابق / إعادة التشغيل من البداية (Previous - بجانب زر نسبة العرض)
                            IconButton(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val isNearBeginning = exoPlayer.currentPosition <= 3_000L
                                    val isRapidDoublePress = (now - lastPreviousClickTime) < 2_000L

                                    if (isNearBeginning || isRapidDoublePress) {
                                        // الضغطة الثانية أو إذا كان الفيديو في بدايته بالفعل: الانتقال للفيديو السابق
                                        if (currentIndex > 0) {
                                            playVideoAtIndex(currentIndex - 1)
                                        } else {
                                            exoPlayer.seekTo(0L)
                                            currentPosition = 0L
                                            Toast.makeText(context, "هذا هو أول فيديو في القائمة", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // الضغطة الأولى: تعيد الفيديو الحالي إلى بدايته (الموضع 0)
                                        exoPlayer.seekTo(0L)
                                        currentPosition = 0L
                                    }
                                    lastPreviousClickTime = now
                                },
                                modifier = Modifier.testTag("player_bottom_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "السابق",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 5. زر نسب العرض (Ratio Button)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .pointerInput(tapToSwitchDirectly, selectedRatioItem, rememberRatio) {
                                        detectTapGestures(
                                            onTap = {
                                                if (tapToSwitchDirectly) {
                                                    val cycle = AspectRatioItem.QUICK_SWITCH_CYCLE
                                                    val currentIndex = cycle.indexOfFirst { it.id == selectedRatioItem.id }
                                                    val nextIndex = if (currentIndex != -1) (currentIndex + 1) % cycle.size else 0
                                                    selectedRatioItem = cycle[nextIndex]
                                                    if (rememberRatio) {
                                                        NovaRatioPreferences.saveRatioId(context, selectedRatioItem.id)
                                                    }
                                                    showAspectRatioBanner = true
                                                } else {
                                                    showRatioBottomSheet = true
                                                }
                                            },
                                            onLongPress = {
                                                showRatioBottomSheet = true
                                            }
                                        )
                                    }
                                    .testTag("player_bottom_ratio_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "نسبة العرض",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // النافذة السفلية لاختيار الصوت والترجمة
    if (showTracksSheet) {
        TracksSelectionBottomSheet(
            trackManager = trackManager,
            onDismissRequest = { showTracksSheet = false },
            sheetState = sheetState
        )
    }

    // النافذة السفلية للمعادل الصوتي (Equalizer & Bass Boost)
    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            effects = audioEffects,
            onDismissRequest = { showEqualizerSheet = false },
            sheetState = eqSheetState
        )
    }

    // النافذة السفلية لخيارات التشغيل المتقدمة (Play Options Menu Bottom Sheet - xPlayer style)
    if (showPlayOptionsMenu) {
        PlayOptionsMenuBottomSheet(
            orientationMode = orientationMode,
            onOrientationChange = { mode ->
                orientationMode = mode
                showOrientationBanner = true
            },
            decoderMode = decoderMode,
            onDecoderChange = { mode ->
                decoderMode = mode
                Toast.makeText(context, "تم تحويل فك التشفير إلى: ${mode.title}", Toast.LENGTH_SHORT).show()
            },
            isMuted = isMuted,
            onToggleMute = toggleMute,
            isNightMode = isNightMode,
            onNightModeToggle = { isNightMode = it },
            nightDimLevel = nightDimLevel,
            onNightDimLevelChange = { nightDimLevel = it },
            isMirrored = isMirrored,
            onMirrorToggle = { isMirrored = it },
            isVisualEnhancer = isVisualEnhancer,
            onVisualEnhancerToggle = { isVisualEnhancer = it },
            onTakeScreenshot = takeScreenshot,
            onOpenEqualizer = { showEqualizerSheet = true },
            onOpenSpeedDialog = { showSpeedDialog = true },
            abRepeatPointA = abRepeatPointA,
            abRepeatPointB = abRepeatPointB,
            isAbRepeatActive = isAbRepeatActive,
            onSetPointA = {
                abRepeatPointA = exoPlayer.currentPosition
                Toast.makeText(context, "تم تحديد النقطة A عند ${formatTime(exoPlayer.currentPosition)}", Toast.LENGTH_SHORT).show()
            },
            onSetPointB = {
                if (abRepeatPointA != null && exoPlayer.currentPosition > abRepeatPointA!!) {
                    abRepeatPointB = exoPlayer.currentPosition
                    isAbRepeatActive = true
                    Toast.makeText(context, "تم تحديد النقطة B وتفعيل التكرار!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "يجب تحديد النقطة A أولاً قبل B", Toast.LENGTH_SHORT).show()
                }
            },
            onClearAbRepeat = {
                isAbRepeatActive = false
                abRepeatPointA = null
                abRepeatPointB = null
                Toast.makeText(context, "تم إلغاء التكرار", Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { showPlayOptionsMenu = false },
            sheetState = playOptionsSheetState
        )
    }

    // النافذة السفلية لنسب العرض الاحترافية (Ratio Bottom Sheet - xPlayer style)
    if (showRatioBottomSheet) {
        RatioBottomSheet(
            selectedRatio = selectedRatioItem,
            onRatioSelected = { option ->
                selectedRatioItem = option
                if (rememberRatio) {
                    NovaRatioPreferences.saveRatioId(context, option.id)
                }
                showAspectRatioBanner = true
            },
            rememberRatio = rememberRatio,
            onRememberRatioChange = { enabled ->
                rememberRatio = enabled
                NovaRatioPreferences.setRememberRatioEnabled(context, enabled)
                if (enabled) {
                    NovaRatioPreferences.saveRatioId(context, selectedRatioItem.id)
                }
            },
            tapToSwitchDirectly = tapToSwitchDirectly,
            onTapToSwitchDirectlyChange = { enabled ->
                tapToSwitchDirectly = enabled
                NovaRatioPreferences.setTapToSwitchDirectlyEnabled(context, enabled)
            },
            onDismissRequest = { showRatioBottomSheet = false },
            sheetState = ratioSheetState
        )
    }

    // مربع حوار سرعة التشغيل (Playback Speed)
    if (showSpeedDialog) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سرعة التشغيل (مع تصحيح النبرة)")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر سرعة تشغيل المقطع:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        items(speeds) { speed ->
                            val isSelected = currentSpeed == speed
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // مربع حوار المزامنة وتكرار A-B (Tune & A-B Repeat Dialog)
    if (showDelayDialog) {
        AlertDialog(
            onDismissRequest = { showDelayDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تكرار A-B والمزامنة والتشغيل الخلفي")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. تكرار المقطع A-B Repeat
                    Text(
                        text = "تكرار جزء محدد (A-B Loop):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                abRepeatPointA = exoPlayer.currentPosition
                                Toast.makeText(context, "تم تحديد النقطة A عند ${formatTime(exoPlayer.currentPosition)}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(text = if (abRepeatPointA != null) "A: ${formatTime(abRepeatPointA!!)}" else "تحديد A")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (abRepeatPointA != null && exoPlayer.currentPosition > abRepeatPointA!!) {
                                    abRepeatPointB = exoPlayer.currentPosition
                                    isAbRepeatActive = true
                                    Toast.makeText(context, "تم تحديد النقطة B وتفعيل التكرار!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "يجب تحديد النقطة A أولاً قبل B", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(text = if (abRepeatPointB != null) "B: ${formatTime(abRepeatPointB!!)}" else "تحديد B")
                        }

                        OutlinedButton(
                            onClick = {
                                isAbRepeatActive = false
                                abRepeatPointA = null
                                abRepeatPointB = null
                                Toast.makeText(context, "تم إلغاء التكرار", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("إلغاء")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. خيار التشغيل في الخلفية (Background Playback)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استمرار التشغيل بالخلفية", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = isBackgroundPlaybackEnabled,
                            onCheckedChange = { isBackgroundPlaybackEnabled = it }
                        )
                    }

                    // 3. تأخير الصوت (Audio Delay Offset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مزامنة الصوت:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { audioDelayMs -= 50 }) { Text("-50ms") }
                            Text("${audioDelayMs}ms", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { audioDelayMs += 50 }) { Text("+50ms") }
                        }
                    }

                    // 4. تأخير الترجمة (Subtitle Delay Offset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مزامنة الترجمة:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { subtitleDelayMs -= 100 }) { Text("-100ms") }
                            Text("${subtitleDelayMs}ms", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { subtitleDelayMs += 100 }) { Text("+100ms") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDelayDialog = false }) {
                    Text("تم")
                }
            }
        )
    }

    // مربع حوار مؤقت النوم (Sleep Timer Dialog)
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مؤقت النوم (Sleep Timer)")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر وقتاً لإيقاف المشغل تلقائياً إذا رغبت بالنوم:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SleepTimerOption.entries.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (option == SleepTimerOption.OFF) {
                                        sleepTimerActive = false
                                        sleepAtEndOfVideo = false
                                        sleepTimerRemainingSeconds = 0L
                                        Toast.makeText(context, "تم إلغاء مؤقت النوم", Toast.LENGTH_SHORT).show()
                                    } else if (option == SleepTimerOption.END_OF_VIDEO) {
                                        sleepAtEndOfVideo = true
                                        sleepTimerActive = false
                                        Toast.makeText(context, "سيتم إيقاف التشغيل عند نهاية هذا الفيديو", Toast.LENGTH_SHORT).show()
                                    } else {
                                        sleepAtEndOfVideo = false
                                        sleepTimerRemainingSeconds = option.minutes * 60L
                                        sleepTimerActive = true
                                        Toast.makeText(context, "تم ضبط مؤقت النوم على ${option.label}", Toast.LENGTH_SHORT).show()
                                    }
                                    showSleepTimerDialog = false
                                }
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

/**
 * مؤشر السحب الأفقي للتقديم والترجيع السريع مع إظهار الوقت المستهدف
 */
@Composable
fun HorizontalSeekFeedbackIndicator(
    targetPosition: Long,
    totalDuration: Long,
    deltaSeconds: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xE61E1E2C),
        shadowElevation = 10.dp,
        modifier = modifier
            .width(220.dp)
            .height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (deltaSeconds >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = if (deltaSeconds >= 0) Color(0xFF66BB6A) else Color(0xFFFFA726),
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "${if (deltaSeconds >= 0) "+" else ""}${deltaSeconds}s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (deltaSeconds >= 0) Color(0xFF66BB6A) else Color(0xFFFFA726)
                )
            }

            Text(
                text = "${formatTime(targetPosition)} / ${formatTime(totalDuration)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                val ratio = if (totalDuration > 0) (targetPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * بطاقة المؤشر البصري لحركات السحب للتحكم بالصوت أو السطوع
 */
@Composable
fun GestureFeedbackIndicator(
    type: GestureIndicatorType,
    percent: Int,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val label: String
    val accentColor: Color

    when (type) {
        GestureIndicatorType.VOLUME -> {
            label = "الصوت"
            accentColor = Color(0xFF388E3C)
            icon = when {
                percent == 0 -> Icons.Default.VolumeMute
                percent < 50 -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }
        }
        GestureIndicatorType.BRIGHTNESS -> {
            label = "السطوع"
            accentColor = Color(0xFFFFA000)
            icon = when {
                percent < 35 -> Icons.Default.BrightnessLow
                percent < 75 -> Icons.Default.BrightnessMedium
                else -> Icons.Default.BrightnessHigh
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xE61E1E2C),
        shadowElevation = 8.dp,
        modifier = modifier
            .width(82.dp)
            .height(180.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )

            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = (percent / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor)
                )
            }

            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * دالة مساعدة لتنسيق الوقت بصيغة (HH:mm:ss أو mm:ss)
 */
private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
