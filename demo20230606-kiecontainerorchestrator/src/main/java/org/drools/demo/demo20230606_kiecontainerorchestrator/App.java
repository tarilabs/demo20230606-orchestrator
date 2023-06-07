package org.drools.demo.demo20230606_kiecontainerorchestrator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.drools.demo.demo20230606_datamodel.Fact;
import org.drools.demo.demo20230606_utils.KieSessionUtils;
import org.kie.api.KieServices;
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

    public static void main(String[] args) throws Exception {
        LOG.info("App starting.");
        JSON = Files.readString(Paths.get(App.class.getResource("/test1.json").toURI()));
        KieServices ks = KieServices.get();
        do {
            doOnce(ks);
            System.gc();
            pressEnterKeyToContinue("all done, pending for a new loop");
        } while(true);
    }

    private static void doOnce(KieServices ks) throws Exception {
        KieContainer kieContainer = ks.newKieContainer(ks.newReleaseId("org.drools.demo", "demo20230606-kjar", "1.0-SNAPSHOT"));
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

    private static void pressEnterKeyToContinue(String message) {
        System.out.print(message + ". "); 
        System.out.println("Press Enter key to continue..."); // deliberate on sysout
        s.nextLine();
    }
}