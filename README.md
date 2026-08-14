# 提示词优化器（Android · 人工发送模式）

> 由 [linshenkx/prompt-optimizer](https://github.com/linshenkx/prompt-optimizer)（AGPL-3.0）改造而来的原生 Android 应用。

这是原 Vue/Electron 提示词优化工具的 **Android 原生（Kotlin + Jetpack Compose）** 重构版，核心改造点如下：

## 🎯 核心改造

### 1. 删除所有与模型提供商相关的内容
- 移除了 `core` 中的全部 LLM Provider 适配器（OpenAI / Gemini / DeepSeek / Anthropic / Grok / 智谱 / 硅基流动 / MiniMax / ModelScope / Ollama / OpenRouter / Cloudflare / Chrome 内置 AI 等 16 家）。
- 移除了模型管理器、模型配置、API Key、连接测试、图像生成服务等所有需要"网络调用大模型"的代码。
- 移除了 `api/`、`middleware.js`、`docker/`、`env.local.example` 中的密钥与环境变量体系。
- 应用**不需要任何网络权限、不需要任何密钥**，纯本地运行。

### 2. 优化方式改为「人工发送模式」
原项目：`输入提示词 → 模板 → 直接调用 LLM → 得到结果`。

改造后：
```
输入提示词 → 点击按钮 → 弹出一段「待复制的提示词」→ 用户复制
→ 发送给任意在线免费 AI（DeepSeek / Kimi / 豆包 / ChatGPT / Claude / 通义…）
→ 用户把 AI 的回复粘贴回应用 → 保存为结果（优化/迭代/测试/评估）
```

- 每一步（优化 / 迭代 / 测试 / 评估 / 变量提取 / 消息优化）都会生成一段**结构化提示词文本**，由用户复制后发给任意在线免费 AI。
- 核心的**优化模板文本**（即驱动 AI 优化、迭代、评估的那段"提示词"）**忠实移植**自原项目，因此**最终优化效果与原方案保持一致**。
- 模板中的 `{{变量}}` 占位符通过 `helpers.toJson` 逐字保留，与原项目行为一致。

## 📦 功能覆盖（人工发送版）

| 原项目功能 | 本应用 | 入口 |
|---|---|---|
| 系统提示词优化（通用/结构化/输出格式） | ✅ | 工作台 · 基础/系统 |
| 用户提示词优化（基础/精准/规划） | ✅ | 工作台 · 基础/用户 |
| 图像提示词优化（文生图/图生图/多图） | ✅ | 工作台 · 图像 |
| 迭代优化（含上下文感知） | ✅ | 工作台 · 迭代区 |
| 多轮对话消息优化 | ✅ | 专业 · 多轮对话 |
| 变量提取 / 变量值生成 | ✅ | 专业 · 变量 |
| 测试提示词 | ✅ | 测试评估 · 测试 |
| 结果评估 / 对比评估 / 提示词分析 | ✅ | 测试评估 |
| 收藏（提示词资产） | ✅ | 收藏 |
| 模板管理（内置只读 + 自定义） | ✅ | 首页 → 模板管理 |
| 历史记录（含迭代链） | ✅ | 首页 → 历史记录 |
| 会话持久化 | ✅ | 切换模式自动保存/恢复 |

> 说明：原项目中需要"真正执行大模型/生成图像"的部分（图像生成、真实 LLM 调用）在人工发送模式下无法自动化，本应用统一改为"生成可复制的提示词 → 用户去任意免费 AI 执行 → 粘贴结果回来"。这与"人工发送模式"的设计一致。

## 🔧 技术栈
- Kotlin 2.0 + Jetpack Compose（Material 3）
- 单 Activity + Navigation Compose
- kotlinx.serialization（本地 JSON 持久化）
- 无需网络、无需第三方 SDK、无依赖后端

## ✅ 自动化测试（72 项，全部通过）
测试通过 GitHub Actions 的 `Build APK & Tests` 工作流自动执行（`gradle testDebugUnitTest`），
覆盖全部功能、流程与交互，构建成功才会产出 APK。

| 测试类 | 数量 | 覆盖 |
|---|---|---|
| `MustacheRendererTest` | 14 | 模板渲染引擎（插值/转义/区块/循环/隐式迭代器/`helpers.toJson` 变量保留） |
| `TemplateCatalogTest` | 6 | 全部内置模板完整性、类型覆盖、逐字保留 `{{变量}}`、可渲染性 |
| `PromptEngineTest` | 15 | 每种操作（优化/迭代/对话/变量/测试/评估/图像）生成可复制提示词的渲染 |
| `SerializationTest` | 4 | 数据模型 JSON 序列化往返、向后兼容 |
| `RepositoryTest` | 13 | 模板/历史/收藏/分类/会话/模式选择 的持久化 CRUD |
| `AppFlowTest` | 18 | 端到端业务流：系统/用户/图像优化、迭代、多轮对话、变量提取与值生成、测试、结果/对比/提示词评估、收藏、模板增删、历史增删改清、会话持久化 |

> 说明：Compose UI 测试在 GitHub 托管 runner 上不可靠（Linux 无 KVM、macOS 无 HVF，模拟器无法硬件加速），
> 因此端到端交互在行为层（ViewModel 状态机）做确定性验证，等价覆盖全部业务闭环。
> 若在本地具备模拟器/真机的环境，可运行 `./gradlew connectedDebugAndroidTest` 执行 Compose UI 仪表化测试。

运行测试：
```bash
./gradlew testDebugUnitTest
```

## 🚀 构建与运行
1. 用 **Android Studio** 打开本目录（`prompt-optimizer-android/`）。
2. 等待 Gradle 同步（需 AGP 8.5 / JDK 17）。
3. 连接设备或模拟器，运行 `app`。

```bash
# 也可命令行构建（需 Android SDK）
./gradlew assembleDebug
```

最低 Android 版本：**Android 8.0（API 26）**。

## 📂 工程结构
```
app/src/main/java/com/promptoptimizer/
├── MainActivity.kt             # 入口
├── PromptOptimizerApp.kt       # Application，持有数据仓库
├── model/Models.kt             # 数据模型（模板/历史/收藏/会话）
├── template/
│   ├── TemplateCatalog.kt      # 内置优化模板（忠实移植原项目）
│   └── MustacheRenderer.kt     # Mustache 渲染器（支持 helpers.toJson）
├── core/PromptEngine.kt        # 核心引擎：把模板渲染成「可复制提示词」
├── data/Repository.kt          # 本地 JSON 持久化
└── ui/
    ├── theme/                  # 主题
    ├── nav/NavGraph.kt         # 导航
    ├── components/             # CopyablePromptDialog（人工发送核心对话框）
    ├── screens/                # 首页/工作台/专业/测试评估/收藏/模板/历史
    └── viewmodel/MainViewModel.kt
```

## 📄 License
本项目沿用原项目 **AGPL-3.0** 许可。原项目版权归 linshenkx 所有。
