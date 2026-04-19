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
- `ops/prod/docker-compose.yml` 的前端镜像运行基线继续承接 `web/`
- 当前前端部署与联调主入口明确收口到 `ops/dev` 与 `ops/prod`
- Docker 镜像发布改为先等待“后端基础检查”通过，再执行镜像打包与推送

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

### 1. 旧前端 `admin/` 的最终去留

在迁移初稿阶段，`admin/` 一度被保留为对照与兜底。

但在当前分支继续完成以下收口后：

- 新前端页面已全部补齐
- 新前端 Docker / CI 已切换到 `web/`
- 登录、菜单、业务页、部署链都已在新前端上验证

当前已经明确：

- `web/` 是唯一前端主线
- `admin/` 已从仓库中删除，不再保留

### 2. 当前边界

这轮重点是：

- 先把新前端完整迁出来
- 先保证能构建、能接接口、能承接旧页面

当前还没有继续深入做：

- OpenAPI 自动生成类型安全客户端
- 更完整的页面级拆包优化
- 更激进的 UI 重新设计

这些都可以在后续继续演进，但不阻塞本轮“新前端已经完整迁出”的目标。

### 3. UI 美化与全量汉化修正

在此阶段额外完成了前端界面的深度美化和部分组件未汉化的修正：

- **深度汉化**：通过引入 `ElConfigProvider` 配置给 Element Plus 的分页、日期时间选择器等内置组件应用了 `zh-cn` 中文语言包，彻底解决了全英文问题。
- **重返极简 (Semi Design 风格)**：彻底废弃了初期由于沟通偏差导致的带有过渡夸张的暖色渐变（粉/橙）的护眼暗深色。新的 UI 秉承最高效的 B 端思维，以无色系的深灰/银白为主，蓝色（Tech Blue）为唯一的重点点缀色。抛弃沉重阴影，将 `el-card` 改为干净无边的面板框体系。
- **明暗切换支持与纠正**：优化并严格梳理了 `.dark` 模式，所有弹窗与表格表头背景皆以深空灰进行精准重写补全，并在系统右上角保留太阳/月亮图标一键无缝切换模式（localStorage 记录）。
- **极致的屏幕利用空间**：鉴于部分业务如 `InventoryManageView` 长期遇到“横向宽度不够塞下表单”的问题，全局强制压缩表单控件 `margin/padding` 设置，搜索区域缩小化处理，表格采用 `small` 尺寸和 `13px` 专业阅读字号，让视野极为专业广阔。
- **登录页面重制**：登录入口化作最沉稳的极素卡片面板，去除廉价特效干扰。

### 4. 交互体验深层优化与 Bug 修复

本轮除了迁移页面外，彻底重塑并增强了前端的交互体验：

- **新增 TagsView（多页签栏）**：在头部面包屑下方补齐了系统级的访问页签栏追踪功能。基于 Vue Router 与 Pinia，自动追踪页面浏览历史。对“首页（Dashboard）”进行了硬编码防关闭保护并永远置顶于最左侧，完全还原并超越了以往老一代 Vue 2 的后台交互体验。
- **全局分页对齐重构**：统一在 `index.css` 提供包裹类 `.pagination-container`，全端剥除并整理了硬编码的内联右侧对齐代码，实现了整套系统分页组件在左下方清一色对齐的视觉一致性。
- **左侧栏智能记忆闭合状态**：通过劫持 Element Plus `el-menu` 原生暴露的 `@open` 与 `@close` 事件及外部状态树 `openedPaths`，彻底根治了在折叠栏开启与缩回时内部子菜单全部清空的问题，保证菜单在交互中的逻辑连续感。
- **首页 Dashboard 数据高密度再构**：丢弃了旧版寒酸的纯文字占位页面，重新切图编排了四张附带轻微悬浮动画的高端响应式统计卡片展示营业额、成本等核心业务数据。
- **修复安全退出注销链路故障**：排查并修正了由于 `/user/logout` 不小心被加入“免令牌调用白名单（`AUTH_FREE_ENDPOINTS`）”从而导致触发退出行为后引发后端 401 和 20003 拦截，进一步导致前端 Promise 中断、卡死无法重定向的问题，增加了 `try/catch` 强退机制确保安全兜底。

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

### 5. Refresh Token 链路缺陷

现象：

- 新前端虽然拿到了后端返回的 `refreshToken`
- 但本地没有真正持久化，也没有在访问令牌失效时尝试刷新
- 一旦登录态过期，只会直接清掉访问令牌，容易留下 Pinia 状态和浏览器实际登录页跳转不同步的问题

根因：

- `web/src/utils/auth.ts` 只管理了 access token
- `web/src/stores/auth.ts` 登录后没有保存 refresh token
- `web/src/utils/request.ts` 没有补 `/user/refresh` 刷新链，也没有把刷新接口纳入免鉴权白名单

修复：

- 为前端补齐 refresh token 的读写与删除
- 登录成功后同时保存 access token 和 refresh token
- 请求拦截器补 `/user/refresh`
- 响应拦截器在令牌失效时先尝试刷新，再重放原请求
- 刷新失败后统一强制回到登录页，避免页面状态残留

### 6. Docker 镜像发布绕过集成验证

现象：

- 之前 Docker 镜像工作流会直接在 `main` / `next` 推送时执行
- 即使后端基础检查还没通过，镜像仍可能被发布

根因：

- `.github/workflows/docker-publish.yml` 和 `.github/workflows/backend-ci.yml` 是并行关系
- 镜像发布没有依赖“后端基础检查”成功

修复：

- 镜像发布工作流改为监听“后端基础检查”完成事件
- 只有检查结论为 `success` 时，才允许自动打包并推送镜像
- 同时保留 `workflow_dispatch` 作为人工兜底入口


