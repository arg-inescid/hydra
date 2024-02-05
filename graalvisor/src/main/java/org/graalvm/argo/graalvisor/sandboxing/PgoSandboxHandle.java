package org.graalvm.argo.graalvisor.sandboxing;

import org.graalvm.argo.graalvisor.function.PolyglotFunction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getenv;
import static java.nio.file.attribute.PosixFilePermission.*;
import static java.util.Arrays.asList;
import static org.graalvm.argo.graalvisor.Main.APP_DIR;
import static org.graalvm.argo.graalvisor.Main.MINIO_PASSWORD;
import static org.graalvm.argo.graalvisor.Main.MINIO_SERVER;
import static org.graalvm.argo.graalvisor.Main.MINIO_URL;
import static org.graalvm.argo.graalvisor.Main.MINIO_USER;

public class PgoSandboxHandle extends SandboxHandle {
    private final PgoSandboxProvider pgoProvider;

    public PgoSandboxHandle(PgoSandboxProvider pgoProvider) {
        this.pgoProvider = pgoProvider;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        final PolyglotFunction function = pgoProvider.getFunction();
        return executeCommand(function.getName(), jsonArguments);
    }

    private String executeCommand(String function, String parameter) {
        StringBuilder response = new StringBuilder();
        try {
            final String newAppPath = APP_DIR + function + "-app";
            final Runtime runtime = getRuntime();
            final File functionPath = new File(newAppPath);

            if (functionPath.mkdir()) {
                final Path source = Path.of(APP_DIR + function);
                final Path newDir = Path.of(newAppPath);
                Files.move(source, newDir.resolve(source.getFileName()));
                makeFunctionExecutable(newDir, source);
            }


            final String[] envs = {};
            final String command = "." + newAppPath + "/" + function + " " + parameter;
            Process process = runtime.exec(command, envs);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("line: " + line);
                response.append(line);
            }

            System.out.println("Standard error:");
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();

            final Path source = Path.of(newAppPath + "/default.iprof");

            if (Files.exists(source)) {
                final File pgoPath = new File(newAppPath + "/pgo-profiles");

                pgoPath.mkdir();

                final Path newDir = Path.of(pgoPath.getPath());
                final String pgoFileName = "pgo-" + currentTimeMillis() + ".iprof";
                Files.move(source, newDir.resolve(pgoFileName));
                copyIprofToMinio(pgoPath, function, pgoFileName);
            }

            System.out.println("exit: " + process.exitValue());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response.toString();
    }

    private void copyIprofToMinio(File pgoPath, String functionName, String pgoFileName) throws IOException, InterruptedException {
        final Runtime runtime = getRuntime();
            final String[] envs = { "LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                    "_=/usr/bin/env" };

            // the command below creates an alias for the MINION_SERVER using the MINIO_URL with user and password,
            // then the mc mb command uses the functionName to create a bucket with the same name in the Minio and ignore if already exists.
            // In the end the mc cp copies all iprof files to Minio
            final String command = String.format("mc alias set %s %s %s %s && mc mb --ignore-existing %s/%s"  +
                    " && mc cp %s/%s %s/%s", MINIO_SERVER, MINIO_URL, MINIO_USER, MINIO_PASSWORD, MINIO_SERVER, functionName, pgoPath, pgoFileName, MINIO_SERVER, functionName);

            final String[] commandMinio = { "/bin/bash", "-c", command };
            Process processMinio = runtime.exec(commandMinio, envs, pgoPath);
            BufferedReader brMinio = new BufferedReader(
                    new InputStreamReader(processMinio.getInputStream()));

            BufferedReader stdErrorMinio = new BufferedReader(new
                    InputStreamReader(processMinio.getErrorStream()));

            String lineMinio;
            while ((lineMinio = brMinio.readLine()) != null) {
                System.out.println("lineMinio: " + lineMinio);
            }

            System.out.println("Standard error Minio:");
            while ((lineMinio = stdErrorMinio.readLine()) != null) {
                System.out.println(lineMinio);
            }

            processMinio.waitFor();
            System.out.println("exit Minio: " + processMinio.exitValue());
    }

    private static void makeFunctionExecutable(Path newDir, Path source) throws IOException {
        HashSet<PosixFilePermission> permissions = new HashSet<>(asList(GROUP_EXECUTE, OWNER_EXECUTE, OTHERS_EXECUTE));
        final File file = new File(newDir.toString() + "/" + source.getFileName());
        Files.setPosixFilePermissions(file.toPath(), permissions);
    }

    @Override
    public String toString() {
        return null;
    }

}
