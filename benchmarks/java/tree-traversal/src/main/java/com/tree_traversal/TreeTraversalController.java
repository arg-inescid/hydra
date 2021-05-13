package com.tree_traversal;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.reactivex.Single;

import static io.micronaut.http.MediaType.TEXT_PLAIN;

@SuppressWarnings("unused")
@Controller()
public class TreeTraversalController {

    @Get
    @Produces(TEXT_PLAIN)
    public Single<String> maxTreeDepth() {
        int depth = new Workload().maxDepth(ArgumentStorage.getServerArgumentStorage().getArraySize());
        return Single.just(String.format("Max tree depth is %d!%n", depth));
    }
}