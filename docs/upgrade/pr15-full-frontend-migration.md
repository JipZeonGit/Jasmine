# PR15 新前端全量迁移记录

## 目标

这轮不再按 `PR15`、`PR16`、`PR17` 分批慢慢迁，而是一次性把新前端 `web/` 的底座、系统页和业务页全部迁完，形成可以替代旧 `admin/` 的新前端主线。

## 本轮内容

### 1. 新建 `web/`

当前已经新增：

- `web/`

并基于 `Vite + Vue 3 + TypeScript` 起好了新前端工程。

### 2. 新前端技术栈

当前 `web/` 使用：

- Vue 3
- Vite
- TypeScript
- Pinia
- Router 4
- Element Plus
- Axios
- Bun

### 3. 基础工程能力

当前已经补齐：

- `@` 别名
- `.env.development`
- `.env.production`
- 统一请求封装
- Token 本地存储
- Pinia 认证状态管理
- 动态菜单路由转换
- 新布局骨架
- 登录页
- 404 页面

### 4. 系统级页面迁移

当前已经迁移：

- 首页 / 仪表盘
- 登录页
- 个人信息
- 修改密码
- 用户管理
- 角色管理

### 5. 业务页面迁移

当前已经迁移：

- 花卉管理
- 库存管理
- 销售管理
- 会员管理
- 用户预约

### 6. 动态菜单与权限路由

当前新前端继续沿用后端返回 `menuList` 的动态菜单模式：

- 登录成功后获取 `/user/info`
- 读取 `menuList`
- 转换成 Router 4 可识别的动态路由
- 侧边栏根据菜单树渲染

### 7. Docker / CI 基线对齐

为了让新前端正式成为后续主线，本轮同步做了这些切换：

- 新增 `web/Dockerfile`
- 新增 `web/.dockerignore`
- 新增 `web/default.conf`
- GitHub Actions 镜像构建切换到 `web/`
- `ops/dev/docker-compose.yml` 的前端构建上下文切换到 `web/`

## 构建结果

当前已完成本地安装与构建验证：

```bash
bun install
bun run build
```

结果：

- 构建成功
- 新前端已能产出 `dist/`

当前还存在一个非阻塞提示：

- 部分 chunk 体积超过 500kB

这说明后续仍可继续做按路由拆包和更细的代码分块，但不影响当前迁移主链成立。

## 说明

### 1. 为什么这轮仍保留 `admin/`

当前保留 `admin/` 的原因不是继续以它为主，而是：

- 作为迁移对照
- 作为老页面逻辑参考
- 在新前端完全稳定前提供兜底

但新的迁移主线已经明确切到：

- `web/`

### 2. 当前边界

这轮重点是：

- 先把新前端完整迁出来
- 先保证能构建、能接接口、能承接旧页面

当前还没有继续深入做：

- OpenAPI 自动生成类型安全客户端
- 更完整的页面级拆包优化
- 更激进的 UI 重新设计

这些都可以在后续继续演进，但不阻塞本轮“新前端已经完整迁出”的目标。

## 当前结论

当前 `PR15` 已经不再是“只搭新前端脚手架”，而是：

- 新建了 `web/`
- 迁移了登录、布局、动态菜单、权限路由
- 迁移了系统管理页与业务页
- 把前端 Docker / CI / 本地构建链同步切到了 `web/`

这意味着后续前端主线已经可以正式围绕 `web/` 继续推进，而不必再回头从 `admin/` 慢慢拆页。

## 迁移中遇到的问题与修复

### 1. 新前端开发代理路径错误

现象：

- 新前端请求登录时，后端日志显示：
  - `uri=/prod-api/user/login`
- 返回 `403`

根因：

- `Vite` 代理没有像旧 `Nginx` 那样自动剥掉 `/prod-api`

修复：

- 在 `web/vite.config.ts` 中补：

```ts
rewrite: (path) => path.replace(/^\/prod-api/, '')
```

### 2. 新前端登录请求误带旧 token

现象：

- 登录接口仍然被拦
- 即使用户名密码正确也返回 `403`

根因：

- 前端请求拦截器会给 `/user/login` 自动附带旧 `Authorization`

修复：

- 在 `web/src/utils/request.ts` 中对这些接口跳过自动带 token：
  - `/user/login`
  - `/user/logout`
  - `/user/refreshToken`

### 3. 新前端本地开发端口 `5173` 未进入后端 CORS 白名单

现象：

- 新前端继续登录失败
- 后端日志显示：
  - `uri=/user/login status=403`

根因：

- 后端默认只放行了旧前端 `8888`
- 新前端 `http://localhost:5173` 和 `http://127.0.0.1:5173` 没进白名单

修复：

- 更新：
  - `src/main/resources/application.yml`
  - `src/main/java/com/nfu/jasmine/config/MyCorsConfig.java`
- 默认允许：
  - `http://localhost:5173`
  - `http://127.0.0.1:5173`

### 4. 新前端工程初始化后无法构建

现象：

- `web/` 初始脚手架无法直接构建
- 出现：
  - 别名无法识别
  - Element Plus 图标导出错误
  - `vue-tsc` / Axios 类型拦截报错

修复：

- 补齐：
  - `tsconfig.app.json`
  - `vite-env.d.ts`
  - `@` 路径别名
- 修正图标名
- 请求层改成迁移优先的宽松模式
- API 模块去掉当前阶段不必要的泛型约束

最终 `web/` 已可以：

```bash
bun x vue-tsc -b
bun run build
```
