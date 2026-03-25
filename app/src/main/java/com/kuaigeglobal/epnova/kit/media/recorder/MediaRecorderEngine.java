package com.kuaigeglobal.epnova.kit.media.recorder;

import android.media.MediaRecorder;
import android.os.Handler;

import org.jetbrains.annotations.Nullable;

public class MediaRecorderEngine extends RecorderEngineBase {
    private MediaRecorder mediaRecorder;
    private String outputPath;

    private SoundAmplitudeListener soundAmplitudeListener;
    private Handler handler = new Handler();
    private boolean isRecording = false;
    private static final int REFRESH_LIMIT = 100;
    private long startTime;
    private long duration;

    @Override
    public void startRecord(@Nullable String path) {
        outputPath = path;
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);//提高音色还原度,降低噪音
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mediaRecorder.setAudioSamplingRate(44100);//提高音色还原度
            mediaRecorder.setAudioEncodingBitRate(128000);//提高音色还原度
            mediaRecorder.setOutputFile(outputPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            startTime = System.currentTimeMillis();
            duration = 0;
            isRecording = true;

            if (recordingTracker != null) {
                recordingTracker.onRecorderStart();
            }
            updateAmplitude();

        } catch (Throwable e) {
            if (recordingTracker != null) {
                recordingTracker.onRecorderStreamError(e);
            }
        }
    }

    @Override
    public void pause() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.pause();
                duration += (System.currentTimeMillis() - startTime);
                startTime = 0;
                if (recordingTracker != null) {
                    recordingTracker.onRecorderPause();
                }
                isRecording = false;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void resume() {
        if (mediaRecorder != null && !isRecording) {
            try {
                mediaRecorder.resume();
                startTime = System.currentTimeMillis();
                if (recordingTracker != null) {
                    recordingTracker.onRecorderResume();
                }
                isRecording = true;
                updateAmplitude();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                duration += (System.currentTimeMillis() - startTime);
                startTime = 0;
                mediaRecorder = null;
                isRecording = false;

                if (recordingTracker != null) {
                    recordingTracker.onRecorderStop();
                }

            } catch (Throwable e) {
                if (recordingTracker != null) {
                    recordingTracker.onRecorderStreamError(e);
                }
            } finally {
                isRecording = false;
                mediaRecorder = null;
            }
        }
    }


    @Override
    public void setSoundAmplitudeListener(SoundAmplitudeListener listener) {
        this.soundAmplitudeListener = listener;
    }

    // 定时任务用于更新振幅
    private void updateAmplitude() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaRecorder != null && isRecording) {
                    int amplitude = mediaRecorder.getMaxAmplitude();
                    int ratio = (int) (amplitude / 32767.0 * 100);
                    int db = (int) (20 * Math.log10(amplitude / 32767.0));
                    if (soundAmplitudeListener != null) {
                        soundAmplitudeListener.onAmplitude(ratio, db, amplitude);
                    }
                    handler.postDelayed(this, REFRESH_LIMIT);
                }
            }
        }, REFRESH_LIMIT);
    }

    public long getDuration() {
        return duration + (startTime > 0 ? (System.currentTimeMillis() - startTime) : 0);
    }
}
