package org.graalvm.argo.lambda_manager;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.Single;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.utils.MetricsProvider;

import javax.inject.Inject;

@SuppressWarnings("unused")
@ExecuteOn(TaskExecutors.IO)
@Controller()
public class LambdaManagerController {

    @Inject
    private BeanContext beanContext;

    @Post(value = "/{username}/{function_name}", consumes = MediaType.APPLICATION_JSON)
    public Single<String> processRequest(@PathVariable("username") String username,
                                         @PathVariable("function_name") String functionName,
                                         @Nullable @Body String arguments,
                                         @Nullable @QueryValue("count") String warmupCount) {
        // Note: by default, warmupCount is null. Only used for demos using the web interface.
        if (warmupCount != null) {
            for (int i = 0; i < Integer.valueOf(warmupCount); i++) {
                LambdaManager.processRequest(username, functionName, arguments);
            }
        }
        return LambdaManager.processRequest(username, functionName, arguments);
    }

    @Get("/get_functions")
    public Single<String> getFunctions() {
        return LambdaManager.getFunctions();
    }

    @Post(value = "/upload_function", consumes = MediaType.APPLICATION_OCTET_STREAM)
    public Single<String> uploadFunction(@QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName,
                                         @QueryValue("function_language") String functionLanguage,
                                         @QueryValue("function_entry_point") String functionEntryPoint,
                                         @QueryValue("function_memory") String functionMemory,
                                         @Nullable @QueryValue("function_runtime") String functionRuntime,
                                         @Nullable @QueryValue("function_isolation") Boolean functionIsolation,
                                         @Body byte[] functionCode) {
        return LambdaManager.uploadFunction(username, functionName, functionLanguage, functionEntryPoint, functionMemory, functionRuntime, functionCode, Boolean.TRUE.equals(functionIsolation));
    }

    @Post("/remove_function")
    public Single<String> removeFunction(@QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName) {
        return LambdaManager.removeFunction(username, functionName);
    }

    @Post(value = "/configure_manager", consumes = MediaType.APPLICATION_JSON)
    public Single<String> configureManager(@Body String lambdaManagerConfiguration) {
        return LambdaManager.configureManager(lambdaManagerConfiguration, beanContext);
    }

    @Get(value = "/metrics", produces = MediaType.TEXT_PLAIN)
    public Single<String> scrapeMetrics() {
        return MetricsProvider.getFootprintAndScalability();
    }

}
