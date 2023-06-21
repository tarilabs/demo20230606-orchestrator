package org.drools.demo.demo20230606_kjar;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.drools.demo.demo20230606_datamodel.Fact;
import org.drools.demo.demo20230606_utils.KieSessionUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RulesTest {
    static final Logger LOG = LoggerFactory.getLogger(RulesTest.class);
    
    @Test
    public void test() throws Exception {
        KieServices ks = KieServices.get();
        KieContainer kContainer = ks.getKieClasspathContainer();

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
        session.addEventListener(new DebugRuleRuntimeEventListener());
        session.addEventListener(new DebugAgendaEventListener());

        final var JSON = Files.readString(Paths.get(RulesTest.class.getResource("/100-line-items.json").toURI()));
        try {
            // LOG.info("Populating globals");
            // Set<String> check = new HashSet<String>();
            // session.setGlobal("controlSet", check);

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
                .hasSize(100)
                .first()
                .hasFieldOrPropertyWithValue("fields.Source", "Promotion")
                ;
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("failed assumptions in test", e);
        } finally {
            session.dispose();
            kContainer.dispose();
        }
    }

    @Disabled("TODO inconsistent test data pending for clarifications")
    @Test
    public void testKiwiPayload2023060() throws Exception {
        KieServices ks = KieServices.get();
        KieContainer kContainer = ks.getKieClasspathContainer();

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
        session.addEventListener(new DebugRuleRuntimeEventListener());
        session.addEventListener(new DebugAgendaEventListener());

        final var JSON = Files.readString(Paths.get(RulesTest.class.getResource("/kiwipayload_gen20230606.json").toURI()));
        LOG.info("Now running data");
        List<Fact> unmarshal = new ObjectMapper()
            .readerFor(new TypeReference<List<Fact>>() {})
            .readValue(JSON);
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
        session.dispose();
        kContainer.dispose();
    }
}
