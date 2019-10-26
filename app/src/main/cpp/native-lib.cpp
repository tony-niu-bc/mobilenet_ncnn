#include <jni.h>
#include <unistd.h>
#include <sys/time.h>

#include <android/bitmap.h>
#include <android/log.h>

#include <string>
#include <vector>

// ncnn
#include "ncnn/net.h"
#include "mobilenet_v2.id.h"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator         g_workspace_pool_allocator;

static ncnn::Net g_ncnn_net;

extern "C"
{
    // public native boolean Init(byte[] param, byte[] bin, byte[] words);
    JNIEXPORT jboolean JNICALL
    Java_com_wzhnsc_mobilenet_1ncnn_MainActivity_Init(JNIEnv*    env,
                                                      jobject    thiz,
                                                      jbyteArray param,
                                                      jbyteArray bin)
    {
        ncnn::Mat ncnn_param;
        ncnn::Mat ncnn_bin;

        // init param
        {
            int len = env->GetArrayLength(param);
            __android_log_print(ANDROID_LOG_DEBUG,
                                "NCNN_JNI",
                                "env->GetArrayLength return %d",
                                len);

            ncnn_param.create(len,
                              (size_t)1U);

            env->GetByteArrayRegion(param,
                                    0,
                                    len,
                                    (jbyte*)ncnn_param);

            int ret = g_ncnn_net.load_param((const unsigned char*)ncnn_param);

            __android_log_print(ANDROID_LOG_DEBUG,
                                "NCNN_JNI",
                                "g_ncnn_net.load_param return %d",
                                ret);
        }

        // init bin
        {
            int len = env->GetArrayLength(bin);
            __android_log_print(ANDROID_LOG_DEBUG,
                                "NCNN_JNI",
                                "env->GetArrayLength return %d",
                                len);

            ncnn_bin.create(len,
                            (size_t)1U);

            env->GetByteArrayRegion(bin,
                                    0,
                                    len,
                                    (jbyte*)ncnn_bin);

            int ret = g_ncnn_net.load_model((const unsigned char*)ncnn_bin);

            __android_log_print(ANDROID_LOG_DEBUG,
                                "NCNN_JNI",
                                "g_ncnn_net.load_model return %d",
                                ret);
        }

        return JNI_TRUE;
    }

    // public native String Detect(Bitmap bitmap);
    JNIEXPORT jfloatArray JNICALL
    Java_com_wzhnsc_mobilenet_1ncnn_MainActivity_Detect(JNIEnv* env,
                                                        jobject thiz,
                                                        jobject bitmap)
    {
        // ncnn from bitmap
        ncnn::Mat matBMP;
        {
            AndroidBitmapInfo info;
            AndroidBitmap_getInfo(env,
                                  bitmap,
                                  &info);

            if (ANDROID_BITMAP_FORMAT_RGBA_8888 != info.format) {
                return nullptr;
            }

            void* pBmpData;
            AndroidBitmap_lockPixels(env,
                                     bitmap,
                                     &pBmpData);

            AndroidBitmap_unlockPixels(env,
                                       bitmap);

            // 把像素转换成data，并指定通道顺序
            matBMP = ncnn::Mat::from_pixels((const unsigned char*)pBmpData,
                                            ncnn::Mat::PIXEL_RGBA2BGR,
                                            info.width,
                                            info.height);

            // 减去均值和乘上比例
            const float mean_vals[3] = {103.94F, 116.78F, 123.68F};
            const float scale[3]     = {0.017F,  0.017F,  0.017F};

            matBMP.substract_mean_normalize(mean_vals, scale);
        }

        // g_ncnn_net
        std::vector<float> cls_scores;
        {
            ncnn::Extractor ext = g_ncnn_net.create_extractor();

            ext.set_light_mode(true);
            ext.set_num_threads(4);
            ext.set_blob_allocator(&g_blob_pool_allocator);
            ext.set_workspace_allocator(&g_workspace_pool_allocator);

            // 如果时不加密是使用ex.input("data", matBMP);
            ext.input(mobilenet_v2_param_id::BLOB_data,
                      matBMP);

            ncnn::Mat out;
            // 如果是不加密时使用 ext.extract("prob", out);
            ext.extract(mobilenet_v2_param_id::BLOB_prob,
                        out);

            jfloat* pfOutput[out.w];

            for (int idx = 0;
                     idx < out.w;
                     idx++)
            {
                pfOutput[idx] = &out[idx];
            }

            jfloatArray jOutputData = env->NewFloatArray(out.w);

            if (nullptr == jOutputData) {
                return nullptr;
            }

            env->SetFloatArrayRegion(jOutputData,
                                     0,
                                     out.w,
                                     reinterpret_cast<const jfloat*>(*pfOutput)); // copy

            return jOutputData;
        }
    }
}
