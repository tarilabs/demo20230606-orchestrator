package org.drools.demo.demo20230606_kiecontainerorchestrator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.drools.demo.demo20230606_datamodel.Fact;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class AppMultiJar {
    public static Logger LOG = LoggerFactory.getLogger(AppMultiJar.class);
    static Scanner s = new Scanner(System.in);
    static String kjarName = "demo20230606-2000-rules-kjar-";
    
    public static void main(String[] args) throws Exception {
        LOG.info("App starting.");
        String jsonFilename = "test1.json";

        // accept the JSON file name as an argument
        if(args.length == 1) {
            jsonFilename = args[0];
        } else if(args.length == 2) {
            jsonFilename = args[0];
            kjarName = args[1];
        } else if(args.length > 2) {
            System.out.println("Usage: App [jsonFileName kjarNamePrefix]");
            System.exit(1);
        }

        pressEnterKeyToContinue("App started.  Attach debugger?");

        // read the JSON payload from disk
        final var JSON = Files.readString(Paths.get(AppMultiJar.class.getResource("/" + jsonFilename).toURI()));

        // prep Drools
        int i = 0;
        do {
            doOnce(i, JSON);
        } while(i++ < 1);

        System.gc();
        pressEnterKeyToContinue("Nothing more to do, app will exit");
    }

    private static void doOnce(int i, final String JSON) throws Exception {
        LOG.info("Loop count: " + i);
        KieServices ks = KieServices.get();
        ReleaseId releaseId = ks.newReleaseId("org.drools.demo", kjarName + i, "1.0-SNAPSHOT");
        KieContainer kieContainer = ks.newKieContainer(releaseId);
        // create a new KieSession
        KieSession session = kieContainer.newKieSession();

        // parse the JSON paylaod and insert the facts
        List<Fact> unmarshal = new ObjectMapper()
            .readerFor(new TypeReference<List<Fact>>() {})
            .readValue(JSON);
        unmarshal.forEach(session::insert);

        // fire the rules
        LOG.info("Rules fired: " + session.fireAllRules());

        // release the KieSession
        session.dispose();

        // release the KieContainer
        kieContainer.dispose();
        System.gc();
    }

    private static void pressEnterKeyToContinue(String message) {
        System.out.print(message + ". "); 
        System.out.println("Press Enter key to continue..."); // deliberate on sysout
        s.nextLine();
    }
}