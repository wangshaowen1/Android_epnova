package com.kuaigeglobal.epnova.kit.media.recorder;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecordEngine implements RecorderEngine {
    public static final int SAMPLE_RATE = 48000; // 44.1kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private FileOutputStream outputStream;
    private Thread recordingThread;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private int maxDuration = 60; // 最大录音时长（秒）
    private long startTime = 0; // 录音开始时间
    private long pauseTime = 0;//暂停的时间
    private long pausedMillis = 0;//暂停的时长
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecorderEngineListener recorderListener;
    private SoundAmplitudeListener soundAmplitudeListener;

    @SuppressLint("MissingPermission")
    @Override
    public void startRecord(@Nullable String path) {
        if (isRecording.get()) {
            return;
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );

            audioRecord.startRecording();
            isRecording.set(true);
            isPaused.set(false);
            startTime = System.currentTimeMillis();
            pauseTime = 0;
            pausedMillis = 0;
            if (recorderListener != null) {
                recorderListener.onRecorderStart();
            }

            startRecordingThread();
        } catch (IOException e) {
            if (recorderListener != null) {
                recorderListener.onRecorderStreamError(e);
            }
        }
    }

    @Override
    public void pause() {
        pauseTime = System.currentTimeMillis();
        if (isRecording.get() && !isPaused.get()) {
            isPaused.set(true);
            if (recorderListener != null) {
                recorderListener.onRecorderPause();
            }
        }
    }

    @Override
    public void resume() {
        pausedMillis += (System.currentTimeMillis() - pauseTime);
        if (isRecording.get() && isPaused.get()) {
            isPaused.set(false);
            if (recorderListener != null) {
                recorderListener.onRecorderResume();
            }
        }
    }

    @Override
    public void stop() {
        if (isRecording.get()) {
            isRecording.set(false);
            isPaused.set(false);

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    if (recorderListener != null) {
                        recorderListener.onRecorderStreamError(e);
                    }
                }
            }
            if (recorderListener != null) {
                recorderListener.onRecorderStop();
            }
        }
    }

    @Override
    public void setDuring(int seconds) {
        this.maxDuration = seconds;
    }

    @Override
    public int getDuring() {
        return maxDuration;
    }

    @Override
    public void setRecorderListener(RecorderEngineListener recorderEngineListener) {
        this.recorderListener = recorderEngineListener;
    }

    @Override
    public void setSoundAmplitudeListener(SoundAmplitudeListener soundAmplitudeListener) {
        this.soundAmplitudeListener = soundAmplitudeListener;
    }

    private void startRecordingThread() {
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRecording.get()) {
                if (!isPaused.get()) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        try {
                            outputStream.write(buffer, 0, buffer.length);
                        } catch (IOException e) {
                            if (recorderListener != null) {
                                recorderListener.onRecorderStreamError(e);
                            }
                            break;
                        }

                        // 计算音量振幅
                        if (soundAmplitudeListener != null) {
                            int amplitude = calculateAmplitude(buffer, bytesRead);
                            int db = (int) (20 * Math.log10(amplitude));
                            int ratio = (int) ((amplitude / 32767.0) * 100);
                            soundAmplitudeListener.onAmplitude(ratio, db, amplitude);
                        }

                        // 更新录音进度
                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTime - pausedMillis;
                        if (recorderListener != null) {
                            handler.post(() -> recorderListener.onRecorderProgress(elapsedTime));
                        }

                        // 检查最大录音时长
                        if (maxDuration > 0 && elapsedTime >= maxDuration * 1000L) {
                            stop();
                            break;
                        }
                    }
                }
            }
        });
        recordingThread.start();
    }

    private int calculateAmplitude(byte[] buffer, int length) {
        int maxAmplitude = 0;
        for (int i = 0; i < length; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | buffer[i]);
            if (Math.abs(sample) > maxAmplitude) {
                maxAmplitude = Math.abs(sample);
            }
        }
        return maxAmplitude;
    }
}