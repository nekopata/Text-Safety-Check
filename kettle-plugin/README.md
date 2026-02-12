# Text Safety Check — Kettle 步骤插件

Pentaho Kettle (PDI) 原生步骤插件，用于在 ETL 数据流中对文本内容进行安全风险检测。

完整文档请参阅 [项目 README](../README.md)。

## 构建

> **注意：** 构建前需将 Kettle 安装目录 `lib/` 下的 JAR 文件复制到 `src/main/resources/lib/`，至少包含 `kettle-core`、`kettle-engine`、`kettle-ui-swt`、`metastore`。

```bash
mvn clean package
```

产物：`target/TextSafetyCheck.zip`

## 安装

将 `TextSafetyCheck.zip` 解压到 `<Kettle安装目录>/plugins/steps/`，重启 Spoon 即可。

