// Prevent h5App.js from using UMD wrapper that overwrites global properties
// (e.g., window.com, window.callKotlinMethod) set by nativevue2.js.
// h5App.js is an executable entry, not a library, so it doesn't need to export anything.
//
// 背景（官方 Kuikly h5App 模板同款修复，见 KuiklyUI/h5App/webpack.config.d/output.js）：
// 业务 nativevue2.js（shared）与壳 h5App.js 是两个独立 webpack UMD 产物，各自把顶层
// `com` 分支逐 key 覆盖挂到 window。h5App.js 后加载会整体覆盖 window.com，抹掉业务先挂的
// `window.com.tencent.kuikly.core.nvi`（registerCallNative / callNative），导致白屏。
// 方案 X：h5App.js 只作可执行入口，不需要对外导出 —— 改用 IIFE，不触碰 window 全局。
config.output = config.output || {};
config.output.libraryTarget = undefined;
config.output.library = undefined;
config.output.iife = true;
