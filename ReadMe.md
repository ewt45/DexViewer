一个安卓上的app,寻找包名为`org.buildsmali.provider`的apk,解析其中的dex,并显示smali/java代码

JavaProjectSample文件夹是被解析的apk的项目目录，可以通过[AndroidIDE](https://github.com/AndroidIDEOfficial/AndroidIDE/releases)来便捷地修改代码和安装。\
本来想写成模块 和本项目共用一个项目，结果本项目gradle版本太高了AndroidIDE不支持，github action的最新版支持但是文件又过期了，自己编译又看不明白它的密钥配置，总之就是只好分成两个项目了。
