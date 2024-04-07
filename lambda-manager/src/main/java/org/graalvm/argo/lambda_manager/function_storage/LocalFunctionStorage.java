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
    public Function register(String functionName, Function function, byte[] codePath) throws Exception {
        Path src = Paths.get(new String(codePath));
        Path dst = function.buildFunctionSourceCodePath();
        if (dst.getParent().toFile().mkdirs()) {
            Files.copy(src, dst);
        }
        return functions.put(function.getName(), function);
    }

}
