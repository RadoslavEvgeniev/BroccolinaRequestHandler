package app.broccolina.util;

import app.broccolina.solet.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ApplicationLoadingService {

    private String applicationFolderPath;

    private Map<String, HttpSolet> loadedApplications;

    private JarFileUnzipService jarFileUnzipService;

    private List<URL> libraryClassUrls;

    public ApplicationLoadingService() {
        this.jarFileUnzipService = new JarFileUnzipService();
    }

    private boolean isJarFile(File file) {
        return file.isFile() && file.getName().endsWith(".jar");
    }

    private void loadSolet(Class soletClass, String applicationName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (soletClass == null
                || soletClass.getSuperclass() == null
                || !soletClass.getSuperclass().getName().equals(BaseHttpSolet.class.getSimpleName())) {
            return;
        }

        Object soletObject = soletClass.getDeclaredConstructor().newInstance();

        Object soletAnnotation = Arrays
                .stream(soletClass.getAnnotations())
                .filter(a -> a.annotationType().getSimpleName().equals(WebSolet.class.getSimpleName()))
                .findFirst()
                .orElse(null);

        String soletRoute = soletAnnotation.getClass().getMethod("route").invoke(soletAnnotation).toString();

        if (!applicationName.equals("ROOT")) {
            soletRoute = "/" + applicationName + soletRoute;
        }

        HttpSolet httpSoletProxy = (HttpSolet) Proxy.newProxyInstance(
                HttpSolet.class.getClassLoader(),
                new Class[]{HttpSolet.class},
                (proxy, method, args) -> {
                    Method extractedMethod = Arrays
                            .stream(soletClass.getMethods())
                            .filter(m -> m.getName().equals(method.getName()))
                            .findFirst()
                            .orElse(null);

                    if (extractedMethod.getName().equals("service")) {
                        Class<?>[] requestParameters = extractedMethod.getParameterTypes();

                        Object proxyRequest = Proxy.newProxyInstance(
                                requestParameters[0].getClassLoader(),
                                new Class[]{requestParameters[0]},
                                (requestProxy, requestMethod, requestArgs) -> {
                                    Method extractedRequestMethod = Arrays
                                            .stream(soletClass.getMethods())
                                            .filter(m -> m.getName().equals(method.getName()))
                                            .findFirst()
                                            .orElse(null);

                                    return extractedRequestMethod.invoke(args[0], requestArgs);
                                }
                        );

                        Object proxyResponse = Proxy.newProxyInstance(
                                requestParameters[1].getClassLoader(),
                                new Class[]{requestParameters[1]},
                                (responseProxy, responseMethod, responseArgs) -> {
                                    Method extractedResponseMethod = Arrays
                                            .stream(soletClass.getMethods())
                                            .filter(m -> m.getName().equals(method.getName()))
                                            .findFirst()
                                            .orElse(null);

                                    return extractedResponseMethod.invoke(args[1], responseArgs);
                                }
                        );

                        return extractedMethod.invoke(soletObject, proxyRequest, proxyResponse);
                    } else if (extractedMethod.getName().equals("init")) {
                        SoletConfig soletConfigObject = new SoletConfigImpl();

                        soletConfigObject.setAttribute("application-folder", this.applicationFolderPath);

                        return extractedMethod.invoke(soletObject, soletConfigObject);
                    }

                    return extractedMethod.invoke(soletObject);
                }
        );

        httpSoletProxy.init(null);

        this.loadedApplications.put(soletRoute, httpSoletProxy);
    }

    private void loadClass(File currentFile, URLClassLoader classLoader, String packageName, String applicationName) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (currentFile.isDirectory()) {
            for (File childFile : currentFile.listFiles()) {
                this.loadClass(childFile, classLoader, (packageName + currentFile.getName() + "."), applicationName);
            }
        } else {
            if (!currentFile.getName().endsWith(".class")) {
                return;
            }

            String className = (packageName.replace("classes.", "")) + currentFile.getName().replace(".class", "").replace(".", "/");

            Class currentClassFile = classLoader.loadClass(className);

            this.loadSolet(currentClassFile, applicationName);
        }
    }

    private void loadApplicationClasses(String classesRootFolderPath, String applicationName) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        File classesRootDirectory = new File(classesRootFolderPath);

        if (!classesRootDirectory.exists() || !classesRootDirectory.isDirectory()) {
            return;
        }

        this.libraryClassUrls.add(new URL("file:/" + classesRootDirectory.getCanonicalPath() + File.separator));

        URL[] urls = this.libraryClassUrls.toArray(new URL[this.libraryClassUrls.size()]);

        URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(urlClassLoader);

        this.loadClass(classesRootDirectory, urlClassLoader, "", applicationName);
    }

    private void loadLibraryFile(JarFile library, String cannonicalPath, String applicationName) throws MalformedURLException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Enumeration<JarEntry> jarFileEntries = library.entries();

        while (jarFileEntries.hasMoreElements()) {
            JarEntry currentEntry = jarFileEntries.nextElement();

            if (!currentEntry.isDirectory() && currentEntry.getName().endsWith(".class")) {
                URL[] urls = new URL[] {
                        new URL("jar:file:" + cannonicalPath + "!/")
                };

                URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                Thread.currentThread().setContextClassLoader(urlClassLoader);

                String className = currentEntry.getName().replace(".class", "").replace("/", ".");

                Class currentClassFile = urlClassLoader.loadClass(className);

                this.loadSolet(currentClassFile, applicationName);
                this.libraryClassUrls.add(urls[0]);
            }
        }
    }

    private void loadApplicationLibraries(String librariesRootFolderPath, String applicationName) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.libraryClassUrls = new ArrayList<>();

        File libraryFolder = new File(librariesRootFolderPath);

        if (!libraryFolder.exists() || !libraryFolder.isDirectory()) {
            throw new IllegalArgumentException("Library Folder does not exist or is not a folder!");
        }

        List<File> allJarFiles = Arrays
                .stream(libraryFolder.listFiles())
                .filter(f -> this.isJarFile(f))
                .collect(Collectors.toList());

        for (File jarFile : allJarFiles) {
            if (jarFile != null) {
                JarFile fileAsJar = new JarFile(jarFile.getCanonicalPath());
                this.loadLibraryFile(fileAsJar, jarFile.getCanonicalPath(), applicationName);
            }
        }
    }

    private Map<String, HttpSolet> loadApplicationFromFolder(String applicationFolderPath, String applicationName) throws IOException {
        this.loadedApplications = new HashMap<>();

        File applicationsFolder = new File(applicationFolderPath);

        if (applicationsFolder.exists() && applicationsFolder.isDirectory()) {
            List<File> allJarFiles = Arrays
                    .stream(applicationsFolder.listFiles())
                    .filter(f -> this.isJarFile(f))
                    .collect(Collectors.toList());

            for (File applicationJarFile : allJarFiles) {
                this.jarFileUnzipService.unzipJar(applicationJarFile);

                this.loadApplicationFromFolder(applicationJarFile.getCanonicalPath().replace(".jar", File.separator), applicationJarFile.getName().replace(".jar", ""));
            }
        }

        return this.loadedApplications;
    }
}
