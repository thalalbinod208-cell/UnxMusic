package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.HomeDataCombine
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.view_count

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    // The page background image YouTube sends with Home, null when it sends none.
    private val _homeBackground: MutableStateFlow<String?> = MutableStateFlow(null)
    val homeBackground: StateFlow<String?> = _homeBackground

    init {
        homeJob = Job()
        viewModelScope.launch {
            if (dataStoreManager.cookie.first().isEmpty() &&
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            ) {
                _showLogInAlert.update { true }
            }
            regionCodeChart.value = dataStoreManager.chartKey.first()
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            // Several preference flows emit their initial value together. Coalesce those events
            // into one home refresh instead of cancelling/restarting the same network requests.
            val refreshRequests = MutableSharedFlow<String?>(extraBufferCapacity = 1)
            val refreshJob = launch {
                // SharedFlow drops events emitted before its first subscriber starts collecting.
                // Preference collectors can emit immediately during startup, which used to leave
                // Home empty until the user manually refreshed. Seed the stream after subscription
                // so every cold launch performs the initial feed request.
                refreshRequests
                    .onStart { emit(params.value) }
                    .debounce(180)
                    .collectLatest { getHomeItemList(it) }
            }
            //  refresh when region change
            val job1 =
                launch {
                    dataStoreManager.location.distinctUntilChanged().collect {
                        regionCode = it
                        refreshRequests.emit(params.value)
                    }
                }
            //  refresh when language change
            val job2 =
                launch {
                    dataStoreManager.language.distinctUntilChanged().collect {
                        language = it
                        refreshRequests.emit(params.value)
                    }
                }
            val job3 =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect {
                        refreshRequests.emit(params.value)
                        _accountInfo.emit(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first(),
                            ),
                        )
                    }
                }
            val job4 =
                launch {
                    params.collectLatest {
                        refreshRequests.emit(it)
                    }
                }
            val job6 =
                launch {
                    homeItemList.collectLatest { list ->
                        _mainHomeThumbnail.value =
                            list
                                .firstOrNull()
                                ?.contents
                                ?.firstOrNull()
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                    }
                }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job6.join()
            refreshJob.join()
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun getHomeItemList(params: String? = null, forceRefresh: Boolean = false) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                // Never leave a cold Home blank indefinitely; cached music is rendered first,
                // and an unavailable network falls through to the retry state after three seconds.
                val homeLoadTimeout = launch {
                    delay(if (forceRefresh) 5_000 else 3_000)
                    if (loading.value && (_homeItemList.value.isEmpty() || forceRefresh)) {
                        _homeListState.value = ListState.IDLE
                        loading.value = false
                        showSnackBarErrorState.emit(
                            if (_homeItemList.value.isEmpty()) {
                                "Home is taking too long to load. Check your connection and retry."
                            } else {
                                "Refresh is taking too long. Showing the saved music."
                            },
                        )
                    }
                }
                try {
                language =
                dataStoreManager.getString(SELECTED_LANGUAGE).first()
                    ?: SUPPORTED_LANGUAGE.codes.first()
                regionCode = dataStoreManager.location.first()
                combine(
                    homeRepository.getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                        skipCache = forceRefresh,
                    ).onEach { home ->
                        // Render the main shelf as soon as its response arrives. Waiting for the
                        // slower mood, chart, and release endpoints made Home look frozen even
                        // when its primary data was already ready.
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val freshItems = home.data?.second.orEmpty()
                                // YouTube sometimes omits Quick picks from an authenticated feed or
                                // a refresh response. Keep the last usable radio shelf so signing
                                // in or pulling to refresh cannot make that Home section vanish.
                                val quickPicksTitle = getString(Res.string.quick_picks)
                                val previousQuickPicks = _homeItemList.value.firstOrNull { it.title == quickPicksTitle }
                                _homeItemList.value =
                                    if (params == null && previousQuickPicks != null && freshItems.none { it.title == quickPicksTitle }) {
                                        freshItems + previousQuickPicks
                                    } else {
                                        freshItems
                                    }
                                _homeBackground.value = home.data?.third
                                _homeListState.value = ListState.IDLE
                                loading.value = false
                            }

                            is Resource.Error -> {
                                _homeListState.value = ListState.IDLE
                                loading.value = false
                            }
                        }
                    },
                    homeRepository.getMoodAndMomentsData(),
                    homeRepository.getChartData(dataStoreManager.chartKey.first()),
                    homeRepository.getNewRelease(
                        getString(Res.string.new_release),
                        getString(Res.string.music_video),
                    ),
                ) { home, exploreMood, exploreChart, newRelease ->
                    HomeDataCombine(home, exploreMood, exploreChart, newRelease)
                }.collect { result ->
                    val home = result.home
                    Logger.d("home size", "${home.data?.second?.size}")
                    val exploreMoodItem = result.mood
                    val chart = result.chart
                    val newRelease = result.newRelease
                    when (home) {
                        is Resource.Success -> {
                            _continuation.value = home.data?.first
                            val freshItems = home.data?.second.orEmpty()
                            val quickPicksTitle = getString(Res.string.quick_picks)
                            val previousQuickPicks = _homeItemList.value.firstOrNull { it.title == quickPicksTitle }
                            _homeItemList.value =
                                if (params == null && previousQuickPicks != null && freshItems.none { it.title == quickPicksTitle }) {
                                    freshItems + previousQuickPicks
                                } else {
                                    freshItems
                                }
                            _homeBackground.value = home.data?.third
                        }

                        else -> {
                            if (_homeItemList.value.isEmpty()) {
                                _continuation.value = null
                            }
                        }
                    }
                    if (continuation.value.isNullOrEmpty()) {
                        _homeListState.value = ListState.PAGINATION_EXHAUST
                    } else {
                        _homeListState.value = ListState.IDLE
                    }
                    when (chart) {
                        is Resource.Success -> {
                            _chart.value = chart.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                    when (newRelease) {
                        is Resource.Success -> {
                            _newRelease.value = newRelease.data ?: arrayListOf()
                        }

                        else -> {
                            _newRelease.value = arrayListOf()
                        }
                    }
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                        }

                        else -> {
                            _exploreMoodItem.value = null
                        }
                    }
                    regionCodeChart.value = dataStoreManager.chartKey.first()
                    Logger.d("HomeViewModel", "getHomeItemList: $result")
                    dataStoreManager.cookie.first().let {
                        if (it != "") {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                    }
                    when {
                        home is Resource.Error -> home.message
                        exploreMoodItem is Resource.Error -> exploreMoodItem.message
                        chart is Resource.Error -> chart.message
                        else -> null
                    }?.let {
                        showSnackBarErrorState.emit(it)
                        Logger.w("Error", "getHomeItemList: ${home.message}")
                        Logger.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                        Logger.w("Error", "getHomeItemList: ${chart.message}")
                    }
                    loading.value = false
                }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Logger.w("HomeViewModel", "Home load failed: ${error.message}")
                    _homeListState.value = ListState.IDLE
                    loading.value = false
                    showSnackBarErrorState.emit(error.message ?: "Unable to load music. Check your connection and retry.")
                } finally {
                    homeLoadTimeout.cancel()
                }
            }
    }

    /** A shelf's "Play all": YouTube's own finite queue of that shelf's songs. */
    fun playAll(item: HomeItem) {
        item.playAllEndpoint?.let { playFromEndpoint(it, item.title) }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val newItems = home.data?.second ?: listOf()
                                _homeItemList.update { it + newItems }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}
