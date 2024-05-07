#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <memisolation.h>
#include <time.h>

static void * ( * real_dlopen)(const char * , int) = NULL;

/*
 * Debug prints
 */
#ifdef PRL_DBG
  #define PRL_DBM(...)				\
    do {					\
      fprintf(stderr, __VA_ARGS__);		\
      fprintf(stderr, "\n");			\
    } while(0)
#else // disable debug
  #define PRL_DBM(...)
#endif

char*
extract_id(const char* filePath)
{
    char* baseName = strrchr(filePath, '/');
    if (baseName != NULL) {
        return baseName + 1;
    }
    return (char*)filePath;
}

void *
dlopen(const char * input, int flag)
{
    if (real_dlopen == NULL) {
        real_dlopen = (void *(*) (const char *, int)) dlsym(RTLD_NEXT, "dlopen");
    }

    PRL_DBM("[PRELOAD]: Opening library: %s", input);

    if (strstr(input, "/tmp/app/skondlap") != NULL) {
        char* argo_home = getenv("ARGO_HOME");
        char* graal_libs_dir = "graalvisor/build/libs";
        char jni_dir[128];
        sprintf(jni_dir, "%s/%s", argo_home, graal_libs_dir);

        char* id = extract_id(input);

        char native_path[256];
        sprintf(native_path, "%s/lib%s-jni.so", jni_dir, id);
        
        real_dlopen(native_path, RTLD_NOW | RTLD_DEEPBIND | RTLD_GLOBAL);
        insert_memory_regions(id, native_path);
    }

    return real_dlopen(input, flag);
}

static void 
__attribute__((constructor)) init(void)
{
    PRL_DBM("[PRELOAD] Initializing...");
    real_dlopen = (void *(*) (const char *, int)) dlsym(RTLD_NEXT, "dlopen");
}
