English | **[简体中文](how_to_use_codelocator_zh.md)**

# CodeLocator Instructions

## 1. Install Plugin
[Click here to download plugin](https://github.com/bytedance/CodeLocator/releases/download/1.2.1/CodeLocatorPlugin-1.2.1.zip)

Select Android Studio > Preferences > Plugins > Install Plugin from Disk

Select the downloaded Zip file to install

<img src="misc/codelocator_install.png" width="680"/>

## 2. Interface instructions
CodeLocator is a sidebar plug-in that will be installed on the right side of Android Studio. Click Tab to expand the plug-in. The initial status is as follows

<img src="misc/codelocator_init.png" width="680"/>

## 3. Function Instructions
CodeLocator can support 26 kinds of button operations currently. When the button is highlighted in green, it is clickable; while the button is in gray, it is not clickable. After hovering the mouse for a while, the specific function description will appear.

| <img src="misc/icon_grab.png" alt="Grab" width="88"/> | <img src="misc/icon_grab_stop.png" alt="Pause & Grab" width="88"/> | <img src="misc/icon_open_file.png" alt="Load file" width="88"/> | <img src="misc/icon_id.png" alt="Jump to FindViewById" width="88"/> | <img src="misc/icon_click.png" alt="Jump to OnClickListener" width="88"/> | <img src="misc/icon_touch.png" alt="Jump to OnTouchListener" width="88"/> | <img src="misc/icon_xml.png" alt="Jump to xml" width="88"/> | <img src="misc/icon_holder.png" alt="Jump to ViewHolder" width="88"/> | <img src="misc/icon_fragment.png" alt="Jump to Fragment" width="88"/> |  
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:| :-------:| 
| <sub>Grab</sub> | <sub>Pause & Grab</sub> | <sub>Load file</sub> | <sub>Jump to Find</sub> | <sub>Jump to Click</sub> | <sub>Jump to Touch</sub> | <sub>Jump to xml</sub> | <sub>Jump to Holder</sub> | <sub>Jump to Fragment</sub> |

| <img src="misc/icon_activity.png" alt="Jump to Activity" width="88"/> | <img src="misc/icon_open_source.png" alt="Jump to StartActivity" width="88"/> | <img src="misc/icon_trace_touch.png" alt="Click event tracking" width="88"/> | <img src="misc/icon_pop.png" alt="Popup window tracking" width="88"/> | <img src="misc/icon_image.png" alt="Copy the screenshot" width="88"/> | <img src="misc/icon_data.png" alt="Get View data" width="88"/> | <img src="misc/icon_modify.png" alt="Edit View property" width="88"/> | <img src="misc/icon_dep.png" alt="Fixing project dependencies" width="88"/> | <img src="misc/icon_new_window.png" alt="Open a new window" width="88"/> |
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:| :-------:| 
| <sub>Jump to Activity</sub> | <sub>StartActivity</sub> | <sub>Click event tracking</sub> | <sub>Popup window tracking</sub> | <sub>Copy screenshot</sub> | <sub>Get View data</sub> | <sub>Edit View</sub> | <sub>Fixing dependencies</sub> | <sub>Open a new window</sub> |

| <img src="misc/icon_history.png" alt="Show grabbed history" width="88"/> | <img src="misc/icon_report.png" alt="Fix jump error" width="88"/> | <img src="misc/icon_apk.png" alt="Install Apk" width="88"/> | <img src="misc/icon_save.png" alt="Save grabbed data" width="88"/> | <img src="misc/icon_tools.png" alt="Tools box" width="88"/> | <img src="misc/icon_doc.png" alt="Instructions" width="88"/> | <img src="misc/icon_feedback.png" alt="Feedback issue" width="88"/> |  <img src="misc/icon_settings.png" alt="Settings" width="88"/> |  
|:---------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:| :-------:|
| <sub>Show grabbed history</sub> | <sub>Fix jump error</sub> | <sub>Install Apk</sub> | <sub>Save grabbed data</sub> | <sub>Tools box</sub> | <sub>Instructions</sub> | <sub>Feedback issue</sub> | <sub>Settings</sub> |

## 4. Usage

### Grab 
Make sure the application is in the foreground, and then click the crawl button to get the interface information

<img src="misc/icon_grab.png" alt="grab" width="24"/> This mode is direct grabbing

<img src="misc/icon_grab_stop.png" alt="pause & grab" width="24"/> This mode is to pause the animation and grab

<img src="misc/codelocator_grab.gif"  alt="grab" />

### Select View
Click the image on the left to select View, or select the Item in the View Tree View on the right

Select View in the following modes:

Click: The View will be found according to the clickable properties, and the upper clickable View will overwrite the bottom View  
Alt + Click: It will look for the View that is currently clicked according to the View area, may cause click through problems  
Shift + Click: Select multiple views and compare the spacing of the last two views

<img src="misc/codelocator_view.gif"  alt="select View" />

### Code Jump
After selecting View, the corresponding jump code button will change according to the current information. Click the corresponding button to jump to the FindViewById, Xml, ViewHolder, Fragment and other code locations of the View

<img src="misc/codelocator_jump.gif"  alt="jump code" />

### Tab Switch
By default, there are four tabs: View, Activity, File, and AppInfo  
View Tab contains View tree browsing, and View details Table is at the bottom.  
Activity Tab contains Activity and Fragment tree, Fragment detail Table is at the bottom.
File Tab contains all files in the application directory
The AppInfo panel contains app runtime information. Click to copy automatically.
And support to add custom Tab.

<img src="misc/codelocator_tab.gif"  alt="Tab Switch" />

### Tree display
Except AppInfo panel, the other panels are organized in the form of trees  

The display format of View Tree: [(number of subviews) [current View depth] (can be set to display) class name  top left vertex coordinates  bottom right vertex coordinates  width px height PX width dp height dp]  
The display format of Fragment Tree: [(Number of fragments) (* indicates that the Fragment is visible) Class name]  
The display format of File Tree: [(number of subfiles) Filename [Total file size (folder size will contain subfiles)]]  

<img src="misc/codelocator_tree.gif"  alt="Tree Display" />

### Tree search
View Tree and File Tree search are supported;
Keyboard input any content can be searched;
Fuzzy matching are supported;
Macthing View Class, Text, ID content are supported.

<img src="misc/codelocator_search.gif"  alt="Tree Search" />

### Click event tracking
Track the current click event chain by touching the View on the device and clicking the trace button

<img src="misc/codelocator_trace_touch.gif"  alt="Click event tracking" />

### Popup window tracking
Traceable to the location of popover code displayed by the App

<img src="misc/codelocator_trace_dialog.gif"  alt="Popup window tracking" />

### Get screenshots
Copy the current screenshot to the clipboard or to the drawing content of a View

<img src="misc/codelocator_image.gif"  alt="Get screenshots" />

### Get View data
Get the data content bound to the current View

<img src="misc/codelocator_data.gif"  alt="Get View data" />

### Modify View properties
Modify the properties of the selected View in real time

<img src="misc/codelocator_edit.gif"  alt="Modify View properties" />

### Open a new window
According to the current grab content, it can display in a new window

<img src="misc/codelocator_new_window.gif"  alt="Open a new window" />

### Display history grab
It enables to open the last 30 pieces of content grabbed

<img src="misc/codelocator_history.gif"  alt="Display history grab" />

### Install Apk
It enables to find the Apk files in the current installation project

<img src="misc/codelocator_install.gif"  alt="Install Apk" />

### Save grab information
It enables to save the current grabbed information to the specified location and load

<img src="misc/codelocator_save.gif"  alt="Save grab information" />

### CodeLocator Tools box
It enables to open the layout boundary, display touch position, display transition drawing and other development tools

<img src="misc/codelocator_tools.gif"  alt="Tools box" />
