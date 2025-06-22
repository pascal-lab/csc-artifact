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

package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.MergedObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Set;

public class AvgPts extends ProgramAnalysis<Map<Var, Long>> {

    public static final String ID = "avg-pts";

    private static final Logger logger = LogManager.getLogger(AvgPts.class);

    public AvgPts(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Map<Var, Long> analyze() {
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        Map<Var, Long> result = Maps.newMap();
        pta.getVars().forEach(var -> {
            Set<Obj> objs = pta.getPointsToSet(var);
            result.put(var, objs.stream().mapToLong(obj -> {
                if (obj instanceof MergedObj mergedObj
                        && mergedObj.getModel() != null) {
                    return mergedObj.getAllocation().size();
                } else {
                    return 1;
                }
            }).sum());
        });
        logger.info(String.format("#%s %.2f", ID,
                1.D * result.values().stream().mapToLong(x -> x).sum() / result.size()));
        return result;
    }
}
