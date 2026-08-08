---
name: moekoe-device-qa
description: 使用用户明确指定且已连接的 Android 物理设备验证 MoeKoeMusic 的 Insets、IME、触控、厂商系统栏、系统覆盖层和真实生命周期行为。涉及 adb、真机截图、UIAutomator、设备日志或准备报告真机验收结果时使用。
---

# MoeKoe 真机验收

## 工作流

1. 先完成相关单元、截图回归与 `$moekoe-visual-qa`；普通几何和设计稿复刻不进入真机循环。
2. 要求用户明确给出设备 serial；运行 `adb devices -l`，禁止自动选择首台设备。
3. 运行 `adb -s <serial> shell getprop ro.boot.qemu`；值为 `1`、设备离线或 serial 不匹配时停止。不得创建、启动或用模拟器替代。
4. 明确 APK、applicationId、Activity 和场景后才能安装或启动；不得操作范围外 App。
5. 优先获取 UIAutomator 语义树，只在语义无法证明 Insets、IME、触控或系统覆盖时截图。交互前记录动作，页面变化后重新获取语义树。
6. 使用 [`scripts/collect-device-evidence.sh`](scripts/collect-device-evidence.sh) 收集只读证据到 `build/reports/device-qa/<scenario>/`，人工查看图片并报告实际结论。

## 安全门禁

- 所有 adb 命令必须带 `-s <serial>`；禁止通用 shell、恢复出厂、清除用户数据、解锁 Bootloader 和未授权安装。
- 不提交 serial、手机号、账号、Cookie、设备标识、完整 logcat、UI 树或截图；报告只引用本地 build 产物并脱敏。
- 只过滤当前 applicationId 的日志；发现用户内容或凭据立即停止并删除未脱敏产物。
- 真机结果只证明本轮明确场景，不替代其他 API、窗口、主题、字体或设计符合度。

## 完成输出

- 报告设备型号/API（不含 serial）、APK/提交、场景、动作、证据目录、通过/失败和未验证项。
- 完成前调用 `$moekoe-validate-change`；失败或误通过按 `$moekoe-skill-evolution` 登记 incident。
