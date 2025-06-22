package pascal.taie.analysis.pta.client;

import pascal.taie.analysis.StmtResult;
import pascal.taie.ir.stmt.Stmt;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

public record WantedStmtResult(Set<Stmt> wantedStmts,
                               Predicate<Stmt> filter) implements StmtResult<Boolean> {

    @Override
    public boolean isRelevant(Stmt stmt) {
        return filter.test(stmt);
    }

    @Override
    public Boolean getResult(Stmt stmt) {
        return wantedStmts.contains(stmt);
    }

    public Set<Stmt> getWantedStmts() {
        return Collections.unmodifiableSet(wantedStmts);
    }
}
