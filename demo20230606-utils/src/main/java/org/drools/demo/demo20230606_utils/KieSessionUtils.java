package org.drools.demo.demo20230606_utils;

import java.util.Collection;
import java.util.function.Predicate;

import org.kie.api.runtime.KieSession;

public class KieSessionUtils {
    public static <F> Collection<F> getFactsHaving(KieSession session, Class<F> ofType, Predicate<F> having) {
        @SuppressWarnings("unchecked") // deliberate and constrained by generic erasure.
        var result =(Collection<F>) session.getObjects(object -> ( ofType.isInstance(object) && having.test((F) object) ) );
        return result;
    }
}
