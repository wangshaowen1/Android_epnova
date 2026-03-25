package com.kuaigeglobal.epnova.kit.media.recorder;

import org.jetbrains.annotations.Nullable;

public interface RecorderEngine {
    /**
     * 开始录音
     *
     * @param path
     */
    public void startRecord(@Nullable String path);

    /**
     * 暂停录音
     */
    public void pause();

    public void resume();

    /**
     * 停止录音
     */
    public void stop();

    /**
     * 录音最大时长
     *
     * @param seconds
     */
    public void setDuring(int seconds);

    public int getDuring();

    public void setRecorderListener(RecorderEngineListener recorderEngineListener);

    public void setSoundAmplitudeListener(SoundAmplitudeListener soundAmplitudeListener);

    public interface RecorderEngineListener {
        public void onRecorderStart();

        public void onRecorderStop();

        public void onRecorderPause();

        public void onRecorderResume();

        public void onRecorderProgress(long millis);

        public void onRecorderStreamError(Throwable e);
    }

    public interface SoundAmplitudeListener {
        public void onAmplitude(int ratio, int db, int value);
    }

}