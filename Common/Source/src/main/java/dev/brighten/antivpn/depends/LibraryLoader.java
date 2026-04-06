/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.depends;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.NonnullByDefault;
import dev.brighten.antivpn.utils.Supplier;
import dev.brighten.antivpn.utils.Suppliers;
import lombok.Getter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Resolves {@link MavenLibrary} annotations for a class, and loads the dependency
 * into the classloader.
 */
@SuppressWarnings("CallToPrintStackTrace")
@NonnullByDefault
public final class LibraryLoader {
    private static final int RELOCATION_FORMAT_VERSION = 5;
    private static final String RELOCATION_METADATA_PATH = "META-INF/antivpn-relocation.properties";

    @SuppressWarnings("Guava")
    private static final Supplier<URLClassLoaderAccess> URL_INJECTOR = AntiVPN.getInstance().getClass().getClassLoader() instanceof URLClassLoader ?
            Suppliers.memoize(() ->
                    URLClassLoaderAccess.create((URLClassLoader) AntiVPN.getInstance().getClass().getClassLoader()))
            : null;

    public static void loadAll(Object object) {
        if(URL_INJECTOR == null)
            return;
        loadAll(object.getClass());
    }

    public static void loadAll(Class<?> clazz) {
        if(URL_INJECTOR == null)
            return;
        MavenLibrary[] libs = clazz.getDeclaredAnnotationsByType(MavenLibrary.class);

        for (MavenLibrary lib : libs) {
            // Create relocations map if any are defined
            Map<String, String> relocations = new HashMap<>();
            for (Relocate relocate : lib.relocations()) {
                relocations.put(relocate.from().replace("\\", ""), relocate.to());
            }

            load(lib.groupId().replace("\\", ""), lib.artifactId(), lib.version(), lib.repo().url(), relocations);
        }
    }

    public static void load(String groupId, String artifactId, String version, String repoUrl,
                            Map<String, String> relocations) {
        load(new Dependency(groupId, artifactId, version, repoUrl), relocations);
    }

    public static void load(Dependency d, Map<String, String> relocations) {
        System.out.printf("Loading dependency %s:%s:%s from %s%n",
                d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getRepoUrl());
        String name = d.getArtifactId() + "-" + d.getVersion();

        // If we have relocations, add a suffix to identify the relocated version
        String fileName = name + ".jar";
        if (!relocations.isEmpty()) {
            fileName = name + "-relocated.jar";
        }

        File saveLocation = new File(getLibFolder(), fileName);
        File originalJar = new File(getLibFolder(), name + ".jar");

        // Download the original jar if it doesn't exist
        if (!originalJar.exists()) {
            try {
                System.out.println("Dependency '" + name +
                        "' is not already in the libraries folder. Attempting to download...");
                URL url = d.getUrl();

                try (InputStream is = url.openStream()) {
                    Files.copy(is, originalJar.toPath());
                }
                System.out.println("Dependency '" + name + "' successfully downloaded.");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to download dependency: " + d, e);
            }
        }

        // Rebuild relocated jars when the relocation format changes or the cached jar is stale.
        if (!relocations.isEmpty() && shouldRebuildRelocatedJar(saveLocation, relocations)) {
            try {
                System.out.println("Relocating packages for " + name + "...");
                relocateJar(originalJar, saveLocation, relocations);
                System.out.println("Successfully relocated packages for " + name);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to relocate packages for dependency: " + d, e);
            }
        }

        // Load the appropriate jar (original or relocated)
        File jarToLoad = relocations.isEmpty() ? originalJar : saveLocation;

        if (!jarToLoad.exists()) {
            throw new RuntimeException("Unable to find dependency jar: " + jarToLoad.getAbsolutePath());
        }

        try {
            URL_INJECTOR.get().addURL(jarToLoad.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load dependency: " + jarToLoad, e);
        }

        System.out.println("Loaded dependency '" + name + "' successfully.");
    }

    private static void relocateJar(File sourceJar, File targetJar, Map<String, String> relocations)
            throws IOException {
        // Track service files to avoid duplicates
        Map<String, StringBuilder> serviceFiles = new HashMap<>();

        Files.deleteIfExists(targetJar.toPath());

        try (JarFile jar = new JarFile(sourceJar);
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar.toPath()))) {

            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    if (name.startsWith("META-INF/services/")) {
                        // Process service files but don't write yet
                        processServiceFile(name, is, serviceFiles, relocations);
                    } else if (name.endsWith(".class")) {
                        // Relocate class file path as well as content
                        String relocatedPath = relocateClassPath(name, relocations);

                        JarEntry newEntry = new JarEntry(relocatedPath);
                        jos.putNextEntry(newEntry);

                        byte[] classBytes = readAllBytes(is);
                        byte[] relocatedBytes = relocateClass(name, classBytes, relocations);
                        jos.write(relocatedBytes);
                        jos.closeEntry();
                    } else {
                        // Relocate package-scoped resources so ResourceBundle lookups follow relocated packages.
                        String relocatedPath = relocateResourcePath(name, relocations);

                        JarEntry newEntry = new JarEntry(relocatedPath);
                        jos.putNextEntry(newEntry);
                        copyStream(is, jos);
                        jos.closeEntry();
                    }
                }
            }

            // Now write all service files after processing
            for (Map.Entry<String, StringBuilder> entry : serviceFiles.entrySet()) {
                try {
                    JarEntry serviceEntry = new JarEntry(entry.getKey());
                    jos.putNextEntry(serviceEntry);
                    jos.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
                    jos.closeEntry();
                } catch (Exception e) {
                    // Log but continue with other service files
                    System.out.println("Warning: Could not write service file " +
                            entry.getKey() + ": " + e.getMessage());
                }
            }

            writeRelocationMetadata(jos, relocations);
        }

        validateRelocatedJar(targetJar, relocations);
    }

    private static boolean shouldRebuildRelocatedJar(File relocatedJar, Map<String, String> relocations) {
        if (!relocatedJar.exists()) {
            return true;
        }

        try (JarFile jar = new JarFile(relocatedJar)) {
            JarEntry metadataEntry = jar.getJarEntry(RELOCATION_METADATA_PATH);
            if (metadataEntry == null) {
                return true;
            }

            Properties metadata = new Properties();
            try (InputStream is = jar.getInputStream(metadataEntry)) {
                metadata.load(is);
            }

            if (!String.valueOf(RELOCATION_FORMAT_VERSION).equals(metadata.getProperty("formatVersion"))) {
                return true;
            }

            for (Map.Entry<String, String> relocation : relocations.entrySet()) {
                String key = "relocation." + relocation.getKey();
                if (!relocation.getValue().equals(metadata.getProperty(key))) {
                    return true;
                }
            }

            return Integer.toString(relocations.size()).equals(metadata.getProperty("relocationCount"));
        } catch (IOException e) {
            return true;
        }
    }

    private static void writeRelocationMetadata(JarOutputStream jos, Map<String, String> relocations)
            throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty("formatVersion", Integer.toString(RELOCATION_FORMAT_VERSION));
        metadata.setProperty("relocationCount", Integer.toString(relocations.size()));

        Map<String, String> sortedRelocations = new TreeMap<>(relocations);
        for (Map.Entry<String, String> relocation : sortedRelocations.entrySet()) {
            metadata.setProperty("relocation." + relocation.getKey(), relocation.getValue());
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        metadata.store(buffer, "AntiVPN relocation metadata");

        JarEntry metadataEntry = new JarEntry(RELOCATION_METADATA_PATH);
        jos.putNextEntry(metadataEntry);
        jos.write(buffer.toByteArray());
        jos.closeEntry();
    }

    private static void processServiceFile(String name, InputStream is,
                                           Map<String, StringBuilder> serviceFiles,
                                           Map<String, String> relocations) throws IOException {
        // Read service file content
        String content = new String(readAllBytes(is));
        StringBuilder contentBuilder = serviceFiles.computeIfAbsent(name, k -> new StringBuilder());

        // Process and relocate service implementations
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                for (Map.Entry<String, String> relocation : relocations.entrySet()) {
                    if (trimmed.startsWith(relocation.getKey())) {
                        trimmed = relocation.getValue() +
                                trimmed.substring(relocation.getKey().length());
                        break;
                    }
                }
            }
            contentBuilder.append(trimmed).append("\n");
        }
    }

    private static byte[] relocateClass(String entryName, byte[] classBytes, Map<String, String> relocations) {
        try {
            // Convert to slash notation for ASM
            Remapper prefixRemapper = getPrefixRemapper(relocations);

            // Create custom ClassWriter to handle missing classes
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    try {
                        return super.getCommonSuperClass(type1, type2);
                    } catch (RuntimeException e) {
                        // Fall back to Object when classes can't be loaded
                        return "java/lang/Object";
                    }
                }
            };

            ClassVisitor visitor = createStringRelocationVisitor(new ClassRemapper(writer, prefixRemapper), relocations);
            visitor = createMySqlUtilFallbackVisitor(entryName, visitor);

            // Process class with remapper
            reader.accept(visitor, 0);

            return relocateUtf8Constants(writer.toByteArray(), relocations);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to relocate class entry " + entryName, e);
        }
    }

    public static String relocateReflectiveClassName(String className) {
        if (className == null || className.startsWith("dev.brighten.antivpn.shaded.")) {
            return className;
        }

        if (className.startsWith("com.mysql.cj") || className.startsWith("com.mysql.jdbc")) {
            return "dev.brighten.antivpn.shaded." + className;
        }

        return className;
    }

    private static byte[] relocateUtf8Constants(byte[] classBytes, Map<String, String> relocations) throws IOException {
        Map<String, String> dotMappings = new HashMap<>();
        Map<String, String> slashMappings = new HashMap<>();
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            dotMappings.put(entry.getKey(), entry.getValue());
            slashMappings.put(entry.getKey().replace('.', '/'), entry.getValue().replace('.', '/'));
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(classBytes.length + 256);
        DataOutputStream out = new DataOutputStream(baos);

        out.writeInt(in.readInt());
        out.writeShort(in.readUnsignedShort());
        out.writeShort(in.readUnsignedShort());

        int constantPoolCount = in.readUnsignedShort();
        out.writeShort(constantPoolCount);

        for (int i = 1; i < constantPoolCount; i++) {
            int tag = in.readUnsignedByte();
            out.writeByte(tag);

            switch (tag) {
                case 1 -> {
                    String value = in.readUTF();
                    String relocated = relocateStringValue(value, dotMappings, slashMappings);
                    out.writeUTF(relocated);
                }
                case 3, 4 -> out.writeInt(in.readInt());
                case 5, 6 -> {
                    out.writeLong(in.readLong());
                    i++;
                }
                case 7, 8, 16, 19, 20 -> out.writeShort(in.readUnsignedShort());
                case 9, 10, 11, 12, 17, 18 -> {
                    out.writeShort(in.readUnsignedShort());
                    out.writeShort(in.readUnsignedShort());
                }
                case 15 -> {
                    out.writeByte(in.readUnsignedByte());
                    out.writeShort(in.readUnsignedShort());
                }
                default -> throw new IOException("Unknown constant pool tag " + tag);
            }
        }

        copyStream(in, out);
        out.flush();
        return baos.toByteArray();
    }

    private static Remapper getPrefixRemapper(Map<String, String> relocations) {
        Map<String, String> slashMappings = new HashMap<>();
        Map<String, String> dotMappings = new HashMap<>();
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            dotMappings.put(entry.getKey(), entry.getValue());
            String fromSlash = entry.getKey().replace('.', '/');
            String toSlash = entry.getValue().replace('.', '/');
            slashMappings.put(fromSlash, toSlash);
        }

        // Create customized remapper for package prefixes
        return new Remapper() {
            @Override
            public String map(String typeName) {
                if (typeName == null) return null;

                for (Map.Entry<String, String> entry : slashMappings.entrySet()) {
                    String from = entry.getKey();
                    String to = entry.getValue();

                    if (typeName.startsWith(from)) {
                        return to + typeName.substring(from.length());
                    }
                }
                return typeName;
            }

            @Override
            public Object mapValue(Object value) {
                if (value instanceof String stringValue) {
                    return relocateStringValue(stringValue, dotMappings, slashMappings);
                }
                return super.mapValue(value);
            }
        };
    }

    private static ClassVisitor createMySqlUtilFallbackVisitor(String entryName, ClassVisitor delegate) {
        if (!"com/mysql/cj/util/Util.class".equals(entryName)) {
            return delegate;
        }

        return new ClassVisitor(Opcodes.ASM9, delegate) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (visitor == null) {
                    return null;
                }

                if (!"getInstance".equals(name)
                        || !"(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;Lcom/mysql/cj/exceptions/ExceptionInterceptor;)Ljava/lang/Object;".equals(descriptor)) {
                    return visitor;
                }

                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "dev/brighten/antivpn/depends/LibraryLoader",
                                "relocateReflectiveClassName",
                                "(Ljava/lang/String;)Ljava/lang/String;",
                                false);
                        super.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                };
            }
        };
    }

    private static ClassVisitor createStringRelocationVisitor(ClassVisitor delegate,
                                                              Map<String, String> relocations) {
        Map<String, String> dotMappings = new HashMap<>();
        Map<String, String> slashMappings = new HashMap<>();
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            dotMappings.put(entry.getKey(), entry.getValue());
            slashMappings.put(entry.getKey().replace('.', '/'), entry.getValue().replace('.', '/'));
        }

        return new ClassVisitor(Opcodes.ASM9, delegate) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return wrapAnnotationVisitor(super.visitAnnotation(descriptor, visible), dotMappings, slashMappings);
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                         String descriptor, boolean visible) {
                return wrapAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
                        dotMappings, slashMappings);
            }

            @Override
            public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
                RecordComponentVisitor visitor = super.visitRecordComponent(name, descriptor, signature);
                if (visitor == null) {
                    return null;
                }
                return new RecordComponentVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitAnnotation(descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                                 String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
                                dotMappings, slashMappings);
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                FieldVisitor visitor = super.visitField(access, name, descriptor, signature,
                        relocateAsmValue(value, dotMappings, slashMappings));
                if (visitor == null) {
                    return null;
                }
                return new FieldVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitAnnotation(descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                                 String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
                                dotMappings, slashMappings);
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (visitor == null) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return wrapAnnotationVisitor(super.visitAnnotationDefault(), dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitAnnotation(descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                                 String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor,
                                                                     boolean visible) {
                        return wrapAnnotationVisitor(super.visitParameterAnnotation(parameter, descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitInsnAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                                 String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitInsnAnnotation(typeRef, typePath, descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, org.objectweb.asm.TypePath typePath,
                                                                     String descriptor, boolean visible) {
                        return wrapAnnotationVisitor(super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                                                                          org.objectweb.asm.TypePath typePath,
                                                                          org.objectweb.asm.Label[] start,
                                                                          org.objectweb.asm.Label[] end,
                                                                          int[] index, String descriptor,
                                                                          boolean visible) {
                        return wrapAnnotationVisitor(
                                super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible),
                                dotMappings, slashMappings);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        super.visitLdcInsn(relocateAsmValue(value, dotMappings, slashMappings));
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, org.objectweb.asm.Handle bootstrapMethodHandle,
                                                       Object... bootstrapMethodArguments) {
                        Object[] relocatedArgs = new Object[bootstrapMethodArguments.length];
                        for (int i = 0; i < bootstrapMethodArguments.length; i++) {
                            relocatedArgs[i] = relocateAsmValue(bootstrapMethodArguments[i], dotMappings, slashMappings);
                        }
                        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, relocatedArgs);
                    }
                };
            }
        };
    }

    private static AnnotationVisitor wrapAnnotationVisitor(AnnotationVisitor delegate,
                                                           Map<String, String> dotMappings,
                                                           Map<String, String> slashMappings) {
        if (delegate == null) {
            return null;
        }

        return new AnnotationVisitor(Opcodes.ASM9, delegate) {
            @Override
            public void visit(String name, Object value) {
                super.visit(name, relocateAsmValue(value, dotMappings, slashMappings));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return wrapAnnotationVisitor(super.visitAnnotation(name, descriptor), dotMappings, slashMappings);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return wrapAnnotationVisitor(super.visitArray(name), dotMappings, slashMappings);
            }
        };
    }

    private static Object relocateAsmValue(Object value, Map<String, String> dotMappings,
                                           Map<String, String> slashMappings) {
        if (value instanceof String stringValue) {
            return relocateStringValue(stringValue, dotMappings, slashMappings);
        }

        return value;
    }

    private static String relocateStringValue(String value, Map<String, String> dotMappings,
                                              Map<String, String> slashMappings) {
        for (Map.Entry<String, String> entry : dotMappings.entrySet()) {
            String from = entry.getKey();
            String relocated = relocateByPrefixes(value, from, entry.getValue(), '.', '$');
            if (!relocated.equals(value)) {
                return relocated;
            }
        }

        for (Map.Entry<String, String> entry : slashMappings.entrySet()) {
            String from = entry.getKey();
            String to = entry.getValue();

            String relocated = relocateByPrefixes(value, from, to, '/', '$');
            if (!relocated.equals(value)) {
                return relocated;
            }

            relocated = relocateByPrefixes(value, "/" + from, "/" + to, '/', '$');
            if (!relocated.equals(value)) {
                return relocated;
            }

            relocated = relocateByPrefixes(value, "L" + from, "L" + to, '/', '$', ';');
            if (!relocated.equals(value)) {
                return relocated;
            }

            relocated = relocateByPrefixes(value, "[L" + from, "[L" + to, '/', '$', ';');
            if (!relocated.equals(value)) {
                return relocated;
            }
        }

        return value;
    }

    private static String relocateByPrefixes(String value, String from, String to, char... delimiters) {
        if (value.equals(from)) {
            return to;
        }

        for (char delimiter : delimiters) {
            if (value.startsWith(from + delimiter)) {
                return to + value.substring(from.length());
            }
        }

        return value;
    }

    private static void validateRelocatedJar(File targetJar, Map<String, String> relocations) throws IOException {
        Set<String> relocatedPrefixes = new HashSet<>();
        Map<String, String> dotMappings = new HashMap<>();
        Map<String, String> slashMappings = new HashMap<>();
        for (Map.Entry<String, String> relocation : relocations.entrySet()) {
            relocatedPrefixes.add(relocation.getValue().replace('.', '/') + "/");
            dotMappings.put(relocation.getKey(), relocation.getValue());
            slashMappings.put(relocation.getKey().replace('.', '/'), relocation.getValue().replace('.', '/'));
        }

        try (JarFile jar = new JarFile(targetJar)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                boolean shouldValidate = false;
                for (String relocatedPrefix : relocatedPrefixes) {
                    if (entry.getName().startsWith(relocatedPrefix)) {
                        shouldValidate = true;
                        break;
                    }
                }

                if (!shouldValidate) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    findUnrelocatedConstant(entry.getName(), readAllBytes(is), dotMappings, slashMappings);
                }
            }
        }
    }

    private static void findUnrelocatedConstant(String entryName, byte[] classBytes, Map<String, String> dotMappings,
                                                Map<String, String> slashMappings) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes));
        in.readInt();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int constantPoolCount = in.readUnsignedShort();

        for (int i = 1; i < constantPoolCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> {
                    String value = in.readUTF();
                    String relocated = relocateStringValue(value, dotMappings, slashMappings);
                    if (!value.equals(relocated)) {
                        throw new IOException("Relocated jar still contains original reference '" + value
                                + "' in class entry " + entryName);
                    }
                }
                case 3, 4 -> in.readInt();
                case 5, 6 -> {
                    in.readLong();
                    i++;
                }
                case 7, 8, 16, 19, 20 -> in.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }
                case 15 -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant pool tag " + tag + " while validating " + entryName);
            }
        }
    }

    private static String relocateClassPath(String path, Map<String, String> relocations) {
        // Convert path to package format (replacing / with .)
        String packagePath = path.substring(0, path.length() - 6).replace('/', '.');

        // Apply relocations
        for (Map.Entry<String, String> relocation : relocations.entrySet()) {
            if (packagePath.startsWith(relocation.getKey())) {
                packagePath = relocation.getValue() + packagePath.substring(relocation.getKey().length());
                break;
            }
        }

        // Convert back to path format
        return packagePath.replace('.', '/') + ".class";
    }

    private static String relocateResourcePath(String path, Map<String, String> relocations) {
        if (path.startsWith("META-INF/")) {
            return path;
        }

        for (Map.Entry<String, String> relocation : relocations.entrySet()) {
            String fromPath = relocation.getKey().replace('.', '/');
            String toPath = relocation.getValue().replace('.', '/');

            if (path.startsWith(fromPath + "/")) {
                return toPath + path.substring(fromPath.length());
            }
        }

        return path;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] data = new byte[1024];
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    private static File getLibFolder() {
        File pluginDataFolder = AntiVPN.getInstance().getPluginFolder();
        File libs = new File(pluginDataFolder, "libraries");
        if(libs.mkdirs()) {
            System.out.println("Created libraries folder!");
        }
        return libs;
    }

    @Getter
    @NonnullByDefault
// Fix the Dependency class to preserve original groupId for downloading
    public static final class Dependency {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String repoUrl;
        // Keep the original groupId/artifactId for Maven downloads
        private final String originalGroupId;
        private final String originalArtifactId;

        public Dependency(String groupId, String artifactId, String version, String repoUrl) {
            this.originalGroupId = Objects.requireNonNull(groupId, "groupId");
            this.originalArtifactId = Objects.requireNonNull(artifactId, "artifactId");
            this.groupId = this.originalGroupId;
            this.artifactId = this.originalArtifactId;
            this.version = Objects.requireNonNull(version, "version");
            this.repoUrl = Objects.requireNonNull(repoUrl, "repoUrl");
        }

        public URL getUrl() throws MalformedURLException {
            String repo = this.repoUrl;
            if (!repo.endsWith("/")) {
                repo += "/";
            }
            repo += "%s/%s/%s/%s-%s.jar";

            // Always use original groupId for Maven repository URL
            String url = String.format(repo, this.originalGroupId.replace(".", "/"),
                    this.originalArtifactId, this.version, this.originalArtifactId, this.version);
            return new URL(url);
        }

        // Rest of the class unchanged
    }


}
