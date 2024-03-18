#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <erim.h>
#include <common.h>
#include "memisolation.h"
#include "JNIWrapper.h"

const char* JNICALL Faastion_GetStringUTFChars(JNIEnv* env, jstring string, jboolean* isCopy) {
    JNI_DBM("[JNI]: Received GetStringUTFChars call from domain %d.", domain);
    __wrpkru(0);
    const char* cstring = (*GetStringUTFChars)(env, string, isCopy);
    __wrpkrumem(ERIM_DOMAIN(domain));
    return cstring;
}

void JNICALL Faastion_ReleaseStringUTFChars(JNIEnv *env, jstring string, const char* utf) {
    JNI_DBM("[JNI]: Received ReleaseStringUTFChars call from domain %d.", domain);
    __wrpkru(0);
    (*ReleaseStringUTFChars)(env, string, utf);
    __wrpkrumem(ERIM_DOMAIN(domain));
}

void init_jni_wrapper(JNIEnv* env) {
    GetStringUTFChars = (*env)->GetStringUTFChars;
    ReleaseStringUTFChars = (*env)->ReleaseStringUTFChars;
    (*(struct JNINativeInterface_ **)env)->GetStringUTFChars = Faastion_GetStringUTFChars;
    (*(struct JNINativeInterface_ **)env)->ReleaseStringUTFChars = Faastion_ReleaseStringUTFChars;
}
