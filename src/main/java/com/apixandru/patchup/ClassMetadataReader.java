package com.apixandru.patchup;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

class ClassMetadataReader {

    private static Method m;

    static {
        try {
            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public byte[] getClassData(String className) throws IOException {
        String classResourceName = '/' + className.replace('.', '/') + ".class";
        try (InputStream is = ClassMetadataReader.class.getResourceAsStream(classResourceName)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.readNBytes(data, 0, data.length)) != 0) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();

        }
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }

    public void acceptVisitor(String className, ClassVisitor visitor) throws IOException {
        acceptVisitor(getClassData(className), visitor);
    }

    public ArrayList<String> getSuperClasses(String type) {
        ArrayList<String> superclasses = new ArrayList<String>(1);
        superclasses.add(type);
        while ((type = getSuperClass(type)) != null) {
            superclasses.add(type);
        }
        Collections.reverse(superclasses);
        return superclasses;
    }

    private Class getLoadedClass(String type) {
        if (m != null) {
            try {
                ClassLoader classLoader = ClassMetadataReader.class.getClassLoader();
                return (Class) m.invoke(classLoader, type.replace('/', '.'));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getSuperClass(String type) {
        try {
            return getSuperClassASM(type);
        } catch (Exception e) {
            return getSuperClassReflect(type);
        }
    }

    protected String getSuperClassASM(String type) throws IOException {
        CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        acceptVisitor(type, cv);
        return cv.superClassName;
    }

    protected String getSuperClassReflect(String type) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            if (loadedClass.getSuperclass() == null) return null;
            return loadedClass.getSuperclass().getName().replace('.', '/');
        }
        return "java/lang/Object";
    }

    private class CheckSuperClassVisitor extends ClassVisitor {

        String superClassName;

        public CheckSuperClassVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superClassName = superName;
        }
    }

}
