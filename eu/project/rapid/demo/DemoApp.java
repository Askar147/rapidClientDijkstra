package eu.project.rapid.demo;

import eu.project.rapid.ac.DFE;
import eu.project.rapid.common.RapidConstants.ExecLocation;
//import eu.project.rapid.demo.gvirtus.MatrixMul;
import eu.project.rapid.demo.dijkstra.Dijkstra;
import eu.project.rapid.demo.dijkstra.DijkstraResult;
import eu.project.rapid.demo.helloJNI.HelloJNI;
import eu.project.rapid.demo.nqueens.NQueens;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public class DemoApp {
    private DFE dfe;

    private final static Logger log = LogManager.getLogger(DemoApp.class.getSimpleName());

   // private ExecLocation[] execLocations = {ExecLocation.LOCAL, ExecLocation.REMOTE};
//    private ExecLocation[] execLocations = {ExecLocation.LOCAL};
    private ExecLocation[] execLocations = {ExecLocation.REMOTE};

    // Variables for statistics
//    private int[] nrQueens = {4, 5, 6, 7, 8};
    private int[] nrQueens = {4, 5, 6, 7, 8};
    // private int[] nrTestsQueens = {5, 5, 5, 5, 5};
//    private int[] nrTestsQueens = {0, 0, 0, 0, 0};
    private int[] nrTestsQueens = {5, 5, 5, 5, 5};
    private double[] nQueensLocalDur = new double[nrQueens.length];
    private int[] nQueensLocalNr = new int[nrQueens.length];
    private double[] nQueensRemoteDur = new double[nrQueens.length];
    private int[] nQueensRemoteNr = new int[nrQueens.length];

    private int nrJniTests = 5;
    private double jniLocalDur;
    private int jniLocalNr;
    private double jniRemoteDur;
    private int jniRemoteNr;

    private int nrCudaTests = 0;
    private double cudaLocalDur;
    private int cudaLocalNr;
    private double cudaRemoteDur;
    private int cudaRemoteNr;

    private int nrDijkstraTests = 1;
    private double dijkstraLocalDur;
    private int dijkstraLocalNr;
    private double dijkstraRemoteDur;
    private int dijkstraRemoteNr;
    private DijkstraResult result;

    private boolean requestDeclined = false;

    public DemoApp(String vmIP, String connType) {

        if (vmIP != null) {
            log.info("Registering with the given VM...");
            dfe = DFE.getInstance(vmIP);
        } else {
            log.info("Registering with the RAPID system to get a VM...");
            dfe = DFE.getInstance();
        }

        dfe.setUserChoice(ExecLocation.REMOTE);
        dfe.setConnEncrypted(connType != null && connType.equals("ssl"));

//        System.out.println();
//        System.out.println();
//        log.info("Testing JNI...");
//        testHelloJni();

//        System.out.println();
//        System.out.println();
//        log.info("Testing NQueens...");
//        testNQueens();
//        if (requestDeclined)
//            return;


//        System.out.println();
//        System.out.println();
//        log.info("Testing Dijkstra...");
//
//        try{
//            result = testDijkstra(costs, start_x, start_y, end_x, end_y, cycles, potential);
//            System.out.println("----------------Result TRUE---------------------------------------- " + result);
//            System.out.println("----------------Result---------------------------------------- " + result);
//            System.out.println("----------------Result---------------------------------------- " + result);
//            log.info("----------------Result TRUE---------------------------------------- ");
//            if (requestDeclined)
//                return;
//        } finally {
//            dfe.destroy();//////////////////////////////////////////
//        }


//        System.out.println();
//        System.out.println();
//        log.info("Testing CUDA offloading...");
//        testCUDA();

//
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println("---------------- Cumulative statistics of the demo testing applications ----------------");
//
//        System.out.println();
//        log.debug("dijkstra");
//        System.out.printf("%12s %15s%n", "Nr.", "Avg. dur. (ms)");
//        System.out.printf("%-8s %3d %15.2f%n", "Local", dijkstraLocalNr, dijkstraLocalDur / dijkstraLocalNr / 1000000);
//        System.out.printf("%-8s %3d %15.2f%n", "Remote", dijkstraRemoteNr, dijkstraRemoteDur / dijkstraRemoteNr / 1000000);

//        // nQueens
//        for (int i = 0; i < nrQueens.length; i++) {
//            System.out.println();
//            log.debug(nrQueens[i] + "-Queens");
//            System.out.printf("%12s %15s%n", "Nr.", "Avg. dur. (ms)");
//            System.out.printf("%-8s %3d %15.2f%n", "Local", nQueensLocalNr[i], nQueensLocalDur[i] / nQueensLocalNr[i] / 1000000);
//            System.out.printf("%-8s %3d %15.2f%n", "Remote", nQueensRemoteNr[i], nQueensRemoteDur[i] / nQueensRemoteNr[i] / 1000000);
//        }


//        System.out.println();
//        log.debug("jni hello");
//        System.out.printf("%12s %15s%n", "Nr.", "Avg. dur. (ms)");
//        System.out.printf("%-8s %3d %15.2f%n", "Local", jniLocalNr, jniLocalDur / jniLocalNr / 1000000);
//        System.out.printf("%-8s %3d %15.2f%n", "Remote", jniRemoteNr, jniRemoteDur / jniRemoteNr / 1000000);
//
//        System.out.println();
//        log.debug("CUDA MatrixMul");
//        System.out.printf("%12s %15s%n", "Nr.", "Avg. dur. (ms)");
//        System.out.printf("%-8s %3d %15.2f%n", "Local", cudaLocalNr, cudaLocalDur / cudaLocalNr / 1000000);
//        System.out.printf("%-8s %3d %15.2f%n", "Remote", cudaRemoteNr, cudaRemoteDur / cudaRemoteNr / 1000000);

    }

//    private void testNQueens() {
//        NQueens q = new NQueens(dfe);
//
//        for (ExecLocation execLocation : execLocations) {
//            dfe.setUserChoice(execLocation);
//            for (int i = 0; i < nrQueens.length; i++) {
//                for (int j = 0; j < nrTestsQueens[i]; j++) {
//                    int result = q.solveNQueens(nrQueens[i]);
//                    log.info("Result of NQueens(" + nrQueens[i] + "): " + result);
//
//                    String methodName = "localSolveNQueens";
//                    if (dfe.getExecLocation().equals(ExecLocation.LOCAL)) {
//                        requestDeclined = true;
//                        return;
//                    }
//                    if (dfe.getLastExecLocation(methodName).equals(ExecLocation.LOCAL)) {
//                        nQueensLocalNr[i]++;
//                        nQueensLocalDur[i] += dfe.getLastExecDuration(methodName);
//                    } else {
//                        nQueensRemoteNr[i]++;
//                        nQueensRemoteDur[i] += dfe.getLastExecDuration(methodName);
//                    }
//                }
//            }
//        }
//    }

    public void testDijkstraLogs(byte[] costs, double start_x, double start_y, double end_x, double end_y, int cycles, float[] potential) {

        System.out.println();
        System.out.println();
        log.info("Testing Dijkstra...");


        try{
            log.warn("===-------------=testDijsktraLOGS INCOMING VALUES: " + cycles + " " + start_x + " " + start_y + " " + end_x + " " + end_y);
            log.warn("costs:");

            for (int i = 0; i < 3; i++) {
                log.info(costs[i]);
            }

//            log.error("===-------------=testDijsktraLOGS RESULT BEFORE" + result);
//            log.error("===-------------=testDijsktraLOGS RESULT BEFORE" + result.result);
//            log.error("===-------------=testDijsktraLOGS RESULT BEFORE" + result.newPotential);
//            log.error("===-------------=testDijsktraLOGS RESULT BEFORE" + result.newPotentialIndex);

            result = testDijkstra(costs, start_x, start_y, end_x, end_y, cycles, potential);

//            log.error("===-------------=testDijsktraLOGS RESULT AFTER" + result);
//            log.error("===-------------=testDijsktraLOGS RESULT AFTER" + result.result);
//            log.error("===-------------=testDijsktraLOGS RESULT AFTER" + result.newPotential);
//            log.error("===-------------=testDijsktraLOGS RESULT AFTER" + result.newPotentialIndex);
            if (requestDeclined)
                return;
        } finally {
            //FIXME: RETURN DESTROY FROM MAIN METHOD
            //dfe.destroy();//////////////////////////////////////////
        }

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("---------------- Cumulative statistics of the demo testing applications ----------------");

        System.out.println();
        log.debug("dijkstra");
        System.out.printf("%12s %15s%n", "Nr.", "Avg. dur. (ms)");
        System.out.printf("%-8s %3d %15.2f%n", "Local", dijkstraLocalNr, dijkstraLocalDur / dijkstraLocalNr / 1000000);
        System.out.printf("%-8s %3d %15.2f%n", "Remote", dijkstraRemoteNr, dijkstraRemoteDur / dijkstraRemoteNr / 1000000);
    }
    private DijkstraResult testDijkstra(byte[] costs, double start_x, double start_y, double end_x, double end_y, int cycles, float[] potential) {
        Dijkstra d = new Dijkstra(dfe);
        DijkstraResult result = new DijkstraResult();
        for (ExecLocation execLocation : execLocations) {
            dfe.setUserChoice(execLocation);
            for (int i = 0; i < nrDijkstraTests; i++) {
                result = d.solveDijkstra(costs, start_x, start_y, end_x, end_y, cycles, potential);
                log.info("The result of the native call with DFE: " + result);

                String methodName = "localSolveDijkstra";
                if (dfe.getLastExecLocation(methodName).equals(ExecLocation.LOCAL)) {
                    dijkstraLocalNr++;
                    dijkstraLocalDur += dfe.getLastExecDuration(methodName);
                } else {
                    dijkstraRemoteNr++;
                    dijkstraRemoteDur += dfe.getLastExecDuration(methodName);
                }
            }

        }
        return result;
    }

//    private void testHelloJni() {
//        HelloJNI helloJni = new HelloJNI(dfe);
//
//        for (ExecLocation execLocation : execLocations) {
//            dfe.setUserChoice(execLocation);
//            for (int i = 0; i < nrJniTests; i++) {
//                int result = helloJni.printJava();
//                log.info("The result of the native call with DFE: " + result);
//
//                String methodName = "localprintJava";
//                if (dfe.getLastExecLocation(methodName).equals(ExecLocation.LOCAL)) {
//                    jniLocalNr++;
//                    jniLocalDur += dfe.getLastExecDuration(methodName);
//                } else {
//                    jniRemoteNr++;
//                    jniRemoteDur += dfe.getLastExecDuration(methodName);
//                }
//            }
//        }
//    }

//    private void testCUDA() {
//        MatrixMul matrixMul = null;
//        try {
//            matrixMul = new MatrixMul(dfe);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        int wa = 8;
//        int wb = 12;
//
//        for (ExecLocation execLocation : execLocations) {
//            dfe.setUserChoice(execLocation);
//            for (int i = 0; i < nrCudaTests; i++) {
//                log.info("------------ Started running CUDA MatrixMul with DFE.");
//                matrixMul.gpuMatrixMul(wa, wb, wa);
//                log.info("Finished executing CUDA MatrixMul with DFE.");
//
//                String methodName = "localGpuMatrixMul";
//                if (dfe.getLastExecLocation(methodName).equals(ExecLocation.LOCAL)) {
//                    cudaLocalNr++;
//                    cudaLocalDur += dfe.getLastExecDuration(methodName);
//                } else {
//                    cudaRemoteNr++;
//                    cudaRemoteDur += dfe.getLastExecDuration(methodName);
//                }
//            }
//        }
//    }
    private static double bytesToDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getDouble();
    }

    public static void main(String[] argv) {
        String vmIP = "192.168.0.104";
        String connType = "ssl";

        // Parse command-line arguments for VM IP and connection type
        for (int i = 0; i < argv.length; i++) {
            switch (argv[i]) {
                case "-vm":
                    vmIP = argv[i + 1];
                    i++; // Move past the argument value to avoid interpreting it as an option
                    break;

                case "-rapid":
                    vmIP = null; // Use a default or predetermined VM IP
                    break;

                case "-conn":
                    connType = argv[i + 1];
                    i++; // Move past the argument value
                    break;
            }
        }

        int port = 12345;
        DemoApp demo = new DemoApp(vmIP, connType);
        //TODO: REMOVE potential array from method calls
        float[] potential = new float[1];
        int index = 0;
        while (true) {


                // READING
            int numClientData = 200;
            Random random = new Random();
            int randomClientIndex = random.nextInt(numClientData);


            byte[] readCosts = new byte[416 * 160]; // The same size as the original costs array
            String inputCostsDir = "/home/user/Desktop/Costs/costs" + randomClientIndex + ".dat";
            String inputParamDir = "/home/user/Desktop/Costs/costs_param" + randomClientIndex + ".dat";
                try (FileInputStream fis = new FileInputStream(inputCostsDir);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    int totalBytesRead = bis.read(readCosts);
                    System.out.println("Read " + totalBytesRead + " bytes from costs.dat");
                } catch (IOException e) {
                    System.out.println("An error occurred while reading costs from file: " + e.getMessage());
                    e.printStackTrace();
                }

                double start_x_FromFile, start_y_FromFile, end_x_FromFile, end_y_FromFile;
                int cycles_FromFile;

                try (FileInputStream fis = new FileInputStream(inputParamDir);
                     DataInputStream dis = new DataInputStream(fis)) {
                    // Reading the parameters from file
                    start_x_FromFile = dis.readDouble();
                    start_y_FromFile = dis.readDouble();
                    end_x_FromFile = dis.readDouble();
                    end_y_FromFile = dis.readDouble();
                    cycles_FromFile = dis.readInt();
                    System.out.println("Read start_x: " + start_x_FromFile);
                    System.out.println("Read start_y: " + start_y_FromFile);
                    System.out.println("Read end_x: " + end_x_FromFile);
                    System.out.println("Read end_y: " + end_y_FromFile);
                    System.out.println("Read cycles: " + cycles_FromFile);

                    //FIXME: this is just testDijkstraLogs for reading (xz kak eshe), krch drugoi testDijkstraLogs ne nado uzat kogda etot est
                    demo.testDijkstraLogs(readCosts, start_x_FromFile, start_y_FromFile, end_x_FromFile, end_y_FromFile, cycles_FromFile, potential);

                } catch (IOException e) {
                    System.err.println("IOException while reading parameters from file: " + e.getMessage());
                    e.printStackTrace();
                }
        }
        demo.dfe.destroy();
        log.warn("DEMOAPP: OOPS! I'm dead:(");

        System.exit(0);
    }
}
