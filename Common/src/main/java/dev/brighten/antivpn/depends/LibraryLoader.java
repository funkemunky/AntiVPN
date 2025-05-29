/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.brighten.antivpn.depends;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.NonnullByDefault;
import dev.brighten.antivpn.utils.Supplier;
import dev.brighten.antivpn.utils.Suppliers;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

        // If we have relocations, create a relocated jar
        if (!relocations.isEmpty() && !saveLocation.exists()) {
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
                        byte[] relocatedBytes = relocateClass(classBytes, relocations);
                        jos.write(relocatedBytes);
                        jos.closeEntry();
                    } else {
                        // Copy other files as-is
                        JarEntry newEntry = new JarEntry(name);
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
                    jos.write(entry.getValue().toString().getBytes());
                    jos.closeEntry();
                } catch (Exception e) {
                    // Log but continue with other service files
                    System.out.println("Warning: Could not write service file " +
                            entry.getKey() + ": " + e.getMessage());
                }
            }
        }
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

    private static byte[] relocateClass(byte[] classBytes, Map<String, String> relocations) {
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

            ClassVisitor visitor = new ClassRemapper(writer, prefixRemapper);

            // Process class with remapper
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            return writer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return classBytes;
        }
    }

    private static Remapper getPrefixRemapper(Map<String, String> relocations) {
        Map<String, String> slashMappings = new HashMap<>();
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
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
        };
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
