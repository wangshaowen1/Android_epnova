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

package com.kuaigeglobal.epnova.kit.media;

import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo;

import java.util.List;

/**
 * Interface to the player list, providing methods to control playback and manage playlists.
 */
public interface PlaylistPlayer {

    /**
     * Enum representing the different play modes available for the playlist player.
     */
    enum PlayMode {
        /**
         * Normal playback mode, plays tracks in the order they are in the playlist.
         */
        NORMAL,
        /**
         * Shuffle playback mode, plays tracks in a random order.
         */
        SHUFFLE,
        /**
         * Play list loop mode, loops the entire playlist after it finishes.
         */
        PLAY_LIST_LOOP,
        /**
         * Single track loop mode, loops the currently playing track.
         */
        SINGLE_TRACK_LOOP
    }

    /**
     * Sets the playback mode for the playlist player.
     *
     * @param playMode The play mode to set.
     */
    void setPlayMode(PlayMode playMode);

    /**
     * Retrieves the current playback mode of the playlist player.
     *
     * @return The current play mode.
     */
    PlayMode getPlayMode();

    /**
     * Sets the playlist to be played.
     *
     * @param playlist The list of tracks to set as the playlist.
     */
    void setPlaylist(List<WorkInfo> playlist);

    void addToPlaylist(WorkInfo workInfo);

    void addToPlaylist(List<WorkInfo> playlist);

    /**
     * Retrieves the current playlist.
     *
     * @return The current playlist.
     */
    List<WorkInfo> getPlaylist();

    /**
     * Starts playing the current track in the playlist.
     */
    void play();

    /**
     * Retrieves the track that is currently playing.
     *
     * @return The currently playing track, or null if no track is playing.
     */
    @Nullable
    WorkInfo currentTrack();
    /**
     * Gets the index of the track that is currently playing in the playlist.
     *
     * @return The index of the currently playing track, or -1 if no track is playing or the index cannot be determined.
     */
    int getCurrentPlayingIndex();

    /**
     * Retrieves the current playback position in milliseconds.
     *
     * @return The current playback position in milliseconds.
     */
    long playbackPosition();

    /**
     * Checks whether the player is currently playing a track.
     *
     * @return true if the player is playing, false otherwise.
     */
    boolean isPlaying();

    /**
     * Stops the playback.
     */
    void stop();

    /**
     * Releases the player resources.
     */
    void release();

    /**
     * Pauses the playback of the current track.
     */
    public void pause();

    /**
     * Moves to the next track in the playlist.
     *
     * @return true if the player successfully moved to the next track, false otherwise.
     */
    boolean next();

    /**
     * Moves to the previous track in the playlist.
     *
     * @return true if the player successfully moved to the previous track, false otherwise.
     */
    boolean prev();

    /**
     * Jumps to the last playlist.
     *
     * @return true if the player successfully jumped to the last playlist, false otherwise.
     */
    boolean prevList();

    /**
     * Skips to a specific track in the playlist.
     *
     * @param index The index of the track in the playlist to skip to.
     */
    void skipTo(int index);

    /**
     * Adds an event listener to the player engine.
     *
     * @param playerListListener The listener to add for player events.
     */
    void addListener(PlaylistListener playerListListener);

    /**
     * Removes an event listener from the player engine.
     *
     * @param playerListListener The listener to remove for player events.
     */
    void removeListener(PlaylistListener playerListListener);

    /**
     * Fast forwards the current track by the specified time.
     *
     * @param time The time in milliseconds to fast forward.
     */
    void forward(int time);

    /**
     * Rewinds the current track by the specified time.
     *
     * @param time The time in milliseconds to rewind.
     */
    void rewind(int time);

    /**
     * Seeks to a specific position in the current track.
     *
     * @param positionMs The position in milliseconds to seek to.
     */
    void seekTo(Long positionMs);

    /**
     * Retrieves the duration of the current track in milliseconds.
     *
     * @return The duration of the current track in milliseconds.
     */
    long getDuration();

    /**
     * Checks if the specified track is in the playlist.
     *
     * @param workInfo The track information to check.
     * @return true if the track is in the playlist, false otherwise.
     */
    boolean inPlayList(WorkInfo workInfo);
    /**
     * Checks if the entire playlist has finished playback.
     * Returns true if:
     * - The playlist is not empty,
     * - The player is not in a loop mode (PLAY_LIST_LOOP/SINGLE_TRACK_LOOP),
     * - The current track is the last in the playlist,
     * - The playback has reached the end of the last track.
     *
     * @return true if the playlist has ended, false otherwise.
     */
    boolean isPlaylistEnded();
    /**
     * Restarts playback from the beginning of the playlist.
     * Stops the current track, resets the playlist index to 0, and starts playing from the first track.
     * If the playlist is empty, this method does nothing.
     */
    void restart();
}
