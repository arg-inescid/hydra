package org.graalvm.argo.lambda_manager.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.CreateTaps;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;

import io.micronaut.context.BeanContext;
import io.micronaut.http.client.RxHttpClient;

public class NetworkConfigurationUtils {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int FIRST_LAMBDA_PORT = 30100;

    public static void prepareVmConnectionPool(List<LambdaConnection> pool, int connections, String gatewayWithMask, int lambdaPort, BeanContext beanContext) throws ErrorDuringCreatingConnectionPool {
        Iterator<IPv4Address> iPv4AddressIterator = IPv4Subnet.of(gatewayWithMask).iterator();
        iPv4AddressIterator.next(); // Note: skip *.*.*.0
        iPv4AddressIterator.next(); // Note: skip *.*.*.1
        String gateway = gatewayWithMask.split("/")[0];

        try {
            for (int i = 0; i < connections; i++) {
                String ip = getNextIPAddress(iPv4AddressIterator, gateway);
                String tap = String.format("%s-%s", Environment.TAP_PREFIX, generateRandomString());
                RxHttpClient client = beanContext.createBean(RxHttpClient.class,
                                new URL("http", ip, lambdaPort, "/"));
                pool.add(new LambdaConnection(ip, tap, client, lambdaPort));
            }

            ProcessBuilder createTaps = new CreateTaps().build();
            createTaps.start();
            createTaps.join();
        } catch (InterruptedException | MalformedURLException e) {
            throw new ErrorDuringCreatingConnectionPool(Messages.ERROR_POOL_CREATION, e);
        }
    }

    public static void prepareContainerConnectionPool(List<LambdaConnection> pool, int connections, BeanContext beanContext) throws ErrorDuringCreatingConnectionPool {
        try {
            for (int lambdaPort = FIRST_LAMBDA_PORT; lambdaPort < FIRST_LAMBDA_PORT + connections; lambdaPort++) {
                RxHttpClient client = beanContext.createBean(RxHttpClient.class,
                                new URL("http", LOCALHOST_IP, lambdaPort, "/"));
                pool.add(new LambdaConnection(LOCALHOST_IP, null, client, lambdaPort));
            }
        } catch (MalformedURLException e) {
            throw new ErrorDuringCreatingConnectionPool(Messages.ERROR_POOL_CREATION, e);
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
