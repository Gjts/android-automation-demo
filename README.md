# Android 自动化控制 Demo（Accessibility + Instrumentation）

## 📌 项目简介

本项目用于演示 Android UI 自动化控制能力，基于 **AccessibilityService（无障碍服务）** 实现跨应用操作。

项目包含两个应用：

* **Target App（业务应用）**：模拟真实用户操作流程
* **Automation App（自动化控制应用）**：通过系统能力自动完成整个流程

本项目主要体现：

* 跨应用 UI 自动化能力
* Android 系统级能力（Accessibility）
* 事件驱动的自动化设计思路

---

## 🧩 项目结构

```
android-automation-demo/
├── target-app/         # 被操作的业务应用
├── automation-app/     # 自动化控制应用（Accessibility）
├── README.md
```

---

## 📱 Target App 功能说明

### 1️⃣ 登录页

* 输入框（EditText）
* 登录按钮
* 校验密码：`Test@2026`

---

### 2️⃣ 判断页

* 文本：`test text 1`
* 按钮1：`Test text` → 进入下一页
* 按钮2：`Not test text` → 退出应用

---

### 3️⃣ PIN 输入页

* 仅允许输入数字
* 自动弹出系统数字键盘
* 输入内容：`8526`

---

## 🤖 Automation App 功能说明

自动化流程如下：

1. 启动 Target App
2. 自动输入密码 `Test@2026`
3. 点击登录按钮
4. 进入第二页后：

   * 若检测到 `test text 1` → 点击 `Test text`
   * 否则 → 点击 `Not test text`
5. 在第三页输入 PIN：`8526`

---

## ⚙️ 技术实现说明

### 1️⃣ 核心技术：AccessibilityService

使用无障碍服务实现：

* 获取当前界面 UI 树（AccessibilityNodeInfo）
* 查找目标控件（通过文本 / ViewId）
* 执行点击、输入等操作

---

### 2️⃣ 事件驱动模型

通过监听：

```
onAccessibilityEvent()
```

实现 UI 变化触发逻辑执行，而不是轮询查找，提高性能与实时性。

---

### 3️⃣ 状态机控制流程

自动化流程采用简单状态机设计：

```
登录页 → 判断页 → PIN页
```

避免重复操作和逻辑混乱。

---

### 4️⃣ 防抖处理（关键点）

针对：

```
TYPE_WINDOW_CONTENT_CHANGED
```

事件频繁触发的问题，增加 debounce 机制，避免重复处理，提高稳定性。

---

### 5️⃣ UI识别策略

优先级：

1. ViewId（如果可用）
2. 文本匹配
3. 节点结构辅助判断

---

## 🚀 运行方式

### Step 1：安装应用

安装以下两个 APK：

* Target App
* Automation App

---

### Step 2：开启无障碍服务（必须）

路径：

```
系统设置 → 无障碍 → Automation App → 开启
```

⚠️ 未开启将无法执行自动化

---

### Step 3：启动自动化

打开 Automation App，触发自动化流程：

* 自动打开目标应用
* 自动完成全部操作流程

---

## 🛠️ 技术栈

* Kotlin / Java
* Android SDK
* AccessibilityService

---

## 📌 注意事项

* 无障碍权限需手动开启
* 不同厂商 ROM（如 MIUI / EMUI）可能存在限制
* 本项目重点在系统能力验证，不涉及 UI 美观

---

## 🎯 项目总结

本项目展示了：

* Android 跨应用自动化能力
* 基于事件驱动的 UI 控制方式
* 对系统权限模型和 UI 结构的理解

适用于自动化测试、RPA、系统工具类应用等场景。

---
