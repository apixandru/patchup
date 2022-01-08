package com.apixandru.patchup;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM5;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

public class QuickReturnTransformer implements ClassFileTransformer {

    private static final String CLASS_NAME = normalize("class.name");
    private static final String METHOD_NAME = normalize("method(params)");

    private static String normalize(String string) {
        return string.replace(".", "/");
    }

    public static void premain(String args, Instrumentation inst) throws Exception {
        QuickReturnTransformer transformer = new QuickReturnTransformer();

        inst.addTransformer(transformer, true);

        Set<String> modifiedClass = transformer.modifiedClasses();
        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (modifiedClass.contains(loadedClass.getName())) {
                inst.retransformClasses(loadedClass);
            }
        }
    }

    public Set<String> modifiedClasses() {
        return Collections.singleton(CLASS_NAME);
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer) {
        if (CLASS_NAME.equals(normalize(className))) {
            return transform(classFileBuffer);
        }
        return classFileBuffer;
    }

    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode node = new ClassNode(ASM5);
        reader.accept(node, 0);

        for (MethodNode mn : node.methods) {
            if (METHOD_NAME.equals(mn.name + mn.desc)) {
                mn.instructions.insert(new InsnNode(RETURN));
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}
