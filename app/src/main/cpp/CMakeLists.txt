cmake_minimum_required(VERSION 3.22.1)
project("blespam-secure")

add_library(${CMAKE_PROJECT_NAME} SHARED native-secrets.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
        -fvisibility=hidden -ffunction-sections -fdata-sections -O2 -DNDEBUG)
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE -Wl,--gc-sections -Wl,--strip-all -Wl,-z,max-page-size=16384)