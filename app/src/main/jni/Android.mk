LOCAL_PATH:= $(call my-dir)

#serial-port
include $(CLEAR_VARS)
TARGET_PLATFORM := android-3
LOCAL_MODULE    := serial-port
LOCAL_SRC_FILES := SerialPort.c
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

#psam-jni
include $(CLEAR_VARS)
TARGET_PLATFORM := android-3
LOCAL_MODULE:=psam-jni
LOCAL_SRC_FILES := PsamSerialPort.cpp
LOCAL_LDLIBS += -llog
include $(BUILD_SHARED_LIBRARY)  
