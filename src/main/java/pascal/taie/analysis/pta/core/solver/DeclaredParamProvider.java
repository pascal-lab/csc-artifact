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

import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;

import java.util.Set;

/**
 * This {@link ParamProvider} creates parameter objects of the declared types.
 * For this variable/parameter of abstract class or non-reference type,
 * an empty set is returned.
 */
public class DeclaredParamProvider implements ParamProvider {

    private final JMethod method;

    private final HeapModel heapModel;

    public DeclaredParamProvider(JMethod jMethod, HeapModel heapModel) {
        this.method = jMethod;
        this.heapModel = heapModel;
    }

    @Override
    public Set<Obj> getThisObjs() {
        if (method.isStatic() || method.getDeclaringClass().isAbstract()) {
            return Set.of();
        } else {
            return Set.of(heapModel.getMockObj(getThisObjDesc(), this,
                    method.getDeclaringClass().getType(), method));
        }
    }

    private String getThisObjDesc() {
        return "EntryPointObj-" + method + "/this";
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        if (method.getParamType(i) instanceof ReferenceType refType) {
            if (refType instanceof ClassType cType &&
                    !cType.getJClass().isAbstract()) {
                return Set.of(heapModel.getMockObj(
                        getParamObjDesc(i), this, refType, method));
            }
        }
        return Set.of();
    }

    private String getParamObjDesc(int i) {
        return "EntryPointObj-" + method + '/' + i;
    }
}
