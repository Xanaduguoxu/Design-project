<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  height: 100%;
}

/* 分页 未激活的按钮文字样式 */
.el-pager li {
  color: var(--page-btn-text-color) !important;
}

/* 分页 激活的按钮文字样式 */
.el-pager li.active {
  color: var(--page-btn-active-text-color) !important;
}

/* 下拉框文字（如“10条/页”） */
.el-select .el-input__inner {
  color: var(--page-text-color) !important;
}

/* 跳转区域文字（“前往”“页”） */
.el-pagination__jump {
  color: var(--page-text-color) !important;
}

/* 跳转输入框文字 */
.el-pagination__jump .el-input__inner {
  color: var(--page-text-color) !important;
}

/* 分页向前按钮 */
.btn-prev {
  background-color: var(--page-btn-bg-color) !important;
  color: var(--page-text-color) !important;
}

/* 分页向后按钮 */
.btn-next {
  background-color: var(--page-btn-bg-color) !important;
  color: var(--page-text-color) !important;
}

/* 分页下拉框字体颜色 */
.el-select-dropdown__item {
  color: var(--page-dropdown-text-color) !important;
}

/* 分页下拉框字体颜色悬停 */
.el-select-dropdown__item:hover {
  background-color: var(--page-dropdown-color-hover) !important;
  color: var(--page-dropdown-text-color-hover) !important;
}

/* 分页下拉框字体颜色选中 */
.el-select-dropdown__item.selected {
  font-weight: bold;
}

/* 分页下拉框背景颜色 */
.el-select-dropdown {
  background-color: var(--page-dropdown-bg-color) !important;
  border: 1px solid var(--page-dropdown-border-color) !important;
}

/* 修改空状态区域的背景色，使其与表格背景一致 */
.el-table__empty-block {
  background-color: var(--table-header-bg-color);
}

/* 修改空状态文字的颜色，使用你定义的文字颜色变量 */
.el-table__empty-text {
  color: var(--text-color) !important;
}

/* 表格整体样式 */
.el-card {
  background-color: var(--table-header-bg-color) !important;
}

.el-table {
  background-color: var(--table-header-bg-color) !important;
  color: var(--table-header-text-color) !important;
  border: 1px solid var(--table-border-color) !important;
}

/* 表头单元格样式 */
.el-table th.el-table__cell {
  background-color: var(--table-header-bg-color) !important;
  color: var(--table-header-text-color) !important;
  border-bottom: 1px solid var(--table-border-color) !important;
  border-right: 1px solid var(--table-border-color) !important;
}

.el-table td.el-table__cell {
  background-color: var(--table-header-bg-color) !important;
  border-bottom: 1px solid var(--table-border-color) !important;
  border-right: 1px solid var(--table-border-color) !important;
}

/* 修复边框重叠问题，使表格边框更整洁 */
/* .el-table--border::after,
.el-table--group::after,
.el-table::before {
  background-color: var(--table-border-color);
} */

/* 输入框样式 */
.el-input .el-input__inner {
  background-color: var(--input-bg-color) !important;
  color: var(--input-text-color);
}

/* 修改所有 el-input 的 placeholder 颜色 */
.el-input .el-input__inner::placeholder {
  color: var(--input-placeholder-color);
}

/* 修改所有 el-textarea 的 placeholder 颜色 */
.el-textarea .el-textarea__inner::placeholder {
  color: var(--input-placeholder-color);
}

/* .el-menu--inline{
  background-color: #0f172a !important;
} */

/* .el-menu--collapse .el-submenu .el-menu {
  display: none !important;
}

.el-menu--popup {
  display: none !important;
} */
</style>
