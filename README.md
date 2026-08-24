# 提示词优化器（Android · 人工发送模式）

> 由 [linshenkx/prompt-optimizer](https://github.com/linshenkx/prompt-optimizer)（AGPL-3.0）改造而来的原生 Android 应用。

这是原 Vue/Electron 提示词优化工具的 **Android 原生（Kotlin + Jetpack Compose）** 重构版，具备**工业级模板渲染引擎（Mustache AST + Tokenizer）、结构化 AI 回复解析引擎与高性能本地智能搜索引擎**。

---

## 🎯 核心改造

### 1. 删除所有与模型提供商相关的内容
- 移除了 `core` 中的全部 LLM Provider 适配器（OpenAI / Gemini / DeepSeek / Anthropic / Grok / 智谱 / 硅基流动 / MiniMax / ModelScope / Ollama / OpenRouter / Cloudflare / Chrome 内置 AI 等 16 家）。
- 移除了模型管理器、模型配置、API Key、连接测试、图像生成服务等所有需要"网络调用大模型"的代码。
- 移除了 `api/`、`middleware.js`、`docker/`、`env.local.example` 中的密钥与环境变量体系。
- 应用**不需要任何网络权限、不需要任何密钥**，纯本地离线运行。

### 2. 优化方式采用「人工发送模式」
原项目：`输入提示词 → 模板 → 直接调用 LLM → 得到结果`。

改造后：
```
输入提示词 → 点击按钮 → 弹出一段「待复制的提示词」→ 用户复制
→ 发送给任意在线免费 AI（DeepSeek / Kimi / 豆包 / ChatGPT / Claude / 通义…）
→ 用户把 AI 的回复粘贴回应用 → 智能解析并保存为结果（优化/迭代/测试/评估）
```

- 每一步（优化 / 迭代 / 测试 / 评估 / 变量提取 / 消息优化）都会生成一段**结构化提示词文本**，由用户复制后发给任意在线免费 AI。
- 核心的**优化模板文本**（即驱动 AI 优化、迭代、评估的那段"提示词"）**忠实移植**自原项目，因此**最终优化效果与原方案保持一致**。
- 模板中的 `{{变量}}` 占位符通过 `helpers.toJson` 逐字保留，与原项目行为一致。

---

## ⚡ 三大核心引擎优化

### 1. 工业级 Mustache 模板引擎（`MustacheRenderer`）
- **Tokenizer + 语法树 AST 两阶段架构**：彻底告别简陋的字符递归匹配，支持任意深度的区块嵌套、反向区块、注释标签（`{{! ... }}`）与点号路径多层对象安全导航。
- **并发 AST 编译缓存**：同一模板仅解析一次，后续渲染达到微秒级响应速度。
- **高精度 JSON 编码辅助（`helpers.toJson`）**：严格转义控制字符、换行与引号，100% 逐字保留待优化提示词中的 `{{变量}}` 占位符。

### 2. 智能 AI 回复解析引擎（`PromptEngine`）
- **容错 JSON 提取**：智能识别 Markdown 代码块（````json ... ````）、自然语言混排和截断文本中的结构化数据。
- **变量与评估自动解析**：自动解析变量列表、示例值推测与评估打分维度（综合得分、各维度得分、改进建议）。

### 3. 本地智能搜索引擎（`SearchEngine`）
- **拼音与缩写首字母模糊匹配**：支持输入拼音首字母（如 `xt` 命中 `系统优化`、`wst` 命中 `文生图`）与全拼匹配。
- **多词组合与多字段加权打分**：支持空格分词 AND 组合搜索；名称（10x）、分类（8x）、描述（4x）、内容（2x）多维度加权排序。
- **全局搜索覆盖**：模板管理、历史记录、收藏资产均配备实时智能检索与分类过滤。

---

## 📦 功能覆盖（人工发送版）

| 原项目功能 | 本应用 | 入口 | 智能搜索支持 |
|---|---|---|---|
| 系统提示词优化（通用/结构化/输出格式） | ✅ | 工作台 · 基础/系统 | ✅ 模板与历史检索 |
| 用户提示词优化（基础/精准/规划） | ✅ | 工作台 · 基础/用户 | ✅ 模板与历史检索 |
| 图像提示词优化（文生图/图生图/多图） | ✅ | 工作台 · 图像 | ✅ 模板与历史检索 |
| 迭代优化（含上下文感知） | ✅ | 工作台 · 迭代区 | ✅ 历史链检索 |
| 多轮对话消息优化 | ✅ | 专业 · 多轮对话 | ✅ 历史记录检索 |
| 变量提取 / 变量值生成 | ✅ | 专业 · 变量 | ✅ 智能提取解析 |
| 测试提示词 | ✅ | 测试评估 · 测试 | ✅ 历史与测试检索 |
| 结果评估 / 对比评估 / 提示词分析 | ✅ | 测试评估 | ✅ 结构化打分解析 |
| 收藏（提示词资产） | ✅ | 收藏 | ✅ 智能搜索与分类过滤 |
| 模板管理（内置只读 + 自定义） | ✅ | 首页 → 模板管理 | ✅ 实时智能搜索 |
| 历史记录（含迭代链） | ✅ | 首页 → 历史记录 | ✅ 实时智能搜索与操作筛选 |
| 会话持久化 | ✅ | 切换模式自动保存/恢复 | - |

---

## 🔧 技术栈
- Kotlin 2.0 + Jetpack Compose（Material 3）
- 单 Activity + Navigation Compose
- kotlinx.serialization（本地 JSON 持久化）
- 无需网络、无需第三方 SDK、无依赖后端

---

## ✅ 自动化测试（全部通过）
测试通过 GitHub Actions 的 `Build APK & Tests` 工作流自动执行（`gradle testDebugUnitTest`），覆盖全部引擎、功能、搜索与交互流程，构建成功才会产出 APK。

| 测试类 | 数量 | 覆盖内容 |
|---|---|---|
| `MustacheRendererTest` | 17 | 模板 AST 编译、插值、转义、注释、嵌套区块、循环、`helpers.toJson` 变量保留与缓存一致性 |
| `SearchEngineTest` | 8 | 模板/历史/收藏智能搜索、拼音首字母匹配、多关键词 AND 匹配、多字段打分与过滤 |
| `PromptEngineTest` | 18 | 各模式提示词生成渲染、Markdown JSON 提取、变量提取解析、变量值生成解析、评估报告解析 |
| `TemplateCatalogTest` | 6 | 全部内置模板完整性、类型覆盖、逐字保留 `{{变量}}`、可渲染性 |
| `SerializationTest` | 4 | 数据模型 JSON 序列化往返、向后兼容 |
| `RepositoryTest` | 13 | 模板/历史/收藏/分类/会话/模式选择 的持久化 CRUD |
| `AppFlowTest` | 19 | 端到端业务流：优化、迭代、多轮对话、变量提取与示例值生成、测试、评估、收藏检索、模板搜索、历史检索、会话持久化 |

运行测试：
```bash
./gradlew testDebugUnitTest
```

---

## 🚀 构建与运行
1. 用 **Android Studio** 打开本目录（`prompt-optimizer-android/`）。
2. 等待 Gradle 同步（需 AGP 8.5 / JDK 17+）。
3. 连接设备或模拟器，运行 `app`。

```bash
# 也可命令行构建（需 Android SDK）
./gradlew assembleDebug
```

最低 Android 版本：**Android 8.0（API 26）**。

---

## 📂 工程结构
```
app/src/main/java/com/promptoptimizer/
├── MainActivity.kt             # 入口
├── PromptOptimizerApp.kt       # Application，持有数据仓库
├── model/Models.kt             # 数据模型（模板/历史/收藏/会话）
├── template/
│   ├── TemplateCatalog.kt      # 内置优化模板（忠实移植原项目）
│   └── MustacheRenderer.kt     # 工业级 Mustache AST 模板引擎
├── core/
│   ├── PromptEngine.kt         # 提示词引擎与结构化 AI 回复解析
│   └── SearchEngine.kt         # 本地智能多维度搜索引擎
├── data/Repository.kt          # 本地 JSON 持久化仓库
└── ui/
    ├── theme/                  # 主题
    ├── nav/NavGraph.kt         # 导航
    ├── components/             # CopyablePromptDialog（人工发送核心对话框）
    ├── screens/                # 首页/工作台/专业/测试评估/收藏/模板/历史
    └── viewmodel/MainViewModel.kt
```

## 📄 License
本项目沿用原项目 **AGPL-3.0** 许可。原项目版权归 linshenkx 所有。
