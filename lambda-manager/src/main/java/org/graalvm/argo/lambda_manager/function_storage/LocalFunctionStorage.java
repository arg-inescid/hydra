package org.graalvm.argo.lambda_manager.function_storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;

public class LocalFunctionStorage extends SimpleFunctionStorage {

    /**
     * This method expects the byte array parameter to contain
     * path to the code on a local file system.
     */
    @Override
    public Function register(String functionName, Function function, byte[] codePathEncoded) throws Exception {
        String codePath = new String(codePathEncoded).trim();
        if (!codePath.startsWith("http") && !codePath.startsWith("knative-")) {
            if (Environment.FAASTION_LPI_RUNTIME.equals(function.getRuntime()) && codePath.contains(":")) {
                // Copying two files for Faastion-LPI - instrumented and vanilla.
                String[] codePaths = codePath.split(":");
                String codePathInstrumented = codePaths[0];
                String codePathVanilla = codePaths[1];

                Path srcInstrumented = Paths.get(codePathInstrumented);
                Path srcVanilla = Paths.get(codePathVanilla);
                Path dst = function.buildFunctionSourceCodePath();

                if (dst.getParent().toFile().mkdirs()) {
                    Files.copy(srcInstrumented, dst);
                    String codePathVanillaDst = dst.getFileName().toString() + "_vanilla";
                    Files.copy(srcVanilla, dst.getParent().resolve(codePathVanillaDst));
                }
            } else {
                // The other runtimes have just one function binary.
                Path src = Paths.get(codePath);
                Path dst = function.buildFunctionSourceCodePath();
                if (dst.getParent().toFile().mkdirs()) {
                    Files.copy(src, dst);

                    // Attempt to copy the snapshot files if sandbox snapshotting enabled.
                    if (function.snapshotSandbox()) {
                        int lastDot = codePath.lastIndexOf(".");
                        String commonPath = codePath.substring(0, lastDot == -1 ? codePath.length() : lastDot);
                        Path memsnapSrc = Paths.get(commonPath + ".memsnap");
                        Path metasnapSrc = Paths.get(commonPath + ".metasnap");

                        if (Files.exists(memsnapSrc) && Files.exists(metasnapSrc)) {
                            Path memsnapDst = dst.resolveSibling(dst.getFileName() + ".memsnap");
                            Path metasnapDst = dst.resolveSibling(dst.getFileName() + ".metasnap");

                            Files.copy(memsnapSrc, memsnapDst);
                            Files.copy(metasnapSrc, metasnapDst);
                        }
                    }
                }
            }
        }
        return functions.put(function.getName(), function);
    }

}
