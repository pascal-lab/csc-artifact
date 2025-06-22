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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.plugin.container.HostMap.HostList;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.util.collection.ArraySet;
import pascal.taie.util.collection.HybridIndexableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

abstract class AbstractPointer implements Pointer {

    private PointsToSet pointsToSet;

    private final HostList hostMap = new HostList();

    private final int index;

    private final Set<Pointer> successors = new HybridIndexableSet<>(true);

    private final Set<Pointer> predecessors = new HybridIndexableSet<>(true);

    private final ArrayList<PointerFlowEdge> outEdges = new ArrayList<>(4);

    private final ArrayList<PointerFlowEdge> inEdges = new ArrayList<>(4);

    protected AbstractPointer(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public PointsToSet getPointsToSet() {
        return pointsToSet;
    }

    @Override
    public void setPointsToSet(PointsToSet pointsToSet) {
        this.pointsToSet = pointsToSet;
    }

    @Override
    public Set<CSObj> getObjects() {
        PointsToSet pts = getPointsToSet();
        return pts == null ? Set.of() : pts.getObjects();
    }

    @Override
    public Stream<CSObj> objects() {
        return getObjects().stream();
    }

    @Override
    public boolean addOutEdge(PointerFlowEdge edge) {
        if (successors.add(edge.getTarget())) {
            return outEdges.add(edge);
        }
        return false;
    }

    @Override
    public boolean addInEdge(PointerFlowEdge edge) {
        if (predecessors.add(edge.getSource())) {
            return inEdges.add(edge);
        }
        return false;
    }

    @Override
    public Set<PointerFlowEdge> getOutEdges() {
        return Collections.unmodifiableSet(new ArraySet<>(outEdges, true));
    }

    @Override
    public Set<PointerFlowEdge> getInEdges() {
        return Collections.unmodifiableSet(new ArraySet<>(inEdges, true));
    }

    @Override
    public int getOutDegree() {
        return outEdges.size();
    }

    @Override
    public HostList getHostList() {
        return hostMap;
    }

}
