cmake_minimum_required(VERSION 3.22.1)

project("voiceconfig")

# 编译 .so 动态库
add_library(
        voiceconfig       # 库名 → libvoiceconfig.so
        SHARED
        voice_config.cpp
)

# 链接 Android log 库
find_library(log-lib log)

target_link_libraries(
        voiceconfig
        ${log-lib}
)

# 启用优化，增加逆向难度
target_compile_options(voiceconfig PRIVATE
        -O2
        -fvisibility=hidden
        -fstack-protector-strong
)
