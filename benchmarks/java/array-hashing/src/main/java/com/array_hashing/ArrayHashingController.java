package com.array_hashing;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.reactivex.Single;

import static io.micronaut.http.MediaType.TEXT_PLAIN;

@SuppressWarnings("unused")
@Controller()
public class ArrayHashingController {

    @Get
    @Produces(TEXT_PLAIN)
    public Single<String> findLHS() {
        int longestSequence = new Workload().findLHS(ArgumentStorage.getServerArgumentStorage().getArraySize());
        return Single.just(String.format("Longest harmonious sequence is %s!%n", longestSequence));
    }
}