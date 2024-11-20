package org.graalvm.argo.lambda_manager.utils;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.CreateTap;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTap;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class NetworkConfigurationUtils {

    private static final String LOCALHOST_IP = "127.0.0.1";

    public static void prepareVmConnectionPool(Queue<LambdaConnection> pool, int connections, String gatewayWithMask, int lambdaPort) {
        Iterator<IPv4Address> iPv4AddressIterator = IPv4Subnet.of(gatewayWithMask).iterator();
        iPv4AddressIterator.next(); // Note: skip *.*.*.0
        iPv4AddressIterator.next(); // Note: skip *.*.*.1
        String gateway = gatewayWithMask.split("/")[0];

        try {
            for (int i = 0; i < connections; i++) {
                String ip = getNextIPAddress(iPv4AddressIterator, gateway);
                String tap = String.format("%s-%s", Environment.TAP_PREFIX, generateRandomString());
                pool.add(new LambdaConnection(ip, tap, lambdaPort));
            }
        } catch (MalformedURLException e) {
            Logger.log(Level.INFO, "Failed to prepare lambda connection", e);
        }
    }

    public static void prepareContainerConnectionPool(Queue<LambdaConnection> pool, int connections) {
        try {
            for (int lambdaPort = Configuration.argumentStorage.getFirstLambdaPort(); lambdaPort < Configuration.argumentStorage.getFirstLambdaPort() + connections; lambdaPort++) {
                pool.add(new LambdaConnection(LOCALHOST_IP, null, lambdaPort));
            }
        } catch (MalformedURLException e) {
            Logger.log(Level.INFO, "Failed to prepare lambda connection", e);
        }
    }

    private static String getNextIPAddress(Iterator<IPv4Address> iPv4AddressIterator, String gateway) {
        String nextIPAddress = iPv4AddressIterator.next().toString();
        if (nextIPAddress.equals(gateway)) {
            return iPv4AddressIterator.next().toString();
        } else {
            return nextIPAddress;
        }
    }

    private static String generateRandomString() {
        return new Random().ints('a', 'z' + 1)
                        .limit(Environment.RAND_STRING_LEN)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
    }

    public static void createTap(String tap) throws InterruptedException {
        // Create os-level network interface (tap).
        ProcessBuilder createTaps = new CreateTap(tap).build();
        createTaps.start();
        createTaps.join();
    }

    public static void removeTap(String tap) throws InterruptedException {
        ProcessBuilder removeTap = new RemoveTap(tap).build();
        removeTap.start();
        removeTap.join();
    }

}
