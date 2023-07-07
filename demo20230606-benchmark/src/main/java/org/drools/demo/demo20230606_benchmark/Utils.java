package org.drools.demo.demo20230606_benchmark;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Utils {
    private Utils() {
        // only static utils methods.
    }
    public static int getNextAvailable() {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress((InetAddress) null, 0), 1);
            int port = ss.getLocalPort();
            return port;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find free port", e);
        }
    }

}
