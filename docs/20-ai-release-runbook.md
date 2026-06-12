# AI 线上发布 Runbook

本文档用于让新会话里的 AI 或人工操作者发布 Tuyoo 私有 SkillHub。目标是把发布变成可验证的非交互流程，避免依赖 GateShell TUI 手工选机器。

## 目标环境

- 源码目录：`/Users/zhaoxy/Mac/tuyoo/Shenzhen/skillhub`
- 镜像仓库：`tyhub.tuyoo.com/iflytek_skillhub`
- 线上主机：`sa201-cn-beijing-pt82-opsre-single-test-0004-172-16-1-9`
- 运行用户：`tywork`
- 运行目录：`/home/tywork/skillhub`
- 发布配置：`/home/tywork/skillhub/.env.release`
- Compose 文件：`/home/tywork/skillhub/compose.release.yml`
- 健康检查：`http://127.0.0.1:8080/actuator/health`

## 成功标准

- 本地测试或最小相关验证已通过。
- server 镜像已推送到 `tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:<tag>`。
- 线上主机名等于 `sa201-cn-beijing-pt82-opsre-single-test-0004-172-16-1-9`。
- 线上 `skillhub-server-1` 使用目标 server 镜像。
- 后端健康检查返回 `{"status":"UP"}`。
- web、scanner 镜像不因 server-only 发布被误更新。

## 首次安装服务器发布脚本

先把仓库里的脚本放到线上运行目录。推荐给 SSH 配置一个稳定别名，例如 `skillhub-prod`；如果暂时没有别名，也可以通过 GateShell 登录后手工复制。

```bash
scp scripts/deploy-skillhub-server-image.sh \
  skillhub-prod:/home/tywork/skillhub/deploy-skillhub-server-image.sh

ssh skillhub-prod \
  'chmod +x /home/tywork/skillhub/deploy-skillhub-server-image.sh'
```

脚本默认带主机名和用户保护，只允许在目标主机、`tywork` 用户下执行。需要改目标机器时，先显式设置 `SKILLHUB_EXPECT_HOST`，不要直接删保护。

## 构建并推送 server 镜像

只改后端时，不要重建 web/scanner。标签建议带业务含义和时间，便于回滚。

```bash
cd /Users/zhaoxy/Mac/tuyoo/Shenzhen/skillhub

TAG=hunterdock-$(date +%Y%m%d-%H%M)
IMAGE="tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:${TAG}"

./mvnw -pl skillhub-domain -am test

docker buildx build \
  --platform linux/amd64 \
  --push \
  -t "${IMAGE}" \
  -f server/Dockerfile \
  server
```

如果改动触达 app 层或接口层，把测试范围扩大到对应 Maven module；如果改动触达前端，再补前端构建或相关测试。

## 发布到线上

```bash
ssh skillhub-prod \
  "/home/tywork/skillhub/deploy-skillhub-server-image.sh ${IMAGE}"
```

脚本会自动：

- 校验主机名和运行用户。
- 备份 `.env.release`。
- 只替换 `SKILLHUB_SERVER_IMAGE`。
- 拉取目标 server 镜像。
- 校验 Compose 配置。
- 执行 `docker compose --env-file .env.release -f compose.release.yml up -d --no-build`。
- 检查 server 容器实际镜像。
- 调用后端健康检查。

## 手工 GateShell 路径

如果还没有稳定 SSH 别名，可以手工登录目标主机后执行：

```bash
hostname
sudo su - tywork
cd /home/tywork/skillhub
./deploy-skillhub-server-image.sh tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:<tag>
```

只有 `hostname` 输出等于 `sa201-cn-beijing-pt82-opsre-single-test-0004-172-16-1-9` 时才继续。

## 验证命令

```bash
cd /home/tywork/skillhub

docker compose --env-file .env.release -f compose.release.yml ps
curl -fsS http://127.0.0.1:8080/actuator/health
```

预期：

- `skillhub-server-1` 的 IMAGE 是刚发布的 `skillhub-server:<tag>`。
- health 返回 `{"status":"UP"}`。

## 回滚

优先用上一个已知可用的 server 镜像重新执行发布脚本：

```bash
./deploy-skillhub-server-image.sh tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:<previous-tag>
```

如果需要直接恢复环境文件：

```bash
cd /home/tywork/skillhub
cp .env.release.bak.<timestamp> .env.release
docker compose --env-file .env.release -f compose.release.yml up -d --no-build
curl -fsS http://127.0.0.1:8080/actuator/health
```

## 给新会话 AI 的提示词

```text
请在 /Users/zhaoxy/Mac/tuyoo/Shenzhen/skillhub 读取 docs/20-ai-release-runbook.md。
我要发布 SkillHub 线上 server 镜像：先跑最小相关测试，构建并推送
tyhub.tuyoo.com/iflytek_skillhub/skillhub-server:<tag>，然后通过 runbook 的
非交互脚本部署到线上并做健康检查。不要更新 web/scanner，除非我明确说要发布它们。
```
