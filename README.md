# <p align="center">贴吧 Lite · TiebaLite</p>

<p align="center"><strong>非官方百度贴吧 Android 客户端 · Kotlin + Jetpack Compose · 无广告</strong></p>

<p align="center">
    <a href="https://github.com/ZaiLinn/TiebaLite/releases/latest">
        <img alt="Latest Release" src="https://img.shields.io/github/v/release/ZaiLinn/TiebaLite?style=flat&color=blue">
    </a>
    <a href="https://github.com/ZaiLinn/TiebaLite/actions/workflows/build.yml">
        <img alt="Build Status" src="https://github.com/ZaiLinn/TiebaLite/actions/workflows/build.yml/badge.svg">
    </a>
    <a href="LICENSE">
        <img alt="License" src="https://img.shields.io/badge/License-GPL%203.0-green.svg">
    </a>
    <img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen">
</p>

---

## 📖 简介

贴吧 Lite 是一个**非官方**的百度贴吧 Android 客户端，使用 Kotlin 编写，界面基于 Jetpack Compose，支持 Android 5.0（API 21）到 Android 16（API 36）。

本仓库是 ZaiLinn 维护的分支，基于 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite) 的 4.0-dev 分支及 qaqmin 的后续修改版本（v4.0.60）。

> **⚠️ 声明：** 本软件及源码仅供学习交流使用，严禁用于商业用途，与百度公司无关。

## 📦 下载

到 [Releases](https://github.com/ZaiLinn/TiebaLite/releases/latest) 下载最新 APK。安装后可在「关于」页检查更新，应用内更新只检查本仓库的发布。

- 包名：`com.zailinn.tiebalite`，可以和原版贴吧 Lite 同时安装。

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 📝 一键签到 | 分页拉取全部关注的吧依次签到，通知栏显示进度 |
| ⏰ 定时签到 | 到点自动签到，不需要常驻后台 |
| 📝 草稿箱 | 回复自动保存，可从草稿回到原帖楼层 |
| 🔄 倒序浏览 · 只看楼主 | 帖子页排序与筛选 |
| 🎯 消息直达 | 「回复我的」跳转到原帖对应楼层 |
| 🖼️ 图片批量下载 | 图片查看页多选保存 |
| 🔍 检查更新 | 读取本仓库 GitHub Releases，发布附带摘要时下载后校验 SHA-256 |
| 👥 多账号 | 点击头像切换账号 |
| 📋 应用日志 | 关于页 → 查看日志，可复制用于反馈问题 |
| 🧹 类型过滤 | 推荐流和吧页面可隐藏投票、带货、应用/游戏推广、抽奖、打分帖 |
| 🎚️ 等级过滤 | 帖子内隐藏低于指定等级用户的回复 |
| ✍️ 发帖 | 吧页面直接发新帖，支持标题、表情、图片 |
| @ 好友 | 回帖和发帖时从关注列表选择或搜索用户 |
| 🛡️ 验证码 | 回帖、发帖被要求验证时弹出验证，完成后自动重发 |
| 💬 私信（实验） | 消息页和用户主页收发私信，默认关闭，开启前请阅读风险提示 |
| 🔗 干净链接 | 复制网页链接时自动去掉贴吧的跟踪参数 |
| ⚡ 流畅滑动 | 安装时预编译应用代码，请求设备最高刷新率 |
| 🚫 无广告 | 无横幅、无推广、无信息流广告 |

## 🆕 本分支的改动

完整记录见 [AI_CHANGELOG.md](AI_CHANGELOG.md)。 Powered by opus-5.5

## 💡 使用贴士

| 场景 | 建议 |
|------|------|
| 🔋 签到 | 在系统设置里把本应用的电池优化设为「不限制」，避免签到被系统中断 |
| 🔄 更新 | 关于页 → 检查更新，下载完成后按提示安装 |
| 📋 反馈问题 | 关于页 → 查看日志 → 复制，提交 issue 时附上 |
| 💬 私信 | 设置 → 更多设置 → 私信（实验）开启。贴吧私信服务器只支持未加密连接，建立连接时登录凭证会明文发送，请避免在公共 Wi-Fi 下使用 |

## 💬 反馈

到 [Issues](https://github.com/ZaiLinn/TiebaLite/issues) 提交 Bug 或建议，附上应用日志和机型、系统版本。

## 🛠️ 构建

### 环境

- JDK 17+
- Android SDK（compileSdk 36）
- Android Studio（推荐）

### 签名配置

在项目根目录创建 `keystore.properties`（已在 `.gitignore` 中，不会提交）：

```properties
keystore.file=keystore/your-release.jks
keystore.password=your_store_password
keystore.key.alias=your_key_alias
keystore.key.password=your_key_password
```

`keystore.file` 是相对项目根目录的路径。没有 `keystore.properties` 时，Debug 和 Release 都使用本机 debug 签名。

### 构建命令

```bash
# Debug
./gradlew assembleDebug

# Release（开启 R8 压缩混淆）
./gradlew assembleRelease

# 单元测试
./gradlew :app:testDebugUnitTest
```

产物位于 `app/build/outputs/apk/`。

`app/src/main/baseline-prof.txt` 是覆盖全部应用代码的基线配置文件，安装时系统据此把应用代码预编译成机器码；缺少它时应用代码会先由解释器运行，滑动明显卡顿。

## 👨‍💻 致谢

- **[HuanCheng65](https://github.com/HuanCheng65)**：原创开发并维护本项目至 2024 年归档，所有原始代码与架构设计归原作者所有。
- **[qaqmin](https://github.com/qaqmin/TiebaLite)**：v4.0.0 至 v4.0.60 的修改版本。
- **[ForbidAd4TieBa](https://github.com/aikavvak12una/ForbidAd4TieBa)**:部分功能参考
- **[aiotieba](https://github.com/lumina37/aiotieba)**：私信长连接协议参考
- 详见 [CREDITS.md](CREDITS.md)。

## 🔗 相关项目

- [lumina37/aiotieba](https://github.com/lumina37/aiotieba)：Asynchronous I/O Client for Baidu Tieba
- [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf)：百度贴吧客户端 Protocol Buffers 定义文件合集
- [n0099/open-tbclient](https://github.com/n0099/open-tbclient)：历代百度贴吧客户端反编译源码

## ⚠️ 免责声明

1. 本软件为**非官方**贴吧客户端，与百度公司无任何关联。
2. 本软件及源码**仅供学习交流使用，严禁用于商业用途**。
3. 使用本软件产生的一切后果由使用者自行承担。
4. 本软件不保证功能的完整性和稳定性。

---

<details>
<summary>English</summary>

TiebaLite is an **unofficial** Baidu Tieba client for Android (API 21–36), written in Kotlin with Jetpack Compose. This repository is ZaiLinn's fork of [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite) (4.0-dev), based on qaqmin's modified v4.0.60.

- Download the latest APK from [Releases](https://github.com/ZaiLinn/TiebaLite/releases/latest). Package name `com.zailinn.tiebalite`; it installs alongside the original app.
- Build with `./gradlew assembleDebug` or `./gradlew assembleRelease` (JDK 17+, compileSdk 36). See the signing section above for `keystore.properties`.
- For learning and communication only. Commercial use is prohibited. Not affiliated with Baidu, Inc.

</details>

<p align="center">
    <sub>原作者 / Original Author: <a href="https://github.com/HuanCheng65">HuanCheng65</a> · 维护 / Maintainer: <a href="https://github.com/ZaiLinn">ZaiLinn</a> · License: GPL v3</sub>
</p>
