cmake_minimum_required(VERSION 3.25)

# On a Mac, we'll normally get LLVM / AppleClang 13. The line below forces gcc-11
# (via `brew install gcc`), which seems to yield code that's 15% slower than Clang.

# set(CMAKE_C_COMPILER "/usr/local/bin/gcc-11" CACHE STRING "gcc compiler" FORCE)

project(hacl)

# This line requests that we compile universal binaries.
# More details:
#   https://stackoverflow.com/a/65811061
#   https://stackoverflow.com/questions/67490441/cmake-universal-binary-arch-depending-compile-options
# CMAKE_OSX_ARCHITECTURES=arm64;x86_64
set (CMAKE_OSX_ARCHITECTURES arm64 x86_64)

# Doesn't work on cmake 3.21; seems to require a newer version
# (Had to run `brew install homebrew/cask/cmake` to make it work on my Mac.)

include(FetchContent)
FetchContent_Declare(hacl
        DOWNLOAD_EXTRACT_TIMESTAMP TRUE
        URL https://github.com/cryspen/hacl-packages/archive/refs/tags/c-v0.6.0.zip
    )
FetchContent_MakeAvailable(hacl)

# Old usage:
# - mkdir build
# - cd build
# - cmake -DCMAKE_BUILD_TYPE=Release ..
# - make
# Results in build/libhacl.a 

# New usage: (?)