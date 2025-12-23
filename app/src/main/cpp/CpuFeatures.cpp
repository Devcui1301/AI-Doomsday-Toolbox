// CpuFeatures.cpp - Native CPU feature detection for multi-tier binary loading
// Compile as a shared library: libcpufeatures.so

#include <android/log.h>
#include <asm/hwcap.h>
#include <jni.h>
#include <sys/auxv.h>

#define LOG_TAG "CpuFeatures"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ARM64 HWCAP2 flags (from Linux kernel headers)
#ifndef HWCAP2_ASIMDDP
#define HWCAP2_ASIMDDP (1 << 20) // ASIMD dot product
#endif

#ifndef HWCAP2_SVE2
#define HWCAP2_SVE2 (1 << 1) // SVE2 (ARMv9)
#endif

#ifndef HWCAP2_I8MM
#define HWCAP2_I8MM (1 << 13) // Int8 matrix multiply
#endif

extern "C" {

// Check if CPU supports dot product instructions (armv8.2-a+dotprod)
JNIEXPORT jboolean JNICALL
Java_com_example_llamadroid_util_CpuFeatures_hasDotProd(JNIEnv *env,
                                                        jclass clazz) {
  unsigned long hwcap2 = getauxval(AT_HWCAP2);
  bool has_dotprod = (hwcap2 & HWCAP2_ASIMDDP) != 0;
  LOGI("HWCAP2: 0x%lx, DotProd: %d", hwcap2, has_dotprod);
  return has_dotprod ? JNI_TRUE : JNI_FALSE;
}

// Check if CPU supports ARMv9 features (SVE2)
JNIEXPORT jboolean JNICALL
Java_com_example_llamadroid_util_CpuFeatures_hasArmV9(JNIEnv *env,
                                                      jclass clazz) {
  unsigned long hwcap2 = getauxval(AT_HWCAP2);
  bool has_sve2 = (hwcap2 & HWCAP2_SVE2) != 0;
  LOGI("HWCAP2: 0x%lx, SVE2 (ARMv9): %d", hwcap2, has_sve2);
  return has_sve2 ? JNI_TRUE : JNI_FALSE;
}

// Check if CPU supports i8mm (int8 matrix multiply, for CPU repack)
JNIEXPORT jboolean JNICALL Java_com_example_llamadroid_util_CpuFeatures_hasI8mm(
    JNIEnv *env, jclass clazz) {
  unsigned long hwcap2 = getauxval(AT_HWCAP2);
  bool has_i8mm = (hwcap2 & HWCAP2_I8MM) != 0;
  LOGI("HWCAP2: 0x%lx, I8MM: %d", hwcap2, has_i8mm);
  return has_i8mm ? JNI_TRUE : JNI_FALSE;
}

// Get the best CPU tier for this device: "armv9", "dotprod", or "baseline"
JNIEXPORT jstring JNICALL
Java_com_example_llamadroid_util_CpuFeatures_getBestTier(JNIEnv *env,
                                                         jclass clazz) {
  unsigned long hwcap2 = getauxval(AT_HWCAP2);

  const char *tier;
  if (hwcap2 & HWCAP2_SVE2) {
    tier = "armv9";
  } else if (hwcap2 & HWCAP2_ASIMDDP) {
    tier = "dotprod";
  } else {
    tier = "baseline";
  }

  LOGI("Selected CPU tier: %s (HWCAP2: 0x%lx)", tier, hwcap2);
  return env->NewStringUTF(tier);
}
}
