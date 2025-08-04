package me.ygy.pathfinder.code.analysis;

import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Deprecated
public class CallGraphWithSpoon {

    public static void main(String[] args) {
        String projectPath = "/Users/yuguangyuan/code/github/test-proj";
        buildGraph(projectPath);
    }

    public static void buildGraph(String projectPath) {
        MavenLauncher launcher = new MavenLauncher(projectPath,
                MavenLauncher.SOURCE_TYPE.APP_SOURCE,"/Users/yuguangyuan/soft/apache-maven-3.9.9/");

        launcher.getEnvironment().setComplianceLevel(17);
        launcher.getEnvironment().setNoClasspath(true); //有些类没提供, 比如 servlet jar 里面的类
        launcher.buildModel();

        CtModel model = launcher.getModel();

        for(CtPackage p : model.getAllPackages()) {
            System.out.println("package: " + p.getQualifiedName());
        }
        // list all classes of the model
        for(CtType<?> s : model.getAllTypes()) {
            System.out.println("class: " + s.getQualifiedName());
            Set<CtMethod<?>> methods = s.getMethods();
            for (CtMethod<?> method : methods) {
                List<CtInvocation> invocations = method.getElements(new TypeFilter<>(CtInvocation.class));
            }
        }


        // FIXME 注意这里的第二个参数methodName，不能传api_two(java.lang.String,org.example.Auth)，只能传api_two，不能传参数，否则会找不到
        findMethodDefinition(model, "org.example.App", "api_three");

//        findInvocationPoints(model, "org.example.App", "api_three(org.example.AuthType,com.google.common.base.Joiner)");
//        findInvocationPoints(model, "org.example.App", "api_four(org.example.AuthType,com.google.common.base.Joiner[])");
        findInvocationPoints(model, "org.example.App", "api_five(org.example.AuthType,java.util.List)");
    }

    public static void findMethodDefinition(CtModel ctModel, String clazzName, String methodName) {
        CtClass<?> foundClass = ctModel.getElements(new TypeFilter<CtClass<?>>(CtClass.class) {
            @Override
            public boolean matches(CtClass<?> clazz) {
                return clazz.getQualifiedName().equals(clazzName);
            }
        }).stream().findFirst().orElse(null);

        if (foundClass != null) {
            //System.out.println("Found class definition: " + foundClass);
            foundClass.getMethodsByName(methodName).forEach(m -> {
                System.out.println("Found method definition: " + m.getSignature());
                System.out.println(m.toString());
            });
        } else {
            System.out.println("Class definition not found for: " + clazzName);
        }
    }

    public static void findInvocationPoints(CtModel ctModel, String className, String methodName) {
        System.out.println(" Method " + className + "." + methodName + " is called by:");
        findInvocation(ctModel, className, methodName, 0, false);
    }

    public static void findNewClassConstruct(CtModel ctModel, String clazzName) {
        long start = System.currentTimeMillis();
        ctModel.getRootPackage().getElements(new TypeFilter<>(CtConstructorCall.class)).forEach(e -> {
            if (e.getExecutable().getDeclaringType().getQualifiedName().equals(clazzName)) {
                System.out.println(clazzName + "is created at: " + e.getPosition());
            }
        });
        System.out.println("time0: " + (System.currentTimeMillis() - start));
    }
    public static void findNewClassConstruct1(CtModel ctModel, String clazzName) {
        long start = System.currentTimeMillis();
        ctModel.getRootPackage().getElements(new TypeFilter<>(CtConstructorCall.class)).forEach(e -> {
            System.out.println(e.getType());
            String type = e.getType().toString();
            if (type.equals(clazzName)) {
                System.out.println(clazzName + "is created at: " + e.getPosition());
            }
        });
        System.out.println("time1: " + (System.currentTimeMillis() - start));
    }

    /**
     *  here we only find the invocation with the exactly class name and method name, not the declared method in
     *  Interface and parent class.
     *  Sometimes you want to find all the invocation points of a method, include the declared method in Interface and parent class.
     *  ex:
     *    IHello hello = new Hello();
     *    hello.sayHello();
     *    In this case, the invocation point of sayHello() is in IHello, not in Hello.
     * @param ctModel
     * @param className
     * @param methodName
     * @param depth
     * @param recursive
     */
    private static void findInvocation(CtModel ctModel, String className, String methodName, int depth, boolean recursive) {

        List<CtInvocation<?>> invocations = ctModel.getElements(new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
            @Override
            public boolean matches(CtInvocation<?> element) {
                return element.getExecutable().getSignature().toString().equals(methodName) && containsRefType(element.getReferencedTypes(), className);
            }
        });

        for (CtInvocation<?> invocation : invocations) {
            CtExecutable<?> caller = invocation.getParent(CtExecutable.class);
            if (caller != null) {
                for (int i = 0; i < depth; i++) {
                    System.out.print("\t");
                }
                System.out.print(" - " + caller.getParent(CtClass.class).getPackage() + "." + caller.getParent(CtClass.class).getSimpleName() + "." + caller.getSignature());
                if (caller.getThrownTypes().size() > 0) {
                    System.out.print(" throws ");
                    System.out.print(caller.getThrownTypes().stream().map(t -> t.toString()).collect(Collectors.joining( ", ")));
                }
                System.out.println(" at line " + invocation.getPosition().getLine());
                if (recursive) {
                    findInvocation(ctModel, caller.getParent(CtClass.class).getQualifiedName(), caller.getSignature(), 1 + depth, recursive);
                }
            }
        }
    }

    public static boolean containsRefType(Set<CtTypeReference<?>> types, String className) {
        for (CtTypeReference<?> type : types) {
            if (type.getQualifiedName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static void findThrowStatements(CtModel ctModel) {
        List<CtThrow> throwStatements = ctModel.getElements(new TypeFilter<>(CtThrow.class));

        // Process each throw statement
        for (CtThrow throwStatement : throwStatements) {
            CtExecutable<?> executable = throwStatement.getParent(CtExecutable.class);
            if (executable != null) {
                System.out.println(executable.getParent(CtClass.class).getPackage() + " - " + executable.getParent(CtClass.class).getSimpleName() + "." + executable.getSimpleName()
                        + " at line " + throwStatement.getPosition().getLine());
            }
        }
    }

    public static void findNextCalls(CtModel ctModel, String pkg, String clazz, String methodName) {
        //find this method
        ctModel.getElements(new TypeFilter<CtMethod<?>>(CtMethod.class) {
            @Override
            public boolean matches(CtMethod<?> method) {
                if (!(method.getParent() instanceof CtClass)) {
                    return false;
                }

                CtClass cz = (CtClass) method.getParent();
                String curClazz = cz.getSimpleName();
                String curPkg = null != cz.getPackage() ? cz.getPackage().getQualifiedName() : "-";

                if (method.getSimpleName().equals(methodName)) {
                    System.out.println(curPkg + " - " + curClazz + " - " + method.getSimpleName());
                }
                return pkg.equals(curPkg) && clazz.equals(curClazz) && method.getSimpleName().equals(methodName);
            }
        }).forEach(m -> {
            System.out.println("Method found: " + m.getSimpleName());
            //find next calls
            List<CtInvocation<?>> invocations = m.getElements(new TypeFilter<>(CtInvocation.class));
            for (CtInvocation<?> invocation : invocations) {
                // Check if the invocation is a client call
                CtExecutableReference exec = invocation.getExecutable();
                System.out.println(m.getSimpleName() + " -> " + invocation.getExecutable().getDeclaringType() + "."
                        + invocation.getExecutable().getSignature());
            }
        });
    }
}
