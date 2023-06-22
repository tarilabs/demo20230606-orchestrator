package org.drools.demo.demo20230606_kjar;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.drools.demo.demo20230606_datamodel.Fact;
import org.drools.demo.demo20230606_utils.KieSessionUtils;
import org.drools.model.codegen.ExecutableModelProject;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UsingKieBuilderTest {
    static final Logger LOG = LoggerFactory.getLogger(UsingKieBuilderTest.class);
    
    @Test
    public void test() throws Exception {
        KieServices ks = KieServices.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/" + "org/drools/demo/demo20230606_kjar" + "/rules.drl",
                  ks.getResources().newInputStreamResource(this.getClass().getClassLoader().getResourceAsStream("org/drools/demo/demo20230606_kjar" + "/rules.drl")));
        ReleaseId releaseId = ks.newReleaseId("org.drools.demo", UUID.randomUUID().toString(), "0.1-SNAPSHOT");
        kfs.generateAndWritePomXML(releaseId);
        ks.newKieBuilder(kfs).buildAll(ExecutableModelProject.class);
        KieContainer kContainer = ks.newKieContainer(releaseId);

        LOG.info("Creating kieBase");
        KieBase kieBase = kContainer.getKieBase();

        LOG.info("There should be rules: ");
        for ( KiePackage kp : kieBase.getKiePackages() ) {
            for (Rule rule : kp.getRules()) {
                LOG.info("kp " + kp + " rule " + rule.getName());
            }
        }

        LOG.info("Creating kieSession");
        KieSession session = kieBase.newKieSession();

        final var JSON = Files.readString(Paths.get(UsingKieBuilderTest.class.getResource("/test1.json").toURI()));
        try {
            LOG.info("Now running data");
            List<Fact> unmarshal = new ObjectMapper()
                .readerFor(new TypeReference<List<Fact>>() {})
                .readValue(JSON);
            unmarshal.forEach(x -> LOG.info("{}", x));
            unmarshal.forEach(session::insert);
            session.fireAllRules();

            LOG.info("Final checks");
            Collection<Fact> results = KieSessionUtils.getFactsHaving(session, Fact.class, t -> t.getObjectType().equals("PriceAdjustment"));
            Assertions.assertThat(results)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("id", "abc-123")
                .hasFieldOrPropertyWithValue("fields.Source", "Promotion")
                .hasFieldOrPropertyWithValue("fields.Value", new BigDecimal("-1.23"))
                ;
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("failed assumptions in test", e);
        } finally {
            session.dispose();
            kContainer.dispose();
        }
    }
}
