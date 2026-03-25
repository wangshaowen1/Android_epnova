package com.kuaigeglobal.epnova.kit.media

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.kit.media.source.TracksPageSource

/** State class to hold the information about the current track and playback position.*/
class State() {

    var trackList:List<WorkInfo>?=null
    // Variable to store the track information, nullable to accommodate the absence of a track.
    var workInfo: WorkInfo? = null

    // Variable to store the current playback position, nullable to represent the absence of a position.
    var playbackPosition: Long? = 0
    var mediaSource: TracksPageSource? = null
    /**
     * Secondary constructor to initialize the State with specific track information and position.
     */
    constructor(workInfo: WorkInfo?, position: Long?) : this() {
        this.workInfo = workInfo
        this.playbackPosition = position
    }

    /**
     * Function to update the state by peeking at the current state of the player.
     */
    fun peek(player: TrackPlayer) {
        workInfo = player.currentTrack()
        playbackPosition = player.currentPosition
    }

    /**
     * Function to clear the state, setting track information and position to null.
     */
    fun clear() {
        workInfo = null
        playbackPosition = null
        trackList=null
        mediaSource=null
    }

    fun isAvailable(): Boolean {
        return workInfo!=null
    }
}