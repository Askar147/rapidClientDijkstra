package eu.project.rapid.ac;

import eu.project.rapid.ac.db.DBCache;
import eu.project.rapid.ac.db.DBEntry;
import eu.project.rapid.ac.profilers.NetworkProfiler;
import eu.project.rapid.common.RapidConstants.ExecLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Deque;

/**
 * The Design Space Explorer class is responsible for deciding where to execute a method.
 *
 * @author sokol
 */
class DSE {

    private static DSE instance;

    private static final boolean VERBOSE_LOG = false;

    // To be used in case of no previous remote execution.
    // If the ulRate and dlRate are bigger than these values then we offload
    private static final int MIN_UL_RATE_OFFLOAD_1_TIME = 256 * 1000; // b/s
    private static final int MIN_DL_RATE_OFFLOAD_1_TIME = 256 * 1000; // b/s
    private static String appName;
    private DBCache dbCache;

    // private int programOrientedDecNr = 1; // Keep track when the decision is taken

    private static final Logger log = LogManager.getLogger(DSE.class.getName());

    private DSE(String appName) {
        DSE.appName = appName;
        this.dbCache = DBCache.getDbCache(appName);
    }

    static DSE getInstance(String appName) {
        // local variable increases performance by 25 percent according to
        // Joshua Bloch "Effective Java, Second Edition", p. 283-284
        DSE result = instance;

        if (result == null) {
            synchronized (DFE.class) {
                result = instance;
                if (result == null) {
                    instance = result = new DSE(appName);
                }
            }
        }

        return result;
    }

    ExecLocation findExecLocationDbCache(String appName, String methodName) {
        if (shouldOffloadDBCache(appName, methodName, NetworkProfiler.lastUlRate,
                NetworkProfiler.lastDlRate)) {
            log.info("Decided to execute REMOTE");
            return ExecLocation.REMOTE;
        }
        log.info("Decided to execute LOCAL");
        return ExecLocation.LOCAL;
    }

    /**
     * @param appName The name of the application, usually is the name of the jar file.
     * @param methodName The name of the method.
     * @param currUlRate Current upload data rate towards the VM.
     * @param currDlRate Current download data rate towards the VM.
     * @return <b>True</b> if the method should be executed remotely<br>
     * <b>False</b> otherwise.
     */
    private boolean shouldOffloadDBCache(String appName, String methodName, int currUlRate, int currDlRate) {

        log.info("Deciding exec location for appName=" + appName
                + ", methodName=" + methodName + ", currUlRate=" + currUlRate + ", currDlRate="
                + currDlRate);
        log.info(String.format("DB cache has %d entries and %d measurements in total.",
                dbCache.size(), dbCache.nrElements()));


        // Variables needed for the local executions
        int nrLocalExec;
        long meanDurLocal = 0;

        // Variables needed for the remote executions
        int nrRemoteExec;
        long meanDurRemote1 = 0;
        long meanDurRemote2;
        long meanDurRemote;

        // Check if the method has been executed LOCALLY in previous runs
        Deque<DBEntry> localResults = dbCache.getAllEntriesFilteredOn(appName, methodName, ExecLocation.LOCAL);
        nrLocalExec = localResults.size();

        // Check if the method has been executed REMOTELY in previous runs
        Deque<DBEntry> remoteResults = dbCache.getAllEntriesFilteredOn(appName, methodName, ExecLocation.REMOTE);
        nrRemoteExec = remoteResults.size();

        // DECISION 1
        // If the number of previous remote executions is zero and the current connection is good
        // then offload the method to see how it goes.
        if (nrRemoteExec == 0) {
            if (currUlRate > MIN_UL_RATE_OFFLOAD_1_TIME && currDlRate > MIN_DL_RATE_OFFLOAD_1_TIME) {
                log.info("Decision 1: No previous remote executions. Good connectivity.");
                return true;
            } else {
                log.info("Decision 1: No previous remote executions. Bad connectivity.");
                return false;
            }
        }

        // Local part
        // Calculate the meanDurLocal and meanEnergyLocal from the previous runs.
        // Give more weight to recent measurements.
        if (VERBOSE_LOG) {
            log.info("------------ The local executions of the method:");
        }

        long localDuration = 0;
        int i = 0;
        for (DBEntry e : localResults) {
            meanDurLocal += e.getExecDuration();
            i++;
            if (i > 1) {
                meanDurLocal /= 2;
            }

            if (VERBOSE_LOG) {
                log.info("duration: " + localDuration + " timestamp: " + e.getTimestamp());
            }
        }
        log.info("nrLocalExec: " + nrLocalExec);
        log.info("meanDurLocal: " + meanDurLocal + "ns (" + meanDurLocal / 1000000000.0 + "s)");

        // Remote part
        long[] remoteDurations = new long[nrRemoteExec];
        int[] remoteUlRates = new int[nrRemoteExec];
        int[] remoteDlRates = new int[nrRemoteExec];
        long[] remoteTimestamps = new long[nrRemoteExec];
        i = 0;
        if (VERBOSE_LOG) {
            log.info("------------ The remote executions of the method:");
        }
        for (DBEntry e : remoteResults) {
            remoteDurations[i] = e.getExecDuration();
            remoteUlRates[i] = e.getUlRate();
            remoteDlRates[i] = e.getDlRate();
            remoteTimestamps[i] = e.getTimestamp();

            if (VERBOSE_LOG) {
                log.info("duration: " + remoteDurations[i] + " ulRate: " + remoteUlRates[i] + " dlRate: "
                        + remoteDlRates[i] + " timestamp: " + remoteTimestamps[i]);
            }

            i++;
        }
        log.info("nrRemoteExec: " + nrRemoteExec);

        // DECISION 3
        // Calculate two different mean values for the offloaded execution:
        // 1. The first are the same as for the local execution, gives more weight to recent runs.
        // 2. The second are calculated as the average of the three closest values to the currentUlRate
        // and currDlRate
        int minDistIndex1 = 0, minDistIndex2 = 0, minDistIndex3 = 0;
        double minDist1 = Double.POSITIVE_INFINITY, minDist2 = Double.POSITIVE_INFINITY,
                minDist3 = Double.POSITIVE_INFINITY;
        for (i = 0; i < nrRemoteExec; i++) {
            // Calculate the first meanDuration and meanEnergy
            // The first formula is the same as for the local executions,
            // gives more importance to the last measurements.
            meanDurRemote1 += remoteDurations[i];
            if (i > 0) {
                meanDurRemote1 /= 2;
            }

            // Keep the indexes of the three measurements that have
            // the smallest distance dist(ulRate, dlRate, currUlRate, currDlRate)
            // minDist1 < minDist2 < minDist3
            double newDist = dist(remoteUlRates[i], remoteDlRates[i], currUlRate, currDlRate);
            if (newDist < minDist1) {
                minDist3 = minDist2;
                minDistIndex3 = minDistIndex2;

                minDist2 = minDist1;
                minDistIndex2 = minDistIndex1;

                minDist1 = newDist;
                minDistIndex1 = i;
            } else if (newDist < minDist2) {
                minDist3 = minDist2;
                minDistIndex3 = minDistIndex2;

                minDist2 = newDist;
                minDistIndex2 = i;
            } else if (newDist < minDist3) {
                minDist3 = newDist;
                minDistIndex3 = i;
            }
        }

        // Give more weight to the closest point
        meanDurRemote2 = (((remoteDurations[minDistIndex3] + remoteDurations[minDistIndex2]) / 2)
                + remoteDurations[minDistIndex1]) / 2;

        meanDurRemote = (meanDurRemote1 + meanDurRemote2) / 2;

        // log.debug("meanDurRemote1: " + meanDurRemote1 + " meanDurRemote2: " + meanDurRemote2);
        log.debug("meanDurRemote: " + meanDurRemote + "ns (" + meanDurRemote / 1000000000.0 + "s)");

        // DECISION 2: Count nr. of times in a row the method has been executed on the last location (LOCAL or REMOTE).
        int count = 0;
        ExecLocation prevExecLocation = null;
        for (DBEntry e : dbCache.getAllEntriesFilteredOn(methodName)) {
            if (prevExecLocation == null || e.getExecLocation().equals(prevExecLocation)) {
                prevExecLocation = e.getExecLocation();
                count++;
            } else {
                break;
            }
        }

        // if we have been executing this method remotely for many times in a row, because is convenient,
        // decide to run it locally after some time.
        if(prevExecLocation == ExecLocation.REMOTE && count > 10 * (int)(meanDurLocal / meanDurRemote)) {
            log.info("Decision 2: Too many remote executions in a row.");
            return false;
        } else if(prevExecLocation == ExecLocation.LOCAL && count > 10 * (int)(meanDurRemote / meanDurLocal)) {
            log.info("Decision 2: Too many local executions in a row.");
            if (currUlRate > MIN_UL_RATE_OFFLOAD_1_TIME && currDlRate > MIN_DL_RATE_OFFLOAD_1_TIME) {
                log.info("Decision 2->1: Good connectivity, decided to offload.");
                return true;
            } else {
                log.info("Decision 2->1: Bad connectivity, decided to not offload.");
                return false;
            }
        }

        log.info("Decision 3.");
        return meanDurRemote <= meanDurLocal;
    }

    private double dist(int ul1, int dl1, int ul2, int dl2) {
        return Math.sqrt((ul2 - ul1) * (ul2 - ul1) + (dl2 - dl1) * (dl2 - dl1));
    }

    ExecLocation getLastExecLocation(String methodName) {
        Deque<DBEntry> results = dbCache.getAllEntriesFilteredOn(appName, methodName);
        if (results.size() == 0) {
            log.warn("There is no execution history for: " + appName + ":" + methodName);
            return ExecLocation.DYNAMIC;
        }
        return results.getFirst().getExecLocation();
    }

    long getLastExecDuration(String methodName) {
        Deque<DBEntry> results = dbCache.getAllEntriesFilteredOn(appName, methodName);
        if (results.size() == 0) {
            log.warn("There is no execution history for: " + appName + ":" + methodName);
            return -1;
        }
        return results.getFirst().getExecDuration();
    }
}


