package org.graalvm.argo.lambda_manager.function_storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Function;

public class LocalFunctionStorage extends InMemoryFunctionStorage {

    /**
     * This method expects the byte array parameter to contain
     * path to the code on a local file system.
     */
    @Override
    public Function register(String functionName, Function function, byte[] codePathEncoded) throws Exception {
        String codePath = new String(codePathEncoded);
        Path src = Paths.get(codePath);
        Path dst = function.buildFunctionSourceCodePath();
        if (dst.getParent().toFile().mkdirs()) {
            Files.copy(src, dst);

            // Attempt to copy the snapshot files if sandbox snapshotting enabled.
            if (function.snapshotSandbox()) {
                String commonPath = codePath.substring(codePath.lastIndexOf("."));
                Path memsnapSrc = Paths.get(commonPath + "memsnap");
                Path metasnapSrc = Paths.get(commonPath + "metasnap");

                if (Files.exists(memsnapSrc) && Files.exists(metasnapSrc)) {
                    Path memsnapDst = dst.resolveSibling(dst.getFileName() + ".memsnap");
                    Path metasnapDst = dst.resolveSibling(dst.getFileName() + ".metasnap");

                    Files.copy(memsnapSrc, memsnapDst);
                    Files.copy(metasnapSrc, metasnapDst);
                }
            }
        }
        return functions.put(function.getName(), function);
    }

}
