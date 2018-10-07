cmake_minimum_required(VERSION 3.0)
set(CLEAN_ALL_COMMAND rm -rf ${CMAKE_SOURCE_DIR}/build/*)
FILE(GLOB SRC_FILES *.c)

add_custom_target(
cleanall ${CLEAN_ALL_COMMAND}
)

add_subdirectory(src)