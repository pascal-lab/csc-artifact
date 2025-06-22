/*
 * Tai-e: A Static Analysis Framework for Java
 *
<<<<<<< HEAD
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
=======
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
>>>>>>> 4286a4e1ce618959341cf8fb2cbec19aa77483b7
 */

package pascal.taie.analysis.pta.pts;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.util.collection.HybridHashSet;
import pascal.taie.util.collection.SetEx;

class HybridHashPointsToSet extends DelegatePointsToSet {

    HybridHashPointsToSet() {
        this(new HybridHashSet<>());
    }

    private HybridHashPointsToSet(SetEx<CSObj> set) {
        super(set);
    }

    @Override
    protected PointsToSet newSet(SetEx<CSObj> set) {
        return new HybridHashPointsToSet(set);
    }
}
