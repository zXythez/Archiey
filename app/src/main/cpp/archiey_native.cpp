#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <unistd.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_shattercore_archiey_files_NativeFileOps_renameNoReplaceNative(
    JNIEnv* env,
    jobject,
    jstring source,
    jstring destination
) {
    if (!source || !destination) {
        return EINVAL;
    }

    const char* src = env->GetStringUTFChars(source, nullptr);
    if (!src) return ENOMEM;

    const char* dst = env->GetStringUTFChars(destination, nullptr);
    if (!dst) {
        env->ReleaseStringUTFChars(source, src);
        return ENOMEM;
    }

    long result = syscall(
        __NR_renameat2,
        AT_FDCWD, src,
        AT_FDCWD, dst,
        1
    );

    int error = result == 0 ? 0 : errno;

    env->ReleaseStringUTFChars(source, src);
    env->ReleaseStringUTFChars(destination, dst);

    return error;
}
