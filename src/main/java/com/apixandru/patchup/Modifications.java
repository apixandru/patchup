package com.apixandru.patchup;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.RETURN;

class Modifications {

    static byte[] printStacktrace(byte[] classBytes, String methodName) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode(ASM5);
            reader.accept(node, 0);
            for (MethodNode mn : node.methods) {
                if (methodName.equals(mn.name)) {
                    mn.instructions.insert(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/RuntimeException", "printStackTrace", "()V"));
                    mn.instructions.insert(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V"));
                    mn.instructions.insert(new InsnNode(Opcodes.DUP));
                    mn.instructions.insert(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"));
                }
            }
            ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(1);
            throw throwable;
        }
    }

    static byte[] quickReturn(byte[] classBytes, String overrideMethod) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode(ASM5);
            reader.accept(node, 0);

            for (MethodNode mn : node.methods) {
                if (overrideMethod == null) {
                    System.out.println(mn.name + mn.desc);
                } else if (overrideMethod.equals(mn.name + mn.desc)) {
                    mn.instructions.insert(new InsnNode(RETURN));
                } else if (overrideMethod.length() == 1 && mn.name.startsWith(overrideMethod)) {
                    System.out.println(mn.name + mn.desc);
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(1);
            throw throwable;
        }
    }

}
