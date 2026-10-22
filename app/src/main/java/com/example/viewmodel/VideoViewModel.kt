package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.VideoModel
import com.example.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * خيارات الفرز المتاحة لقائمة الفيديوهات
 */
enum class SortBy {
    DATE,    // حسب تاريخ الإضافة
    NAME,    // أبجدياً حسب الاسم
    SIZE,    // حسب حجم الملف
    DURATION // حسب مدة الفيديو
}

/**
 * اتجاه الفرز (تصاعدي أو تنازلي)
 */
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

/**
 * معايير التصفية والبحث
 */
data class FilterParams(
    val query: String = "",
    val selectedFolder: String? = null,
    val sortBy: SortBy = SortBy.DATE,
    val sortOrder: SortOrder = SortOrder.DESCENDING
)

/**
 * حالة واجهة المستخدم لشاشة الفيديوهات والمجلدات
 */
data class VideoUiState(
    val isLoading: Boolean = true,
    val videos: List<VideoModel> = emptyList(),
    val folders: Map<String, List<VideoModel>> = emptyMap(),
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.DATE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val errorMessage: String? = null
)

/**
 * الـ ViewModel المسؤول عن إدارة حالة الفيديوهات والمجلدات والبحث والفرز
 */
class VideoViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _rawVideos = MutableStateFlow<List<VideoModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow(SortBy.DATE)
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // دمج معايير البحث والفرز في تدفق واحد لتفادي قيود overload في Flow combine
    private val _filterParams: StateFlow<FilterParams> = combine(
        _searchQuery,
        _selectedFolder,
        _sortBy,
        _sortOrder
    ) { query, folder, sortBy, sortOrder ->
        FilterParams(query, folder, sortBy, sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FilterParams()
    )

    /**
     * تدفق حالة واجهة المستخدم النهائي (UiState)
     */
    val uiState: StateFlow<VideoUiState> = combine(
        _rawVideos,
        _filterParams,
        _isLoading,
        _errorMessage
    ) { rawVideos, filters, loading, error ->

        val foldersMap = rawVideos.groupBy { it.folderName }

        val folderFilteredVideos = if (filters.selectedFolder != null) {
            rawVideos.filter { it.folderName.equals(filters.selectedFolder, ignoreCase = true) }
        } else {
            rawVideos
        }

        val searchFilteredVideos = if (filters.query.isNotBlank()) {
            folderFilteredVideos.filter {
                it.title.contains(filters.query, ignoreCase = true) ||
                it.folderName.contains(filters.query, ignoreCase = true)
            }
        } else {
            folderFilteredVideos
        }

        val sortedVideos = when (filters.sortBy) {
            SortBy.NAME -> {
                if (filters.sortOrder == SortOrder.ASCENDING) {
                    searchFilteredVideos.sortedBy { it.title.lowercase() }
                } else {
                    searchFilteredVideos.sortedByDescending { it.title.lowercase() }
                }
            }
            SortBy.DATE -> {
                if (filters.sortOrder == SortOrder.ASCENDING) {
                    searchFilteredVideos.sortedBy { it.dateAdded }
                } else {
                    searchFilteredVideos.sortedByDescending { it.dateAdded }
                }
            }
            SortBy.SIZE -> {
                if (filters.sortOrder == SortOrder.ASCENDING) {
                    searchFilteredVideos.sortedBy { it.size }
                } else {
                    searchFilteredVideos.sortedByDescending { it.size }
                }
            }
            SortBy.DURATION -> {
                if (filters.sortOrder == SortOrder.ASCENDING) {
                    searchFilteredVideos.sortedBy { it.duration }
                } else {
                    searchFilteredVideos.sortedByDescending { it.duration }
                }
            }
        }

        VideoUiState(
            isLoading = loading,
            videos = sortedVideos,
            folders = foldersMap,
            selectedFolder = filters.selectedFolder,
            searchQuery = filters.query,
            sortBy = filters.sortBy,
            sortOrder = filters.sortOrder,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VideoUiState()
    )

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                videoRepository.getAllVideos().collect { fetchedVideos ->
                    _rawVideos.value = fetchedVideos
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "حدث خطأ أثناء تحميل الفيديوهات"
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun setSortBy(sortBy: SortBy) {
        if (_sortBy.value == sortBy) {
            toggleSortOrder()
        } else {
            _sortBy.value = sortBy
            _sortOrder.value = if (sortBy == SortBy.NAME) SortOrder.ASCENDING else SortOrder.DESCENDING
        }
    }

    fun toggleSortOrder() {
        _sortOrder.update { current ->
            if (current == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        }
    }

    fun refresh() {
        loadVideos()
    }
}
