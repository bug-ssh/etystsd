cmake_minimum_required(VERSION 3.22.1)
project("voiceconfig")

add_library(voiceconfig SHARED native_config.cpp)

find_library(log-lib log)
target_link_libraries(voiceconfig ${log-lib})
