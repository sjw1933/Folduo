# Folduo Fold8 傻瓜式教程

照着做，一步都别跳。每一步都写了「看到什么算成功」，对上了再往下走。全程大约 20 分钟。

---

## 开始前先确认

**手机**
- 必须是 **Galaxy Z Fold8，型号 SM-F9710**（国行）。
  查看方法：设置 → 关于手机 → 型号名称。不是 SM-F9710 就不用往下看了，这一版装不上。

**电脑**
- 一台 Mac（本教程以 Mac 为准，Windows 的差异在文末）。
- 一根能传数据的 USB 线。

**先下载两个文件**
打开 https://github.com/sjw1933/Folduo/releases ，在最新版本下面点击下载：
- `Folduo-0.1.21-fold8.1.apk`
- `folduo-wallpaper-setup-0.1.21-fold8.1.zip`

下载完，在「下载」文件夹里**双击 zip 解压**，会得到一个 `folduo-wallpaper-setup` 文件夹。

---

## 第 1 步：关掉自动拦截程序

手机上：设置 → 安全和隐私 → **自动拦截程序**

1. 把总开关**关掉**。
2. 同一页面里，把「自动开启」这类选项也**关掉**。

> 为什么：它开着会拦住电脑连接手机；只关总开关不行，大约 30 分钟后它会自己重新打开，Folduo 就会突然失效。

---

## 第 2 步：打开 USB 调试

1. 设置 → 关于手机 → 软件信息 → 连续点「**编译编号**」7 次，直到提示「开发者模式已打开」（可能要输锁屏密码）。
2. 返回设置主页，最下面多出「**开发者选项**」，点进去，打开「**USB 调试**」。

---

## 第 3 步：电脑装好工具

打开 Mac 上的「**终端**」（启动台里搜「终端」），逐行复制粘贴下面的命令，每行回车一次。

1. 安装 ADB（手机调试工具）：
   ```sh
   brew install android-platform-tools
   ```
   > 如果提示 `brew: command not found`，先到 https://brew.sh 按首页那一行命令装好 Homebrew，再回来执行。

2. 检查 Python（Mac 一般自带）：
   ```sh
   python3 --version
   ```
   ✅ 成功：显示 `Python 3.x.x`。

---

## 第 4 步：手机连上电脑

1. 用 USB 线连接手机和电脑。
2. 手机弹出「允许 USB 调试吗？」→ 勾选「**一律允许使用这台计算机进行调试**」→ 点「**允许**」。
3. 终端里输入：
   ```sh
   adb devices
   ```
   ✅ 成功：列表里有一行，结尾是 `device`。
   ❌ 显示 `unauthorized`：看手机屏幕，点「允许」；没弹窗就拔掉线重新插。
   ❌ 列表是空的：换一根能传数据的线，或换一个 USB 口。

---

## 第 5 步：安装并启动 Shizuku

Shizuku 是 Folduo 需要的「权限助手」，不用 root。

1. 在手机上安装 Shizuku：Google Play 搜 Shizuku，或从 https://github.com/RikkaApps/Shizuku/releases 下载 apk 安装。
2. 在终端里**分两行**执行：
   ```sh
   DIR=$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed 's/^package://; s#/base.apk$##')
   ```
   ```sh
   adb shell "$DIR/lib/arm64/libshizuku.so"
   ```
   ✅ 成功：最后几行里有 `shizuku_server pid is` 和 `exit with 0`。
3. 打开手机上的 Shizuku，首页显示「**正在运行**」。

> 注意：**手机每次重启后，都要重新做这一步的第 2 小步。**

---

## 第 6 步：安装 Folduo

终端里执行（先进入下载文件夹）：
```sh
cd ~/Downloads && adb install -r Folduo-0.1.21-fold8.1.apk
```
✅ 成功：最后一行是 `Success`。
❌ 提示签名冲突（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`）：手机上装过原作者的 Folduo，先打开它点 **Stop and release display control**，卸载后再执行一次。

---

## 第 7 步：设置壁纸（最关键）

> **先说清楚：这一步做完，屏幕上看不出任何变化，这是正常的。** 后面会用 Folduo 自己的桌面盖住系统壁纸，但 Folduo 读取开合角度靠的就是这张系统壁纸。实测跳过这一步，动画一定出不来。

1. 终端里进入解压出来的文件夹：
   ```sh
   cd ~/Downloads/folduo-wallpaper-setup
   ```
2. 检查外屏壁纸：
   ```sh
   python3 cover-wallpaper.py status
   ```
   看最后一行：

   | 显示 | 怎么办 |
   | --- | --- |
   | `Cover home: original stock image` | 正常，做下一小步 |
   | `Cover home: angle-aware stock video` | 已经设置好了，**直接跳到第 8 步** |
   | `Cover home: other wallpaper` | 手机上：合上手机 → 长按外屏桌面 → 壁纸和样式 → 选三星自带的原厂图片（和内屏动态壁纸同一套的那张）→ 再执行一次这条命令 |

3. 设置外屏：
   ```sh
   python3 cover-wallpaper.py apply
   ```
   ✅ 成功：最后一行是下面两种之一：
   - `Applied angle-aware stock video to front HOME only`
   - `Already configured; no change made`

   ❌ 显示 `Expected inner angle-aware wallpaper unavailable`：内屏壁纸不对。手机上：展开手机 → 长按内屏桌面 → 壁纸和样式 → 选三星自带、**会随开合变化的动态壁纸** → 再执行一次 `apply`。

这一步只改外屏桌面的壁纸，锁屏和内屏都不动。

---

## 第 8 步：设置 Folduo

在手机上打开 **Folduo**（界面是英文），**按顺序**点：

1. **Connect Shizuku** → 弹窗里选「允许」。
2. **Allow display over other apps** → 找到 Folduo，打开开关 → 返回。
3. **Allow temporary screen access and enable** → 允许通知、允许屏幕访问。
4. **Use Folduo as the home app** → 选 **Folduo** → 选「设为默认」。

> 第 4 小步**必须做**。不用 Folduo 桌面的话，展开后内屏会黑屏、图标错乱。

---

## 第 9 步：启用并看效果

1. 手机保持**解锁**，**把手机完全合上**，等 2 秒。
   ✅ 外屏显示 Folduo 桌面（带时钟和图标）。
2. 在外屏打开「计算器」。
3. **慢慢展开手机** → 能看到磨砂玻璃一样的过渡，计算器跑到内屏上。
4. **慢慢合上** → 计算器回到外屏。

🎉 看到效果就全部完成了，USB 线可以拔掉。

> 如果一直提示 `Close the phone fully once to finish setup.`：确认第 7 步显示过成功，然后解锁状态下再完全合上一次。

---

## 以后怎么用

| 情况 | 做法 |
| --- | --- |
| **手机重启了** | 连上电脑，重做第 5 步的第 2 小步启动 Shizuku；再打开 Folduo 点 **Resume animation**，然后完全合上一次 |
| 用着用着效果没了 | 多半是 Shizuku 停了。按上一行重新启动。再检查第 1 步的自动拦截程序是不是又被打开了 |
| 屏幕卡住、点不动 | 合上手机，在外屏打开 Folduo 点 **Stop and release display control**；不行就重启手机 |
| 耗电变快 | 正常，开着效果时两块屏会一直亮着 |

---

## 不想用了，怎么恢复

1. 打开 Folduo → 点 **Stop and release display control** → 卸载 Folduo。
2. 设置 → 应用 → 选择默认应用 → 主屏幕应用 → 选「**One UI 主屏幕**」。
3. 连上电脑，恢复外屏原来的图片：
   ```sh
   cd ~/Downloads/folduo-wallpaper-setup && python3 cover-wallpaper.py restore-stock
   ```
4. 需要的话，回到第 1 步把自动拦截程序重新打开。

---

## Windows 用户看这里（未实测）

Windows 上我没有实际跑过，以下是替代做法：

- **第 3 步**：从 https://developer.android.com/tools/releases/platform-tools 下载「SDK Platform-Tools for Windows」并解压；从 https://www.python.org 安装 Python 3，安装时勾选「Add python.exe to PATH」。
- 命令里的 `adb` 要换成 platform-tools 文件夹里 `adb.exe` 的完整路径，或者在那个文件夹里打开 PowerShell，用 `.\adb`。
- **第 5 步**：不要用那两行命令。改为打开手机上的 Shizuku，选「**通过无线调试启动**」，按屏幕提示配对（手机需连 Wi-Fi）。
- **第 7 步**：命令里的 `python3` 换成 `python`，并加上 adb 路径，例如：
  ```
  python cover-wallpaper.py status --adb C:\platform-tools\adb.exe
  ```

---

更多背景和原理见 [README.zh-CN.md](../README.zh-CN.md)。本教程只在一台 SM-F9710（One UI 9）上实测过，属于实验版本。
