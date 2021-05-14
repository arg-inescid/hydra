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

    private final LambdaManager lambdaManager;

    public LambdaManagerController() {
        this.lambdaManager = LambdaManager.getLambdaManager();
    }

    @Get("/{user}/{name}")
    public Single<String> processRequest(@PathVariable("user") String username, @PathVariable("name") String functionName,
                                        @Nullable @QueryValue("args") String functionArguments) {
        return lambdaManager.processRequest(username, functionName, functionArguments);
    }

    @Post(value = "/upload_function", consumes = APPLICATION_OCTET_STREAM)
    public Single<String> uploadLambda(@QueryValue("allocate") int allocate, @QueryValue("user") String username,
                                       @QueryValue("name") String functionName, @Body byte[] functionCode) {
        return lambdaManager.uploadFunction(allocate, username, functionName, functionCode);
    }

    @Post("/remove_function")
    public Single<String> removeLambda(@QueryValue("user") String username, @QueryValue("name") String functionName) {
        return lambdaManager.removeFunction(username, functionName);
    }

    @Post(value = "/configure_manager", consumes = APPLICATION_JSON)
    public Single<String> configureManager(@Body String lambdaManagerConfiguration) {
        return lambdaManager.configureManager(lambdaManagerConfiguration, beanContext);
    }
}