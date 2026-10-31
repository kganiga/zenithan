package com.zennature.wallpaper.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zennature.wallpaper.data.FilterOptions
import com.zennature.wallpaper.data.LicenseFilter
import com.zennature.wallpaper.data.OrientationFilter
import com.zennature.wallpaper.data.ProviderFilter
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.data.WallpaperRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GridDensity { STANDARD, DENSE }

data class FeedState(
    val filters: FilterOptions = FilterOptions(),
    val searchInput: String = "",
    val items: List<WallpaperItem> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = true,
    val loadingInitial: Boolean = true,
    val loadingMore: Boolean = false,
    val showFilters: Boolean = false,
    val density: GridDensity = GridDensity.STANDARD,
    val shuffleToken: Int = 0,
) {
    val filtersActive: Boolean
        get() = filters.orientation != OrientationFilter.ALL || filters.licenseOnly != LicenseFilter.ALL
}

/** Infinite wallpaper stream (WallpaperFeedScreen.tsx). */
@OptIn(FlowPreview::class)
class FeedViewModel(private val repository: WallpaperRepository) : ViewModel() {

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private var loadJob: Job? = null
    private var moreJob: Job? = null

    init {
        // Fresh shuffle per app session (the web build reshuffled on every visit to this tab).
        repository.reshuffleSession()
        loadInitial()
        searchInput
            .drop(1)
            .debounce(350)
            .distinctUntilChanged()
            .onEach { query -> setFilters { it.copy(query = query) } }
            .launchIn(viewModelScope)
    }

    fun onSearchInput(text: String) {
        _state.update { it.copy(searchInput = text) }
        searchInput.value = text
    }

    fun clearSearch() {
        _state.update { it.copy(searchInput = "") }
        searchInput.value = ""
        setFilters { it.copy(query = "") }
    }

    fun setCategory(categoryId: String) = setFilters { it.copy(category = categoryId) }

    fun toggleCategory(categoryId: String) = setFilters {
        it.copy(category = if (it.category == categoryId) "" else categoryId)
    }

    fun setProvider(provider: ProviderFilter) = setFilters { it.copy(provider = provider) }

    fun setOrientation(o: OrientationFilter) = setFilters { it.copy(orientation = o) }

    fun setLicense(l: LicenseFilter) = setFilters { it.copy(licenseOnly = l) }

    fun toggleFiltersPanel() = _state.update { it.copy(showFilters = !it.showFilters) }

    fun toggleDensity() = _state.update {
        it.copy(density = if (it.density == GridDensity.STANDARD) GridDensity.DENSE else GridDensity.STANDARD)
    }

    fun shuffle() {
        repository.reshuffleSession()
        _state.update { it.copy(shuffleToken = it.shuffleToken + 1) }
        loadInitial()
    }

    fun resetAndShuffle() {
        _state.update { it.copy(searchInput = "", filters = FilterOptions()) }
        searchInput.value = ""
        shuffle()
    }

    fun refresh() = loadInitial()

    private fun setFilters(transform: (FilterOptions) -> FilterOptions) {
        val before = _state.value.filters
        val after = transform(before)
        if (after == before) return
        _state.update { it.copy(filters = after) }
        loadInitial()
    }

    private fun loadInitial() {
        loadJob?.cancel()
        moreJob?.cancel()
        _state.update { it.copy(loadingInitial = true, loadingMore = false, page = 1) }
        val filters = _state.value.filters
        loadJob = viewModelScope.launch {
            val result = runCatching { repository.fetchWallpapers(filters, 1) }.getOrNull()
            _state.update {
                it.copy(
                    items = result?.items ?: it.items,
                    hasMore = (result?.items?.size ?: 0) >= 10,
                    loadingInitial = false,
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || !s.hasMore || s.loadingInitial) return
        _state.update { it.copy(loadingMore = true) }
        val nextPage = s.page + 1
        val filters = s.filters
        moreJob = viewModelScope.launch {
            val result = runCatching { repository.fetchWallpapers(filters, nextPage) }.getOrNull()
            _state.update { current ->
                when {
                    result == null -> current.copy(hasMore = false, loadingMore = false)
                    result.items.isEmpty() -> current.copy(hasMore = false, loadingMore = false)
                    else -> {
                        val existing = current.items.mapTo(HashSet()) { it.id + it.thumbnailUrl }
                        val fresh = result.items.filterNot { existing.contains(it.id + it.thumbnailUrl) }
                        val appended = fresh.ifEmpty {
                            // Keep the stream rolling: re-key repeated items so list keys stay unique.
                            result.items.mapIndexed { idx, item -> item.copy(id = "${item.id}-p$nextPage-$idx") }
                        }
                        current.copy(items = current.items + appended, page = nextPage, loadingMore = false)
                    }
                }
            }
        }
    }
}
