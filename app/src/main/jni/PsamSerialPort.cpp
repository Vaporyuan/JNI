#include <stdio.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>  
#include <stdio.h>  
#include <jni.h>  
#include <assert.h>  
  
#include <termios.h>  
#include <unistd.h>  
#include <sys/types.h>  
#include <sys/stat.h>  
#include <fcntl.h>  
#include <string.h>  
#include <android/log.h>  
#include <errno.h>
 

//#include "com_meigsmart_meigrs32_util_SerialPort.h"
#include "com_serialport_PsamSerialPort.h"

static const char *TAG = "serial_port";  
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)  
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)  
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)  

#define PORT_LENGHT 256


/*static JNINativeMethod gMethods[] = {  

};*/
  
/* 
 * 为某一个类注册本地方法 
 */  
/*static int registerNativeMethods(JNIEnv* env, const char* className,  
        JNINativeMethod* gMethods, int numMethods) {  
    jclass clazz;  
    clazz = env->FindClass(className);  
    if (clazz == NULL) {  
        return JNI_FALSE;  
    }  
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {  
        return JNI_FALSE;  
    }  
  
    return JNI_TRUE;  
}*/
  
/* 
 * 为所有类注册本地方法 
 */  
/*static int registerNatives(JNIEnv* env) {  
    const char* kClassName = "com/example/serialutil/SerialActivity"; //指定要注册的类  
    return registerNativeMethods(env, kClassName, gMethods,  
            sizeof(gMethods) / sizeof(gMethods[0]));  
}*/ 
  
/* 
 * System.loadLibrary("lib")时调用 
 * 如果成功返回JNI版本, 失败返回-1 
 */  
/*JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {  
    JNIEnv* env = NULL;  
    jint result = -1; */ 
  
	/*
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {  
        return -1;  
    }  
    assert(env != NULL);  
  
    if (!registerNatives(env)) { //注册  
        return -1;  
    }  
	*/
    //成功  
	//result=JNI_VERSION_1_6;
    //result = JNI_VERSION_1_4;  
  
   // return result;  
//}



struct PortInfo{
    int busy;
    char name[32];
    int handle;
};
#define MAX_PORTS 4
struct PortInfo ports[MAX_PORTS];

long GetBaudRate_K(long baudRate)
{
    long BaudR;
    switch(baudRate)
    {
    case 115200:
        BaudR=B115200;
        break;
    case 57600:
        BaudR=B57600;
        break;
    case 38400:
        BaudR=B38400;
        break;
    case 9600:
        BaudR=B9600;
        break;
    default:
        BaudR=B0;
    }
    return BaudR;
}

char* Jstring2CStr(JNIEnv* env, jstring jstr)
{
	 char* rtn = NULL;
	 jclass clsstring = env->FindClass("java/lang/String");
	 jstring strencode = env->NewStringUTF("GB2312");
	 jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
	 jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr,mid,strencode);
	 jsize alen = env->GetArrayLength(barr);
	 jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);
	 if(alen > 0)
	 {
	  rtn   =   (char*)malloc(alen+1);         //new   char[alen+1];
	  memcpy(rtn,ba,alen);
	  rtn[alen]=0;
	 }
	 env->ReleaseByteArrayElements(barr,ba,0);
	 return rtn;
}

int OpenComConfig(int port,
                  const char deviceName[],
                  long baudRate/*,
                  int parity,
                  int dataBits,
                  int stopBits,
                  int iqSize,
                  int oqSize*/)
{
    struct termios newtio;
    //long BaudR;

    ports[port].busy = 1;
    strcpy(ports[port].name,deviceName);
	LOGD("name '%s'\n", ports[port].name);
    if ((ports[port].handle = open(deviceName, O_RDWR)) == -1)
    {
        perror("open");
        assert(0);
    }


    newtio.c_cflag = CS8 | CLOCAL | CREAD ;
    newtio.c_iflag = IGNPAR;
    newtio.c_oflag = 0;
    newtio.c_lflag = 0;
    newtio.c_cc[VINTR]    = 0;
    newtio.c_cc[VQUIT]    = 0;
    newtio.c_cc[VERASE]   = 0;
    newtio.c_cc[VKILL]    = 0;
    newtio.c_cc[VEOF]     = 4;
    newtio.c_cc[VTIME]    = 0;
    newtio.c_cc[VMIN]     = 1;
    newtio.c_cc[VSWTC]    = 0;
    newtio.c_cc[VSTART]   = 0;
    newtio.c_cc[VSTOP]    = 0;
    newtio.c_cc[VSUSP]    = 0;
    newtio.c_cc[VEOL]     = 0;
    newtio.c_cc[VREPRINT] = 0;
    newtio.c_cc[VDISCARD] = 0;
    newtio.c_cc[VWERASE]  = 0;
    newtio.c_cc[VLNEXT]   = 0;
    newtio.c_cc[VEOL2]    = 0;
    cfsetospeed(&newtio, GetBaudRate_K(baudRate));
    cfsetispeed(&newtio, GetBaudRate_K(baudRate));
    tcsetattr(ports[port].handle, TCSANOW, &newtio);
    return 0;
}

int OpenCom(int portNo, const char deviceName[],long baudRate)
{
    return OpenComConfig(portNo, deviceName, baudRate/*, 1, 8, 1, 0, 0*/);
}

int CloseCom(int portNo)
{
    if (ports[portNo].busy)
    {
        close(ports[portNo].handle);
        ports[portNo].busy = 0;
        return 0;
    }
    else
    {
        return -1;
    }
}

int ComRd(int portNo, char buf[], int maxCnt,int Timeout)
{
    int actualRead = 0;
    fd_set rfds;
    struct timeval tv;
    int retval;

    if (!ports[portNo].busy)
    {
        assert(0);
    }
	LOGD("portNo '%d'\n", portNo);
	LOGD("busy '%d'", ports[portNo].busy);

    FD_ZERO(&rfds);
	LOGD("handle '%d'", ports[portNo].handle);
    FD_SET(ports[portNo].handle, &rfds);
	retval = FD_ISSET(ports[portNo].handle, &rfds);
	LOGD("retval1 %d\n", retval);
    tv.tv_sec = Timeout/1000;
    tv.tv_usec = (Timeout%1000)*1000;
    retval = select(ports[portNo].handle+1, &rfds, NULL, NULL, &tv);
	LOGD("retval %d\n", retval);
    if (retval)
    {
        actualRead = read(ports[portNo].handle, buf, maxCnt);
    }
    
    if(actualRead>0)
    {
        int i;
        for (i = 0; i < actualRead; ++i)
        {
           LOGD("<%02X", buf[i]);
        }
    }
    fflush(stdout);

    return actualRead;
}

int ComWrt(int portNo, const char *buf, int maxCnt)
{
    int written;

    if (!ports[portNo].busy)
    {
        assert(0);
    }

    {
        int i;
        for (i = 0; i < maxCnt; ++i)
        {
           LOGD(">%02X", buf[i]);
        }
    }

    fflush(stdout);
    
    written = write(ports[portNo].handle, buf, maxCnt);
    return written;
}


/*
 * Class:     com_serialport_PsamSerialPort
 * Method:    test
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_serialport_PsamSerialPort_test(JNIEnv * env, jobject obj, jstring jport, jint jbaud, jintArray reqArray, jintArray respArray){
    int portno=0;
    int ret,nWritten,nRead;
    char err[50];
	char req[PORT_LENGHT];
	char resp[PORT_LENGHT];
	char array[PORT_LENGHT];
	char *port;
	jint *arr_req;
	jint *arr_resp;
	jint req_length =0;
	jint resp_length =0;
	
	port = Jstring2CStr(env, jport);
	
	arr_req = env->GetIntArrayElements(reqArray,NULL);
	req_length = env->GetArrayLength(reqArray);
	for(int i = 0; i < req_length; i++){
		req[i] = (char)arr_req[i];
	}
	
	arr_resp = env->GetIntArrayElements(respArray,NULL);
	resp_length = env->GetArrayLength(respArray);
	for(int i = 0; i < resp_length; i++){
		resp[i] = (char)arr_resp[i];
	}
	
	ret=OpenCom(portno,port,jbaud);
	LOGD("port = %s baud = %d \n",port,jbaud);
    if(ret==-1)
    {
        LOGE(err,"The %s open error.",port);
        perror(err);
        exit(1);
    }

    //-------------------------------------------------------->
    //-------------------------------------------------------->
	usleep(1000*20);
	nWritten=ComWrt(portno, req, req_length);
	LOGD("/dev/ttyHSL1 has send %d chars!\n",nWritten);
	fflush(stdout);
	usleep(1000*1000);
	memset(array, 0, PORT_LENGHT);
	nRead=ComRd(portno,array,PORT_LENGHT,10000);
	if(nRead>0)
		LOGD("@@@@ array = %s\n",&array[0]);
	else
		LOGD("Timeout\n");

	/**
	for(int i = 0; i < resp_length; i++){
		LOGD("resp_length = %d, resp = %02x \n",resp_length,resp[i]);
	}*/

	//if(nRead > 0 && (array[0] == 0x80 && array[7] == 0x00) ){
		if(nRead > 0 && (array[0] == 0x80)&& (array[1] == 0x0D) ){
		// jclass java_class = env->FindClass("com/meigsmart/meigrs32/util/SerialPort");
		jclass java_class = env->FindClass("com/serialport/PsamSerialPort");
		if(java_class == 0){
			LOGI("SerialPort class not found");
		}else{
			LOGI("SerialPort class found");
			jmethodID method = env->GetMethodID(java_class, "setStatus", "(Z)V");
			if(method == 0){
				LOGI("setStatus method not found");
			}else{
				LOGI("callback setStatus method");
				env->CallVoidMethod( obj, method,true);
			}
		}
	}

    if((CloseCom(portno)==-1))
    {
        LOGD("Close com");
        exit(1);
    }

    LOGD("Exit now.\n");
}
