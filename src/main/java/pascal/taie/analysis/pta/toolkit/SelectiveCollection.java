package pascal.taie.analysis.pta.toolkit;

import pascal.taie.World;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectiveCollection {
    private final Set<JMethod> collectionMethods;

    private final Set<JClass> collectionClass;

    private Set<JClass> collectionInterface;

    private  Set<JClass> excludeClass;

    public SelectiveCollection(){
        this.collectionClass = new HashSet<>();
        this.collectionMethods = new HashSet<>();
        initialize();
    }

    private void initialize(){
        setExcludeClass();
        setCollectionInterface();
        setCollectionClass();
        setCollectionMethod();
    }

    public Set<JMethod> getCollectionMethods(){
        return this.collectionMethods;
    }

    private void setExcludeClass(){
        this.excludeClass = World.get().getClassHierarchy()
                .applicationClasses()
                .filter(jClass -> getAllInnerClassesOf(jClass).size()>10)
                .collect(Collectors.toSet());

    }

    private void setCollectionInterface(){
        this.collectionInterface = World.get().getClassHierarchy()
                .allClasses()
                .filter(jClass -> jClass.toString().equals("java.util.Collection")
                        || jClass.toString().equals("java.util.Dictionary")
                        || jClass.toString().equals("java.util.Map"))
                .collect(Collectors.toSet());
    }

    private void setCollectionClass(){
        //add subtype of collection interface
        Set<JClass> temp = World.get()
                .getClassHierarchy()
                .allClasses()
                .filter(jClass -> (!this.excludeClass.contains(jClass)) && isSubTypeOfCollectionInterface(jClass))
                .collect(Collectors.toSet());

        //add Arrays, Collections
        World.get().getClassHierarchy()
                .allClasses()
                .filter(jClass -> jClass.toString().equals("java.util.Arrays")
                        || jClass.toString().equals("java.util.Collections"))
                .forEach(temp::add);

        //add inner classes of each class
        temp.forEach(jClass -> {
            this.collectionClass.add(jClass);
            this.collectionClass.addAll(getAllInnerClassesOf(jClass));
        });
    }

    private void setCollectionMethod(){
        this.collectionClass.forEach(jClass -> {
            this.collectionMethods.addAll(jClass.getDeclaredMethods());
        });
    }

    public Set<JClass> getAllInnerClassesOf(JClass jClass){
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        Set<JClass> innerClasses = new HashSet<>();
        classHierarchy.getDirectInnerClassesOf(jClass).forEach(inner ->{
            innerClasses.add(inner);
            innerClasses.addAll(getAllInnerClassesOf(inner));
        });
        return innerClasses;
    }

    public boolean isSubTypeOfCollectionInterface(JClass jClass){
        TypeSystem typeSystem = World.get().getTypeSystem();
        for(JClass collectionInterface : this.collectionInterface){
            if(typeSystem.isSubtype(collectionInterface.getType(),jClass.getType())){
                return true;
            }
        }
        return false;
    }

}
