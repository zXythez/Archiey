package com.shattercore.archiey.files

internal object NativeFileOps {

    init {
        System.loadLibrary("archiey_native")
    }

    /**
     * Returns 0 on success, otherwise a Linux errno value.
     *
     * This is a low-level operation. Callers must validate
     * both paths and handle errors.
     */
    external fun renameNoReplaceNative(
        source: String,
        destination: String
    ): Int
}
