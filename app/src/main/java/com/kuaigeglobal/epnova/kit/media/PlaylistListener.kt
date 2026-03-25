/*
 * Copyright (C) 2009 Teleca Poland Sp. z o.o. <android@teleca.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kuaigeglobal.epnova.kit.media

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo


/**
 * Listener interface for PlayerEngine events.
 *
 */
interface PlaylistListener {
    /**
     * Enumeration representing the playback status of the PlayerEngine.
     */
    enum class Status {
        BUFFERING,  // The engine is buffering data.
        STARTED,  // Playback has started.
        PAUSED,  // Playback is paused.
        STOPPED // Playback has stopped.
    }

    /**
     * Callback method called when the playlist has changed.
     *
     * @param tracks The new list of tracks in the playlist.
     */
    fun onPlaylistChanged(tracks: List<WorkInfo?>?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called before a new track starts playing.
     * Returning false prevents the track from playing.
     *
     * @param workInfo The information about the track that is about to start.
     * @return true to allow the track to play, false to prevent it.
     */
    fun onTrackStart(workInfo: WorkInfo?): Boolean {
        return true
    }

    /**
     * Callback method called when a new track begins playing.
     *
     * @param workInfo The information about the track that has started playing.
     */
    fun onTrackChanged(workInfo: WorkInfo?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called periodically to update the track's playback progress.
     *
     * @param milliseconds The current playback position in milliseconds.
     */
    fun onTrackProgress(milliseconds: Long) {
        // Default implementation is empty.
    }

    /**
     * Callback method called when the playing status of the PlayerEngine changes.
     *
     * @param status The new status of the PlayerEngine.
     */
    fun onPlayingStatusChange(status: Status?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called when an error occurs with the track stream.
     */
    fun onTrackStreamError() {
        // Default implementation is empty.
    }
}
