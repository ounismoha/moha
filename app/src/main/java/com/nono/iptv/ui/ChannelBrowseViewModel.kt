package com.nono.iptv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nono.iptv.data.ChannelEntity
import com.nono.iptv.data.PlaylistRepository
import kotlinx.coroutines.launch

/**
 * Owns the channel list for the browse screen. On first run it imports the default
 * playlist; afterwards it serves channels straight from Room.
 */
class ChannelBrowseViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlaylistRepository(app)

    private val _channelsByGroup = MutableLiveData<Map<String, List<ChannelEntity>>>()
    val channelsByGroup: LiveData<Map<String, List<ChannelEntity>>> = _channelsByGroup

    private val _status = MutableLiveData<Status>(Status.Idle)
    val status: LiveData<Status> = _status

    fun load() {
        viewModelScope.launch {
            _status.value = Status.Loading
            try {
                if (repository.channelCount() == 0) {
                    repository.importFromUrl(PlaylistRepository.DEFAULT_PLAYLIST_URL)
                }
                _channelsByGroup.value = repository.channelsByGroup()
                _status.value = Status.Ready
            } catch (t: Throwable) {
                _status.value = Status.Error(t.message ?: "Failed to load playlist")
            }
        }
    }

    /** Import a user-supplied playlist URL and refresh the grouped list. */
    fun importPlaylist(url: String, userAgent: String = PlaylistRepository.DEFAULT_USER_AGENT) {
        viewModelScope.launch {
            _status.value = Status.Loading
            try {
                repository.importFromUrl(url, userAgent)
                _channelsByGroup.value = repository.channelsByGroup()
                _status.value = Status.Ready
            } catch (t: Throwable) {
                _status.value = Status.Error(t.message ?: "Import failed")
            }
        }
    }

    sealed interface Status {
        data object Idle : Status
        data object Loading : Status
        data object Ready : Status
        data class Error(val message: String) : Status
    }
}
