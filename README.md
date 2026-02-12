# Text Safety Check — Kettle 文本安全检测插件

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3.9%2B-blue.svg)](https://www.python.org/)
[![Kettle](https://img.shields.io/badge/Kettle-9.3-green.svg)](https://github.com/pentaho/pentaho-kettle)

**Pentaho Kettle (PDI)** 原生步骤插件，用于在 ETL 数据流中对文本内容进行安全风险检测。配合轻量级 Python 后端服务，使用本地安全分类模型完成推理，无需依赖任何云端 API。

## 功能特性

- 🛡️ **拖拽即用** — 在 Spoon 中直接拖入安全检测步骤
- 🔌 **零代码集成** — 只需配置输入字段和阈值即可运行
- 🏠 **完全本地化** — 模型在本地运行，无需调用外部 API
- ⚡ **逐行检测** — 自动追加 `is_safe`、`risk_category`、`risk_score` 三个输出字段
- 🎯 **可配置阈值** — 每个转换可独立设置风险灵敏度
- 🌐 **29 种风险类别** — 覆盖暴力、欺诈、隐私、色情、法律等场景

## 架构

```
Kettle Spoon
  ┌───────────────────────────────────────────────────┐
  │  表输入 → [Text Safety Check] → 过滤行 → 输出    │
  └────────────────────┬──────────────────────────────┘
                       │ HTTP POST（逐行调用）
                       ▼
  ┌───────────────────────────────────────────────────┐
  │  text_filter_service（Python，端口 8001）          │
  │  FastAPI + YuFeng-XGuard-Reason-0.6B              │
  └───────────────────────────────────────────────────┘
```

## 环境要求

| 组件 | 版本 |
|------|------|
| Java | 8+ |
| Maven | 3.6+ |
| Python | 3.9+ |
| Pentaho Kettle (PDI) | 9.x |
| CUDA（可选） | 11.8+，用于 GPU 加速 |

## 快速开始

### 1. 下载安全分类模型

将 [YuFeng-XGuard-Reason-0.6B](https://modelscope.cn/models/Alibaba-AAIG/YuFeng-XGuard-Reason-0.6B) 下载到本地目录：

```bash
# 使用 modelscope CLI
pip install modelscope
modelscope download --model Alibaba-AAIG/YuFeng-XGuard-Reason-0.6B --local_dir ./YuFeng-XGuard-Reason-0.6B
```

### 2. 启动 Python 后端服务

```bash
cd text_filter_service
pip install -r requirements.txt
```

在 `text_filter_service/` 目录下创建 `.env` 文件配置模型路径：

```env
MODEL_PATH=/path/to/YuFeng-XGuard-Reason-0.6B
DEVICE=auto
HOST=0.0.0.0
PORT=8001
THRESHOLD=0.5
```

启动服务：

```bash
python app.py
```

验证服务是否正常运行：

```bash
curl http://localhost:8001/health
# {"status":"ok"}
```

### 3. 构建 Kettle 插件

```bash
cd kettle-plugin
mvn clean package
```

构建产物为 `target/TextSafetyCheck.zip`。

### 4. 安装到 Kettle

1. 将 `TextSafetyCheck.zip` 解压到 Kettle 的插件目录：
   ```
   <kettle安装目录>/plugins/steps/TextSafetyCheck/
   ```

2. 目录结构如下：
   ```
   TextSafetyCheck/
   ├── text-safety-check-plugin-1.0.0.jar
   ├── plugin.xml
   ├── text_safety.svg
   └── lib/
       └── gson-2.10.1.jar
   ```

3. 重启 Spoon。

### 5. 在 Spoon 中使用

1. 新建转换
2. 添加数据输入步骤（CSV、数据库等）
3. 从 **Transform（转换）** 分类中拖入 **「Text Safety Check」** 步骤
4. 双击打开配置对话框：

   | 配置项 | 说明 | 默认值 |
   |--------|------|--------|
   | Input Text Field | 选择上游数据流中的文本字段 | — |
   | Service URL | Python 后端服务地址 | `http://localhost:8001/api/check` |
   | Risk Threshold | 风险阈值（0~1），超过此值判定为不安全 | `0.5` |
   | Output: Is Safe | 输出布尔字段名 | `is_safe` |
   | Output: Risk Category | 输出风险类别字段名 | `risk_category` |
   | Output: Risk Score | 输出风险分数字段名 | `risk_score` |

5. 后接 **Filter Rows（过滤行）** 步骤，按 `is_safe` 分流
6. 分别连接安全数据和违规数据的输出步骤

### 示例流程

```
CSV输入 → Text Safety Check → 过滤行
                                 ├── is_safe = true  → 安全数据输出
                                 └── is_safe = false → 违规记录输出
```
<img width="661" height="365" alt="image" src="https://github.com/user-attachments/assets/bda4a69c-0192-423e-93a8-5393570e6dd2" />
<img width="726" height="342" alt="image" src="https://github.com/user-attachments/assets/6071d61e-0d8e-40a5-a062-235084258a11" />

## 风险类别

| 代码 | 说明 | 代码 | 说明 |
|------|------|------|------|
| `sec` | 安全 | `dw` | 危险物品/武器 |
| `pc` | 政治敏感 | `dc` | 歧视内容 |
| `pi` | 个人信息泄露 | `ec` | 经济犯罪 |
| `ac` | 成人内容 | `def` | 诽谤 |
| `ti` | 恐怖主义煽动 | `cy` | 网络安全威胁 |
| `ph` | 人身伤害 | `mh` | 心理健康 |
| `se` | 社会工程攻击 | `sci` | 科学谣言 |
| `pp` | 隐私侵犯 | `cs` | 儿童安全 |
| `acc` | 账户安全 | `mc` | 恶意代码 |
| `ha` | 骚扰 | `ps` | 公共安全 |
| `ter` | 恐怖主义 | `sd` | 自残 |
| `ext` | 极端主义 | `fin` | 金融欺诈 |
| `med` | 医疗误导 | `law` | 违法内容 |
| `cm` | 内容操纵 | `ma` | 恶意软件 |
| `md` | 虚假信息 | | |

## 项目结构

```
.
├── README.md
├── LICENSE
├── .gitignore
├── kettle-plugin/                  # Kettle 步骤插件（Java）
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/pentaho/di/
│       │   ├── trans/steps/textsafety/
│       │   │   ├── TextSafetyCheckStepMeta.java    # 步骤元数据与配置
│       │   │   ├── TextSafetyCheckStepData.java    # 运行时数据容器
│       │   │   └── TextSafetyCheckStep.java        # 核心逻辑（HTTP 调用 + 行处理）
│       │   └── ui/trans/steps/textsafety/
│       │       └── TextSafetyCheckStepDialog.java  # SWT 配置对话框
│       ├── resources/
│       │   ├── plugin.xml                          # 插件描述文件
│       │   └── text_safety.svg                     # 插件图标
│       └── assembly/
│           └── plugin.xml                          # Maven 打包描述文件
└── text_filter_service/            # Python 后端服务
    ├── app.py                      # FastAPI 入口
    ├── text_classifier.py          # 模型加载与推理
    ├── api_models.py               # 请求/响应数据模型
    ├── service_config.py           # 配置（支持 .env）
    └── requirements.txt            # Python 依赖
```

## API 接口

### `GET /health`

健康检查接口。

**响应：** `{"status": "ok"}`

### `POST /api/check`

对单条文本进行安全风险检测。

**请求体：**
```json
{
  "text": "待检测的文本内容",
  "threshold": 0.5
}
```

**响应体：**
```json
{
  "is_safe": true,
  "risk_category": null,
  "risk_score": 0.02,
  "risk_details": {
    "sec": 0.95,
    "ac": 0.02,
    "ph": 0.01
  }
}
```

## 配置说明

### Python 服务配置（`text_filter_service/.env`）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MODEL_PATH` | 安全分类模型目录路径 | `./YuFeng-XGuard-Reason-0.6B` |
| `DEVICE` | PyTorch 设备（`auto`、`cpu`、`cuda`） | `auto` |
| `HOST` | 服务监听地址 | `0.0.0.0` |
| `PORT` | 服务端口 | `8001` |
| `THRESHOLD` | 默认风险阈值（可被 Kettle 传入值覆盖） | `0.5` |

## 从源码构建

### Kettle 插件

> **注意：** `kettle-plugin/src/main/resources/lib/` 目录需要包含本地 Kettle 的 JAR 文件（`kettle-core`、`kettle-engine`、`kettle-ui-swt`、`metastore`）。由于许可证限制，这些文件不包含在仓库中。请从你的 Kettle 安装目录的 `lib/` 文件夹中复制。

```bash
cd kettle-plugin
mvn clean package
# 产物：target/TextSafetyCheck.zip
```

### Python 服务

```bash
cd text_filter_service
python -m venv .venv
source .venv/bin/activate  # Windows 系统使用：.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

## 参与贡献

欢迎提交 Pull Request！

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/amazing-feature`）
3. 提交更改（`git commit -m '添加某个新功能'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 创建 Pull Request

## 许可证

本项目基于 Apache License 2.0 开源，详见 [LICENSE](LICENSE) 文件。

## 致谢

- [YuFeng-XGuard-Reason-0.6B](https://modelscope.cn/models/Alibaba-AAIG/YuFeng-XGuard-Reason-0.6B) — 安全分类模型
- [Pentaho Data Integration](https://github.com/pentaho/pentaho-kettle) — ETL 平台
- [FastAPI](https://fastapi.tiangolo.com/) — Python Web 框架
