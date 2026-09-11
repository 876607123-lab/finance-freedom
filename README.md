# 我的财务自由 · Android APK

单文件 HTML App（财务自由计算器）通过 Capacitor 封装为 Android 安装包。

## 自动构建

每次推送代码到 `main` 分支，GitHub Actions 会自动：

1. 安装 Node 20 + JDK 17
2. `npm install` 安装 Capacitor
3. `npx cap add android && npx cap sync android` 生成安卓工程
4. `./gradlew assembleDebug` 编译 Debug APK
5. 在运行页面底部的 **Artifacts** 中上传 `app-debug-apk`

手动触发：仓库顶部 **Actions** → **Build APK** → **Run workflow**。

## 下载安装

1. 打开最新一次成功运行的 Build APK
2. 页面底部 Artifacts 下载 `app-debug-apk`
3. 解压得到 `app-debug.apk`，发送到安卓手机安装
4. 如被拦截，允许「未知来源应用」

## 修改 App

直接替换 `www/index.html`，推送后 Actions 自动出新 APK。

## 工程结构

```
www/index.html                 # 单文件 App（页面、样式、逻辑全部在此）
package.json                   # Capacitor 依赖
capacitor.config.ts            # App 配置（包名、应用名）
.github/workflows/build-apk.yml# 自动构建流水线
```

## 云端同步说明

App 内「云端同步」页使用 Gitee 码云私有仓库做备份：

1. gitee.com 新建私有仓库（如 `ff-backup`）
2. 「设置 → 私人令牌」生成令牌，勾选 `projects` 权限
3. 在 App 云端同步页填入令牌 / 用户名 / 仓库名，开启自动上传
4. 换手机后填同样信息，点「从云端下载」完整恢复
