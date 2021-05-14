# JDK 11+ performance regression


Benchmark modelled on hot method in scalac, `SymbolTable.phase_=`


```
$ mvn clean install && java -cp /Users/jz/code/test/target/benchmarks.jar org.sample.Jdk11PerfRegressionBenchmark 'setPhase.*' /Users/jz/.jabba/jdk/adopt@1.8.0-272/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.9.0-0/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.10.0-2/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.11.0-9/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.15.0-1/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.16.0-1/Contents/Home/bin/java
...
Benchmark                                   (jvm)  Mode  Cnt  Score   Error  Units
Jdk11PerfRegressionBenchmark.setPhase   1.8.0-272  avgt   10  2.174 ± 0.018  ns/op
Jdk11PerfRegressionBenchmark.setPhase2  1.8.0-272  avgt   10  2.158 ± 0.014  ns/op
Jdk11PerfRegressionBenchmark.setPhase3  1.8.0-272  avgt   10  1.266 ± 0.007  ns/op
Jdk11PerfRegressionBenchmark.setPhase     1.9.0-0  avgt   10  3.074 ± 0.157  ns/op
Jdk11PerfRegressionBenchmark.setPhase2    1.9.0-0  avgt   10  2.582 ± 0.027  ns/op
Jdk11PerfRegressionBenchmark.setPhase3    1.9.0-0  avgt   10  2.191 ± 0.023  ns/op
Jdk11PerfRegressionBenchmark.setPhase    1.10.0-2  avgt   10  3.840 ± 0.092  ns/op
Jdk11PerfRegressionBenchmark.setPhase2   1.10.0-2  avgt   10  2.944 ± 0.091  ns/op
Jdk11PerfRegressionBenchmark.setPhase3   1.10.0-2  avgt   10  2.517 ± 0.066  ns/op
Jdk11PerfRegressionBenchmark.setPhase    1.11.0-9  avgt   10  3.593 ± 0.077  ns/op
Jdk11PerfRegressionBenchmark.setPhase2   1.11.0-9  avgt   10  3.000 ± 0.283  ns/op
Jdk11PerfRegressionBenchmark.setPhase3   1.11.0-9  avgt   10  2.483 ± 0.038  ns/op
Jdk11PerfRegressionBenchmark.setPhase    1.15.0-1  avgt   10  3.088 ± 0.045  ns/op
Jdk11PerfRegressionBenchmark.setPhase2   1.15.0-1  avgt   10  2.706 ± 0.091  ns/op
Jdk11PerfRegressionBenchmark.setPhase3   1.15.0-1  avgt   10  2.534 ± 0.058  ns/op
Jdk11PerfRegressionBenchmark.setPhase    1.16.0-1  avgt   10  3.071 ± 0.042  ns/op
Jdk11PerfRegressionBenchmark.setPhase2   1.16.0-1  avgt   10  2.624 ± 0.008  ns/op
Jdk11PerfRegressionBenchmark.setPhase3   1.16.0-1  avgt   10  2.495 ± 0.021  ns/op
```

[Graphed](https://jmh.morethan.io/?gist=687f7e0f7325cd5dba6c088c3ae46325)

## Observations

### Virtual call cost in phase_=

I started this investigation because I noticed async-profiler was reporting a `vtable stub`
cost in `phase_=` in compiler profiles. 

```
Stack Trace	Count	Percentage
.vtable stub():0 [BCI: 0]	1054	5.49%
  Symbols$Symbol scala.reflect.internal.Types.rebind(...):0 [BCI: 0]	72	0.375%
  void scala.reflect.internal.SymbolTable.phase_$eq(...):254 [BCI: 2]	45	0.234%
  List scala.reflect.internal.Symbols$Symbol.completeTypeParams$1():1768 [BCI: 1]	24	0.125%
```

I haven't reproduced this here. I think the reason
is that `def currentRunId = curRunId` (in `Global`) is not a field accessor, it delegates to
the accessor method of `var curRunId`.
    
```
-  public currentRunId()I
-    ALOAD 0
-    INVOKEVIRTUAL scala/tools/nsc/Global.scala$tools$nsc$Global$$curRunId ()I
-    IRETURN
-    MAXSTACK = 1
-    MAXLOCALS = 1
```

So even if `currentRunId` can be devirtualized and inlined by the JIT, it might not be if
if it is being inlined into a method that hits a JIT threshold (either inlining depth or size).

Changing `curRunId` to `private[this]` and re-running the profiler could answer this question.
We could also gather use hotspot compilation logs and JITWatch to find the exact reason for
insufficient inlining.

### Performance varies across JDKs, 8 is fastest

JDK 8 wins on all variations of the benchmark. Why?

We could use JITWatch to compare the generated assembly and JIT journal. Using the smallest
benchmark that still shows a significant performance gap makes analysis a little easier.
Debug builds of the JDK have additional tools for tracing JIT activity:
[IdealGraphVisualizer](https://wiki.openjdk.java.net/display/HotSpot/IdealGraphVisualizer)

Otherwise nerd sniping via a well crafted stack overflow question might do the trick!

Keep in mind that the microbenchmark might be poorly designed in a way that lets JIT do
too good a job (inlining something that is not static in the real application).

### Opportuninty to hoist some work out of `phase_=`

This method computes the shifted current run ID repeatedly `currentRunID << 8`. We could just
precompute this into a field of global.

We could go even further and precomputed `currentRunID << 8 + ph.id` in a field of `Phase` beside `id`.
This works well for all phases except `NoPhase` and `SomePhase` which are singletons and shared
between different instances of `Global`. Maybe these are never seen in `phase_=` anyway?
