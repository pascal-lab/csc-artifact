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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.core.cs.element.MapBasedCSManager;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelectorFactory;
import pascal.taie.analysis.pta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.CutShortcutSolver;
import pascal.taie.analysis.pta.core.solver.DefaultSolver;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.AnalysisTimer;
import pascal.taie.analysis.pta.plugin.ClassInitializer;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.analysis.pta.plugin.field.FieldAccessHandler;
import pascal.taie.analysis.pta.plugin.container.MakeDefaultContainerConfig;
import pascal.taie.analysis.pta.plugin.container.ContainerAccessHandler;
import pascal.taie.analysis.pta.plugin.container.ContainerConfig;
import pascal.taie.analysis.pta.plugin.localflow.LocalFlowHandler;
import pascal.taie.analysis.pta.plugin.natives.NativeModeller;
import pascal.taie.analysis.pta.plugin.EntryPointHandler;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.ReferenceHandler;
import pascal.taie.analysis.pta.plugin.ResultProcessor;
import pascal.taie.analysis.pta.plugin.ThreadHandler;
import pascal.taie.analysis.pta.plugin.exception.ExceptionAnalysis;
import pascal.taie.analysis.pta.plugin.invokedynamic.InvokeDynamicAnalysis;
import pascal.taie.analysis.pta.plugin.invokedynamic.LambdaAnalysis;
import pascal.taie.analysis.pta.plugin.reflection.ReflectionAnalysis;
import pascal.taie.analysis.pta.plugin.taint.TaintAnalysis;
import pascal.taie.analysis.pta.toolkit.CollectionMethods;
import pascal.taie.analysis.pta.toolkit.mahjong.Mahjong;
import pascal.taie.analysis.pta.toolkit.scaler.Scaler;
import pascal.taie.analysis.pta.toolkit.zipper.Zipper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;

import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import pascal.taie.config.ConfigException;
import pascal.taie.util.AnalysisException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class PointerAnalysis extends ProgramAnalysis<PointerAnalysisResult> {

    public static final String ID = "pta";

    private static final Logger logger = LogManager.getLogger(PointerAnalysis.class);

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PointerAnalysisResult analyze() {
        AnalysisOptions options = getOptions();
        HeapModel heapModel = new AllocationSiteBasedModel(options);
        ContextSelector selector = null;
        String advanced = options.getString("advanced");
        String cs = options.getString("cs");
        String algorithm = options.getString("algorithm");
        String programID = options.getString("program");
        logger.info("Pointer Analysis starts ...");
        if (advanced != null) {
            if (advanced.equals("collection")) {
                selector = ContextSelectorFactory.makeSelectiveSelector(cs,
                        new CollectionMethods(World.get().getClassHierarchy()).get());
            } else {
                // run context-insensitive analysis as pre-analysis
                logger.info("Running pre-analysis of {} ...", advanced);
                PointerAnalysisResult preResult = runAnalysis(programID, heapModel,
                        ContextSelectorFactory.makeCISelector(), algorithm);

                if (advanced.startsWith("scaler")) {
                    selector = Timer.runAndCount(() -> ContextSelectorFactory
                                    .makeGuidedSelector(Scaler.run(preResult, advanced)),
                            "Scaler", Level.INFO);
                } else if (advanced.startsWith("zipper")) {
                    selector = Timer.runAndCount(() -> ContextSelectorFactory
                                    .makeSelectiveSelector(cs, Zipper.run(preResult, advanced)),
                            "Zipper", Level.INFO);
                    // This is stupid:
                    // dumpMethods("../output/tai-e/" + programID + "-zippere-selected-methods.txt", World.get().getResult("zipper-methods"));
                    logger.info("Running main analysis of {} ...", advanced);
                } else if (advanced.equals("mahjong")) {
                    heapModel = Timer.runAndCount(() -> Mahjong.run(preResult, options),
                            "Mahjong", Level.INFO);
                } else {
                    throw new IllegalArgumentException(
                            "Illegal advanced analysis argument: " + advanced);
                }
            }
        }
        if (selector == null) {
            // origin
            selector = ContextSelectorFactory.makePlainSelector(cs);
        }
        return runAnalysis(programID, heapModel, selector, algorithm);
    }

    protected PointerAnalysisResult runAnalysis(String programID, HeapModel heapModel,
                                              ContextSelector selector, String algorithm) {
        AnalysisOptions options = getOptions();
        Solver solver;
        if (algorithm.equals("origin")) {
            solver = new DefaultSolver(options,
                heapModel, selector, new MapBasedCSManager());
            // The initialization of some Plugins may read the fields in solver,
            // e.g., contextSelector or csManager, thus we initialize Plugins
            // after setting all other fields of solver.
        }
        else if (algorithm.equals("correlation")) {
            solver = new CutShortcutSolver(options,
                heapModel, selector, new MapBasedCSManager());
        }
        else {
            throw new IllegalArgumentException("Illegal algorithm argument: " + algorithm);
        }
        setPlugin(solver, options, algorithm);
        solver.solve();
        if (algorithm.equals("correlation")) {
            // This is stupid:
            // dumpMethods("../output/tai-e/" + programID + "-csc-involved-methods.txt", ((CutShortcutSolver) solver).getInvolvedMethods());
        }
        return solver.getResult();
    }

    private static void setPlugin(Solver solver, AnalysisOptions options, String algorithm) {
        CompositePlugin plugin = new CompositePlugin();
        // add builtin plugins
        // To record elapsed time precisely, AnalysisTimer should be added at first.
        plugin.addPlugin(
                new AnalysisTimer(),
                new EntryPointHandler(),
                new ClassInitializer(),
                new ThreadHandler(),
                new NativeModeller(),
                new ExceptionAnalysis(),
                new ReflectionAnalysis()
        );
        if (algorithm.equals("correlation")) {
            MakeDefaultContainerConfig.make();
            plugin.addPlugin(
                new LocalFlowHandler(),
                new FieldAccessHandler(),
                new ContainerAccessHandler()
            );
        }
        if (World.get().getOptions().getJavaVersion() < 9) {
            // current reference handler doesn't support Java 9+
            plugin.addPlugin(new ReferenceHandler());
        }
        if (World.get().getOptions().getJavaVersion() >= 8) {
            plugin.addPlugin(new LambdaAnalysis());
        }
        if (options.getBoolean("handle-invokedynamic") &&
                InvokeDynamicAnalysis.useMethodHandle()) {
            plugin.addPlugin(new InvokeDynamicAnalysis());
        }
        if (options.getString("taint-config") != null) {
            plugin.addPlugin(new TaintAnalysis());
        }
        plugin.addPlugin(new ResultProcessor());
        // add plugins specified in options
        // noinspection unchecked
        addPlugins(plugin, (List<String>) options.get("plugins"));
        // connects plugins and solver
        plugin.setSolver(solver);
        solver.setPlugin(plugin);
    }

    private static void addPlugins(CompositePlugin plugin,
                                   List<String> pluginClasses) {
        for (String pluginClass : pluginClasses) {
            try {
                Class<?> clazz = Class.forName(pluginClass);
                Constructor<?> ctor = clazz.getConstructor();
                Plugin newPlugin = (Plugin) ctor.newInstance();
                plugin.addPlugin(newPlugin);
            } catch (ClassNotFoundException e) {
                throw new ConfigException(
                        "Plugin class " + pluginClass + " is not found");
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new AnalysisException("Failed to get constructor of " +
                        pluginClass + ", does the plugin class" +
                        " provide a public non-arg constructor?");
            } catch (InvocationTargetException | InstantiationException e) {
                throw new AnalysisException(
                        "Failed to create plugin instance for " + pluginClass, e);
            }
        }
    }

    private void dumpMethods(String filePath, Set<JMethod> methods) {
        PrintStream out;
        File outFile = new File(filePath);
        try {
            out = new PrintStream(new FileOutputStream(outFile));
            methods.forEach(out::println);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open output file", e);
        }
    }
}
