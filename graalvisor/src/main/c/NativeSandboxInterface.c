#include <jni.h>
#include <stdio.h>
#include "org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface.h"

JNIEXPORT void JNICALL Java_org_graalvm_argo_graalvisor_sandboxing_NativeSandboxInterface_sayHello(JNIEnv *env, jobject thisObj) {
   printf("Hello World in C!\n");
   return;
}
