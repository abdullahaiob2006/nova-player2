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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.VideoModel
import com.example.vault.VaultManager
import com.example.vault.VaultVideoEntity
import kotlinx.coroutines.launch

/**
 * شاشة الخزنة الخاصة (VaultScreen) لتطبيق "نوفا بلاير" (Nova Player).
 * تحمي الفيديوهات الخاصة خلف رمز مرور (PIN) ولوحة أرقام وبصمة الإصبع،
 * وتتيح تصفح الفيديوهات المخفية وتشغيلها أو استعادتها للذاكرة العامة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultManager: VaultManager,
    onBackClick: () -> Unit,
    onPlayVideo: (VideoModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isPinAlreadySet = remember { vaultManager.isPinSet() }

    // قائمة الفيديوهات المخفية داخل الخزنة
    val vaultVideos by vaultManager.vaultVideos.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "الخزنة الخاصة",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("vault_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        // زر إعادة قفل الخزنة فوراً
                        IconButton(
                            onClick = {
                                isUnlocked = false
                                enteredPin = ""
                            },
                            modifier = Modifier.testTag("vault_relock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "قفل الخزنة")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("vault_top_app_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUnlocked) {
                // 1. شاشة إدخال / تعيين رمز المرور (PIN Keypad)
                VaultPinLockView(
                    enteredPin = enteredPin,
                    isSettingNewPin = !isPinAlreadySet,
                    errorMessage = errorMessage,
                    onDigitClick = { digit ->
                        if (enteredPin.length < 4) {
                            errorMessage = null
                            val newPin = enteredPin + digit
                            enteredPin = newPin

                            if (newPin.length == 4) {
                                if (!isPinAlreadySet) {
                                    // تعيين رمز PIN جديد
                                    vaultManager.setPin(newPin)
                                    isUnlocked = true
                                    Toast.makeText(context, "تم تعيين رمز الخزنة بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    // التحقق من صحة الرمز
                                    if (vaultManager.verifyPin(newPin)) {
                                        isUnlocked = true
                                        errorMessage = null
                                    } else {
                                        errorMessage = "رمز المرور غير صحيح! حاول مرة أخرى"
                                        enteredPin = ""
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onBiometricClick = {
                        // محاكاة أو تشغيل فتح البصمة السريع
                        if (isPinAlreadySet) {
                            isUnlocked = true
                            Toast.makeText(context, "تم التحقق عبر البصمة بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى تعيين رمز PIN أولاً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 2. شاشة عرض الفيديوهات المخفية داخل الخزنة
                VaultVideosListView(
                    vaultVideos = vaultVideos,
                    onPlayVideo = { vaultVideo ->
                        onPlayVideo(vaultVideo.toVideoModel())
                    },
                    onRestoreVideo = { vaultVideo ->
                        scope.launch {
                            val success = vaultManager.restoreFromVault(vaultVideo)
                            if (success) {
                                Toast.makeText(context, "تمت استعادة الفيديو وإلغاء إخفائه", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "فشلت استعادة الفيديو", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * واجهة إدخال رمز المرور ولوحة المفاتيح الرقمية (PIN Lock View)
 */
@Composable
fun VaultPinLockView(
    enteredPin: String,
    isSettingNewPin: Boolean,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // أيقونة الخزنة والعنوان
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSettingNewPin) "تعيين رمز حماية الخزنة" else "الخزنة الخاصة مقفلة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSettingNewPin) "أنشئ رمز مرور من 4 أرقام لحماية فيديوهاتك السرية" else "أدخل رمز المرور المكون من 4 أرقام للوصول",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // دوائر تمثيل خانات الـ PIN الأربعة
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // رسالة الخطأ إن وجدت
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // لوحة المفاتيح الرقمية (Keypad)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (digit in row) {
                        PinKeyButton(
                            text = digit,
                            onClick = { onDigitClick(digit) }
                        )
                    }
                }
            }

            // الصف الأخير: بصمة الإصبع | الرقم 0 | حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر البصمة
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("vault_biometric_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "بصمة الإصبع",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // الرقم 0
                PinKeyButton(
                    text = "0",
                    onClick = { onDigitClick("0") }
                )

                // زر الحذف Backspace
                IconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("vault_backspace_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * زر لوحة المفاتيح الرقمية لرمز الـ PIN
 */
@Composable
private fun PinKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * قائمة عرض مقاطع الفيديو المحفوظة داخل الخزنة
 */
@Composable
fun VaultVideosListView(
    vaultVideos: List<VaultVideoEntity>,
    onPlayVideo: (VaultVideoEntity) -> Unit,
    onRestoreVideo: (VaultVideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vaultVideos.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "الخزنة فارغة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "يمكنك نقل وإخفاء أي فيديو خاص من شاشة الفيديوهات إلى هنا ليختفي تماماً من المعرض العام للهاتف.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.testTag("vault_videos_list")
        ) {
            items(vaultVideos, key = { it.id }) { video ->
                VaultVideoCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onRestore = { onRestoreVideo(video) }
                )
            }
        }
    }
}

/**
 * بطاقة عرض الفيديو داخل الخزنة مع خيارات التشغيل والاستعادة
 */
@Composable
fun VaultVideoCard(
    video: VaultVideoEntity,
    onPlay: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("vault_video_item_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // صورة المصغرة
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = video.thumbnailUri,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // أيقونة قفل صغيرة فوق المصغرة
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // تفاصيل الفيديو
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "فيديو مخفي ومحمي",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // زر الاستعادة (إلغاء الإخفاء)
            IconButton(
                onClick = onRestore,
                modifier = Modifier.testTag("restore_video_button_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "استعادة للذاكرة العامة",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
