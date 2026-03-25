package com.kuaigeglobal.epnova.kit.media

import androidx.media3.common.Player
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo

/**
 * Defines a TrackPlayer interface that extends the Player interface.
 * The TrackPlayer interface provides functionality for managing track playback and listening for track changes.
 */
interface TrackPlayer: Player {

    /**
     * The OnTrackChangeListener interface defines the method that should be called when a track change occurs.
     * Classes implementing this interface can be notified when a track change happens.
     */
    interface OnTrackChangeListener {
        /**
         * This method is called when the track changes.
         * @param lastState The state of the player before the change.
         * @param workInfo The information of the track after the change.
         */
        fun onTrackChanged(lastState: State?, workInfo: WorkInfo?)
    }

    /**
     * Adds a track change listener to be notified when a track change occurs.
     * @param listener The track change listener to be added.
     */
    fun addOnTrackChangeListener(listener: OnTrackChangeListener?)

    /**
     * Removes a track change listener so it no longer receives notifications when a track changes.
     * @param listener The track change listener to be removed.
     */
    fun removeOnTrackChangeListener(listener: OnTrackChangeListener?)

    /**
     * Sets the track information to be played.
     * @param workInfo The track information to set for the current playback
     */
    fun setTrack(workInfo: WorkInfo?)

    /**
     * Gets the information of the currently playing track.
     * @return The information of the currently playing track, or null if no track is playing.
     */
    fun currentTrack(): WorkInfo?
}