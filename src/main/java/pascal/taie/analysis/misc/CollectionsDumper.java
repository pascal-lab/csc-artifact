package pascal.taie.analysis.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;

public class CollectionsDumper extends ProgramAnalysis<Void> {
    public static final String ID = "collections-dumper";

    private static final Logger logger = LogManager.getLogger(CollectionsDumper.class);

    private static final ClassHierarchy hierarchy = World.get().getClassHierarchy();

    public CollectionsDumper(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Void analyze() {
        JClass colClass = hierarchy.getClass("java.util.TreeMap$Entry");
        if (colClass != null) {
//            hierarchy.getAllSubclassesOf(colClass, false).stream().filter(c -> c.getName().contains("Entry"))
//                .forEach(System.out::println);
            colClass.getDeclaredMethods().forEach(m -> {
                if (!m.isAbstract()) {
                    m.getIR().forEach(stmt -> {
                        if (stmt instanceof LoadField || stmt instanceof LoadArray) {
                            System.out.println(m + " " + stmt);
                            System.out.println(m + "/" + stmt.getIndex());
                        }
                    });
                }
            });
        }
        return null;
    }
}
