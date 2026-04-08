# MoviePilot Android App

Android WebView 客户端，用于访问 MoviePilot 影视媒体库管理系统。

## 功能特性

- 🌐 **可配置服务器地址** — 登录页可自定义服务器 URL
- 🔐 **自动登录** — 保存凭证后，打开 APP 自动注入并提交登录
- 💾 **凭证持久化** — 账号密码安全保存，长期有效
- 🍪 **Session 保持** — Cookie 持久化，关闭 APP 再打开无需重新登录
- 👆 **下拉刷新** — 支持下拉刷新页面内容
- ↩️ **返回键导航** — WebView 内页面回退，退出 APP 保留登录状态

## 技术栈

- Java (Android SDK 36)
- Gradle 8.13 + AGP 8.7.3
- AndroidX + SwipeRefreshLayout
- WebView + CookieManager

## 构建

### 前置要求

- Android SDK (compileSdk 36)
- JDK 17+

### 构建步骤

1. 创建 `local.properties` 文件：
   ```
   sdk.dir=你的Android SDK路径
   ```

2. 构建 Debug APK：
   ```bash
   ./gradlew assembleDebug
   ```

3. APK 输出路径：
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 构建 Release APK

```bash
./gradlew assembleRelease
```

> 注意：Release 构建需要配置签名密钥。请勿将 `.jks` 或 `.keystore` 文件提交到 Git。

## 项目结构

```
├── app/src/main/
│   ├── java/com/moviepilot/app/
│   │   ├── ConfigManager.java      # 配置和常量管理
│   │   ├── LoginActivity.java       # 登录页（服务器地址+账号密码）
│   │   ├── MainActivity.java        # WebView 主页
│   │   └── WebAppInterface.java     # JS Bridge 接口
│   ├── res/
│   │   ├── layout/                  # 布局文件
│   │   ├── drawable/                # 图形资源
│   │   ├── values/                  # 颜色、字符串、主题
│   │   └── xml/                     # 网络安全配置
│   └── AndroidManifest.xml
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 许可证

MIT
