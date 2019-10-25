# mobilenet_ncnn
在 Android 上使用腾讯的 ncnn 实现图像分类

# [Ubuntu18.04x64Caffe环境准备](http://blog.sina.com.cn/s/blog_4c451e0e0102ye99.html)

## 1.安装 Caffe 的所有依赖包：

```
sudo apt-get install git cmake build-essential

sudo apt-get install libprotobuf-dev libleveldb-dev libsnappy-dev libopencv-dev libhdf5-serial-dev protobuf-compiler

sudo apt-get install --no-install-recommends libboost-all-dev

sudo apt-get install libopenblas-dev liblapack-dev libatlas-base-dev

sudo apt-get install python-dev

sudo apt-get install libgflags-dev libgoogle-glog-dev liblmdb-dev
```

## 2.下载 Caffe 并安装：

```
git clone https://github.com/bvlc/caffe.git

cd caffe

mv Makefile.config.example Makefile.config
```

修改 Makefile.config 配置文件：

```
gedit Makefile.config
```

#初学者条件有限，为了快速入门，可以先学习在 CPU 上运行 Caffe 并阅读其 C++ 代码，

#而不是将大量精力浪费在重装系统、更新 GPU 驱动、阅读 CUDA 代码上。

#仅 CPU 模式开关，打开该选项(去掉#)表示 Caffe 编译时仅支持 CPU 不支持 GPU

**CPU_ONLY := 1**

#BLAS choice:

#atlas for ATLAS (default)

#mkl for MKL

#open for OpenBlas

#BLAS := atlas

#选择开源且高效的 OpenBlas 实现

**BLAS := open**

#用八个线程编译

**make -j8**



### 问题一：fatal error: hdf5.h: 没有那个文件或目录

修改 Makefile.config 文件：

INCLUDE_DIRS := $(PYTHON_INCLUDE) /usr/local/include

改为

**INCLUDE_DIRS := $(PYTHON_INCLUDE) /usr/local/include /usr/include/hdf5/serial/**



修改 Makefile 文件：

LIBRARIES += hdf5_hl hdf5

改为

**LIBRARIES += hdf5_serial_hl hdf5_serial**



#清理编译：

```
make clean
```

#重新编译

```
make -j8
```

### 问题二：libcaffe.so:对‘cv::imread(cv::String const&, int)’未定义的引用

将 Makefile.config 中 OPENCV_VERSION := 3 取消注释，

即 caffe 不默认使用 opencv 2.X 版本。



## 3、升级 Caffe 模型：

mobilenet_v2 下载自： https://github.com/shicai/MobileNet-Caffe

切到 caffe/tools 目录下

**./upgrade_net_proto_text      mobilenet_v2_deploy.prototxt  new_mobilenet_v2_deploy.prototxt**
**./upgrade_net_proto_binary  mobilenet_v2.caffemodel          new_mobilenet_v2.caffemodel**

检查模型配置文件，因为只能一张一张图片预测，所以输入要设置为dim: 1。

```
name: "MOBILENET_V2"
layer {
	name: "input"
	type: "Input"
	top:  "data"
	input_param {
		shape {
			dim: 1
      	  	dim: 3
      	  	dim: 224
      	  	dim: 224
 		}
 	}
}
```

# [VS2019编译ncnn](http://blog.sina.com.cn/s/blog_4c451e0e0102ygdy.html)

## 1.预备工作：

先安装好：VS2019 + CMake3.16.0

## 2.编译Protobuf：


https://github.com/google/protobuf/archive/v3.4.0.zip

打开 CMake-GUI ：
1)点击 Browse Source...：D:/protobuf-3.4.0/cmake  <- 根 CMakeLists.txt目录
2)点击 Browse Build...   ：D:/protobuf-3.4.0/Builds <- 存在 BUILD 文件故多加s
3)点击 Configure            ：指定产生 Visual Studio 16 2019 工程 且 x64 操作系统
4)点击 Add Entry            ：

Name                      Value

CMAKE_BUILD_TYPE                       Release

5)修改：
CMAKE_CONFIGURATION_TYPES  Release

6)修改：

CMAKE_INSTALL_PREFIX                 D:/protobuf-3.4.0/install

7)点 2 次 Configure   按钮
8)点 1 次 Generate    按钮
9)点 1 次 Open Project 按钮

备注：
6.1)不勾选：protobuf_BUILD_TESTS 和 protobuf_MSVC_STATIC_RUNTIME

A)
  在系统环境变量 PATH 中添加路径：
  D:/protobuf-3.4.0/install/bin；
  然后重启系统！
  以便使用 Protoc.exe 程序！

## 3.编译ncnn：(同上操作步骤)

1)修改：

OpenCV_DIR                                D:/opencv-4.1.1/build/install

2)修改：
Protobuf_SRC_ROOT_FOLDER  D:/protobuf-3.4.0/install

## 4.把新的 Caffe 模型转换成 NCNN 模型：

把已经升级的网络定义文件和权重文件复制到 tools/caffe/ 目录，并执行以下命令

```
./caffe2ncnn new_mobilenet_v2_deploy.prototxt new_mobilenet_v2.caffemodel mobilenet_v2.param mobilenet_v2.bin
```

## 5.对 mobilenet_v2.param、mobilenet_v2.bin 进行加密

复制到上一级的 tools/ 目录，并执行以下命令

```
./ncnn2mem mobilenet_v2.param mobilenet_v2.bin mobilenet_v2.id.h mobilenet_v2.mem.h
```

mobilenet_v2.mem.h  内容如下：

将 mobilenet_v2.param.bin 和 mobilenet_v2.bin 两文件转成两个字节数组：

mobilenet_v2_param_bin 和 mobilenet_v2_bin

得到以下三个文件是需要的：

**mobilenet_v2.bin                 网络的权重**

**mobilenet_v2.param.bin   网络的模型参数**

**mobilenet_v2.id.h                在预测图片的时候使用到**

# [AndroidNDK编译NCNN](http://blog.sina.com.cn/s/blog_4c451e0e0102ygez.html)

## 1. Windows 平台上编译 NCNN ：

在 ncnn 根目录下创建 build_android.bat 文件：

```
@@echo off
::其中ANDROID_NDK要换成你本机android ndk所在的目录
set ANDROID_NDK=F:\AndroidSdk\ndk-bundle
mkdir build_android
cd build_android
::在.bat脚本文件中，换行符是"^"，即 shift + 6
cmake ^
-G "Unix Makefiles" ^
-DCMAKE_TOOLCHAIN_FILE="%ANDROID_NDK%\build\cmake\android.toolchain.cmake" ^
-DCMAKE_MAKE_PROGRAM="%ANDROID_NDK%\prebuilt\windows-x86_64\bin\make.exe" ^
-DCMAKE_BUILD_TYPE=Release ^
-DANDROID_ABI="armeabi-v7a" ^
-DANDROID_ARM_NEON=ON ^
-DANDROID_PLATFORM=android-14 ^
..
cmake --build .
cmake --build . --target install
cd ..
pause
```

编译完成，会在 build_android 目录下生成一个 install 目录：

```
include 该文件夹将会存放在 Android 项目的 src/main/cpp 目录下；
lib     libncnn.a 将会存放在 Android 项目的 src/main/jniLibs/armeabi-v7a 目录下；
```

# 创建 Android 项目

创建一个 mobilenet_ncnn 项目，往下拉选择 **Native C++** ，选择 **C++11** 支持。



在 main 目录下创建 assets 目录，并复制以下文件到该目录下：

**mobilenet_v2.param.bin  上一步获取网络的模型参数**

**mobilenet_v2.bin               上一步获取网络的权重**

**synset.txt                              label对应的名称**



