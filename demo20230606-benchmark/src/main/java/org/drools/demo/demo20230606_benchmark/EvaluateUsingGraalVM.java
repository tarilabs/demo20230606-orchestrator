package org.drools.demo.demo20230606_benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import org.drools.util.IoUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/*
 * The scope of this benchmark is to measure the time to:
 * 1. startup a GraalVM executable
 * 2. hit readiness probe until ready
 * 3. evaluate the rules
 * Result "org.drools.demo.demo20230606_benchmark.EvaluateUsingGraalVM.testMethod":
  0.331 ±(99.9%) 0.026 s/op [Average]
  (min, avg, max) = (0.275, 0.331, 0.387), stdev = 0.035
  CI (99.9%): [0.305, 0.357] (assumes normal distribution)
 */
@State(Scope.Benchmark)
public class EvaluateUsingGraalVM {
    static byte[] payload;
    static {
        InputStream resourceAsStream = EvaluatingRulesBenchmark.class.getResourceAsStream("/100-line-items.json");
        try {
            payload = IoUtils.readBytesFromInputStream(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    private static HttpClient client = HttpClient.newHttpClient();
    private ProcessBuilder procBuilder;
    private Process proc;
    private HttpRequest ready;
    private HttpRequest request;
    private int findPort;

    @Setup(Level.Invocation)
    public void init() throws Exception {
        System.out.println("init");

        findPort = 8080;
        procBuilder = new ProcessBuilder("../demo20230606-asquarkus/target/demo20230606-asquarkus-1.0-SNAPSHOT-runner",
                "-Dquarkus.http.port=" + findPort)
                //.inheritIO()
                ;

        ready = HttpRequest.newBuilder(URI.create("http://localhost:" + findPort + "/q/health/ready"))
                .GET()
                .build();
        request = HttpRequest.newBuilder(URI.create("http://localhost:" + findPort + "/drools"))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public String testMethod() throws Exception {
        //System.out.println("start using port " + findPort);
        proc = procBuilder.start();
        BodyHandler<String> handler = BodyHandlers.ofString();
        boolean pReady = false;
        while (!pReady) {
            try {
                HttpResponse<String> readyResponse = client.send(ready, handler);
                //System.out.println(readyResponse.body());
                pReady = true;
            } catch (Exception e) {
                //System.out.print("\u231B");
            }
        }
        HttpResponse<String> response = client.send(request, handler);
        //System.out.println(response.body().substring(0, 100));
        return response.body();
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        System.out.println("tearDown");
        proc.destroy();
        while (proc.isAlive()) {
            // busy loop waiting for proc to shut down
        }
    }
}
