package pascal.taie.analysis.pta.client;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.StmtResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

public class ClientComparator extends ProgramAnalysis<Void> {

    public static final String ID = "comparator";

    private final PrintStream out;

    public ClientComparator(AnalysisConfig config) {
        super(config);
        File outFile = new File("pta-output/pmd-polycall-comparator.txt");
        try {
            out = new PrintStream(new FileOutputStream(outFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open output file", e);
        }
    }

    @Override
    public Void analyze() {
        String algorithm = getOptions().getString("algorithm");
        AnalysisConfig config1 = AnalysisConfig.of(algorithm, "algorithm", "pta"),
                config2 = AnalysisConfig.of(algorithm, "algorithm", "cbpta");
        if (algorithm.equals("may-fail-cast")) {
            WantedStmtResult result_pta = new MayFailCast(config1).analyze();
            WantedStmtResult result_cbpta = new MayFailCast(config2).analyze();
            compare(result_pta.getWantedStmts(), result_cbpta.getWantedStmts());
        }
        else if (algorithm.equals("poly-call")) {
            WantedStmtResult result_pta = new PolymorphicCallSite(config1).analyze();
            WantedStmtResult result_cbpta = new PolymorphicCallSite(config2).analyze();
            compare(result_pta.getWantedStmts(), result_cbpta.getWantedStmts());
        }
        return null;
    }

    public void compare(Set<Stmt> pta, Set<Stmt> cbpta) {
        // dump those statements which contained in the result of cbpta but not pta
        out.println("Statements that involved in the result of correlationBasePTA but not the original PTA (" + pta.size() + "-" + cbpta.size() + ")");
        int counter = 0;
        for (Stmt stmt: cbpta) {
            if (!pta.contains(stmt)) {
                if (stmt instanceof Invoke) {
                    out.println("[" + counter ++ + "] " + stmt);
                }
            }
        }
    }
}
