cmake_minimum_required(VERSION 3.22.1)

project("Grogu")

add_compile_definitions(APPLICATION_ID="${APP_ID_BASE}${APP_ID_SDK}${APP_ID_TYPE}")
# ===
add_compile_options(-fno-rtti -fno-exceptions -fvisibility=hidden)
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-fstack-protector)
else ()
    add_compile_options(-Os -g0)
endif ()

# Define the path to the pre-built libusb library and include directory.
set(LIBUSB_INCLUDE_DIR /home/skywalker/libusb/libusb)
set(LIBUSB_LIB_DIR_ARMV7 /home/skywalker/libusb/android/libs/armeabi-v7a)
set(LIBUSB_LIB_DIR_ARM64 /home/skywalker/libusb/android/libs/arm64-v8a)
set(LIBUSB_LIB_DIR_X86 /home/skywalker/libusb/android/libs/x86)
set(LIBUSB_LIB_DIR_X86_64 /home/skywalker/libusb/android/libs/x86_64)

# Add the include directories for the pre-built libusb.
include_directories(${LIBUSB_INCLUDE_DIR})

# Determine target architecture and set appropriate library path
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(LIBUSB_LIB_DIR ${LIBUSB_LIB_DIR_ARMV7})
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(LIBUSB_LIB_DIR ${LIBUSB_LIB_DIR_ARM64})
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(LIBUSB_LIB_DIR ${LIBUSB_LIB_DIR_X86})
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(LIBUSB_LIB_DIR ${LIBUSB_LIB_DIR_X86_64})
else()
    message(FATAL_ERROR "Unsupported target architecture: ${CMAKE_ANDROID_ARCH_ABI}")
endif()

# Specifies the pre-built libusb library.
add_library(libusb1.0 SHARED IMPORTED)
set_target_properties(libusb1.0 PROPERTIES
        IMPORTED_LOCATION ${LIBUSB_LIB_DIR}/libusb1.0.so
)

# Creates and names a library, sets it as either STATIC or SHARED,
# and provides the relative paths to its source code.
add_library(beskar_usb_validator SHARED
        beskar_usb_validator.c
)

target_link_libraries(beskar_usb_validator
        android
        log
        libusb1.0
)

add_library(raw_usb_socket_test SHARED
        raw_usb_socket_test.c
)

# Link the libraries to the targets
target_link_libraries(raw_usb_socket_test
        libusb1.0
        android
        log
)
