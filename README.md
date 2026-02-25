# toplinks

[![CI](https://github.com/Yuz-Studio/toplinks/actions/workflows/ci.yml/badge.svg)](https://github.com/Yuz-Studio/toplinks/actions/workflows/ci.yml)

A Spring Boot application for file upload and display.

## Google OAuth2 登录配置

使用 Google 登录前，需要在 Google Cloud Console 完成以下设置：

### 1. 创建 Google Cloud 项目

1. 打开 [Google Cloud Console](https://console.cloud.google.com/)
2. 点击顶部导航栏的项目选择器，然后点击 **新建项目**
3. 输入项目名称（例如 `toplinks`），点击 **创建**

### 2. 启用 API

1. 在左侧菜单中选择 **API 和服务** → **库**
2. 搜索并启用 **Google+ API**（或 **Google People API**）

### 3. 配置 OAuth 同意屏幕

1. 进入 **API 和服务** → **OAuth 同意屏幕**
2. 选择用户类型：
   - **外部**（External）— 所有 Google 账号都可以登录
   - **内部**（Internal）— 仅限本组织的 Google Workspace 用户
3. 填写必要信息：
   - **应用名称**：`TopLinks`
   - **用户支持邮箱**：填写你的邮箱
   - **开发者联系信息**：填写你的邮箱
4. 在 **范围（Scopes）** 页面，添加以下范围：
   - `openid`
   - `email`
   - `profile`
5. 如果选择了"外部"类型，在 **测试用户** 页面添加需要测试登录的 Google 邮箱
6. 点击 **保存并继续** 完成配置

### 4. 创建 OAuth 2.0 客户端凭据

1. 进入 **API 和服务** → **凭据**
2. 点击 **+ 创建凭据** → **OAuth 客户端 ID**
3. 应用类型选择 **Web 应用**
4. 填写名称（例如 `toplinks-web`）
5. 配置 **已获授权的重定向 URI**：
   - 本地开发：`http://localhost:8080/login/oauth2/code/google`
   - 生产环境：`https://你的域名/login/oauth2/code/google`
6. 点击 **创建**，记录生成的 **客户端 ID** 和 **客户端密钥**

### 5. 配置环境变量

将上一步获取的客户端 ID 和密钥设置为环境变量：

```bash
export GOOGLE_CLIENT_ID=你的客户端ID
export GOOGLE_CLIENT_SECRET=你的客户端密钥
```

应用会通过 `application.properties` 中的以下配置自动读取：

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:disabled}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:disabled}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

> **注意**：如果不设置环境变量，默认值为 `disabled`，Google 登录按钮仍然会显示，但点击后会报错。

---

## Cloudflare R2 存储配置

项目使用 Cloudflare R2 作为文件存储后端。如果未配置 R2 凭证，系统会自动降级到本地文件存储。

### 1. 注册 Cloudflare 账号

1. 打开 [Cloudflare Dashboard](https://dash.cloudflare.com/sign-up)
2. 使用邮箱注册并完成验证
3. 登录后进入 Dashboard 首页

### 2. 获取 Account ID

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. 在页面右侧或 URL 中找到你的 **Account ID**（格式为 32 位十六进制字符串）
3. 也可以进入任意域名的 **概述** 页面，在右侧栏找到 **Account ID**
4. 记录此 ID，后续配置需要使用

### 3. 开通 R2 存储服务

1. 在 Dashboard 左侧菜单中选择 **R2 对象存储**
2. 首次使用需要同意服务条款并完成开通
3. R2 提供每月 10 GB 的免费存储额度

### 4. 创建存储桶（Bucket）

1. 进入 **R2 对象存储** 页面，点击 **创建存储桶**
2. 输入存储桶名称（例如 `toplinks`）
3. 选择存储桶位置（建议选择离用户最近的区域）
4. 点击 **创建存储桶** 完成创建

### 5. 生成 R2 API 令牌（Access Key 和 Secret Key）

1. 进入 **R2 对象存储** 页面，点击右上角 **管理 R2 API 令牌**
2. 点击 **创建 API 令牌**
3. 填写令牌名称（例如 `toplinks-r2`）
4. 权限选择 **对象读和写**
5. 可以选择将令牌限制到特定存储桶（例如只允许访问 `toplinks` 桶）
6. 点击 **创建 API 令牌**
7. 创建成功后会显示：
   - **Access Key ID** — 对应环境变量 `CLOUDFLARE_R2_ACCESS_KEY`
   - **Secret Access Key** — 对应环境变量 `CLOUDFLARE_R2_SECRET_KEY`

> **⚠️ 重要**：Secret Access Key 只会显示一次，请立即复制保存。如果丢失需要重新创建令牌。

### 6. 配置公开访问（可选）

如果需要通过 URL 直接访问上传的文件：

1. 进入对应的存储桶设置页面
2. 在 **公开访问** 部分，启用 **R2.dev 子域** 或绑定自定义域名
3. 启用后会获得一个公开访问 URL，例如：
   - R2.dev 子域：`https://pub-xxxxxxxxxxxx.r2.dev`
   - 自定义域名：`https://files.yourdomain.com`
4. 记录此 URL，用于配置 `CLOUDFLARE_R2_PUBLIC_URL`

### 7. 配置环境变量

将上述获取的信息设置为环境变量：

```bash
export CLOUDFLARE_ACCOUNT_ID=你的AccountID
export CLOUDFLARE_R2_ACCESS_KEY=你的AccessKeyID
export CLOUDFLARE_R2_SECRET_KEY=你的SecretAccessKey
export CLOUDFLARE_R2_BUCKET=toplinks
export CLOUDFLARE_R2_PUBLIC_URL=https://你的公开访问域名
```

应用会通过 `application.properties` 中的以下配置自动读取：

```properties
cloudflare.r2.account-id=${CLOUDFLARE_ACCOUNT_ID:}
cloudflare.r2.access-key=${CLOUDFLARE_R2_ACCESS_KEY:}
cloudflare.r2.secret-key=${CLOUDFLARE_R2_SECRET_KEY:}
cloudflare.r2.bucket=${CLOUDFLARE_R2_BUCKET:toplinks}
cloudflare.r2.public-url=${CLOUDFLARE_R2_PUBLIC_URL:}
```

> **注意**：如果不配置 R2 相关环境变量，应用会自动降级为本地文件存储，上传的文件将保存在服务器的 `uploads` 目录中。

---

## Running Tests Locally

Make sure you have a MySQL instance running with a database named `toplinks_test`, then:

```bash
mvn clean verify -Dspring.profiles.active=test
```

Or with a custom DB username/password:

```bash
DB_USERNAME=myuser DB_PASSWORD=mypass mvn clean verify -Dspring.profiles.active=test
```

## CI

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs automatically on every push and pull request targeting `main`. It:

- Starts a MySQL 8.0 service container with the `toplinks_test` database
- Sets up Java 17 (Temurin) with Maven dependency caching
- Runs `mvn -B -ntp clean verify -Dspring.profiles.active=test`