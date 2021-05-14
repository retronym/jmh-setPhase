# JDK 11+ performance regression


Benchmark modelled on hot method in scalac, `SymbolTable.phase_=`


```
$ mvn clean install && java -cp /Users/jz/code/test/target/benchmarks.jar org.sample.Jdk11PerfRegressionBenchmark setPhase /Users/jz/.jabba/jdk/adopt@1.8.0-272/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.9.0-0/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.10.0-2/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.11.0-9/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.15.0-1/Contents/Home/bin/java /Users/jz/.jabba/jdk/adopt@1.16.0-1/Contents/Home/bin/java
...
Benchmark                                  (jvm)  Mode  Cnt  Score   Error  Units
Jdk11PerfRegressionBenchmark.setPhase  1.8.0-272  avgt   10  2.202 ± 0.039  ns/op
Jdk11PerfRegressionBenchmark.setPhase    1.9.0-0  avgt   10  3.072 ± 0.039  ns/op
Jdk11PerfRegressionBenchmark.setPhase   1.10.0-2  avgt   10  3.824 ± 0.036  ns/op
Jdk11PerfRegressionBenchmark.setPhase   1.11.0-9  avgt   10  3.542 ± 0.036  ns/op
Jdk11PerfRegressionBenchmark.setPhase   1.15.0-1  avgt   10  3.066 ± 0.027  ns/op
Jdk11PerfRegressionBenchmark.setPhase   1.16.0-1  avgt   10  3.058 ± 0.012  ns/op
```