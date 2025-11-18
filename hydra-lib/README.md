# Hydra API library

Hydra API library, containing APIs for guest and host applications.

## Project Structure

[Hydra](src/main/java/com/oracle/svm/hydra/Hydra.java): Definition of Java Access methods to the underlying C struct.

[HydraImpl](src/main/java/com/oracle/svm/hydra/HydraImpl.java): Implementations of functions that are instrumented into Hydra struct. The functions pointers are assigned here and later exposed to guest Isolate.

[Hydra API](src/main/java/com/oracle/svm/hydra/api/) APIs used for host applications. Using Hydra API, the host application can load dynamically-linked library into memory. After that, the host application can manage guest isolates and call invocation into them. More details please refer to [HydraAPI](src/main/java/com/oracle/svm/hydra/api/HydraAPI.java)

[Hydra Guest API](src/main/java/com/oracle/svm/hydra/guestapi/) APIs used for guest applications. Through the APIs defined in [GuestAPI](src/main/java/com/oracle/svm/hydra/guestapi/GuestAPI.java) guest application can delegate different function call to the host isolate, including string copying and file manipulation.

[Hydra Isolate Types](src/main/java/com/oracle/svm/hydra/types/) Types with empty extension to distinguish host isolates and guest isolates.

[utils](src/main/java/com/oracle/svm/hydra/utils/) Utilities including String retrieving, build options checking and file access mode.

[resources](src/main/resources/com.oracle.svm.hydra.headers) Contains header files that describe isolate native api and Hydra C struct.

## Native Image Builder Options

`-DHydraHost=true` Telling native-image builder that current application is going to build as a Hydra host application. In this case `com.oracle.svm.hydra-1.0-host.jar` should be used as dependency.

`-DHydraGuest=true` Telling native-image builder that current application is going to build as a Hydra guest application. In this case `com.oracle.svm.hydra-1.0-guest.jar` should be used as dependency.

***One and only one of the `-DHydraGuest` and `-DHydraGuest` must be set as true.***

`-Dcom.oracle.svm.hydra.libraryPath=PATH_TO_HEADERS` Telling the native-image builder the position of the header files `graal_visor.h` and `graal_isolate.h`

## How to build

run `./build.sh` would create Hydra guest library, host library and commons library under `build/libs/*.jar`
