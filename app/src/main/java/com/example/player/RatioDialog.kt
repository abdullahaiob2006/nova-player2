@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

/**
 * فئات نسب العرض والشاشة
 */
enum class RatioCategory(val title: String) {
    SCREEN("Screen"),
    STANDARD("Standard"),
    CINEMA("Cinema")
}

/**
 * عنصر نسبة العرض أو وضع الشاشة المطابق تماماً لـ xPlayer
 */
data class AspectRatioItem(
    val id: String,
    val label: String,
    val category: RatioCategory,
    val floatRatio: Float? = null,
    val exoResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    companion object {
        // قسم Screen
        val FIT = AspectRatioItem(
            id = "FIT",
            label = "Fit",
            category = RatioCategory.SCREEN,
            floatRatio = null,
            exoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        )
        val FILL = AspectRatioItem(
            id = "FILL",
            label = "Fill",
            category = RatioCategory.SCREEN,
            floatRatio = null,
            exoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
        val ORIGINAL = AspectRatioItem(
            id = "ORIGINAL",
            label = "Original",
            category = RatioCategory.SCREEN,
            floatRatio = null,
            exoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        )
        val STRETCH = AspectRatioItem(
            id = "STRETCH",
            label = "Stretch",
            category = RatioCategory.SCREEN,
            floatRatio = null,
            exoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        )

        // قسم Standard
        val RATIO_16_9 = AspectRatioItem("16_9", "16:9", RatioCategory.STANDARD, 16f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val RATIO_4_3 = AspectRatioItem("4_3", "4:3", RatioCategory.STANDARD, 4f / 3f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val RATIO_18_9 = AspectRatioItem("18_9", "18:9", RatioCategory.STANDARD, 18f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val RATIO_19_5_9 = AspectRatioItem("19_5_9", "19.5:9", RatioCategory.STANDARD, 19.5f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val RATIO_20_9 = AspectRatioItem("20_9", "20:9", RatioCategory.STANDARD, 20f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val RATIO_21_9 = AspectRatioItem("21_9", "21:9", RatioCategory.STANDARD, 21f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL)

        // قسم Cinema
        val CINEMA_1_85 = AspectRatioItem("1_85_1", "1.85:1", RatioCategory.CINEMA, 1.85f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val CINEMA_2_21 = AspectRatioItem("2_21_1", "2.21:1", RatioCategory.CINEMA, 2.21f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val CINEMA_2_35 = AspectRatioItem("2_35_1", "2.35:1", RatioCategory.CINEMA, 2.35f, AspectRatioFrameLayout.RESIZE_MODE_FILL)
        val CINEMA_2_39 = AspectRatioItem("2_39_1", "2.39:1", RatioCategory.CINEMA, 2.39f, AspectRatioFrameLayout.RESIZE_MODE_FILL)

        val ALL_SCREEN_OPTIONS = listOf(FIT, FILL, ORIGINAL, STRETCH)
        val ALL_STANDARD_OPTIONS = listOf(RATIO_16_9, RATIO_4_3, RATIO_18_9, RATIO_19_5_9, RATIO_20_9, RATIO_21_9)
        val ALL_CINEMA_OPTIONS = listOf(CINEMA_1_85, CINEMA_2_21, CINEMA_2_35, CINEMA_2_39)

        val ALL_ITEMS = ALL_SCREEN_OPTIONS + ALL_STANDARD_OPTIONS + ALL_CINEMA_OPTIONS

        fun fromId(id: String): AspectRatioItem {
            return ALL_ITEMS.firstOrNull { it.id == id } ?: FIT
        }

        // تسلسل التبديل السريع عند تمكين "Tap ratios to switch directly"
        val QUICK_SWITCH_CYCLE = listOf(
            FIT,
            FILL,
            ORIGINAL,
            STRETCH,
            RATIO_16_9,
            CINEMA_2_35
        )
    }
}

/**
 * حفظ واسترجاع تفضيلات نسب العرض
 */
object NovaRatioPreferences {
    private const val PREFS_NAME = "nova_player_ratio_prefs"
    private const val KEY_REMEMBER_RATIO = "remember_ratio"
    private const val KEY_SAVED_RATIO_ID = "saved_ratio_id"
    private const val KEY_TAP_TO_SWITCH = "tap_to_switch_directly"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isRememberRatioEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_RATIO, false)
    }

    fun setRememberRatioEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMEMBER_RATIO, enabled).apply()
    }

    fun isTapToSwitchDirectlyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TAP_TO_SWITCH, false)
    }

    fun setTapToSwitchDirectlyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TAP_TO_SWITCH, enabled).apply()
    }

    fun getSavedRatioId(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_RATIO_ID, AspectRatioItem.FIT.id) ?: AspectRatioItem.FIT.id
    }

    fun saveRatioId(context: Context, ratioId: String) {
        getPrefs(context).edit().putString(KEY_SAVED_RATIO_ID, ratioId).apply()
    }
}

/**
 * نافذة نسب العرض الاحترافية (Ratio Bottom Sheet Dialog) مطابقة تماماً لواجهة xPlayer
 */
@Composable
fun RatioBottomSheet(
    selectedRatio: AspectRatioItem,
    onRatioSelected: (AspectRatioItem) -> Unit,
    rememberRatio: Boolean,
    onRememberRatioChange: (Boolean) -> Unit,
    tapToSwitchDirectly: Boolean,
    onTapToSwitchDirectlyChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = Modifier.testTag("ratio_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // شريط العنوان مع سهم الرجوع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("ratio_dialog_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Ratio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // قسم Screen
            Text(
                text = "Screen",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AspectRatioItem.ALL_SCREEN_OPTIONS.forEach { option ->
                    ScreenOptionCircleButton(
                        item = option,
                        isSelected = option.id == selectedRatio.id,
                        onClick = { onRatioSelected(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // قسم Standard
            Text(
                text = "Standard",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(AspectRatioItem.ALL_STANDARD_OPTIONS) { option ->
                    RatioCircleButton(
                        label = option.label,
                        isSelected = option.id == selectedRatio.id,
                        onClick = { onRatioSelected(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // قسم Cinema
            Text(
                text = "Cinema",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(AspectRatioItem.ALL_CINEMA_OPTIONS) { option ->
                    RatioCircleButton(
                        label = option.label,
                        isSelected = option.id == selectedRatio.id,
                        onClick = { onRatioSelected(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = Color(0xFF2C2C2C),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // خيار: Remember ratio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRememberRatioChange(!rememberRatio) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remember ratio",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Remember ratio for all videos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }

                Switch(
                    checked = rememberRatio,
                    onCheckedChange = onRememberRatioChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E676),
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.testTag("switch_remember_ratio")
                )
            }

            // خيار: Tap ratios to switch directly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTapToSwitchDirectlyChange(!tapToSwitchDirectly) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tap ratios to switch directly",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to switch, long press for the full menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }

                Switch(
                    checked = tapToSwitchDirectly,
                    onCheckedChange = onTapToSwitchDirectlyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E676),
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.testTag("switch_tap_to_switch")
                )
            }
        }
    }
}

/**
 * زر دائري لخيارات قسم Screen مع أيقونة واسم الخيار أدناه
 */
@Composable
private fun ScreenOptionCircleButton(
    item: AspectRatioItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val greenColor = Color(0xFF00E676)
    val darkButtonBg = Color(0xFF262626)

    val icon: ImageVector = when (item.id) {
        "FIT" -> Icons.Default.FitScreen
        "FILL" -> Icons.Default.Fullscreen
        "ORIGINAL" -> Icons.Default.AspectRatio
        "STRETCH" -> Icons.Default.CropFree
        else -> Icons.Default.AspectRatio
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("ratio_screen_button_${item.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) greenColor else darkButtonBg,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.label,
                    tint = if (isSelected) Color.White else Color(0xFFCCCCCC),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) greenColor else Color(0xFFCCCCCC),
            fontSize = 12.sp
        )
    }
}

/**
 * زر دائري للنسب الرقمية (Standard & Cinema)
 */
@Composable
private fun RatioCircleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val greenColor = Color(0xFF00E676)
    val darkButtonBg = Color(0xFF242424)

    Surface(
        shape = CircleShape,
        color = if (isSelected) greenColor else darkButtonBg,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("ratio_circle_button_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFFE2E2E2),
                fontSize = 12.sp
            )
        }
    }
}
