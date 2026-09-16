# Folduo Fold8 快速上手

本文面向 Galaxy Z Fold8 **SM-F9710**（国行，One UI 9）用户，照着做大约 15 分钟。背景和原理见 [README.zh-CN.md](../README.zh-CN.md)。

## 需要准备

- Galaxy Z Fold8 SM-F9710。其他版本的 Fold8 型号号码不同，这一版会被型号检查挡住。
- 一台电脑（Mac、Windows、Linux 都可以），装好 ADB 和 Python 3。只在设置时用一次。
- 一根 USB 数据线。
- 从 [Releases](https://github.com/sjw1933/Folduo/releases) 下载两个文件：
  - `Folduo-0.1.21-fold8.1.apk`
  - `folduo-wallpaper-setup-0.1.21-fold8.1.zip`（下载后解压）

可以用 `SHA256SUMS` 核对下载的文件：Mac 上运行 `shasum -a 256 文件名`，Linux 上运行 `sha256sum 文件名`。

## 1. 手机准备

1. 进「设置 → 安全和隐私 → 自动拦截程序」，**把它关掉，并关掉自动开启**。它开着时会拦截 USB 调试和安装外部应用，而且关掉后约 30 分钟会自己重新打开。
2. 进「设置 → 关于手机 → 软件信息」，连续点「编译编号」7 次，打开开发者选项。
3. 进「设置 → 开发者选项」，打开 **USB 调试**。

## 2. 电脑连接手机

1. 安装 ADB：
   - Mac：`brew install android-platform-tools`
   - Windows / Linux：下载 Google 的 [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)，解压后在解压目录里打开终端。
2. 用 USB 线连上手机，手机上弹出「允许 USB 调试吗？」时，勾选「一律允许」，再点允许。
3. 电脑上运行 `adb devices`，列表里出现一台 `device`，就说明连好了。

## 3. 安装并启动 Shizuku

1. 从 Google Play 或 [Shizuku 的 GitHub Releases](https://github.com/RikkaApps/Shizuku/releases) 安装 Shizuku。
2. 启动 Shizuku，两种方式任选一种：
   - **无线调试（不用电脑）**：打开 Shizuku，按「通过无线调试启动」的提示配对并启动。
   - **用电脑（Mac / Linux）**：
     ```sh
     DIR=$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed 's/^package://; s#/base.apk$##')
     adb shell "$DIR/lib/arm64/libshizuku.so"
     ```
     看到 `shizuku_server pid is …` 就是启动成功了。

## 4. 安装 Folduo

```sh
adb install -r Folduo-0.1.21-fold8.1.apk
```

如果手机上装过原作者的 Folduo，签名不同，会装不上。先打开原版点 **Stop and release display control**，卸载后再装。

## 5. 设置壁纸（最关键，漏了就没有动画）

1. **内屏**：展开手机，长按内屏桌面，进「壁纸和样式」，选三星自带、**会随开合变化的动态壁纸**（内部文件名是 `video_002.mp4`）。
2. **外屏**：在解压后的 `folduo-wallpaper-setup` 目录里运行：
   ```sh
   python3 cover-wallpaper.py status
   python3 cover-wallpaper.py apply
   ```
   看到 `Applied angle-aware stock video to front HOME only` 就是设置成功了。它只改外屏桌面，锁屏和内屏不动。
   如果提示 `Expected inner angle-aware wallpaper unavailable`，说明内屏选的不是那张动态壁纸，回到上一步重新选。

## 6. 打开 Folduo 完成设置

1. 点 **Connect Shizuku**，在弹窗里允许授权。
2. 点 **Allow display over other apps**，把 Folduo 的开关打开，然后返回。
3. 点 **Allow temporary screen access and enable**，允许通知和临时屏幕访问。
4. **推荐**点 **Use Folduo as the home app**，选 Folduo。不设的话，开着动画时内屏桌面会是黑背景，布局也不对。
5. 保持解锁，**把手机完全合上一次**，等 1～2 秒。
6. 打开一个应用（比如计算器），慢慢展开、再合上，就能看到磨砂过渡效果。

## 日常使用

- **每次重启手机后**，都要重新启动 Shizuku（第 3 步），再在 Folduo 里点 **Resume animation**。
- 开着动画时两块屏会一直亮着，会更耗电。
- 屏幕卡住时：合上手机，在外屏打开 Folduo 点停止；点不了就重启手机。

## 恢复原样

1. 在 Folduo 里点 **Stop and release display control**，然后卸载 Folduo。
2. 换回三星桌面：进「设置 → 应用 → 选择默认应用 → 主屏幕应用」，选 One UI 主屏幕。
3. 恢复外屏原厂图片：
   ```sh
   python3 cover-wallpaper.py restore-stock
   ```
4. 需要的话，重新打开自动拦截程序。

## 常见问题

| 现象 | 原因和办法 |
| --- | --- |
| 一直显示 “Close the phone fully once to finish setup.” | 外屏壁纸没设置，或者内屏不是那张动态壁纸。重做第 5 步。 |
| 用着用着效果没了 | Shizuku 停了。常见原因是自动拦截程序自己重新打开，关掉了 USB 调试。关掉它的自动开启，再重新启动 Shizuku。 |
| 提示这一版只支持 SM-F966Z 和 SM-F9710 | 你的手机型号不在支持列表里。 |
| 展开后内屏桌面是黑的 | 默认桌面还是三星桌面。按第 6 步把 Folduo 设为默认桌面。 |
