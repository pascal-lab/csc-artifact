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
import pascal.taie.language.type.Type;
import pascal.taie.util.Indexable;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents all pointers (nodes) in context-sensitive
 * pointer analysis (pointer flow graph).
 */
public interface Pointer extends Indexable {

    /**
     * Retrieves the points-to set associated with this pointer.
     * <p>
     * This method may return {@code null}.
     * We recommend use {@link #getObjects()} and {@link #objects()}
     * for accessing the objects pointed by this pointer after
     * the pointer analysis finishes.
     *
     * @return the points-to set associated with this pointer.
     */
    @Nullable
    PointsToSet getPointsToSet();

    /**
     * Sets the associated points-to set of this pointer.
     */
    void setPointsToSet(PointsToSet pointsToSet);

    HostList getHostList();

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty set if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns set of objects in the
     * {@code PointsToSet}.
     */
    Set<CSObj> getObjects();

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty stream if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns stream of objects in the
     * {@code PointsToSet}.
     */
    Stream<CSObj> objects();

    /**
     * @param edge an out edge of this pointer
     * @return true if new out edge was added to this pointer as a result
     * of the call, otherwise false.
     */
    boolean addOutEdge(PointerFlowEdge edge);

    /**
     * @param edge an in edge of this pointer
     * @return true if new in edge was added to this pointer as a result
     * of the call, otherwise false.
     */
    boolean addInEdge(PointerFlowEdge edge);

    /**
     * @return out edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getOutEdges();

    /**
     * @return in edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getInEdges();

    /**
     * @return out degree of this pointer in pointer flow graph.
     */
    int getOutDegree();

    /**
     * @return the type of this pointer
     */
    Type getType();
}
