package com.apixandru.patchup;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

import static com.apixandru.patchup.Modifications.printStacktrace;
import static com.apixandru.patchup.Modifications.quickReturn;
import static com.apixandru.patchup.Utils.*;

public class QuickReturnTransformer implements ClassFileTransformer {

    private static String overrideClass;
    private static String overrideMethod;

    private static String printStacktraceClass;
    private static String printStacktraceMethod;

    public static void premain(String args, Instrumentation inst) throws Exception {
        Map<String, String> props = loadProperties(args);
        String entrypoint = getMandatory(props, "entrypoint");
        File file = getEntrypointFile(entrypoint);

        String printStacktrace = props.get("printStacktrace");
        if (printStacktrace != null && !printStacktrace.isEmpty()) {
            String[] pieces = printStacktrace.split(" ");
            printStacktraceClass = normalize(pieces[0]);
            printStacktraceMethod = pieces[1];
        }
        String md5 = getFileChecksum("MD5", file);
        String overrides = props.get(md5);
        if (overrides == null || overrides.isEmpty()) {
            System.out.println("No override found for " + md5);
        } else {
            String[] parts = overrides.split(" ");
            overrideClass = normalize(parts[0]);
            if (parts.length == 2) {
                overrideMethod = normalize(parts[1]);
            }
        }

        QuickReturnTransformer transformer = new QuickReturnTransformer();

        inst.addTransformer(transformer, true);

        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (loadedClass.getName().equals(overrideClass)) {
                inst.retransformClasses(loadedClass);
            }
        }
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer) {

        String normalize = normalize(className);
        if (normalize.equals(overrideClass)) {
            return quickReturn(classFileBuffer, overrideMethod);
        } else if (normalize.equals(printStacktraceClass)) {
            if (overrideMethod == null || overrideClass == null) {
                return printStacktrace(classFileBuffer, printStacktraceMethod);
            }
        }
        return classFileBuffer;
    }

}
