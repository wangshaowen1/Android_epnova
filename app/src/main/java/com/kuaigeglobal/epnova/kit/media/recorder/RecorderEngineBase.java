package com.kuaigeglobal.epnova.kit.media.recorder;

import android.os.Handler;
import android.os.Message;

/**
 * RecorderEngineBase提供统一的录音计时，计时器和录音过程分离
 */
public abstract class RecorderEngineBase implements RecorderEngine {

    protected RecordingTracker recordingTracker;
    private int maxSeconds = 60;

    public RecorderEngineBase() {
        recordingTracker = new RecordingTracker();
    }

    public class RecordingTracker implements RecorderEngineListener {

        private RecorderEngineListener recorderEngineListener;
        private TrackerThread thread;
        private boolean timeOver = false;

        public RecordingTracker() {
            super();
        }

        public void setRecorderEngineListener(RecorderEngineListener recorderEngineListener) {
            this.recorderEngineListener = recorderEngineListener;
        }

        private final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        long millis = (long) msg.obj;
                        onRecorderProgress(millis);
                        break;
                    case 2:
                        stop();
                        // stopTrack();
                    default:
                        break;
                }
                return false;
            }
        });

        /**
         * 计时器和录音过程分离
         *
         * @author ludq@hyxt.com
         */
        public class TrackerThread extends Thread {
            private volatile boolean pause = false;
            private final Object pauseLock = new Object(); // 暂停锁

            public void stopTrack() {
                interrupt();
                handler.removeCallbacksAndMessages(null);
            }

            public void pauseTrack() {
                pause = true;
            }

            public void resumeTrack() {
                synchronized (pauseLock) {
                    pause = false;
                    pauseLock.notifyAll(); // 唤醒所有等待的线程
                }
            }

            public void run() {
                timeOver = false;
                if (pause) return;
                while (!isInterrupted() && (tracedMillis - pausedMillis) < getDuring() * 1000L) {
                    synchronized (pauseLock) {
                        if (pause) {
                            try {
                                pauseLock.wait(); // 等待直到被唤醒
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                    tracedMillis = System.currentTimeMillis() - startTime;
                    handler.obtainMessage(1, tracedMillis - pausedMillis).sendToTarget();
                }
                if ((tracedMillis - pausedMillis) > getDuring() * 1000L) {
                    timeOver = true;
                }
                tracedMillis = System.currentTimeMillis() - startTime;
                handler.obtainMessage(2).sendToTarget();
            }
        }

        @Override
        public void onRecorderStart() {
            startTracking();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStart();
        }

        private long tracedMillis;
        private long startTime;

        private long pausedMillis;
        private long pauseTime;

        private void startTracking() {
            if (thread != null) {
                stopTrack();
            }
            pauseTime = 0;
            pausedMillis = 0;
            tracedMillis = 0;
            startTime = System.currentTimeMillis();
            thread = new TrackerThread();
            thread.start();
        }

        @Override
        public void onRecorderStop() {
            stopTrack();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStop();
        }

        @Override
        public void onRecorderPause() {
            pauseTime = System.currentTimeMillis();
            thread.pauseTrack();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderPause();
        }

        @Override
        public void onRecorderResume() {
            pausedMillis += (System.currentTimeMillis() - pauseTime);
            thread.resumeTrack();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderResume();
        }

        private void stopTrack() {
            if (thread != null) {
                thread.stopTrack();
                thread = null;
            }
        }

        public int getTrackedSeconds() {
            return (int) ((tracedMillis - pausedMillis) / 1000);
        }

        public long getTracedMillis() {
            return tracedMillis - pausedMillis;
        }


        @Override
        public void onRecorderProgress(long millis) {
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderProgress(millis);
        }

        @Override
        public void onRecorderStreamError(Throwable e) {
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStreamError(e);
            stopTrack();
        }

        public boolean isTimeOver() {
            return timeOver;
        }
    }

    public int getTrackedSeconds() {
        return recordingTracker.getTrackedSeconds();
    }

    public long getTracedMillis() {
        return recordingTracker.getTracedMillis();
    }

    public boolean isTimeOver() {
        return recordingTracker.isTimeOver();
    }

    @Override
    public final void setDuring(int seconds) {
        maxSeconds = seconds;
    }

    @Override
    public final int getDuring() {
        return maxSeconds;
    }

    public final void setRecorderListener(RecorderEngineListener recorderEngineListener) {
        recordingTracker.setRecorderEngineListener(recorderEngineListener);
    }

}
