package com.drejo.openeksin.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.TopicFeed
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.remote.CloudflareException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TopicListUiState {
    data object Loading : TopicListUiState
    data class Success(val topics: List<Topic>) : TopicListUiState
    data class Error(val message: String) : TopicListUiState
    data class NeedsCloudflare(val challengeUrl: String) : TopicListUiState
}

class TopicListViewModel(
    private val repository: EksiRepository = EksiRepository(),
) : ViewModel() {

    private val _feed = MutableStateFlow(TopicFeed.AGENDA)
    val feed: StateFlow<TopicFeed> = _feed.asStateFlow()

    private val _state = MutableStateFlow<TopicListUiState>(TopicListUiState.Loading)
    val state: StateFlow<TopicListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun selectFeed(feed: TopicFeed) {
        if (_feed.value == feed) return
        _feed.value = feed
        load()
    }

    fun load() {
        _state.value = TopicListUiState.Loading
        viewModelScope.launch {
            try {
                val topics = repository.topics(_feed.value)
                _state.value = TopicListUiState.Success(topics)
            } catch (e: CloudflareException) {
                _state.value = TopicListUiState.NeedsCloudflare(e.challengeUrl)
            } catch (e: Exception) {
                _state.value = TopicListUiState.Error(e.message ?: "error")
            }
        }
    }
}
