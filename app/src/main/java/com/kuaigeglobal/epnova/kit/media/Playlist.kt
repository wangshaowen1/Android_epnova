package com.kuaigeglobal.epnova.kit.media

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.kit.media.PlaylistPlayer.PlayMode

/** Playlist class that holds a mutable list of TrackInfo objects*/
class Playlist(val playList: MutableList<WorkInfo>) {
    // Private variable to keep track of the current track index
    private var currentTrackIndex = 0

    // Returns the current track or null if the index is out of bounds
    fun current(): WorkInfo? {
        return playList.getOrNull(currentTrackIndex)
    }

    // Skips to a specific track by index and returns the selected track or null
    fun skipTo(index: Int): WorkInfo? {
        currentTrackIndex = index
        return playList.getOrNull(currentTrackIndex)
    }

    // Moves to the next track in the playlist and returns the next track or null
    fun next(): WorkInfo? {
        if (playList.isEmpty()) return null
        currentTrackIndex = (currentTrackIndex + 1) % playList.size
        return playList.getOrNull(currentTrackIndex)
    }

    // Moves to the previous track in the playlist and returns the previous track or null
    fun prev(): WorkInfo? {
        val size = playList.size
        if (size == 0) {
            return null
        }
        currentTrackIndex = (currentTrackIndex - 1 + size) % size
        return playList.getOrNull(currentTrackIndex)
    }

    // Checks if there is a next track based on the play mode
    fun hasNext(playMode: PlayMode?): Boolean {
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Always has a next track in playlist loop mode
                return true
            }

            PlayMode.NORMAL -> {
                // Has a next track if the current index is less than the playlist size minus one
                return (currentTrackIndex + 1 < playList.size)
            }

            PlayMode.SHUFFLE -> {
                // Always has a next track in shuffle mode
                return true
            }

            PlayMode.SINGLE_TRACK_LOOP -> {
                // Always has a next track in single track loop mode
                return true
            }

            null -> {
                // No play mode specified, so no next track
                return false
            }
        }
    }

    // Automatically selects the next track based on the play mode
    fun autoNext(playMode: PlayMode?): WorkInfo? {
        var next: WorkInfo? = null
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Moves to the next track in the playlist
                next = next()
            }

            PlayMode.NORMAL -> {
                // Moves to the next track if it exists
                if (++currentTrackIndex < playList.size) {
                    next = playList.getOrNull(currentTrackIndex)
                }
            }

            PlayMode.SHUFFLE -> {
                // Selects a random track and updates the current index
                next = playList.random()
                currentTrackIndex = playList.indexOf(next)
            }

            PlayMode.SINGLE_TRACK_LOOP -> {
                // Repeats the current track
                next = playList.getOrNull(currentTrackIndex)
            }

            null -> {
                // No play mode specified, so no next track
                next = null
            }
        }
        return next
    }

    // Automatically selects the previous track based on the play mode
    fun autoPrev(playMode: PlayMode): WorkInfo? {
        var next: WorkInfo? = null
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Moves to the previous track in the playlist
                next = prev()
            }

            PlayMode.NORMAL -> {
                // Moves to the previous track if it exists
                if (--currentTrackIndex >= 0) {
                    next = playList.getOrNull(currentTrackIndex)
                }
            }

            PlayMode.SHUFFLE -> {
                // Selects a random track and updates the current index
                next = playList.random()
                currentTrackIndex = playList.indexOf(next)
            }

            PlayMode.SINGLE_TRACK_LOOP -> {
                // Repeats the current track
                next = playList.getOrNull(currentTrackIndex)
            }
        }
        return next
    }

   fun getCurrentTrackIndex():Int{
        return currentTrackIndex
    }
}
