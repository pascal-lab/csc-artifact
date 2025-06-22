/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta;

import pascal.taie.Main;
import pascal.taie.util.collection.Maps;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command
public class BenchmarkRunner {

    private static final String BENCHMARK_HOME = "java-benchmarks";

    private static final String BENCHMARK_INFO = "java-benchmarks/benchmark-info.yml";

    private static final Map<String, BenchmarkInfo> benchmarkInfos =
            BenchmarkInfo.load(BENCHMARK_INFO);

    @CommandLine.Option(names = "-cs", defaultValue = "ci")
    private String cs;

    @CommandLine.Option(names = "-java", defaultValue = "0")
    private int jdk;

    @CommandLine.Option(names = "-advanced", defaultValue = "null")
    private String advanced;

    @CommandLine.Parameters
    private List<String> benchmarks;

    public static void main(String[] args) {
        BenchmarkRunner runner = CommandLine.populateCommand(new BenchmarkRunner(), args);
        runner.runAll();
    }

    private void runAll() {
        if (benchmarks == null) {
            throw new IllegalArgumentException("benchmarks are not given");
        }
        benchmarks.forEach(this::run);
    }

    private void run(String benchmark) {
        System.out.println("\nAnalyzing " + benchmark);
        Main.main(composeArgs(benchmark));
    }

    private String[] composeArgs(String benchmark) {
        BenchmarkInfo info = benchmarkInfos.get(benchmark);
        List<String> args = new ArrayList<>();
        int jdkVersion = jdk != 0 ? jdk : info.jdk();
        Collections.addAll(args,
                "-java", Integer.toString(jdkVersion),
                "-cp", buildClassPath(info.apps())
                        + File.pathSeparator
                        + buildClassPath(info.libs()),
                "--pre-build-ir",
                "-m", info.main());
        if (info.allowPhantom()) {
            args.add("--allow-phantom");
        }
        Map<String, String> ptaArgs = Maps.newMap();
        ptaArgs.put("merge-string-constants", "true");
        ptaArgs.put("merge-string-objects", "false");
        ptaArgs.put("cs", cs);
        ptaArgs.put("reflection-log", new File(BENCHMARK_HOME, info.reflectionLog()).toString());
        if ("csc".equals(advanced)) {
            ptaArgs.put("algorithm", "correlation");
        } else {
            ptaArgs.put("algorithm", "origin");
            ptaArgs.put("advanced", advanced);
        }
        Collections.addAll(args,
                "-a", "class-dumper",
                "-a", "pta=" + ptaArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";")),
                "-a", "avg-pts",
                "-a", "may-fail-cast",
                "-a", "poly-call",
                "-a", "shared-alloc",
                "-a", "may-alias-pair");
        System.out.println(args);
        return args.toArray(new String[0]);
    }

    private String buildClassPath(List<String> paths) {
        return paths.stream()
                .map(this::extendCP)
                .flatMap(List::stream)
                .collect(Collectors.joining(File.pathSeparator));
    }

    private List<String> extendCP(String path) {
        File file = new File(BENCHMARK_HOME, path);
        List<String> paths = new ArrayList<>();
        if (isJar(file)) {
            paths.add(file.toString());
        } else if (file.isDirectory()) {
            paths.add(file.toString());
            for (File item : Objects.requireNonNull(file.listFiles())) {
                if (isJar(item)) {
                    paths.add(item.toString());
                }
            }
        } else {
            throw new RuntimeException(path + " is neither a directory nor a JAR");
        }
        return paths;
    }

    private static boolean isJar(File file) {
        return file.getName().endsWith(".jar");
    }
}
