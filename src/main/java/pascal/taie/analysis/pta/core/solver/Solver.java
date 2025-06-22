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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.container.ContainerConfig;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.container.HostMap.HostSet;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.Collection;

public interface Solver {

    AnalysisOptions getOptions();

    ClassHierarchy getHierarchy();

    TypeSystem getTypeSystem();

    HeapModel getHeapModel();

    CSManager getCSManager();

    ContextSelector getContextSelector();

    default ContainerConfig getContainerConfig() {
        return null;
    }

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    /**
     * Returns the points-to set of given pointer. If the pointer has not
     * been associated with a points-to set, this method will create a
     * new set and associate it with the pointer.
     */
    PointsToSet getPointsToSetOf(Pointer pointer);

    default HostSet getEmptyHostSet() {
        return null;
    }

    /**
     * Creates a new empty points-to set.
     */
    PointsToSet makePointsToSet();

    /**
     * Sets plugin to this solver.
     */
    void setPlugin(Plugin plugin);

    /**
     * Starts this solver.
     */
    void solve();

    // ---------- side-effect APIs (begin) ----------
    // These side-effect APIs could be used by Plugins to update
    // points-to information.

    // APIs for adding points-to relations
    void addPointsTo(Pointer pointer, PointsToSet pts);

    void addPointsTo(Pointer pointer, CSObj csObj);

    void addPointsTo(Pointer pointer, Context heapContext, Obj obj);

    // convenient APIs for adding var-points-to relations
    void addVarPointsTo(Context context, Var var, PointsToSet pts);

    void addVarPointsTo(Context context, Var var, CSObj csObj);

    void addVarPointsTo(Context context, Var var, Context heapContext, Obj obj);

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    default void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind) {
        addPFGEdge(source, target, kind, Identity.get());
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     * For the objects pointed to by "source", only the ones whose types
     * are subtypes of given type are propagated to "target".
     */
    default void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind, Type type) {
        addPFGEdge(source, target, kind, new TypeFilter(type, this));
    }

    /**
     * Adds an edge "source -> target" (with edge transfer) to the PFG.
     */
    void addPFGEdge(Pointer source, Pointer target, PointerFlowEdge.Kind kind, Transfer transfer);

    /**
     * Adds an entry point.
     * Notes that the method in entry point will be set as an entry in {@link CallGraph}
     */
    void addEntryPoint(EntryPoint entryPoint);

    /**
     * Adds a call edge.
     *
     * @param edge the added edge.
     */
    void addCallEdge(Edge<CSCallSite, CSMethod> edge);

    /**
     * Adds a context-sensitive method.
     *
     * @param csMethod the added context-sensitive method.
     */
    void addCSMethod(CSMethod csMethod);

    /**
     * Adds stmts to the analyzed program. Solver will process given stmts.
     *
     * @param csMethod the container method of the stmts
     * @param stmts    the added stmts
     */
    void addStmts(CSMethod csMethod, Collection<Stmt> stmts);

    /**
     * Analyzes the static initializer (i.e., <clinit>) of given class.
     *
     * @param cls the class to be initialized.
     */
    void initializeClass(JClass cls);

    /**
     * If a plugin takes over the analysis of a method, and wants this solver
     * to ignore the method (for precision and/or efficiency reasons),
     * then it could call this API with the method.
     * After that, this solver will not process the method body.
     * <p>
     * Typically, this API should be called at the initial stage of
     * pointer analysis, i.e., in {@link Plugin#onStart()}.
     *
     * @param method the method to be ignored.
     */
    void addIgnoredMethod(JMethod method);

    // ---------- side-effect APIs (end) ----------

    /**
     * @return pointer analysis result.
     */
    PointerAnalysisResult getResult();
}
