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

import pascal.taie.analysis.pta.plugin.field.ParameterIndex;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.plugin.container.HostMap.HostList;
import pascal.taie.analysis.pta.plugin.container.HostMap.HostSet;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Represents work list in pointer analysis.
 */
final class WorkList {

    /**
     * Pointer entries to be processed.
     */
    private final Map<Pointer, PointsToSet> pointerEntries = new LinkedHashMap<>();

    /**
     * Call edges to be processed.
     */
    private final Queue<Edge<CSCallSite, CSMethod>> callEdges = new ArrayDeque<>();

    void addEntry(Pointer pointer, PointsToSet pointsToSet) {
        PointsToSet set = pointerEntries.get(pointer);
        if (set != null) {
            set.addAll(pointsToSet);
        } else {
            pointerEntries.put(pointer, pointsToSet.copy());
        }
    }

    void addEntry(Edge<CSCallSite, CSMethod> edge) {
        callEdges.add(edge);
    }

    Entry pollEntry() {
        if (!callEdges.isEmpty()) {
            // for correctness, we need to ensure that any call edges in
            // the work list must be processed prior to the pointer entries
            return new CallEdgeEntry(callEdges.poll());
        } else if (!pointerEntries.isEmpty()) {
            var it = pointerEntries.entrySet().iterator();
            var e = it.next();
            it.remove();
            return new PointerEntry(e.getKey(), e.getValue());
        } else if (!setStmtEntries.isEmpty()) {
            return setStmtEntries.poll();
        } else if (!getStmtEntries.isEmpty()) {
            return getStmtEntries.poll();
        } else if (!hostEntries.isEmpty()) {
            return hostEntries.poll();
        } else {
            throw new NoSuchElementException();
        }
    }

    private final Queue<HostEntry> hostEntries = new ArrayDeque<>();

    private final Queue<SetStmtEntry> setStmtEntries = new ArrayDeque<>();

    private final Queue<GetStmtEntry>  getStmtEntries = new ArrayDeque<>();

    interface Entry {
    }

    record PointerEntry(Pointer pointer, PointsToSet pointsToSet)
            implements Entry {
    }

    record CallEdgeEntry(Edge<CSCallSite, CSMethod> edge)
            implements Entry {
    }

    void addHostEntry(Pointer pointer, HostList.Kind kind, HostSet hostSet) {
        hostEntries.add(new HostEntry(pointer, kind, hostSet));
    }
    void addSetStmtEntry(JMethod method, FieldRef fieldRef, ParameterIndex baseIndex, ParameterIndex rhsIndex) {
        setStmtEntries.add(new SetStmtEntry(method, fieldRef, baseIndex, rhsIndex));
    }

    void addGetStmtEntry(JMethod method, int lhsIndex, ParameterIndex baseIndex, FieldRef fieldRef) {
        getStmtEntries.add(new GetStmtEntry(method, lhsIndex, baseIndex, fieldRef));
    }

    boolean isEmpty() {
        return pointerEntries.isEmpty() && hostEntries.isEmpty() && callEdges.isEmpty()
            && setStmtEntries.isEmpty() && getStmtEntries.isEmpty();
    }

    record HostEntry(Pointer pointer, HostList.Kind kind, HostSet hostSet)
            implements Entry {
    }

    record SetStmtEntry(JMethod method, FieldRef fieldRef, ParameterIndex baseIndex, ParameterIndex rhsIndex)
            implements Entry {
    }

    record GetStmtEntry(JMethod method, int lhsIndex, ParameterIndex baseIndex, FieldRef fieldRef)
            implements Entry {
    }
}
