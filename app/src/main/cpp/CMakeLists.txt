cmake_minimum_required(VERSION 3.22.1)

project(archiey_native LANGUAGES CXX)

add_library(
    archiey_native
    SHARED
    archiey_native.cpp
)

target_compile_features(
    archiey_native
    PRIVATE
    cxx_std_17
)

target_compile_options(
    archiey_native
    PRIVATE
    -O2
    -Wall
    -Wextra
)
