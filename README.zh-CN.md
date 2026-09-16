[English](README.md) | [日本語](README.ja.md) | 简体中文

# Folduo（Fold8 SM-F9710 适配版）

这是 [bunkaich/Folduo](https://github.com/bunkaich/Folduo) 的 fork。上游只支持 Galaxy Z Fold7 SM-F966Z，本分支加上了 Galaxy Z Fold8 **SM-F9710**（国行，Android 17 / One UI 9）。

Folduo 的作用：手机开合时，用铰链角度驱动一层磨砂玻璃效果的过渡动画，把当前应用的画面按住不动，等另一块屏准备好了再交接过去。它和你现在用的桌面共存，不需要替换桌面。

本文是中文说明，改动的实测依据写在 [docs/fold8-sm-f9710.md](docs/fold8-sm-f9710.md)。

## 实测机型

2026 年 9 月 16 日，在一台手机上测试通过：

| 项目 | 值 |
| --- | --- |
| 机型 | SM-F9710（h8q） |
| 系统 | Android 17（SDK 37），One UI 9.0 |
| 版本号 | `F9710ZSS2AZH7` |
| Shizuku | 13.6.0.r1086.2650830c，通过 ADB 启动 |
| Folduo | 0.1.21 加本分支改动，本地编译，调试签名 |

能用的部分：应用上的开合过渡动画、两块屏之间的双向交接、Folduo 自带桌面跟着开合走。

## 相比上游改了什么

- **机型判断**：`DeviceSupport` 统一管理允许的机型，SM-F966Z 和 SM-F9710。原来写死在三处的型号判断和外屏壁纸工具都改用它。
- **角度日志格式**：Fold8 的三星动态壁纸打的日志是 `onCommand: action=…, mCurrentAngle=…, isVisible=true`，Fold7 用的是方括号格式。`AngleLog` 两种都认，并有单元测试覆盖。
- **外屏壁纸地址**：Fold8 的原厂外屏图是 webp 资源，地址结尾没有 `.png`，工具两种写法都接受。
- **桌面交接**：Android 17 不允许把任务移进桌面类型的根任务，而上游的桌面交接正好依赖这一步。Fold8 上改成用「从最近任务在目标屏打开」的方式搬 Folduo 桌面，三星桌面则完全不搬。
- **检测脚本**：`tools/fold8-probe.sh` 是只读的 ADB 检测脚本，用来查双屏状态、屏幕编号、铰链传感器和壁纸。

## 使用前提

**1. 启动 Shizuku**

Shizuku 给 Folduo 提供 ADB shell 级别的权限，不需要 root。接上 USB 后可以直接用它自带的启动程序：

```sh
DIR=$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed 's/^package://; s#/base.apk$##')
adb shell "$DIR/lib/arm64/libshizuku.so"
```

成功时会输出 `info: shizuku_server pid is …`。

手机每次重启后都要重新启动一次。

**2. 内屏和外屏都要用能报角度的原厂动态壁纸**

上游把外屏壁纸这一步写成可选，**在 Fold8 上它是必需的**。

精细角度不是从传感器读的。三星的 Folding Angle 传感器精度到 0.01°，但需要三星私有权限，Shizuku 的 shell 身份拿不到；标准铰链传感器精度只有 90°，只能分辨合上、半开、展开。所以角度只能从三星那张会随开合变化的动态壁纸的日志里读，精度 1°，约 60 Hz。

- 内屏：在「壁纸和样式」里选三星自带、内部名为 `video_002` 的动态壁纸。
- 外屏：运行下面的命令，把外屏桌面也设成同一张壁纸。它只改外屏桌面，不动锁屏和内屏。

```sh
python3 tools/cover-wallpaper.py status
python3 tools/cover-wallpaper.py apply
```

不做外屏这一步会怎样：手机合上后内屏壁纸不可见，所有角度日志都带「不可见」标记，Folduo 收不到小于等于 3° 的角度，初始化会一直停在 “Close the phone fully once to finish setup.”。

想换回原来的外屏图：

```sh
python3 tools/cover-wallpaper.py restore-stock
```

**3. 关掉三星自动拦截程序的自动开启**

自动拦截程序（系统内部代号 `rampart`）被关掉后，默认约 30 分钟会自己重新打开，一打开就把 USB 调试关掉。ADB 一停，通过它启动的 Shizuku 会被杀掉，Folduo 随之失效。在「设置 → 安全和隐私 → 自动拦截程序」里关掉自动开启的选项。

## 内屏桌面的限制

Folduo 接管双屏时，外屏是系统里的 0 号屏，内屏是 1 号屏。应用画面和过渡动画在内屏上都是正常的，桌面则不是：

- **背景是黑的**。三星的壁纸服务只给 0 号屏挂壁纸引擎，1 号屏没有。
- **三星桌面在 1 号屏上的布局**和平时的内屏桌面不一样。
- 这两点是三星系统在这种双屏状态下的行为，不是交接代码的问题。上游也把桌面、最近任务列为不支持。

**建议开着效果时用 Folduo 自带的桌面**，它自己画背景和布局，不依赖三星的壁纸：

```sh
adb shell cmd role add-role-holder android.app.role.HOME jp.bunkaich.sukashimotion
```

换回三星桌面：

```sh
adb shell cmd role add-role-holder android.app.role.HOME com.sec.android.app.launcher
```

## 从源码编译

需要 Git、JDK 17 或更新版本、Android SDK（platform 37、build-tools 36.0.0）。

```sh
git clone https://github.com/sjw1933/Folduo.git
cd Folduo
./gradlew :app:assembleRelease :app:testDebugUnitTest
```

产物在 `app/build/outputs/apk/release/app-release.apk`。本分支的 40 项单元测试全部通过。

自己编译的包用的是你本地的签名，和已安装的版本签名不同时要先卸载再装。

## 停用和卸载

先在 Folduo 里点 **Stop and release display control** 释放屏幕控制，再卸载。卸载不会自动恢复外屏壁纸，需要手动运行上面的 `restore-stock`。

如果屏幕卡住：合上手机，在外屏打开 Folduo 点停止；点不了就重启手机。重启后 Shizuku 不会自动启动，Folduo 也就跟着停了。

## 尚未验证

长时间运行的稳定性、耗电、拔掉 USB 后的长期使用、其他 Fold8 型号和其他 One UI 版本，以及上游按 Fold7 调的投影参数在 Fold8 的屏幕比例下是否合适。效果能出来，但没有针对 Fold8 重新调过。

## 许可

自有代码为 [MIT](LICENSE)。依赖的第三方许可见 [第三方声明](THIRD_PARTY_NOTICES.md)。不含任何三星或苹果的界面素材、壁纸和视频。本项目与三星、苹果、Shizuku 无关。
