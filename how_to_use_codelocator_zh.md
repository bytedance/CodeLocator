**[English](how_to_use_codelocator.md)** | 简体中文

# CodeLocator使用说明

## 1. 安装插件
[点此下载最新版插件](https://github.com/bytedance/CodeLocator/releases) 

选择 Android Studio > Preferences > Plugins > Install Plugin from Disk

选择下载的Zip文件安装即可

<img src="misc/codelocator_install.png" width="680"/>

## 2. 界面介绍
CodeLocator 插件是一个侧边栏插件, 安装后会在Android Studio的右侧, 点击Tab即可展开插件, 初始状态如下

<img src="misc/codelocator_init.png" width="680"/>

## 3. 功能介绍
CodeLocator目前支持二十六种按钮操作, 当按钮呈现绿色高亮时表示可点击, 当按钮处于灰色状态时表示不可点击, 鼠标悬停一段时候后会出现具体的功能描述

| <img src="misc/icon_grab.png" alt="抓取" width="88"/> | <img src="misc/icon_grab_stop.png" alt="暂停动画并抓取" width="88"/> | <img src="misc/icon_open_file.png" alt="加载CodeLocator文件" width="88"/> | <img src="misc/icon_id.png" alt="跳转FindViewById" width="88"/> | <img src="misc/icon_click.png" alt="跳转OnClickListener" width="88"/> | <img src="misc/icon_touch.png" alt="跳转OnTouchListener" width="88"/> | <img src="misc/icon_xml.png" alt="跳转xml" width="88"/> | <img src="misc/icon_holder.png" alt="跳转ViewHolder" width="88"/> | <img src="misc/icon_fragment.png" alt="跳转Fragment" width="88"/> |  
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:| :-------:| 
| <sub>抓取</sub> | <sub>暂停动画抓取</sub> | <sub>加载文件</sub> | <sub>跳转Find</sub> | <sub>跳转Click</sub> | <sub>跳转Touch</sub> | <sub>跳转xml</sub> | <sub>跳转Holder</sub> | <sub>跳转Fragment</sub> |

| <img src="misc/icon_activity.png" alt="跳转Activity" width="88"/> | <img src="misc/icon_open_source.png" alt="跳转StartActivity代码" width="88"/> | <img src="misc/icon_trace_touch.png" alt="点击事件传递追踪" width="88"/> | <img src="misc/icon_pop.png" alt="弹窗追溯" width="88"/> | <img src="misc/icon_image.png" alt="复制截图" width="88"/> | <img src="misc/icon_data.png" alt="获取View数据" width="88"/> | <img src="misc/icon_modify.png" alt="编辑View属性" width="88"/> | <img src="misc/icon_dep.png" alt="修复项目依赖" width="88"/> | <img src="misc/icon_new_window.png" alt="打开新窗口" width="88"/> |
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:| :-------:| 
| <sub>跳转Activity</sub> | <sub>StartActivity</sub> | <sub>点击事件追踪</sub> | <sub>弹窗追溯</sub> | <sub>复制截图</sub> | <sub>获取View数据</sub> | <sub>编辑View属性</sub> | <sub>修复项目依赖</sub> | <sub>打开新窗口</sub> |

| <img src="misc/icon_history.png" alt="显示抓取历史" width="88"/> | <img src="misc/icon_report.png" alt="修复跳转错误" width="88"/> | <img src="misc/icon_apk.png" alt="安装Apk" width="88"/> | <img src="misc/icon_save.png" alt="保存抓取数据" width="88"/> | <img src="misc/icon_tools.png" alt="工具箱" width="88"/> | <img src="misc/icon_doc.png" alt="使用文档" width="88"/> | <img src="misc/icon_feedback.png" alt="反馈问题" width="88"/> |  <img src="misc/icon_settings.png" alt="设置" width="88"/> |  
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:|
| <sub>显示抓取历史</sub> | <sub>修复跳转错误</sub> | <sub>安装Apk</sub> | <sub>保存抓取数据</sub> | <sub>工具箱</sub> | <sub>使用说明</sub> | <sub>反馈问题</sub> | <sub>设置</sub> |

## 4. 使用方法

### 抓取 
确保应用在前台, 然后点击抓取按钮, 即可获取界面信息

<img src="misc/icon_grab.png" alt="抓取" width="24"/> 此模式为直接抓取

<img src="misc/icon_grab_stop.png" alt="暂停动画并抓取" width="24"/> 此模式为暂停界面动画并抓取

<img src="misc/codelocator_grab.gif"  alt="CodeLocator抓取" />

### 选择View
点击左侧图片可选中View, 或者选择右侧的View Tree视图中的Item都可以选择View  

面板上选择View有如下几种模式:

直接单击: 会按照可点击属性查找View, 上层可点击View会覆盖底部View  
Alt + 单击: 会按照View面积去查找当前点击的View, 可能会出现点击穿透问题  
Shift + 单击: 多选View, 同时可对比最后选中的两个View的间距

<img src="misc/codelocator_view.gif"  alt="选择View" />

### 代码跳转
选择View后 对应的跳转代码按钮会根据当前信息变化, 点击对应的按钮可跳转View的FindViewById, Xml, ViewHolder, Fragment等代码位置

<img src="misc/codelocator_jump.gif"  alt="代码跳转" />

### Tab切换
默认包含四个Tab, 分别是View, Activity, File, AppInfo  
View Tab 包含View树浏览功能 底部为View详情Table  
Activity Tab 包含Activity 和 Fragment树 底部为Fragment详情Table  
File Tab 包含应用目录下的所有文件  
AppInfo面板 包含App运行时信息, 点击自动复制  
同时支持App添加自定义Tab

<img src="misc/codelocator_tab.gif"  alt="Tab切换" />

### Tree展示
除AppInfo面板, 其他面板都是以Tree的形式组织展示的  

View Tree 的展示格式为: [(子View数量) [当前View深度] (可设置显示) 类名 左上顶点坐标 右下顶点坐标 宽度px 高度px 宽度dp 高度dp ]  
Fragment Tree 的展示格式为: [(子Fragment数量) (*表示当前Fragment可见) 类名]  
File Tree 的展示格式为: [(子文件数量) 文件名 [文件总大小 (文件夹大小会包含子文件)]]  

<img src="misc/codelocator_tree.gif"  alt="Tree展示" />

### Tree搜索
View Tree 和 File Tree支持搜索, 键盘输入任意内容可进行搜索, 支持模糊匹配, 会匹配View的Class, Text, id内容

<img src="misc/codelocator_search.gif"  alt="Tree搜索" />

### 点击事件追踪
可追踪当前点击事件的传递链, 需要在设备上触摸View同时点击追踪按钮

<img src="misc/codelocator_trace_touch.gif"  alt="点击事件追踪" />

### 弹窗追溯
可追溯App显示的弹窗代码位置

<img src="misc/codelocator_trace_dialog.gif"  alt="弹窗追溯" />

### 获取截图
可复制当前截图到剪切板或者某个View的绘制内容

<img src="misc/codelocator_image.gif"  alt="获取截图" />

### 获取View数据
可获取当前View绑定的数据内容

<img src="misc/codelocator_data.gif"  alt="获取View数据" />

### 修改View属性
可实时修改当前选中View的属性

<img src="misc/codelocator_edit.gif"  alt="修改View属性" />

### 新建窗口
可根据当前抓取内容新开窗口展示

<img src="misc/codelocator_new_window.gif"  alt="打开新窗口" />

### 展示历史抓取
可打开最近抓取的30条内容

<img src="misc/codelocator_history.gif"  alt="打开历史抓取" />

### 安装Apk
可查找安装当前项目内的Apk文件

<img src="misc/codelocator_install.gif"  alt="安装Apk" />

### 保存抓取信息
可保存当前抓取信息到指定位置并加载

<img src="misc/codelocator_save.gif"  alt="保存抓取信息" />

### CodeLocator工具集合
可快速打开布局边界, 显示触摸位置, 显示过渡绘制等开发工具

<img src="misc/codelocator_tools.gif"  alt="保存抓取信息" />
