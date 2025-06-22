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

package pascal.taie.ir.exp;

import pascal.taie.analysis.pta.plugin.field.AbstractLoadField;
import pascal.taie.analysis.pta.plugin.field.AbstractStoreField;
import pascal.taie.analysis.pta.plugin.field.ParameterIndex;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Indexable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of method/constructor parameters, lambda parameters,
 * exception parameters, and local variables.
 */
public class Var implements LValue, RValue, Indexable {

    /**
     * The method containing this Var.
     */
    private final JMethod method;

    /**
     * The name of this Var.
     */
    private final String name;

    /**
     * The type of this Var.
     */
    private final Type type;

    /**
     * The index of this variable in {@link #method}.
     */
    private final int index;

    /**
     * If this variable is a (temporary) variable generated for holding
     * a constant value, then this field holds that constant value;
     * otherwise, this field is null.
     */
    private final Literal constValue;

    private final boolean virtual;

    private boolean isDefined;

    private ParameterIndex parameterIndex;

    /**
     * Relevant statements of this variable.
     */
    private RelevantStmts relevantStmts = RelevantStmts.EMPTY;

    public Var(JMethod method, String name, Type type, int index) {
        this(method, name, type, index, null);
    }

    public Var(JMethod method, String name, Type type, int index, boolean virtual) {
        this(method, name, type, index, null, virtual);
    }

    public Var(JMethod method, String name, Type type, int index,
               @Nullable Literal constValue, boolean virtual) {
        this.method = method;
        this.name = name;
        this.type = type;
        this.index = index;
        this.constValue = constValue;
        this.virtual = virtual;
    }

    public Var(JMethod method, String name, Type type, int index,
               @Nullable Literal constValue) {
        this(method, name, type, index, constValue, false);
    }

    /**
     * @return the method containing this Var.
     */
    public JMethod getMethod() {
        return method;
    }

    /**
     * @return the index of this variable in the container IR.
     */
    @Override
    public int getIndex() {
        return index;
    }

    /**
     * @return name of this Var.
     */
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    public boolean isVirtual() {
        return virtual;
    }

    /**
     * @return true if this variable is a (temporary) variable
     * generated for holding constant value, otherwise false.
     */
    public boolean isConst() {
        return constValue != null;
    }

    public void setDefined() {
        isDefined = true;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setParameterIndex(ParameterIndex index) {
        parameterIndex = index;
    }

    public ParameterIndex getParameterIndex() {
        return parameterIndex;
    }

    /**
     * @return the constant value held by this variable.
     * @throws AnalysisException if this variable does not hold const value
     */
    public Literal getConstValue() {
        if (!isConst()) {
            throw new AnalysisException(this
                    + " is not a (temporary) variable for holding const value");
        }
        return constValue;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }

    public void addLoadField(LoadField loadField) {
        ensureRelevantStmts();
        relevantStmts.addLoadField(loadField);
    }

    public List<LoadField> getLoadFields() {
        return relevantStmts.getLoadFields();
    }

    public void addStoreField(StoreField storeField) {
        ensureRelevantStmts();
        relevantStmts.addStoreField(storeField);
    }

    public List<StoreField> getStoreFields() {
        return relevantStmts.getStoreFields();
    }

    public void addLoadArray(LoadArray loadArray) {
        ensureRelevantStmts();
        relevantStmts.addLoadArray(loadArray);
    }

    public List<LoadArray> getLoadArrays() {
        return relevantStmts.getLoadArrays();
    }

    public void addStoreArray(StoreArray storeArray) {
        ensureRelevantStmts();
        relevantStmts.addStoreArray(storeArray);
    }

    public List<StoreArray> getStoreArrays() {
        return relevantStmts.getStoreArrays();
    }

    public void addInvoke(Invoke invoke) {
        ensureRelevantStmts();
        relevantStmts.addInvoke(invoke);
    }

    public List<Invoke> getInvokes() {
        return relevantStmts.getInvokes();
    }

    public void addAbstractLoadField(AbstractLoadField abstractLoadField) {
        ensureRelevantStmts();
        relevantStmts.addAbstractLoadField(abstractLoadField);
    }

    public List<AbstractLoadField> getAbstractLoadFields() {
        return relevantStmts.getAbstractLoadFields();
    }

    public void addAbstractStoreField(AbstractStoreField abstractStoreField) {
        ensureRelevantStmts();
        relevantStmts.addAbstractStoreField(abstractStoreField);
    }

    public List<AbstractStoreField> getAbstractStoreFields() {
        return relevantStmts.getAbstractStoreFields();
    }

    /**
     * Ensure {@link #relevantStmts} points to an instance other than
     * {@link RelevantStmts#EMPTY}.
     */
    private void ensureRelevantStmts() {
        if (relevantStmts == RelevantStmts.EMPTY) {
            relevantStmts = new RelevantStmts();
        }
    }

    /**
     * Relevant statements of a variable, say v, which include:
     * load field: x = v.f;
     * store field: v.f = x;
     * load array: x = v[i];
     * store array: v[i] = x;
     * invocation: v.f();
     * We use a separate class to store these relevant statements
     * (instead of directly storing them in {@link Var}) for saving space.
     * Most variables do not have any relevant statements, so these variables
     * only need to hold one reference to the empty {@link RelevantStmts},
     * instead of several references to empty lists.
     */
    private static class RelevantStmts {

        private static final RelevantStmts EMPTY = new RelevantStmts();

        private static final int DEFAULT_CAPACITY = 4;

        // Contract: if the following fields are empty, they must point to
        // Collections.emptyList();
        private List<LoadField> loadFields = List.of();
        private List<StoreField> storeFields = List.of();
        private List<LoadArray> loadArrays = List.of();
        private List<StoreArray> storeArrays = List.of();
        private List<Invoke> invokes = List.of();

        private List<AbstractLoadField> abstractLoadFields = List.of();

        private List<AbstractStoreField> abstractStoreFields = List.of();

        private List<AbstractStoreField> getAbstractStoreFields() {
            return unmodifiable(abstractStoreFields);
        }

        private void addAbstractStoreField(AbstractStoreField abstractStoreField) {
            if (abstractStoreFields.isEmpty()) {
                abstractStoreFields = new ArrayList<>();
            }
            abstractStoreFields.add(abstractStoreField);
        }

        private List<AbstractLoadField> getAbstractLoadFields() {
            return unmodifiable(abstractLoadFields);
        }

        private void addAbstractLoadField(AbstractLoadField abstractLoadField) {
            if (abstractLoadFields.isEmpty()) {
                abstractLoadFields = new ArrayList<>();
            }
            abstractLoadFields.add(abstractLoadField);
        }

        private List<LoadField> getLoadFields() {
            return unmodifiable(loadFields);
        }

        private void addLoadField(LoadField loadField) {
            if (loadFields.isEmpty()) {
                loadFields = new ArrayList<>();
            }
            loadFields.add(loadField);
        }

        private List<StoreField> getStoreFields() {
            return unmodifiable(storeFields);
        }

        private void addStoreField(StoreField storeField) {
            if (storeFields.isEmpty()) {
                storeFields = new ArrayList<>(DEFAULT_CAPACITY);
            }
            storeFields.add(storeField);
        }

        private List<LoadArray> getLoadArrays() {
            return unmodifiable(loadArrays);
        }

        private void addLoadArray(LoadArray loadArray) {
            if (loadArrays.isEmpty()) {
                loadArrays = new ArrayList<>(DEFAULT_CAPACITY);
            }
            loadArrays.add(loadArray);
        }

        private List<StoreArray> getStoreArrays() {
            return unmodifiable(storeArrays);
        }

        private void addStoreArray(StoreArray storeArray) {
            if (storeArrays.isEmpty()) {
                storeArrays = new ArrayList<>(DEFAULT_CAPACITY);
            }
            storeArrays.add(storeArray);
        }

        private List<Invoke> getInvokes() {
            return unmodifiable(invokes);
        }

        private void addInvoke(Invoke invoke) {
            if (invokes.isEmpty()) {
                invokes = new ArrayList<>(DEFAULT_CAPACITY);
            }
            invokes.add(invoke);
        }

        private static <T> List<T> unmodifiable(List<T> list) {
            return list.isEmpty() ? list : Collections.unmodifiableList(list);
        }
    }
}
