package org.drools.demo.demo20230606_benchmark;

import java.util.List;

import org.drools.demo.demo20230606_datamodel.Fact;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * The scope of this benchmark is to measure the time to evaluate the rules in itself,
 * in order to evaluate a fixed cost consumption in the overall budget of SLA.
 * Result "org.drools.demo.demo20230606_benchmark.EvaluatingRulesBenchmark.testMethod":
  0.129 ±(99.9%) 0.005 s/op [Average]
  (min, avg, max) = (0.116, 0.129, 0.142), stdev = 0.007
  CI (99.9%): [0.124, 0.134] (assumes normal distribution)
 */
@State(Scope.Benchmark)
public class EvaluatingRulesBenchmark {
    static final String kjarName = "demo20230606-2000-rules-kjar-";
    private KieSession session;
    private List<Fact> unmarshal;

    @Setup(Level.Invocation)
    public void init() throws Exception {
        unmarshal = new ObjectMapper()
                .readerFor(new TypeReference<List<Fact>>() {})
                .readValue(EvaluatingRulesBenchmark.class.getResourceAsStream("/100-line-items.json"));
        KieServices ks = KieServices.get();
        // ReleaseId releaseId = ks.newReleaseId("org.drools.demo", kjarName + "1", "1.0-SNAPSHOT");
        // KieContainer kieContainer = ks.newKieContainer(releaseId);
        KieContainer kieContainer = ks.newKieClasspathContainer();
        session = kieContainer.newKieSession();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int testMethod() {
        unmarshal.forEach(session::insert);
        return session.fireAllRules();
    }

    @TearDown
    public void tearDown() {
        session.dispose();
    }
}
