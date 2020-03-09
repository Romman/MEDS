package org.meds.server;

import java.util.EventListener;

public interface Server {

    int getBuildVersion();

    int getMinSupportedVersion();

    void start();

    void shutdown();

    boolean isStopping();

    void addStopListener(StopListener listener);

    void removeStopListener(StopListener listener);

    String getFormattedStartTime();

    int getUptimeMillis();

    interface StopListener extends EventListener {
        void stop();
    }
}
