package com.hello_world;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.reactivex.Single;

import static io.micronaut.http.MediaType.TEXT_PLAIN;

@SuppressWarnings("unused")
@Controller()
public class HelloWorldController {
    @Get
    @Produces(TEXT_PLAIN)
    public Single<String> hello() {
        return Single.just("Hello world!\n");
    }
}