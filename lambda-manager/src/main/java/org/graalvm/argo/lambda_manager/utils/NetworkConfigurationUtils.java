package org.graalvm.argo.lambda_manager.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;

import io.micronaut.context.BeanContext;
import io.micronaut.http.client.RxHttpClient;

public class NetworkConfigurationUtils {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int FIRST_LAMBDA_PORT = 30100;

    public static void prepareVmConnectionPool(Queue<LambdaConnection> pool, int connections, String gatewayWithMask, int lambdaPort, BeanContext beanContext) {
        Iterator<IPv4Address> iPv4AddressIterator = IPv4Subnet.of(gatewayWithMask).iterator();
        iPv4AddressIterator.next(); // Note: skip *.*.*.0
        iPv4AddressIterator.next(); // Note: skip *.*.*.1
        String gateway = gatewayWithMask.split("/")[0];

        try {
            for (int i = 0; i < connections; i++) {
                String ip = getNextIPAddress(iPv4AddressIterator, gateway);
                String tap = String.format("%s-%s", Environment.TAP_PREFIX, generateRandomString());
                URL url = new URL("http", ip, lambdaPort, "/");
                RxHttpClient client = beanContext.createBean(RxHttpClient.class, url);
                pool.add(new LambdaConnection(ip, tap, client, lambdaPort));
            }
        } catch (MalformedURLException e) {
            Logger.log(Level.INFO, "Failed to prepare lambda connection", e);
        }
    }

    public static void prepareContainerConnectionPool(Queue<LambdaConnection> pool, int connections, BeanContext beanContext) {
        try {
            for (int lambdaPort = FIRST_LAMBDA_PORT; lambdaPort < FIRST_LAMBDA_PORT + connections; lambdaPort++) {
                URL url = new URL("http", LOCALHOST_IP, lambdaPort, "/");
                RxHttpClient client = beanContext.createBean(RxHttpClient.class, url);
                pool.add(new LambdaConnection(LOCALHOST_IP, null, client, lambdaPort));
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

}
