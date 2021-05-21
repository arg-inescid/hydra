package com.lambda_manager.connectivity;

import com.lambda_manager.core.LambdaManager;
import io.micronaut.context.BeanContext;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.Single;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM;

@SuppressWarnings("unused")
@ExecuteOn(TaskExecutors.IO)
@Controller()
public class LambdaManagerController {

    @Inject
    private BeanContext beanContext;

    @Get("/{username}/{function_name}")
    public Single<String> processRequest(@PathVariable("username") String username,
                                         @PathVariable("function_name") String functionName,
                                         @Nullable @QueryValue("parameters") String parameters) {
        return LambdaManager.processRequest(username, functionName, parameters);
    }

    @Post(value = "/upload_function", consumes = APPLICATION_OCTET_STREAM)
    public Single<String> uploadFunction(@QueryValue("allocate") int allocate,
                                         @QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName,
                                         @Nullable @QueryValue("arguments") String arguments,
                                         @Body byte[] functionCode) {
        return LambdaManager.uploadFunction(allocate, username, functionName, arguments, functionCode);
    }

    @Post("/remove_function")
    public Single<String> removeFunction(@QueryValue("username") String username,
                                         @QueryValue("function_name") String functionName) {
        return LambdaManager.removeFunction(username, functionName);
    }

    @Post(value = "/configure_manager", consumes = APPLICATION_JSON)
    public Single<String> configureManager(@Body String lambdaManagerConfiguration) {
        return LambdaManager.configureManager(lambdaManagerConfiguration, beanContext);
    }
}