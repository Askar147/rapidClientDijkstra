package eu.project.rapid.ac.profilers;

import eu.project.rapid.common.RapidConstants.ExecLocation;
import eu.project.rapid.utils.Configuration;

public class Profiler {
    private Configuration config;
    private ExecLocation execLocation; // Local or Remote
    private ProgramProfiler progProfiler;
    private NetworkProfiler netProfiler;

    public Profiler(String appName, String methodName, ExecLocation execLocation,
                    Configuration config) {
        this.execLocation = execLocation;
        this.progProfiler = new ProgramProfiler(appName, methodName, execLocation);
        this.config = config;

        if (execLocation == ExecLocation.REMOTE) {
            this.netProfiler = new NetworkProfiler();
        }
    }

    public void start() {
        progProfiler.start();
        if (netProfiler != null) {
            netProfiler.start();
        }
    }

    public void stopAndSave(long pureExecDur) {
        stopAndSave(0, pureExecDur);
    }

    public void stopAndSave(long prepareDataDur, long pureExecDur) {
        progProfiler.setPrepareDataDur(prepareDataDur);
        progProfiler.setPureExecDur(pureExecDur);
        progProfiler.stop();
        if (netProfiler != null) {
            netProfiler.stop();
        }

        LogRecord logRecord = new LogRecord(progProfiler, netProfiler, config);
        logRecord.save();
    }

    public void stopAndDiscard() {
        progProfiler.stop();
        if (netProfiler != null) {
            netProfiler.stop();
        }
    }

    // PureExecTime refers to the time it takes to execute the method without considering networking
    // time.
    public void setPureExecDur(long pureExecDur) {
        progProfiler.setPureExecDur(pureExecDur);
    }
}
