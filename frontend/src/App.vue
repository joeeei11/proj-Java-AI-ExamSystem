<template>
  <router-view />
</template>

<script setup lang="ts">
// 根组件：直接渲染路由视图
// 布局由各路由页面的 layout 组件控制
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  height: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #303133;
  background-color: #f5f7fa;
}

/* Element Plus 主题色覆盖 */
:root {
  --el-color-primary: #409eff;
  --el-border-radius-base: 6px;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
</style>
