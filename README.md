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