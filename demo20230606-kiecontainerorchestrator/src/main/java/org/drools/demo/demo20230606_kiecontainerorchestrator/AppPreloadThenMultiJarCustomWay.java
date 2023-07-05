package org.drools.demo.demo20230606_kiecontainerorchestrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.InternalKieScanner;
import org.drools.compiler.kie.builder.impl.KieContainerImpl;
import org.drools.compiler.kie.builder.impl.KieModuleKieProject;
import org.drools.compiler.kie.builder.impl.KieProject;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.KieScannerFactoryService;
import org.kie.api.builder.ReleaseId;
import org.kie.api.internal.utils.KieService;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * DEPRECATED -- this was helpful for investigations,
 * but turned out another strategy with KieModule/KieProject does not require this workaround.
 */
@Deprecated
public class AppPreloadThenMultiJarCustomWay {
    public static Logger LOG = LoggerFactory.getLogger(AppPreloadThenMultiJarCustomWay.class);
    @Deprecated
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
        for (int i = 0; i <= 1; i++) {
            LOG.debug("Loop count: {}", i);
            Path path = Path.of("../demo20230606-2000-rules-kjar/jar-dir/", kjarBaseName + i + "-1.0-SNAPSHOT.jar");
            LOG.info("Loop path: {}", path);
            Resource jarRes = ks.getResources().newByteArrayResource(Files.readAllBytes(path));
            KieModule km = ks.getRepository().addKieModule(jarRes);
            LOG.info("loaded manually: {}", km.getReleaseId());
        }
        
        Function<ReleaseId, KieContainer> strategy = (releaseId) -> {
            KieModule kieModule = ks.getRepository().getKieModule(releaseId);
            KieProject kProject = new KieModuleKieProject( (InternalKieModule) kieModule );
            return new KieContainerImpl( kProject, ks.getRepository(), releaseId );
        };
        AppMultiJar.strategy = strategy;
        LOG.warn("applied CUSTOM kiecontainer strategy: {}", strategy);
        
        AppMultiJar.main(args);
    }
}
