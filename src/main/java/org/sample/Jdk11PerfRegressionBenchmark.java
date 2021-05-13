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
}
