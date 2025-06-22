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

package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.SimpleGraph;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class ThreadEscapeAnalysis extends ProgramAnalysis<Set<Obj>> {

    public static final String ID = "thread-escape";

    private static final Logger logger = LogManager.getLogger(ThreadEscapeAnalysis.class);

    private PointerAnalysisResult pta;

    private ObjGraph objGraph;

    public ThreadEscapeAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Obj> analyze() {
        pta = World.get().getResult(PointerAnalysis.ID);
        objGraph = new ObjGraph(pta);
        Set<Obj> roots = computeRoots();
        Set<Obj> mayEscape = reachableFrom(roots);
        logger.info("#{}: found {} out of {} objects",
                ID, mayEscape.size(), pta.getObjects().size());
        return mayEscape;
    }

    private Set<Obj> reachableFrom(Set<Obj> roots) {
        Set<Obj> visited = Sets.newSet();
        Queue<Obj> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            Obj obj = queue.poll();
            if (visited.add(obj)) {
                objGraph.getSuccsOf(obj).stream()
                        .filter(Predicate.not(visited::contains))
                        .forEach(queue::add);
            }
        }
        return visited;
    }

    private Set<Obj> computeRoots() {
        ClassHierarchy hierarchy = World.get().getClassHierarchy();
        Set<Obj> roots = Sets.newSet();
        // Static Fields escape their allocation threads
        pta.getStaticFields().stream()
                .map(StaticField::getField)
                .map(pta::getPointsToSet)
                .forEach(roots::addAll);
        // Thread instances escape their fields
        Objects.requireNonNull(hierarchy.getClass("java.lang.Thread"),
                        "java.lang.Thread")
                .getDeclaredMethods().stream()
                .filter(JMethod::isConstructor)
                .forEach(method -> {
                    Var thisVar = method.getIR().getThis();
                    roots.addAll(pta.getPointsToSet(thisVar));
                });
        return roots;
    }

    private static class ObjGraph extends SimpleGraph<Obj> {
        ObjGraph(PointerAnalysisResult pta) {
            Set<Pair<Obj, Obj>> edges = Sets.newConcurrentSet();
            pta.getObjects().forEach(this::addNode);
            pta.getInstanceFields().parallelStream().forEach(instanceField -> {
                Obj fromObj = instanceField.getBase().getObject();
                instanceField.objects().map(CSObj::getObject)
                        .forEach(toObj -> edges.add(new Pair<>(fromObj, toObj)));
            });
            edges.forEach(pair -> addEdge(pair.first(), pair.second()));
        }
    }
}
