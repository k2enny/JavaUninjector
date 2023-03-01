package me.k2enny.uninjector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // Set up file paths and class to modify
        final String jarFilePath = "Client.jar";
        final String outputJarFilePath = "ModifiedClient.jar";
        final String classToModify = "Start";

        // Modify the class
        final byte[] modifiedClassBytes = modifyClass(jarFilePath, classToModify);

        // Create a new jar file with the modified class
        try (final JarOutputStream outputJar = new JarOutputStream(Files.newOutputStream(Paths.get(outputJarFilePath)))) {
            try (final JarFile inputJar = new JarFile(jarFilePath)) {
                final Enumeration<JarEntry> entries = inputJar.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName();
                    final byte[] entryBytes = getBytesFromInputStream(inputJar.getInputStream(entry));

                    // Skip the Updater.class file
                    if (entryName.equals("Updater.class")) {
                        continue;
                    }

                    final JarEntry outputEntry = new JarEntry(entryName);
                    outputJar.putNextEntry(outputEntry);

                    // Write the modified class bytes if it's the class being modified
                    if (entryName.equals(classToModify + ".class")) {
                        outputJar.write(modifiedClassBytes);
                    } else {
                        // Otherwise write the original class bytes
                        outputJar.write(entryBytes);
                    }

                    outputJar.closeEntry();
                }
            }
        }

        System.out.println("UnInjected");
    }

    private static byte[] modifyClass(final String jarFilePath, final String className) throws IOException {
        // Read the class file from the jar file
        final JarFile jar = new JarFile(jarFilePath);
        final JarEntry entry = jar.getJarEntry(className + ".class");
        final InputStream is = jar.getInputStream(entry);
        final byte[] classfileBuffer = getBytesFromInputStream(is);

        // Modify the class using ASM library
        final ClassReader cr = new ClassReader(classfileBuffer);
        final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        final ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Find the main method and modify it
                if (name.equals("main")) {
                    mv = new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKESTATIC && owner.equals("Updater") && name.equals("init") && descriptor.equals("()V")) {
                                // skip the call to Updater.init()
                                return;
                            }

                            // Otherwise, call the original method as usual
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    };
                }

                return mv;
            }
        };

        cr.accept(cv, 0);

        // Return the modified class bytes
        return cw.toByteArray();
    }

    private static byte[] getBytesFromInputStream(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        // Read input stream and write to buffer
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }
}



//☆☆☆☆☆☆☆☆☆☆☆☆
//
//if (WhoMade.is("k2enny")) {
//  System.out.println("k2ennyyyy");
//}
//
//☆☆☆☆☆☆☆☆☆☆☆☆
