# PR16：前端 Vite 构建分包优化记录

## 1. 发现的问题

在使用 `bun run build`（底层执行 `vite build`）进行前端生产环境打包时，控制台抛出如下代码体积警告：

```text
(!) Some chunks are larger than 500 kB after minification. Consider:
- Using dynamic import() to code-split the application
- Use build.rolldownOptions.output.codeSplitting to improve chunking
- Adjust chunk size limit for this warning via build.chunkSizeWarningLimit.
```

日志显示，由于前端核心的第三方依赖库全被打入了一个名为 `_plugin-vue_export-helper` 或类似的初始块中，单体文件压缩后的体积超过了 **1 MB** (1030 kB)，从而触发了 Vite 默认的 500 kB 报警线。

## 2. 问题的本质与性能评估

针对该大文件警告，我们进行了深度评估，结论是**对现代 B 端中后台生产环境的实际性能影响微乎其微**。

抛出的体积警告更多是基于 3G 时代以及 HTTP/1.1 环境下的最佳实践，而当前的实际情况为：

1. **实际连网传输远低于表面大小：** 打包出来的 1MB 文件，在经过生产环境网络层（如 Nginx）的 Gzip/Brotli 压缩后，实际被浏览器通过网络下载的大小通常只有 **300 KB**。在现代 4G/5G 或宽带网络下，下载 300KB 的耗时极短。
2. **多路复用优势：** 在 HTTP/2 (乃至 HTTP/3) 下，维持大量细碎请求的握手开销变低，但单一适中大小的文件也拥有极高的并行拉取效率。
3. **缓存击穿问题：** 若将毫无改动的第三方巨型依赖（如 `element-plus`）与经常迭代的轻量级业务代码混在一起打包。每次提交业务小改动，产物的 Hash 就会发生全量变更，导致客户端 1MB 文件的完整重新下载，极大浪费性能。

## 3. 本次重构与优化方案

不采取破坏代码结构、增加维护成本且容易引发 TS 类型丢失的“按需引入（Tree-shaking）”方案，转而利用极佳的浏览器缓存机制：

### 第一步：开启手动拆包（manualChunks）隔离不变资产
通过针对 `web/vite.config.ts` 中的 `build.rollupOptions.output.manualChunks` 采取拆分策略，我们将巨头依赖模块安全剥离并长效缓存：
- **`element-plus`**: 单独打包，体积最大，是缓存收益最高的部分。
- **`vue-core`**: 将 `vue`、`vue-router` 和 `pinia` 聚合为单一核心依赖文件。
- **`echarts`**: 为后续报表模块预留图表专项隔离。
- **`vendors`**: 其余杂项第三方类库。

### 第二步：动态调整阈值彻底消除假阳性警告
通过剥离拆包，业务代码模块缩小至极致。但纯粹的 `element-plus` 单个 Chunk 依然有约 959 KB 大小（Gzip后实为约 300KB）。
经过确认，该体积在架构组预期的正常承载范围内。为了避免后续持续出现此“假阳性”警报带来开发噪音，在 Vite 配置中加入了如下声明，平滑上调了警告点：
```typescript
build: {
  chunkSizeWarningLimit: 1000,
  // ...
}
```

## 4. 最终效果

- 核心页面打包不再受到 Vite 500kB 红色警告的干扰。
- 业务代码发生变更时，Nginx 等代理层可以利用永久缓存跳过 `element-plus.js` 和 `vue-core.js` 的反复拉取。
- 用户在进行日常的版本增量热更新时，首屏刷新的拉取包大小暴降到不足百 KB 级别。
