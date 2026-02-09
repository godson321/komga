# ![app icon](./.github/readme-images/app-icon.png) Komga

Komga 是一个用于管理漫画、manga、BD、杂志和电子书的媒体服务器。

## 功能特性

- 通过响应式 Web UI 浏览书库、系列和书籍，支持桌面端、平板和手机
- 使用合集和阅读列表组织你的书库
- 编辑系列和书籍的元数据
- 自动导入内嵌元数据
- 支持多种阅读模式的 Web 阅读器
- 多用户管理，支持按书库的访问控制、年龄限制和标签限制
- 提供 REST API，社区中有许多工具和脚本可与 Komga 交互
- 支持 OPDS v1 和 v2
- Kobo 电子阅读器同步
- KOReader 同步
- 下载单本书籍、整个系列或阅读列表
- 重复文件检测
- 重复页面检测与移除
- 从书库外部直接导入书籍到系列文件夹
- 导入 ComicRack `cbl` 阅读列表

## 项目结构

Komga 由 3 个子项目组成：

- `komga`：Spring Boot 后端服务，提供 API，同时也托管前端静态资源
- `komga-webui`：VueJS 前端，编译时构建，运行时由后端提供服务
- `komga-tray`：轻量桌面托盘图标包装器

## 环境要求

- Java JDK 21+
- Node.js 18+（参考 `.nvmrc` 文件）

## 快速启动

### 后端启动

后端使用 Gradle 构建和运行。在项目根目录执行：

```bash

# 使用本地持久化数据库启动
./gradlew bootRun --args="--spring.profiles.active=dev,localdb,noclaim"
```

Windows 下也可以通过环境变量设置 Profile：

```powershell
$env:SPRING_PROFILES_ACTIVE="dev,noclaim"
./gradlew bootRun
```

后端默认运行在 `http://localhost:25600`。

### 前端启动

```bash
# 安装依赖（首次或依赖变更时执行）
npm install --prefix komga-webui

# 启动开发服务器
npm run serve --prefix komga-webui
```

前端开发服务器运行在 `http://localhost:8081`，会自动代理 API 请求到后端 `localhost:25600`。

### 构建前端并由后端托管

```bash
./gradlew prepareThymeLeaf
```

此命令会构建前端并将产物复制到后端的 `/resources/public` 目录，随后可直接通过后端访问完整应用。

## 注意事项

1. **前后端联调时必须使用 `dev` Profile**：后端需要启用 `dev` Profile 才会允许来自 `localhost:8081` 的 CORS 请求，否则前端请求会被拒绝。
2. **`dev` Profile 的特殊行为**：启用 `dev` 后，会增加日志输出、禁用定时扫描、使用内存数据库。
3. **`noclaim` Profile 会自动创建用户**：
   - 在 `dev` 模式下，会创建 `admin@example.org`（密码：`admin`）和 `user@example.org`（密码：`user`）
   - 在非 `dev` 模式下，会创建 `admin@example.org` 并生成随机密码（查看日志获取）
4. **持久化数据**：如需保留数据库数据，请加上 `localdb` Profile，数据库文件会存储在 `./localdb` 目录。
5. **提交规范**：提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。
6. **运行测试**：提交前请务必运行 `./gradlew test` 确保测试通过。

## 中文乱码解决

在 Windows + VSCode + Gradle + Spring Boot 环境下可能出现中文乱码问题，可通过以下配置解决：

### 1. Gradle 编码配置

修改项目根目录下的 `gradlew.bat` 文件，在第39行添加 `-Dfile.encoding=UTF-8`：

```bat
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m" "-Dfile.encoding=UTF-8"
```

### 2. Spring Boot 编码配置

在 `komga/src/main/resources/application-dev.yml` 中添加：

```yaml
server:
  port: 8080
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
  tomcat:
    uri-encoding: UTF-8
  banner:
    charset: UTF-8

spring:
  banner:
    charset: UTF-8
  file:
    charset: UTF-8

logging:
  charset:
    console: UTF-8
    file: UTF-8
```

### 3. VSCode 终端编码配置

在 VSCode 设置（settings.json）中添加终端编码配置：

```json
"terminal.integrated.profiles.windows": {
    "PowerShell": {
        "source": "PowerShell",
        "icon": "terminal-powershell",
        "args": ["-NoExit", "/c", "chcp 65001"]
    },
    "Command Prompt": {
        "path": [
            "${env:windir}\\Sysnative\\cmd.exe",
            "${env:windir}\\System32\\cmd.exe"
        ],
        "args": ["-NoExit", "/c", "chcp 65001"],
        "icon": "terminal-cmd"
    }
}
```


### 5. Windows 系统编码（终极方案）

如上述方法无效，可设置操作系统语言编码：

1. 打开 Windows "语言设置"
2. 点击 "管理语言设置"
3. 在 "管理语言设置" 界面中点击 "更改系统区域设置"
4. 勾选 "Beta版：使用Unicode UTF-8提供全球语言支持"
5. 点击确定，重启操作系统

> 参考：[VSCode+SpringBoot+Gradle运行命令窗口中文乱码解决](https://blog.csdn.net/duihao/article/details/134226861)

## Docker 构建

```bash
# 1. 构建前端并复制到后端资源目录
./gradlew prepareThymeLeaf

# 2. 通过 JReleaser 准备 Docker 镜像
./gradlew jreleaserPackage

# Dockerfile 位于 komga/build/jreleaser/package/docker/
```

## 安装部署

详见[官方文档](https://komga.org/docs/category/installation)。

## 更多文档

访问 [Komga 官方网站](https://komga.org) 获取更多信息。

## 翻译

[![Translation status](https://hosted.weblate.org/widgets/komga/-/webui/horizontal-auto.svg)](https://hosted.weblate.org/engage/komga/)

## 致谢

Komga 图标基于 [Freepik](https://www.freepik.com/home) 在 www.flaticon.com 上制作的图标。










### 4. 启动命令

启动前请先设置控制台编码为 UTF-8：

**后端启动（CMD）：**
```cmd
chcp 65001 && .\gradlew.bat bootRun --no-daemon --args="--spring.profiles.active=dev,noclaim,localdb"
```

**前端启动（CMD）：**
```cmd
chcp 65001 && npm install --prefix komga-webui && npm run serve --prefix komga-webui
```




**后端启动（PowerShell）：**
```powershell
chcp 65001; .\gradlew.bat bootRun --no-daemon --args="--spring.profiles.active=dev,localdb"
chcp 65001; $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User"); .\gradlew.bat bootRun --no-daemon --args="--spring.profiles.active=dev,localdb"
```

**前端启动（PowerShell）：**
```powershell
chcp 65001; npm install --prefix komga-webui; npm run serve --prefix komga-webui
```


> **注意**：后端必须使用 `dev` profile 才能与前端（端口8081）进行 CORS 通信，且端口为 25600。