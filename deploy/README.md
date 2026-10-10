# Linux 服务器部署指引

以下以 Ubuntu / Debian（x86_64）+ systemd 为例。

## 1. 准备环境

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless
java -version
```

## 2. 构建 jar

在本地或 CI 中执行：

```bash
mvn -B clean package
```

产物位于 `target/ezviz-camera-console-1.0.0.jar`，也可从 GitHub Actions 的 `java-app` 产物下载。

## 3. 上传文件

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin ezviz
sudo mkdir -p /opt/ezviz-camera-console /etc/ezviz-camera-console
sudo cp ezviz-camera-console-1.0.0.jar /opt/ezviz-camera-console/ezviz-camera-console.jar
sudo cp deploy/ezviz-camera-console.service /etc/systemd/system/
sudo cp deploy/ezviz.env.example /etc/ezviz-camera-console/ezviz.env
sudo chown -R ezviz:ezviz /opt/ezviz-camera-console
sudo chmod 600 /etc/ezviz-camera-console/ezviz.env
sudo chown ezviz:ezviz /etc/ezviz-camera-console/ezviz.env
```

编辑 `/etc/ezviz-camera-console/ezviz.env`，填入真实的 `EZVIZ_APP_KEY` 和 `EZVIZ_APP_SECRET`。

## 4. 启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ezviz-camera-console
sudo systemctl status ezviz-camera-console
```

查看日志：

```bash
sudo journalctl -u ezviz-camera-console -f
```

验证：

```bash
curl http://127.0.0.1:8080/api/ezviz/token
curl http://127.0.0.1:8080/api/ezviz/devices
```

## 5. 配置反向代理与 HTTPS（可选）

### 方式 A：使用 1Panel（推荐，服务器已安装 1Panel 时）

1. 在 1Panel 左侧进入 **网站 → 创建网站 → 反向代理**。
2. 域名填写你的域名，代理地址填写 `http://127.0.0.1:8080`。
3. 在该网站的 **HTTPS** 页签中申请 Let's Encrypt 证书，并开启强制 HTTPS。
4. 在 1Panel 的 **防火墙** 中仅开放 80/443，不要开放 8080。
5. 访问 `https://你的域名/index.html` 验证。

1Panel 自带 OpenResty，无需手动安装 Nginx。systemd 服务仍由命令行管理，与 1Panel 不冲突。

### 方式 B：手动配置 Nginx

```bash
sudo apt install -y nginx
sudo cp deploy/nginx-ezviz-camera-console.conf /etc/nginx/sites-available/ezviz-camera-console
sudo ln -s /etc/nginx/sites-available/ezviz-camera-console /etc/nginx/sites-enabled/
sudo sed -i 's/your.domain.com/你的域名/' /etc/nginx/sites-available/ezviz-camera-console
sudo nginx -t && sudo systemctl reload nginx
```

HTTPS 可使用 Certbot：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 你的域名
```

## 6. 更新版本

重新上传 jar 后执行：

```bash
sudo systemctl restart ezviz-camera-console
```

## 7. 防火墙

若未使用 Nginx，需开放 8080 端口；使用 Nginx 时开放 80/443 即可，不要将 8080 暴露到公网。

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

## 8. GitHub Actions 自动部署（可选）

`deploy/github-actions-ssh-deploy.example.yml` 提供了构建后通过 SSH 上传 jar 并重启服务的示例。

1. 在服务器上为部署用户配置 SSH 公钥，并允许该用户免密执行 `systemctl restart ezviz-camera-console` 及 `mv`、`chown` 命令（通过 `sudo`）。
2. 在仓库 Settings → Secrets and variables → Actions 中添加 `SERVER_HOST`、`SERVER_USER`、`SERVER_SSH_KEY`，可选添加 `SERVER_PORT`。
3. 将示例文件复制到 `.github/workflows/ezviz-deploy.yml`，然后在 Actions 页面手动运行。

该示例默认只通过 `workflow_dispatch` 手动触发，确认无误后可改为 `push` 触发。

## 注意事项

- 凭据只保存在服务器的
- 访问页面：`http://服务器地址/index.html`（或配置域名后的地址）。
- 萤石云 API 地址需与账号所在区域一致。
