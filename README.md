basic experimental project to test several different strategies for a "orchestrator" of Kjar/kiecontainer/drools-based-evaluators-apps.

# Developer log

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
