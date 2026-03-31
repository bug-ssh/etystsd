cmake_minimum_required(VERSION 3.22.1)
project("voice_config")

add_library(voice_config SHARED native_config.cpp)
find_library(log-lib log)
target_link_libraries(voice_config ${log-lib})

# Strip symbols in release for smaller .so
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s")
