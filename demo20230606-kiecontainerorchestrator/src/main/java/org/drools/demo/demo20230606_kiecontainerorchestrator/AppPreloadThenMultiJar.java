package org.drools.demo.demo20230606_kiecontainerorchestrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.drools.compiler.kie.builder.impl.InternalKieScanner;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.KieScannerFactoryService;
import org.kie.api.internal.utils.KieService;
import org.kie.api.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppPreloadThenMultiJar {
    public static Logger LOG = LoggerFactory.getLogger(AppPreloadThenMultiJar.class);
    public static void main(String[] args) throws Exception {
        KieScannerFactoryService scannerFactoryService = KieService.load(KieScannerFactoryService.class);
        if (scannerFactoryService != null) {
            KieScanner scanner = scannerFactoryService.newKieScanner();
            String scannerName = scanner.getClass().getSimpleName();
            LOG.error("seems kie-ci is on classpath, kieScanner is {}", scannerName);
            throw new IllegalStateException("seems kie-ci is on classpath");
        }
        LOG.info("seems kie-ci is not on classpath, proceeding...");

        KieServices ks = KieServices.get();
        String kjarBaseName = "demo20230606-2000-rules-kjar-";
        for (int i = 0; i <= 10; i++) {
            LOG.debug("Loop count: {}", i);
            Path path = Path.of("../demo20230606-2000-rules-kjar/jar-dir/", kjarBaseName + i + "-1.0-SNAPSHOT.jar");
            LOG.info("Loop path: {}", path);
            Resource jarRes = ks.getResources().newByteArrayResource(Files.readAllBytes(path));
            KieModule km = ks.getRepository().addKieModule(jarRes);
            LOG.info("loaded manually: {}", km.getReleaseId());
        }
        AppMultiJar.main(args);
    }
}
