package com.lambda_manager.connectivity;

import com.lambda_manager.core.LambdaManager;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.Single;

import javax.annotation.Nullable;

import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM;

@SuppressWarnings("unused")
@ExecuteOn(TaskExecutors.IO)
@Controller()
public class LambdaManagerController {

    private final LambdaManager lambdaManager;

    public LambdaManagerController() {
        this.lambdaManager = LambdaManager.getLambdaManager();
    }

    @Get("/{user}/{name}")
    public Single<String> processRequest(@PathVariable("user") String username, @PathVariable("name") String lambdaName,
                                         @Nullable @QueryValue("args") String args) {
        return lambdaManager.processRequest(username, lambdaName, args);
    }

    @Post(value = "/upload_lambda", consumes = APPLICATION_OCTET_STREAM)
    public Single<String> uploadLambda(@QueryValue("allocate") int allocate, @QueryValue("user") String username,
                                       @QueryValue("name") String lambdaName, @Body byte[] octetStreamData) {
        return lambdaManager.uploadLambda(allocate, username, lambdaName, octetStreamData);
    }

    @Post("/remove_lambda")
    public Single<String> removeLambda(@QueryValue("user") String username, @QueryValue("name") String lambdaName) {
        return lambdaManager.removeLambda(username, lambdaName);
    }

    @Post(value = "/configure_manager", consumes = APPLICATION_JSON)
    public Single<String> startManager(@Body String configData) {
        return lambdaManager.startManager(configData);
    }
}