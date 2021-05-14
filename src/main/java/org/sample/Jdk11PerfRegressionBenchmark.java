/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package org.sample;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class Jdk11PerfRegressionBenchmark {
    private Global global;
    @Param("") String jvm;

    static abstract class SymbolTable {
        class Phase {
            public Phase(int id) {
                this.id = id;
            }

            int id;
        }
        private Phase ph;
        private int per;
        abstract int currentRunId();
        int period(int rid, int pid) {
            return (rid << 8) + pid;
        }
        void setPhase(Phase p) {
            this.ph = p;
            this.per = period(currentRunId(), p.id);
        }
    }
    static class Global extends SymbolTable {
        private int curRunId;
        Run curRun;
        @Override
        int currentRunId() {
            return curRunId;
        }
        class Run {
            public Run() {
                curRunId += 1;
            }

            Phase p1 = new Phase(0);
            Phase p2 = new Phase(1);
        }
        Run newRun() {
            curRun = new Run();
            return curRun;
        }
    }
    @Setup public void setup() {
        Global global = new Global();
        global.newRun();
        this.global = global;
    }
    @Benchmark public void setPhase() {
        global.setPhase(global.curRun.p1);
        global.setPhase(global.curRun.p2);
    }
    public static void main(String[] args) throws RunnerException {
        ArrayList<String> argsList = new ArrayList<>();
        argsList.addAll(Arrays.asList(args));
        String include = argsList.remove(0);

        Options baseOpts = new OptionsBuilder()
                .include(Jdk11PerfRegressionBenchmark.class.getName() + "." + include)
                .warmupTime(TimeValue.milliseconds(200))
                .measurementTime(TimeValue.milliseconds(200))
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(2)
                .verbosity(VerboseMode.SILENT)
                .build();
        ArrayList<RunResult> results = new ArrayList<>();
        for (String jvm : argsList) {
            Options theseOpts = new OptionsBuilder()
                    .parent(baseOpts).jvm(jvm).param("jvm", jvm.replaceAll("(^.*@|/Contents.*$)", "")).build();
            Runner runner = new Runner(theseOpts);
            results.addAll(runner.run());
        }
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out).writeOut(results);
        ResultFormatFactory.getInstance(ResultFormatType.JSON, "result.json").writeOut(results);
    }
}
