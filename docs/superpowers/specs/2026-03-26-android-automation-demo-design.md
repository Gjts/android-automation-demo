# Android 自动化控制 Demo 设计文档

日期：2026-03-26
主题：最小可演示版本（方案 A）

## 1. 目标

实现一个最小可演示的 Android 自动化 Demo，对应 README 中描述的两端应用：

- Target App：被操作的业务应用
- Automation App：通过 AccessibilityService 自动完成跨应用操作

本次目标是先跑通一条固定成功路径，不做工程化扩展。

本次设计将直接基于仓库中现有模块实现，而不是 README 中的旧目录名：

- Target App 模块：`src/mobile/targetapp`
- Automation App 模块：`src/mobile/automationapp`

关键修改文件位置预期为：

- `src/mobile/targetapp/app/src/main/java/com/example/targetapp/MainActivity.kt`
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/MainActivity.kt`
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/` 下新增 AccessibilityService 文件
- `src/mobile/automationapp/app/src/main/AndroidManifest.xml`
- `src/mobile/automationapp/app/src/main/res/xml/` 下新增 accessibility service 配置文件

已确认包名 / applicationId：

- Target App：`com.example.targetapp`
- Automation App：`com.example.automationapp`

## 2. 选定方案

采用方案 A：

- Target App 使用单个 `MainActivity`
- 通过 Compose 状态切换实现三页流程
- Automation App 保留一个简单入口 `MainActivity`
- 新增一个 `AccessibilityService` 实现自动化
- 自动化流程固定成功路径，不做随机或可配置分支

选择该方案的原因：

- 文件数量最少
- 最符合“最小 Demo”目标
- 能完整覆盖 README 的核心演示能力
- 便于快速验证跨应用自动化链路

## 3. Target App 设计

Target App 保持单 Activity 结构，在同一个 Compose 页面树中用状态值切换三个界面。

### 3.1 页面一：登录页

元素：
- 明确可见标题文本：`Login Page`
- 明确可见输入标签文本：`Password`
- 一个密码输入框
- `Login` 按钮
- 错误提示文本：`Wrong password`

规则：
- 输入值等于 `Test@2026` 时，点击 `Login` 进入判断页
- 否则显示 `Wrong password`

### 3.2 页面二：判断页

元素：
- 明确可见标题文本：`Decision Page`
- 固定文本：`test text 1`
- 按钮：`Test text`
- 按钮：`Not test text`

规则：
- 自动化演示固定走成功路径，因此 Automation App 会点击 `Test text`
- 如果用户手动点击 `Not test text`，则直接结束 Activity

### 3.3 页面三：PIN 页

元素：
- 明确可见标题文本：`PIN Page`
- 明确可见输入标签文本：`PIN`
- 数字输入框
- 完成状态文本：`Automation complete`

规则：
- Automation App 在此输入 `8526`
- 不设置确认按钮
- 当输入内容达到 `8526` 时，页面显示 `Automation complete`

## 4. Automation App 设计

Automation App 由两个部分组成：

### 4.1 MainActivity

职责：
- 显示简单说明
- 提示用户先手动开启无障碍服务
- 提供一个按钮，用于触发自动化流程启动

触发路径定义如下：

- 用户在 Automation App 的 `MainActivity` 中点击启动按钮
- `MainActivity` 负责设置一个简单的“armed”标记，表示本次允许执行自动化
- `MainActivity` 负责拉起 Target App（包名：`com.example.targetapp`）
- Target App 被打开后，由 `AccessibilityService` 接管页面识别和自动化操作
- `AccessibilityService` 仅在 armed 且当前目标包名为 `com.example.targetapp` 时执行自动化
- 当流程完成并识别到 `Automation complete` 后，服务清除 armed 标记，停止继续操作

因此职责边界是：
- `MainActivity` 负责“开始演示”
- `AccessibilityService` 负责“执行自动化”

### 4.2 AccessibilityService

职责：
- 监听窗口变化和内容变化事件
- 识别当前所在页面
- 对目标控件执行输入与点击操作
- 控制流程只执行一次，避免重复操作

## 5. 自动化执行逻辑

### 5.1 页面识别策略

采用最小 Demo 策略：

- 优先通过可见文本识别页面和目标按钮
- 必要时结合输入框/按钮类型判断
- 不引入复杂 ViewId 协议或深层节点分析

识别契约明确如下：

- 登录页：通过 `Login Page`、`Password`、`Login` 识别
- 判断页：通过 `Decision Page`、`test text 1`、`Test text` 识别
- PIN 页：通过 `PIN Page`、`PIN` 识别
- 完成状态：通过 `Automation complete` 识别

### 5.2 自动化动作

- 登录页：输入 `Test@2026`，点击 `Login`
- 判断页：检测到 `test text 1` 后点击 `Test text`
- PIN 页：输入 `8526`

### 5.3 防重复策略

由于 Accessibility 事件会频繁触发，使用最小限度的控制：

- 使用简单步骤状态，避免重复执行同一步
- 对短时间内重复到达的内容变化事件做轻量 debounce

预期步骤状态示例：
- `LOGIN_DONE`
- `DECISION_DONE`
- `PIN_DONE`

### 5.4 异常处理范围

本次仅做最小 Demo 级别处理：

- 某次事件中找不到目标控件时，直接等待下一次事件
- 不实现复杂重试框架
- 不做后台常驻恢复机制
- 不做复杂错误恢复 UI

## 6. Manifest 与资源变更

### 6.1 Automation App

需要新增：
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/` 下的 AccessibilityService 类
- `src/mobile/automationapp/app/src/main/AndroidManifest.xml` 中的精确 service 声明要求：
  - 使用 `<service>` 节点注册 AccessibilityService 实现类
  - 在 `<service>` 节点上声明 `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`
  - 在 `<service>` 节点上显式声明 `android:exported="false"`
  - 在 `<service>` 内声明 `<intent-filter>`，其中 action 必须为 `android.accessibilityservice.AccessibilityService`
  - 在 `<service>` 内声明 `<meta-data>`，其中：
    - `android:name="android.accessibilityservice"`
    - `android:resource="@xml/accessibility_service_config"`
- `src/mobile/automationapp/app/src/main/res/xml/accessibility_service_config.xml`

该配置 XML 需要明确支持本 Demo 所需能力：
- `android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"`
- `android:accessibilityFeedbackType="feedbackGeneric"`
- `android:canRetrieveWindowContent="true"`
- `android:notificationTimeout` 使用一个小的 debounce 级别值
- `android:packageNames="com.example.targetapp"`，将服务范围固定限制为目标应用

### 6.2 Target App

不新增额外 Activity，保持单 Activity 实现。

## 7. 交付范围

本次交付包含：

- Target App 三页流程可用
- Automation App 可启动 Target App
- AccessibilityService 可自动完成整条固定成功路径
- 两个应用可成功构建

本次不包含：

- 工程化分层重构
- 可配置自动化流程
- 随机路径
- 自动化测试体系
- 超出必要范围的 README 重写

## 8. 验证标准

完成后按以下标准验证：

1. `targetapp` 构建成功
2. `automationapp` 构建成功
3. 安装应用后，用户手动开启无障碍服务
4. 从 Automation App 启动流程后，可自动完成：
   - 输入 `Test@2026`
   - 点击 `Login`
   - 点击 `Test text`
   - 输入 `8526`
5. Target App 最终显示 `Automation complete`
6. AccessibilityService 识别到完成状态后不再重复操作

## 9. 后续实施顺序

建议实施顺序：

1. 先实现 Target App 三页流程
2. 再实现 Automation App 的入口和 AccessibilityService
3. 接入 manifest 与 accessibility service 配置
4. 最后做构建验证与必要的 README 对齐
