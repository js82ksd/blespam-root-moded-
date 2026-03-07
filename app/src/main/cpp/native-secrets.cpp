#include <jni.h>
#include <string>
#include <cstdio>
#include <cstring>
#include <unistd.h>

static bool isDebuggerAttached() {
    char path[64];
    char line[256];
    snprintf(path, sizeof(path), "/proc/%d/status", getpid());
    FILE* f = fopen(path, "r");
    if (!f) return false;
    int tracerPid = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            tracerPid = atoi(line + 10);
            break;
        }
    }
    fclose(f);
    return tracerPid != 0;
}

static bool isFridaPresent() {
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return false;
    char line[512];
    bool found = false;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "frida") || strstr(line, "gadget")) {
            found = true;
            break;
        }
    }
    fclose(f);
    return found;
}

static std::string xorDecrypt(const unsigned char* data, size_t len, const std::string& key) {
    std::string result;
    result.reserve(len);
    for (size_t i = 0; i < len; i++) {
        result += static_cast<char>(data[i] ^ key[i % key.length()]);
    }
    return result;
}

static const char* K1 = "XXXXX"; 
static const char* K2 = "XXXXX";
static const char* K3 = "XXXXX";
static const char* K4 = "XXXXX";
static const char* K5 = "XXXXX";
static const char* K6 = "XXXXX";

static std::string getKey() {
    std::string key;
    key += K1; key += K2; key += K3; key += K4; key += K5; key += K6;
    return key;
}

static const unsigned char D1[] = {0x00}; // TOKEN_API
static const size_t D1_LEN = 0;

static const unsigned char D2[] = {0x00}; // REPORT_API
static const size_t D2_LEN = 0;

static const unsigned char D3[] = {0x00}; // VERSION_API
static const size_t D3_LEN = 0;

static const unsigned char D4[] = {0x00}; // SOCIAL
static const size_t D4_LEN = 0;

static const unsigned char D5[] = {0x00}; // ICONCLICK
static const size_t D5_LEN = 0;

static const unsigned char D6[] = {0x00}; // FIREBASE
static const size_t D6_LEN = 0;

static const unsigned char D7[] = {0x00}; // ANTIFRAUD
static const size_t D7_LEN = 0;


static bool checkSafe() { return !isDebuggerAttached() && !isFridaPresent(); }
static jstring blocked(JNIEnv* e) { return e->NewStringUTF(""); }

// --- JNI Методы (Замените YOUR_PACKAGE_PATH на путь вашего проекта) ---

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getTokenApi(JNIEnv* e, jobject, jobject) {
    if (!checkSafe()) return blocked(e);
    if (D1_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D1, D1_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getReportApi(JNIEnv* e, jobject, jobject) {
    if (!checkSafe()) return blocked(e);
    if (D2_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D2, D2_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getVersionApi(JNIEnv* e, jobject, jobject) {
    if (!checkSafe()) return blocked(e);
    if (D3_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D3, D3_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getSocialLink(JNIEnv* e, jobject) {
    if (D4_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D4, D4_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getIconClick(JNIEnv* e, jobject) {
    if (D5_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D5, D5_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getFirebaseUrl(JNIEnv* e, jobject) {
    if (D6_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D6, D6_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_package_NativeLib_getAntifraudSecret(JNIEnv* e, jobject, jobject) {
    if (!checkSafe()) return blocked(e);
    if (D7_LEN == 0) return e->NewStringUTF("PLACEHOLDER");
    return e->NewStringUTF(xorDecrypt(D7, D7_LEN, getKey()).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_your_package_NativeLib_isEnvironmentSafe(JNIEnv*, jobject) {
    return checkSafe() ? JNI_TRUE : JNI_FALSE;
}