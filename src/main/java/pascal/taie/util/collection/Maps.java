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

package pascal.taie.util.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Static utility methods for various maps, including {@link Map},
 * {@link MultiMap}, and {@link TwoKeyMap}.
 */
public final class Maps {

    private Maps() {
    }

    public static <K, V> Map<K, V> newMap() {
        return new HashMap<>();
    }

    public static <K, V> Map<K, V> newMap(int initialCapacity) {
        if (initialCapacity <= ArrayMap.DEFAULT_CAPACITY) {
            return newSmallMap();
        } else {
            return newMap();
        }
    }

    public static <K, V> Map<K, V> newSmallMap() {
        return new ArrayMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap() {
        return new HybridHashMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap(Map<K, V> map) {
        return new HybridHashMap<>(map);
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    public static <K, V> MultiMap<K, V> newMultiMap() {
        return new MapSetMultiMap<>(newMap(), Sets::newHybridSet);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(Supplier<Set<V>> setFactory) {
        return new MapSetMultiMap<>(newMap(), setFactory);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(Map<K, Set<V>> map) {
        return new MapSetMultiMap<>(map, Sets::newHybridSet);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(int initialCapacity) {
        return new MapSetMultiMap<>(
                newMap(initialCapacity), Sets::newHybridSet);
    }

    public static <K1, K2, V> TwoKeyMap<K1, K2, V> newTwoKeyMap() {
        return new MapMapTwoKeyMap<>(newMap(), Maps::newHybridMap);
    }
}
