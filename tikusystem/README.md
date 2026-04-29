# TIku System

基于 Vue 3 + Vite + Element Plus 构建的前端管理系统。

## 技术栈

- **Vue 3.5.25** - 渐进式 JavaScript 框架
- **Vite 7.3.1** - 下一代前端构建工具
- **Element Plus 2.13.3** - Vue 3 组件库
- **Vue Router 4.6.4** - 官方路由管理器
- **Pinia 3.0.4** - Vue 状态管理
- **Axios 1.13.6** - HTTP 客户端
- **ECharts 6.0.0** - 数据可视化图表库
- **Sass** - CSS 预处理器

## 项目结构

```
tikusystem/
├── src/
│   ├── api/          # API 接口
│   ├── assets/       # 静态资源
│   ├── components/   # 公共组件
│   ├── composables/  # 组合式函数
│   ├── router/       # 路由配置
│   ├── store/        # Pinia 状态管理
│   ├── styles/       # 全局样式
│   ├── utils/        # 工具函数
│   ├── views/        # 页面视图
│   ├── App.vue       # 根组件
│   └── main.js       # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 配置说明

### 开发服务器

- 端口：`3000`
- API 代理：`/api` → `http://localhost:8080`

### 路径别名

- `@` → `src/`

## 开发规范

- 使用 Vue 3 `<script setup>` 语法
- 组件命名采用 PascalCase
- 使用 Composition API 进行逻辑复用

## 许可证

MIT
