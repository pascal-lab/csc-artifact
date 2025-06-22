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

package pascal.taie.analysis.pta.toolkit.zipper;

import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Collectors;

class PrecisionFlowGraph implements Graph<FGNode> {

    private final Type type;

    private final ObjectFlowGraph ofg;

    private final Set<FGNode> nodes;

    private final Set<VarNode> outNodes;

    private final MultiMap<FGNode, FGEdge> inWUEdges;

    private final MultiMap<FGNode, FGEdge> outWUEdges;

    PrecisionFlowGraph(Type type, ObjectFlowGraph ofg,
                       Set<FGNode> nodes, Set<VarNode> outNodes,
                       MultiMap<FGNode, FGEdge> outWUEdges) {
        this.type = type;
        this.ofg = ofg;
        this.nodes = nodes;
        this.outNodes = outNodes
                .stream()
                .filter(nodes::contains)
                .collect(Collectors.toUnmodifiableSet());
        this.outWUEdges = outWUEdges;
        this.inWUEdges = Maps.newMultiMap();
        outWUEdges.values()
                .forEach(edge -> inWUEdges.put(edge.target(), edge));
    }

    Type getType() {
        return type;
    }

    Set<VarNode> getOutNodes() {
        return outNodes;
    }

    @Override
    public boolean hasNode(FGNode node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(FGNode source, FGNode target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<FGNode> getPredsOf(FGNode node) {
        return Views.toMappedSet(getInEdgesOf(node), FGEdge::source);
    }

    @Override
    public Set<FGEdge> getInEdgesOf(FGNode node) {
        Set<FGEdge> inEdges = ofg.getInEdgesOf(node)
                .stream()
                .filter(e -> nodes.contains(e.source()))
                .collect(Collectors.toSet());
        inEdges.addAll(inWUEdges.get(node));
        return inEdges;
    }

    @Override
    public Set<FGNode> getSuccsOf(FGNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), FGEdge::target);
    }

    @Override
    public Set<FGEdge> getOutEdgesOf(FGNode node) {
        Set<FGEdge> outEdges = ofg.getOutEdgesOf(node)
                .stream()
                .filter(e -> nodes.contains(e.target()))
                .collect(Collectors.toSet());
        outEdges.addAll(outWUEdges.get(node));
        return outEdges;
    }

    @Override
    public Set<FGNode> getNodes() {
        return nodes;
    }
}
