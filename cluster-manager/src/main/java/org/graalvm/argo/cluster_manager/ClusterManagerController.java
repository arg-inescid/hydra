package org.graalvm.argo.cluster_manager;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.Single;

import javax.inject.Inject;

import org.graalvm.argo.cluster_manager.utils.JsonUtils;

import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM;

@SuppressWarnings("unused")
@ExecuteOn(TaskExecutors.IO)
@Controller()
public class ClusterManagerController {

    @Inject private BeanContext beanContext;

    @Get("/{username}/{function_name}")
    public Single<String> processRequest(@PathVariable("username") String username,
                    @PathVariable("function_name") String functionName,
                    @Nullable @QueryValue("parameters") String parameters) {
        return JsonUtils.constructJsonResponseObject(ClusterManager.processRequest(username, functionName, parameters, beanContext));
    }

    @Post(value = "/upload_function", consumes = APPLICATION_OCTET_STREAM)
    public Single<String> uploadFunction(@QueryValue("allocate") int allocate,
                    @QueryValue("username") String username,
                    @QueryValue("function_name") String functionName,
                    @QueryValue("function_language") String functionLanguage,
                    @QueryValue("function_entry_point") String functionEntryPoint,
                    @Nullable @QueryValue("arguments") String arguments,
                    @Body byte[] functionCode) {
        return JsonUtils.constructJsonResponseObject(ClusterManager.uploadFunction(allocate, username, functionName, functionLanguage, functionEntryPoint, arguments, functionCode, beanContext));
    }

    @Post("/remove_function")
    public Single<String> removeFunction(@QueryValue("username") String username,
                    @QueryValue("function_name") String functionName) {
        return JsonUtils.constructJsonResponseObject(ClusterManager.removeFunction(username, functionName, beanContext));
    }

    @Post(value = "/configure_manager", consumes = APPLICATION_JSON)
    public Single<String> configureManager(@Body String managerConfiguration) {
    	return JsonUtils.constructJsonResponseObject(ClusterManager.configureManager(managerConfiguration, beanContext));
    }
}
