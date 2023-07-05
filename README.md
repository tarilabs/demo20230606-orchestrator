basic experimental project to test several different strategies for a "orchestrator" of Kjar/kiecontainer/drools-based-evaluators-apps.

# Developer log

## 2023-07-05 investigating...

WithOUT using `kie-ci`:

https://github.com/tarilabs/demo20230606-orchestrator/blob/1252beb23477f859abb18b0607a344153eaf340a/demo20230606-kiecontainerorchestrator/pom.xml#L47-L50

and so when KJARs manually loaded into the KieRepository (see prev log entry):

https://github.com/tarilabs/demo20230606-orchestrator/blob/1252beb23477f859abb18b0607a344153eaf340a/demo20230606-kiecontainerorchestrator/src/main/java/org/drools/demo/demo20230606_kiecontainerorchestrator/AppPreloadThenMultiJar.java#L27-L37

Applied the following dirty-hack on Drools:

```diff
diff --git a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/InternalKieModule.java b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/InternalKieModule.java
index 2978769120..bc78d3745d 100644
--- a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/InternalKieModule.java
+++ b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/InternalKieModule.java
@@ -127,6 +127,13 @@ public interface InternalKieModule extends KieModule, Serializable {
     default void afterKieBaseCreationUpdate(String name, InternalKnowledgeBase kBase) { }
 
     ClassLoader getModuleClassLoader();
+    
+    default void addUsedBy(KieProject project) {
+        return; // do nothing by default.
+    }
+    default void removeUsedBy(KieProject project) {
+        return; // do nothing by default.
+    }
 
     default ResultsImpl build() {
         BuildContext buildContext = new BuildContext();
diff --git a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieContainerImpl.java b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieContainerImpl.java
index 2e37c9db8f..d55f7b9d55 100644
--- a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieContainerImpl.java
+++ b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieContainerImpl.java
@@ -742,6 +742,11 @@ public class KieContainerImpl
         }
         kSessions.clear();
         statelessKSessions.clear();
+        
+        if (kProject instanceof KieModuleKieProject) {
+            KieModuleKieProject kieModuleKieProject = (KieModuleKieProject) kProject;
+            kieModuleKieProject.getInternalKieModule().removeUsedBy(kProject);
+        }
 
         if ( isMBeanOptionEnabled() ) {
             for (CBSKey c : cbskeys) {
diff --git a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieModuleKieProject.java b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieModuleKieProject.java
index 445b33b47e..4795212555 100644
--- a/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieModuleKieProject.java
+++ b/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieModuleKieProject.java
@@ -53,6 +53,7 @@ public class KieModuleKieProject extends AbstractKieProject {
     
     public KieModuleKieProject(InternalKieModule kieModule, ClassLoader parent) {
         this.kieModule = kieModule;
+        this.kieModule.addUsedBy(this);
         this.cl = kieModule.createModuleClassLoader( parent );
     }
 
diff --git a/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java b/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java
index 36e9c5f088..0cdf87a95e 100644
--- a/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java
+++ b/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java
@@ -122,6 +122,7 @@ public class CanonicalKieModule implements InternalKieModule {
 
     private final InternalKieModule internalKieModule;
     private final ConcurrentMap<String, CanonicalKiePackages> pkgsInKbase = new ConcurrentHashMap<>();
+    private final Set<KieProject> usedBy = ConcurrentHashMap.newKeySet(); 
     private final Map<String, Model> models = new HashMap<>();
     private Collection<String> ruleClassesNames;
     private boolean incrementalUpdate = false;
@@ -146,6 +147,21 @@ public class CanonicalKieModule implements InternalKieModule {
         this.ruleClassesNames = ruleClassesNames;
     }
 
+    @Override
+    public void addUsedBy(KieProject project) {
+        usedBy.add(project);
+    }
+
+    @Override
+    public void removeUsedBy(KieProject project) {
+        usedBy.remove(project);
+        if (usedBy.isEmpty()) {
+            synchronized (this) {
+                setModuleClassLoader(null);
+            }
+        }
+    }
+
     private static boolean areModelVersionsCompatible(String runtimeVersion, String compileVersion) {
         return true;
     }

```

as a result,
when all kie containers are disposed,
there are no longer exec model classes present:

![](./images/Screenshot%202023-07-05%20at%2016.35.14.png)

Some consideration for the follow-up continued investigations.

### observation 1

when creating a KieContainer,
regardless of later instantiating a KieProject,
the KieModule classloader is instantiated, here

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-compiler/src/main/java/org/drools/compiler/kie/builder/impl/KieServicesImpl.java#L194-L196

as a result, if the KieModule is in the KieRepository cache, the kiemodule internal classloader will always keep the exec model class definitions.

### observation 2

here

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java#L236

the (canonical)kiemodule will swap its own classloader
with the classloader from the kieproject.

I'm not sure I follow the rationale behind this.

Observed as I tried to workaround by creating the KieContainer manually:

https://github.com/tarilabs/demo20230606-orchestrator/blob/1252beb23477f859abb18b0607a344153eaf340a/demo20230606-kiecontainerorchestrator/src/main/java/org/drools/demo/demo20230606_kiecontainerorchestrator/AppPreloadThenMultiJarCustomWay.java#L54-L57

but this piece of code later vanified my efforts.

### observation 3

there is a classloader reference in the kiepackages:

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java#L124

then 

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKiePackages.java#L26

then

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-base/src/main/java/org/drools/base/definitions/InternalKnowledgePackage.java#L143-L145

the TypeResolver was ClassTypeResolver and this:

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-util/src/main/java/org/drools/util/ClassTypeResolver.java#L37

this classloader contained the class definitions of the exec model.

Hence why the dirty hack on Drools uses this method passing null:

https://github.com/kiegroup/drools/blob/f8b6125840794a360ec453848bdc6b48e6702962/drools-model/drools-model-compiler/src/main/java/org/drools/modelcompiler/CanonicalKieModule.java#L409-L413

so clearing out the pkgsInKbase.

## 2023-06-21 investigating...

When using `kie-ci` all seems to be working as expected.
It is to be noted by this commit this demonstrator does not include: https://github.com/kiegroup/drools/commit/10ea2178ab4e301e10df3777b3d6712d4935b5a5 which would be helpful to pay for extra-overhead when resolving transitive dependencies of the kjars (out of scope of this investigation).

![](./images/Screenshot%202023-06-21%20at%2008.54.24.png)

However when KJARs manually loaded into the KieRepository, there seems to appear some strange behaviour:

![](./images/Screenshot%202023-06-21%20at%2009.49.13.png)

Focusing on this last scenario and investigating...

## 2023-06-07 no metaspace/permgen issue found

As expected good `KieContainer` behaviour found, even when using exec model on Drools v8.

Here is a _Heapdump_ just after `KieSession` is disposed, but to be noted the `KieContainer` is NOT yet disposed:

![](/images/Screenshot%202023-06-07%20at%2011.38.45.png)

We can notice (as we could expect) that Exec model rules are still in metaspace (as the KieContainer is still valid).

It is to be noted _NOT all_ of the classes for the Exec model are present, as remains only those needed for the rete/phreak execution (eg: materialized lambda evaluated in the nodes).

As we proceed to the next part of execution:
- fully disposed the `KieContainer` (not only the `KieSession`)
- given a chance to garbage collector to run (so to evict class def data from metaspace)

We can notice:

![](/images/Screenshot%202023-06-07%20at%2011.39.19.png)

as expected no exec model classes are still present. Please notice there is no filter for "filter-out classes with no instances" and in fact the `Fact` class definition highlighted has zero instances (as expected per the code).

Also, using exec model there is no need for dynamic Java classes which could be a problem as that tends _to pollute permgen/metaspace with class definitions for all knowledge bases for the different KJAR_: in this case the exec model classes are evicted when the `KieContainer` instance is properly disposed as we would expect.

It is important to note that beyond the call to `kieContainer.dispose()` the `kieContainer` was really out-of-scope in the moment this screenshot and Heapdump was taken, so effectively the container had an opportunity to be garbage collected, along with the class definitions from Exec model which are no longer needed.
