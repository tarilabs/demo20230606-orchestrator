package org.acme.drools.demo.demo20230606_asquarkus;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.drools.demo.demo20230606_datamodel.Fact;
import org.kie.api.runtime.KieSession;

@Path("/drools")
public class DroolsResource {
    
    @Inject
    DroolsSingleton drools;

    @POST
    public Collection<? extends Object> evaluate(List<Fact> facts) {
        KieSession session = drools.newKieSession();
        facts.forEach(session::insert);
        session.fireAllRules();
        return session.getObjects();
    }
}
