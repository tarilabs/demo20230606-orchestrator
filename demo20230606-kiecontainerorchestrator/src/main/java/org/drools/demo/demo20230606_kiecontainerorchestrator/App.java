package org.drools.demo.demo20230606_kiecontainerorchestrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.drools.demo.demo20230606_datamodel.Fact;
import org.drools.demo.demo20230606_utils.KieSessionUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static Logger LOG = LoggerFactory.getLogger(App.class);
    static Scanner s = new Scanner(System.in);
    static String JSON;
    private static ReleaseId releaseId;

    public static void main(String[] args) throws Exception {
        LOG.info("App starting.");
        JSON = Files.readString(Paths.get(App.class.getResource("/test1.json").toURI()));
        KieServices ks = KieServices.get();
        releaseId = ks.newReleaseId("org.drools.demo", "demo20230606-kjar", "1.0-SNAPSHOT");
        programmaticallyCacheTheKJAR(ks);
        do {
            doOnce(ks);
            System.gc();
            pressEnterKeyToContinue("all done, pending for a new loop");
        } while(true);
    }

    private static void programmaticallyCacheTheKJAR(KieServices ks) throws IOException {
        Path kjarPath = Paths.get("../demo20230606-kjar/target/demo20230606-kjar-1.0-SNAPSHOT.jar");
        LOG.info("Cache manually a KJAR without kjar-inheritance from: {}", kjarPath.toAbsolutePath());
        byte[] kjarBytes = Files.readAllBytes(kjarPath);
        KieModule manuallyCached = deployJarIntoRepository(ks, kjarBytes); // cache manually as it's just a kjar without kjar inheritance
        LOG.info("Cached manually (as just a KJAR without kjar-inheritance): {}", manuallyCached.getReleaseId());
    }

    private static void doOnce(KieServices ks) throws Exception {
        KieContainer kieContainer = ks.newKieContainer(releaseId);
        KieSession session = kieContainer.newKieSession();
        List<Fact> unmarshal = new ObjectMapper()
            .readerFor(new TypeReference<List<Fact>>() {})
            .readValue(JSON);
        unmarshal.forEach(x -> LOG.info("to insert: {}", x));
        unmarshal.forEach(session::insert);
        session.fireAllRules();
        Collection<Fact> results = KieSessionUtils.getFactsHaving(session, Fact.class, t -> t.getObjectType().equals("PriceAdjustment"));
        LOG.info("PriceAdjustment(s): {}", results);
        session.dispose();
        pressEnterKeyToContinue("RULES EVALUATED, pending to dispose KieContainer");
        kieContainer.dispose();
    }

    public static KieModule deployJarIntoRepository(KieServices ks, byte[] jar) {
        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        KieModule km = ks.getRepository().addKieModule(jarRes);
        return km;
    }

    private static void pressEnterKeyToContinue(String message) {
        System.out.print(message + ". "); 
        System.out.println("Press Enter key to continue..."); // deliberate on sysout
        s.nextLine();
    }
}