package com.array_sorting;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.reactivex.Single;

import static io.micronaut.http.MediaType.TEXT_PLAIN;

@SuppressWarnings("unused")
@Controller()
public class ArraySortingController {

    @Get
    @Produces(TEXT_PLAIN)
    public Single<String> maxTreeDepth() {
        boolean success = new Workload().intersect(ArgumentStorage.getServerArgumentStorage().getArraySize());
        return Single.just(success ? "Array is sorted!\n" : "Array isn't sorted! Something went wrong! :(\n");
    }
}