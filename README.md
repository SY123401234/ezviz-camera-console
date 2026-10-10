# 萤石云摄像头控制台

基于 Spring Boot 3 与原生 HTML/JavaScript 的萤石云（海康威视）摄像头 Web 控制台，通过萤石云开放平台 API 实现：

- 设备列表查询
- 实时视频预览（萤石云在线播放器 iframe）
- 云台控制（上下左右 + 速度调节，支持鼠标、触摸和键盘方向键）

## 项目结构

```
ezviz-camera-console/
├── index.html                         # 根目录前端页面（开发参考）
├── pom.xml                            # Maven 构建配置（Java 17，Spring Boot 3.3.5）
├── src/main/java/com/example/ezviz/
│   ├── EzvizConsoleApplication.java   # 启动类
│   ├── EzvizController.java           # REST 接口 /api/ezviz/*
│   └── EzvizService.java              # 萤石云 API 封装，AccessToken 缓存
├── src/main/resources/
│   ├── application.yml                # 运行配置（端口、萤石云地址、凭据环境变量）
│   └── static/index.html              # 打包进 jar 的前端页面
└── .github/workflows/
    └── azure-webapps-node.yml         # GitHub Actions：Maven 构建并上传 jar 产物
```

> 修改前端时，需要同步更新 `index.html` 与 `src/main/resources/static/index.html`。

## 架构

```
浏览器 (index.html) → Spring Boot 后端 → 萤石云开放平台 API → 摄像头
```

AppKey / AppSecret 只保存在后端环境变量中，不会暴露给前端。

## 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ezviz/token` | GET | 获取 AccessToken |
| `/api/ezviz/devices` | GET | 设备列表，可选参数 `pageStart`、`pageSize` |
| `/api/ezviz/live` | GET | 直播地址，参数 `deviceSerial`、`channelNo`、`quality` |
| `/api/ezviz/ptz` | POST | 云台控制，参数 `deviceSerial`、`channelNo`、`action`（up/down/left/right）、`speed` |
| `/api/ezviz/ptz/stop` | POST | 停止云台，参数 `deviceSerial`、`channelNo` |

## 配置

`src/main/resources/application.yml` 通过环境变量读取配置：

| 环境变量 | 必填 | 默认值 | 说明 |
|----------|------|--------|------|
| `EZVIZ_APP_KEY` | 是 | 空 | 萤石云开放平台 AppKey |
| `EZVIZ_APP_SECRET` | 是 | 空 | 萤石云开放平台 AppSecret |
| `EZVIZ_BASE_URL` | 否 | `https://open.ezvizlife.com` | 萤石云 API 地址，按账号所在区域调整 |
| `PORT` | 否 | `8080` | 服务端口 |

凭据未配置时，服务可以启动，但调用萤石云接口会失败。

## 本地运行

前置要求：JDK 17、Maven 3.8+。

```powershell
$env:EZVIZ_APP_KEY = "你的AppKey"
$env:EZVIZ_APP_SECRET = "你的AppSecret"
mvn -B clean package
java -jar target/ezviz-camera-console-1.0.0.jar
```

启动后访问 `http://localhost:8080/index.html`。

## 云台控制

- 鼠标：按住方向按钮开始，松开或移出按钮即停止。
- 触摸：按下开始，抬起停止。
- 键盘：方向键控制方向，松开停止。长按不会重复发送请求。
- 速度：滑块范围 1–7。

## CI 构建（GitHub Actions）

工作流 `.github/workflows/azure-webapps-node.yml` 在推送到 `main` 或手动触发时运行：

1. 使用 Temurin JDK 17 构建：`mvn -B package`。
2. 将 `target/*.jar` 作为 `java-app` 产物上传，可在 Actions 运行页面下载。

当前工作流只构建，不部署。

## 部署到 Azure App Service（可选）

如需自动部署，先完成以下准备，再在工作流中恢复部署任务。

1. **创建 App Service**：在 Azure 门户创建 Web App，运行时选择 Java 17，记录应用名称。
2. **配置应用设置**：在 App Service 的环境变量中添加 `EZVIZ_APP_KEY`、`EZVIZ_APP_SECRET`，可选添加 `EZVIZ_BASE_URL`。
3. **获取发布配置文件**：在 App Service 概述页点击“获取发布配置文件”，复制 `.PublishSettings` 的全部内容。
4. **配置 GitHub**：在仓库 Settings → Secrets and variables → Actions 中新建 Secret `AZURE_WEBAPP_PUBLISH_PROFILE`，并把工作流中的 `AZURE_WEBAPP_NAME` 改为实际应用名。
5. **恢复部署任务**：在 `build` 之后添加 `deploy` 任务，用 `actions/download-artifact@v4` 下载 `java-app`，再用 `azure/webapps-deploy@v2` 部署，参数为 `app-name`、`publish-profile` 和 `package: '*.jar'`。
6. **验证**：访问 `https://<应用名>.azurewebsites.net/index.html`，检查 `/api/ezviz/devices` 是否返回数据。失败时在 App Service 的日志流中排查。

## 注意事项

- 请勿将 AppKey、AppSecret 或发布配置文件提交到仓库。
- 萤石云返回的错误码非 `200` 时，后端会抛出异常，请根据错误信息核对参数和凭据。
- 请确认萤石云 API 地址与接口路径和你的账号区域及官方文档一致。

## License

MIT
