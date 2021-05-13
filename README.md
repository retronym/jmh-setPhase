# JDK 11+ performance regression


Benchmark modelled on hot method in scalac, `SymbolTable.phase_=`


```
% (jabba use adopt@1.8.0-272 && mvn clean install)
% for jdk in adopt@1.8.0-272 adopt@1.11.0-9; do (java -jar /Users/jz/code/test/target/benchmarks.jar -jvm "$(jabba use $jdk; which java)"); done
```

```
# VM version: JDK 1.8.0_272, OpenJDK 64-Bit Server VM, 25.272-b10
# VM invoker: /Users/jz/.jabba/jdk/adopt@1.8.0-272/Contents/Home/bin/java
Benchmark                              Mode  Cnt  Score   Error  Units
Jdk11PerfRegressionBenchmark.setPhase  avgt   20  2.202 ± 0.018  ns/op
```

```
# VM version: JDK 11.0.9, OpenJDK 64-Bit Server VM, 11.0.9+11
# VM invoker: /Users/jz/.jabba/jdk/adopt@1.11.0-9/Contents/Home/bin/java
Benchmark                              Mode  Cnt  Score   Error  Units
Jdk11PerfRegressionBenchmark.setPhase  avgt   20  3.591 ± 0.031  ns/op
```