#include <jni.h>
#include <libusb.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "RawUSBSocketChecker"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static JavaVM *javaVM = NULL;
static jclass usbScanHandlerClass = NULL;
static jmethodID sendMessageMethod = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass cls = (*env)->FindClass(env, "com/thisistheway/grogu/UsbScanHandler");
    if (cls == NULL) {
        return JNI_ERR;
    }
    usbScanHandlerClass = (jclass)(*env)->NewGlobalRef(env, cls);
    if (usbScanHandlerClass == NULL) {
        return JNI_ERR;
    }
    sendMessageMethod = (*env)->GetStaticMethodID(env, usbScanHandlerClass, "sendMessageToHandler", "(Ljava/lang/String;Ljava/lang/String;)V");
    if (sendMessageMethod == NULL) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

void send_message_to_handler(const char *message) {
    JNIEnv *env;
    int isAttached = 0;

    if ((*javaVM)->GetEnv(javaVM, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*javaVM)->AttachCurrentThread(javaVM, &env, NULL) != 0) {
            return;
        }
        isAttached = 1;
    }

    jstring jMessage = (*env)->NewStringUTF(env, message);
    (*env)->CallStaticVoidMethod(env, usbScanHandlerClass, sendMessageMethod, jMessage, (*env)->NewStringUTF(env, ""));
    (*env)->DeleteLocalRef(env, jMessage);

    if (isAttached) {
        (*javaVM)->DetachCurrentThread(javaVM);
    }
}

static void log_and_send(const char *format, ...) {
    char message[1024];
    va_list args;
    va_start(args, format);
    vsnprintf(message, sizeof(message), format, args);
    va_end(args);

    LOGD("%s", message);
    send_message_to_handler(message);
}

static void print_endpoint_comp(const struct libusb_ss_endpoint_companion_descriptor *ep_comp)
{
    log_and_send("      USB 3.0 Endpoint Companion:");
    log_and_send("        bMaxBurst:           %u", ep_comp->bMaxBurst);
    log_and_send("        bmAttributes:        %02xh", ep_comp->bmAttributes);
    log_and_send("        wBytesPerInterval:   %u", ep_comp->wBytesPerInterval);
}

static void print_endpoint(const struct libusb_endpoint_descriptor *endpoint)
{
    int i, ret;

    log_and_send("      Endpoint:");
    log_and_send("        bEndpointAddress:    %02xh", endpoint->bEndpointAddress);
    log_and_send("        bmAttributes:        %02xh", endpoint->bmAttributes);
    log_and_send("        wMaxPacketSize:      %u", endpoint->wMaxPacketSize);
    log_and_send("        bInterval:           %u", endpoint->bInterval);
    log_and_send("        bRefresh:            %u", endpoint->bRefresh);
    log_and_send("        bSynchAddress:       %u", endpoint->bSynchAddress);

    for (i = 0; i < endpoint->extra_length;)
    {
        if (LIBUSB_DT_SS_ENDPOINT_COMPANION == endpoint->extra[i + 1])
        {
            struct libusb_ss_endpoint_companion_descriptor *ep_comp;

            ret = libusb_get_ss_endpoint_companion_descriptor(NULL, endpoint, &ep_comp);
            if (LIBUSB_SUCCESS != ret)
                continue;

            print_endpoint_comp(ep_comp);

            libusb_free_ss_endpoint_companion_descriptor(ep_comp);
        }

        i += endpoint->extra[i];
    }
}

static void print_altsetting(const struct libusb_interface_descriptor *interface)
{
    uint8_t i;

    log_and_send("    Interface:");
    log_and_send("      bInterfaceNumber:      %u", interface->bInterfaceNumber);
    log_and_send("      bAlternateSetting:     %u", interface->bAlternateSetting);
    log_and_send("      bNumEndpoints:         %u", interface->bNumEndpoints);
    log_and_send("      bInterfaceClass:       %u", interface->bInterfaceClass);
    log_and_send("      bInterfaceSubClass:    %u", interface->bInterfaceSubClass);
    log_and_send("      bInterfaceProtocol:    %u", interface->bInterfaceProtocol);
    log_and_send("      iInterface:            %u", interface->iInterface);

    for (i = 0; i < interface->bNumEndpoints; i++)
        print_endpoint(&interface->endpoint[i]);
}

static void print_2_0_ext_cap(struct libusb_usb_2_0_extension_descriptor *usb_2_0_ext_cap)
{
    log_and_send("    USB 2.0 Extension Capabilities:");
    log_and_send("      bDevCapabilityType:    %u", usb_2_0_ext_cap->bDevCapabilityType);
    log_and_send("      bmAttributes:          %08xh", usb_2_0_ext_cap->bmAttributes);
}

static void print_ss_usb_cap(struct libusb_ss_usb_device_capability_descriptor *ss_usb_cap)
{
    log_and_send("    USB 3.0 Capabilities:");
    log_and_send("      bDevCapabilityType:    %u", ss_usb_cap->bDevCapabilityType);
    log_and_send("      bmAttributes:          %02xh", ss_usb_cap->bmAttributes);
    log_and_send("      wSpeedSupported:       %u", ss_usb_cap->wSpeedSupported);
    log_and_send("      bFunctionalitySupport: %u", ss_usb_cap->bFunctionalitySupport);
    log_and_send("      bU1devExitLat:         %u", ss_usb_cap->bU1DevExitLat);
    log_and_send("      bU2devExitLat:         %u", ss_usb_cap->bU2DevExitLat);
}

static void print_bos(libusb_device_handle *handle)
{
    struct libusb_bos_descriptor *bos;
    uint8_t i;
    int ret;

    ret = libusb_get_bos_descriptor(handle, &bos);
    if (ret < 0)
        return;

    log_and_send("  Binary Object Store (BOS):");
    log_and_send("    wTotalLength:            %u", bos->wTotalLength);
    log_and_send("    bNumDeviceCaps:          %u", bos->bNumDeviceCaps);

    for (i = 0; i < bos->bNumDeviceCaps; i++)
    {
        struct libusb_bos_dev_capability_descriptor *dev_cap = bos->dev_capability[i];

        if (dev_cap->bDevCapabilityType == LIBUSB_BT_USB_2_0_EXTENSION)
        {
            struct libusb_usb_2_0_extension_descriptor *usb_2_0_extension;

            ret = libusb_get_usb_2_0_extension_descriptor(NULL, dev_cap, &usb_2_0_extension);
            if (ret < 0)
                return;

            print_2_0_ext_cap(usb_2_0_extension);
            libusb_free_usb_2_0_extension_descriptor(usb_2_0_extension);
        }
        else if (dev_cap->bDevCapabilityType == LIBUSB_BT_SS_USB_DEVICE_CAPABILITY)
        {
            struct libusb_ss_usb_device_capability_descriptor *ss_dev_cap;

            ret = libusb_get_ss_usb_device_capability_descriptor(NULL, dev_cap, &ss_dev_cap);
            if (ret < 0)
                return;

            print_ss_usb_cap(ss_dev_cap);
            libusb_free_ss_usb_device_capability_descriptor(ss_dev_cap);
        }
    }

    libusb_free_bos_descriptor(bos);
}

static void print_interface(const struct libusb_interface *interface)
{
    int i;

    for (i = 0; i < interface->num_altsetting; i++)
        print_altsetting(&interface->altsetting[i]);
}

static void print_configuration(struct libusb_config_descriptor *config)
{
    uint8_t i;

    log_and_send("  Configuration:");
    log_and_send("    wTotalLength:            %u", config->wTotalLength);
    log_and_send("    bNumInterfaces:          %u", config->bNumInterfaces);
    log_and_send("    bConfigurationValue:     %u", config->bConfigurationValue);
    log_and_send("    iConfiguration:          %u", config->iConfiguration);
    log_and_send("    bmAttributes:            %02xh", config->bmAttributes);
    log_and_send("    MaxPower:                %u", config->MaxPower);

    for (i = 0; i < config->bNumInterfaces; i++)
        print_interface(&config->interface[i]);
}

static void print_device(libusb_device *dev, libusb_device_handle *handle)
{
    struct libusb_device_descriptor desc;
    unsigned char string[256];
    const char *speed;
    int ret;
    uint8_t i;

    switch (libusb_get_device_speed(dev))
    {
        case LIBUSB_SPEED_LOW:
            speed = "1.5M";
            break;
        case LIBUSB_SPEED_FULL:
            speed = "12M";
            break;
        case LIBUSB_SPEED_HIGH:
            speed = "480M";
            break;
        case LIBUSB_SPEED_SUPER:
            speed = "5G";
            break;
        case LIBUSB_SPEED_SUPER_PLUS:
            speed = "10G";
            break;
        case LIBUSB_SPEED_SUPER_PLUS_X2:
            speed = "20G";
            break;
        default:
            speed = "Unknown";
    }

    ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0)
    {
        log_and_send("failed to get device descriptor");
        return;
    }

    log_and_send("Dev (bus %u, device %u): %04X - %04X speed: %s",
                 libusb_get_bus_number(dev), libusb_get_device_address(dev),
                 desc.idVendor, desc.idProduct, speed);

    if (handle)
    {
        if (desc.iManufacturer)
        {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iManufacturer, string, sizeof(string));
            if (ret > 0)
                log_and_send("  Manufacturer:              %s", (char *)string);
        }

        if (desc.iProduct)
        {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iProduct, string, sizeof(string));
            if (ret > 0)
                log_and_send("  Product:                   %s", (char *)string);
        }

        if (desc.iSerialNumber)
        {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iSerialNumber, string, sizeof(string));
            if (ret > 0)
                log_and_send("  Serial Number:             %s", (char *)string);
        }
    }

    for (i = 0; i < desc.bNumConfigurations; i++)
    {
        struct libusb_config_descriptor *config;

        ret = libusb_get_config_descriptor(dev, i, &config);
        if (LIBUSB_SUCCESS != ret)
        {
            log_and_send("  Couldn't retrieve descriptors");
            continue;
        }

        print_configuration(config);

        libusb_free_config_descriptor(config);
    }

    if (handle && desc.bcdUSB >= 0x0201)
        print_bos(handle);
}

JNIEXPORT void JNICALL
Java_com_thisistheway_grogu_RawSocketCheckerActivity_startRawSocketTest(JNIEnv *env, jobject instance, jint fileDescriptor) {
    libusb_context *ctx = NULL;
    libusb_device_handle *devh = NULL;
    libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY);
    int r;

    // Initialize libusb
    r = libusb_init(&ctx);
    if (r < 0) {
        log_and_send("libusb_init failed: %d", r);
        return;
    }

    // Wrap the system device using the file descriptor
    r = libusb_wrap_sys_device(ctx, (intptr_t)fileDescriptor, &devh);
    if (r < 0) {
        log_and_send("libusb_wrap_sys_device failed: %d", r);
        libusb_exit(ctx);
        return;
    }

    // Print device details
    libusb_device *dev = libusb_get_device(devh);
    print_device(dev, devh);

    // Close the device and clean up
    libusb_close(devh);
    libusb_exit(ctx);
}
