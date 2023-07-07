package org.acme.drools.demo.demo20230606_asquarkus;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.kie.api.KieBase;
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.KieSession;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class DroolsSingleton {
    @Inject
    KieRuntimeBuilder kieRuntime;
    private KieBase kieBase;

    @PostConstruct
    public void init() {
        this.kieBase = kieRuntime.getKieBase();
        System.out.println("init done");
        Log.info("init done");
    }

    void onStart(@Observes StartupEvent ev) {               
        Log.info("Quarkus: the application is starting...");
    }

    void onInitializedAppScoped(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        Log.info("CDI spec: Observes @Initialized(ApplicationScoped.class) invoked...");
    }

    public KieSession newKieSession() {
        return kieBase.newKieSession();
    }
}
