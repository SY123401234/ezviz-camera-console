# 萤石云摄像头控制台

纯 HTML + 原生 JS 实现的海康威视摄像头 Web 控制台，通过萤石云开放平台 API 实现：

- 实时视频预览（萤石云 iframe 播放器）
- 设备列表管理
- 云台控制（方向 + 速度）
- 键盘方向键支持

## 架构

```
浏览器(index.html) → Spring Boot 后端 → 萤石云开放平台 API → 摄像头
```

## 使用

1. 启动后端服务（需要实现以下接口）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ezviz/token` | GET | 获取 AccessToken |
| `/api/ezviz/devices` | GET | 设备列表 |
| `/api/ezviz/live?deviceSerial=xxx` | GET | 直播地址 |
| `/api/ezviz/ptz?deviceSerial=xxx&action=left&speed=5` | POST | 云台控制 |
| `/api/ezviz/ptz/stop?deviceSerial=xxx` | POST | 停止云台 |

2. 将 `index.html` 放到后端的静态资源目录（如 Spring Boot 的 `src/main/resources/static/`）

3. 访问 `http://localhost:8080/index.html`

## 云台控制

- 界面按钮：鼠标点击或触摸
- 键盘：方向键控制方向，松开停止
- 速度：滑块调节 1-7

## 技术细节

- 纯前端，零依赖，单文件
- 视频播放使用萤石云在线播放器 iframe（`openstatic.ys7.com`）
- 所有数据通过 `/api/ezviz/*` 接口从后端获取

## License

MIT
