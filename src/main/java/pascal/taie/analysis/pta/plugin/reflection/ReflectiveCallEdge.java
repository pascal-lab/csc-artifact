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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;

/**
 * Represents reflective call edges.
 */
public class ReflectiveCallEdge extends Edge<CSCallSite, CSMethod> {

    public enum ReflectiveCallKind {
        NEW_INSTANCE, // r = c.newInstance(args), r is the baseVar
        METHOD_INVOKE, // r = m.invoke(obj, args): m is a instance of class method, and obj is a instance of m's declaring method,
        // args is a variable of array type which stores the argument of m in order
    }

    /**
     * Variable pointing to the array argument of reflective call,
     * which contains the arguments for the reflective target method, i.e.,
     * args for constructor.newInstance(args)/method.invoke(o, args).
     * This field is null for call edges from Class.newInstance().
     */
    @Nullable
    private final Var args;

    private final ReflectiveCallKind reflectiveKind;

    @Nullable
    private Var virtualArg;

    ReflectiveCallEdge(ReflectiveCallKind kind, CSCallSite csCallSite, CSMethod callee, @Nullable Var args) {
        super(CallKind.OTHER, csCallSite, callee);
        this.args = args;
        this.reflectiveKind = kind;
    }

    public void setVirtualArg() {
        if (args != null && isConcerned(args)) {
            virtualArg = new Var(args.getMethod(), "VirtualArg", ((ArrayType) args.getType()).elementType(), -1, true);
        }
        else
            virtualArg = null;
    }

    public static boolean isConcerned(Exp exp) {
        Type type = exp.getType();
        return type instanceof ReferenceType && !(type instanceof NullType);
    }

    @Nullable
    public Var getArgs() {
        return args;
    }

    @Nullable
    public Var getVirtualArg() {
        return virtualArg;
    }

    public ReflectiveCallKind getReflectiveKind() {
        return reflectiveKind;
    }
}
