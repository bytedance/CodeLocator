English | **[简体中文](README.md)**

# CodeLocator
[![GitHub license](https://img.shields.io/github/license/bytedance/scene)](https://github.com/bytedance/scene/blob/master/LICENSE) 
[![API](https://img.shields.io/badge/api-16%2B-green)](https://developer.android.com/about/dashboards)

A picture takes you through CodeLocator  
<img src="misc/CodeLocator.gif" alt="CodeLocator"/>

CodeLocator is a toolset that includes Android SDK and Android Studio plugins. It has the following functions(Support Mac Only):

1. Display the current View information
2. Display current Activity information
3. Display information about all fragments
4. Display custom App runtime information
5. Display file information about the current application
6. Edit the status of the View in real time, such as visibility, text content, etc
7. Locate the View that currently responds to the touch event 
8. Get the data of the current View
9. Get the drawing content corresponding to the current View
10. Jump to View click event code, findViewById, ViewHolder code location
11. Jump to View's XML layout file
12. Jump to the code location of Toast & Dialog
13. Jump to the location of code which starts the current Activity
14. Display all Schema information supported by the application
15. Send specified Schema to the application
16. Locate the latest Apk file in the project
17. Apk files can be installed from the shortcut menu
18. Quickly open display layout boundaries, transition drawing, click operations, etc
19. Fast connect Charles

## These apps are using CodeLocator

| <img src="misc/douyin.png" alt="douyin" width="100"/> | <img src="misc/douyin.png" alt="tiktok" width="100"/> | <img src="misc/duoshan.png" alt="duoshan" width="100"/> | <img src="misc/resso.png" alt="resso" width="100"/> | <img src="misc/xigua.png" alt="xigua" width="100"/> |  
|:---------:|:-------:|:-------:|:-------:|:-------:|
| 抖音 | TikTok | 多闪 | Resso | 西瓜视频 |  

| <img src="misc/helo.png" alt="helo" width="100"/> |<img src="misc/feishu.png" alt="feishu" width="100"/> |<img src="misc/qingbei.png" alt="qingbei" width="100"/> | <img src="misc/fanqie.png" alt="fanqie" width="100"/> |<img src="misc/qingyan.png" alt="qingyan" width="100"/> |
|:---------:|:-------:|:-------:|:-------:|:-------:|
| Helo | 飞书 | 清北网校 | 番茄小说 | 轻颜相机 |

| <img src="misc/huoshan.png" alt="huoshan" width="100"/> | <img src="misc/guagualong.png" alt="guagualong" width="100"/> | <img src="misc/jianying.png" alt="jianying" width="100"/> | <img src="misc/fanqiefm.png" alt="fanqiefm" width="100"/> | <img src="misc/xingfuli.png" alt="xingfuli" width="100"/> |
|:---------:|:-------:|:-------:|:-------:|:-------:|
| 抖音火山版 | 瓜瓜龙 | 剪映 | 番茄畅听 | 幸福里 |
 
## Integration
The followings describe how to use CodeLocator:

1. Install CodeLocator in Android Studio ([Click here to download plugin](https://github.com/bytedance/CodeLocator/releases))
2. Integrate CodeLocator into the application

```gradle
allprojects {
    repositories {
        mavenCentral()
    }
}

// To integrate the basic capabilities, just add one dependency
dependencies {
    implementation "com.bytedance.tools.codelocator:codelocator-core:1.0.0"
}
```
If you need to integrate code jump capability, you need to integrate [Lancet](https://github.com/eleme/lancet) first and add the following dependencies
```gradle
dependencies {
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-xml:1.0.0"
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-activity:1.0.0"
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-view:1.0.0"
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-toast:1.0.0"
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-dialog:1.0.0"
    debugImplementation "com.bytedance.tools.codelocator:codelocator-lancet-popup:1.0.0"
}
```

## Usage
CodeLocator plug-in is a sidebar plug-in that is displayed on the right side of Android Studio after installation and can be expanded by clicking

The initial state is as follows

<img src="misc/codelocator_init.png" alt="codelocator_init" width="680"/>

The green button is clickable, while the gray button is unavailable currently

Click the Grab button to obtain the status information of the current app. Click the image panel on the left to select the corresponding View, and the state of the button will change according to the currently selected View

<img src="misc/codelocator_grab.png" alt="codelocator_grab" width="680"/>

For more operations, see the [CodeLocator instructions](how_to_use_codelocator.md)

## Contact us

If you have any questions or suggestions about CodeLocator, please join our Wechat group to communicate with us.

<img src="misc/my_wechat.png" alt="WeChat" />

Alternatively, you can send an email to liujian.android@bytedance.com with a detailed description of your problem.  
Applications are also welcome~~

## License
~~~
Copyright (c) 2021 ByteDance Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
~~~

*** 
The following components are provided under an Apache 2.0 license.

1. lancet - For details, https://github.com/eleme/lancet

2. okhttp - For details, https://github.com/square/okhttp

3. gson - For details, https://github.com/google/gson