package com.apixandru.patchup;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

import static com.apixandru.patchup.Utils.*;
import static jdk.internal.org.objectweb.asm.Opcodes.ASM5;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

public class QuickReturnTransformer implements ClassFileTransformer {

    private static String overrideClass;
    private static String overrideMethod;

    public static void premain(String args, Instrumentation inst) throws Exception {
        Map<String, String> props = loadProperties(args);
        String entrypoint = getMandatory(props, "entrypoint");
        File file = getEntrypointFile(entrypoint);
        String md5 = getFileChecksum("MD5", file);
        System.out.println("Identified " + md5);
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
        if (normalize(className).equals(overrideClass)) {
            return transform(classFileBuffer);
        }
        return classFileBuffer;
    }

    public byte[] transform(byte[] classBytes) {
        System.out.println("Override class " + overrideClass + " Override method: " + overrideMethod);
        ClassReader reader = new ClassReader(classBytes);
        ClassNode node = new ClassNode(ASM5);
        reader.accept(node, 0);

        System.out.println("Methods " + node.methods.size());
        for (MethodNode mn : node.methods) {
            System.out.println(mn.name + mn.desc);
            if (overrideMethod == null) {
                System.out.println(mn.name + mn.desc);
            } else if (overrideMethod.equals(mn.name + mn.desc)) {
                mn.instructions.insert(new InsnNode(RETURN));
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}
