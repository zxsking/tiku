<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <router-link to="/" class="logo">
          <el-icon><Collection /></el-icon>
          <span>智能题库系统</span>
        </router-link>
      </div>
      
      <div class="header-center" v-if="!isSearchPage">
        <div class="search-trigger" @click="goToSearch">
          <el-icon class="search-icon"><Search /></el-icon>
          <span class="search-placeholder">搜索题库或题目...</span>
        </div>
      </div>
      
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand" :hide-on-click="true">
            <span class="user-info" tabindex="-1">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="myBanks">
                  <el-icon><Folder /></el-icon>我的题库
                </el-dropdown-item>
                <el-dropdown-item command="myQuestions">
                  <el-icon><Document /></el-icon>我的题目
                </el-dropdown-item>
                <el-dropdown-item command="favorites">
                  <el-icon><Star /></el-icon>我的收藏
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" divided command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    
    <el-container style="overflow: hidden;">
      <el-aside width="180px" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/banks">
            <el-icon><Collection /></el-icon>
            <span>题库列表</span>
          </el-menu-item>
          <el-menu-item index="/questions">
            <el-icon><List /></el-icon>
            <span>题目广场</span>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><Menu /></el-icon>
            <span>分类浏览</span>
          </el-menu-item>
          
          <el-divider v-if="userStore.isLoggedIn" />
          
          <el-menu-item v-if="userStore.isLoggedIn" index="/user/banks">
            <el-icon><Folder /></el-icon>
            <span>我的题库</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/user/questions">
            <el-icon><Document /></el-icon>
            <span>我的题目</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/user/ai-import">
            <el-icon><MagicStick /></el-icon>
            <span>AI 导入题目</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/user/favorites">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/user/practice/start">
            <el-icon><VideoPlay /></el-icon>
            <span>练习中心</span>
          </el-menu-item>
          <el-sub-menu v-if="userStore.isLoggedIn" index="/user/wrong-book">
            <template #title>
              <el-icon><WarningFilled /></el-icon>
              <span>错题本</span>
            </template>
            <el-menu-item index="/user/wrong-book">
              <el-icon><List /></el-icon>
              <span>错题列表</span>
            </el-menu-item>
            <el-menu-item index="/user/wrong-book/review">
              <el-icon><Refresh /></el-icon>
              <span>今日复习</span>
            </el-menu-item>
            <el-menu-item index="/user/wrong-book/stats">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计面板</span>
            </el-menu-item>
          </el-sub-menu>

          <el-divider v-if="userStore.isAdmin" />
          
          <el-menu-item v-if="userStore.isAdmin" index="/admin/review">
            <el-icon><Finished /></el-icon>
            <span>审核中心</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/categories">
            <el-icon><Menu /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" :key="$route.path" />
          </Transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isSearchPage = computed(() => route.name === 'Search')

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/user/practice')) return '/user/practice/start'
  if (path.startsWith('/user/wrong-book/review')) return '/user/wrong-book/review'
  if (path.startsWith('/user/wrong-book/stats')) return '/user/wrong-book/stats'
  if (path.startsWith('/user/wrong-book')) return '/user/wrong-book'
  return path
})

function goToSearch() {
  router.push({ name: 'Search' })
}

function handleCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'myBanks':
      router.push('/user/banks')
      break
    case 'myQuestions':
      router.push('/user/questions')
      break
    case 'favorites':
      router.push('/user/favorites')
      break
    case 'admin':
      router.push('/admin/review')
      break
    case 'logout':
      userStore.logout()
      router.push('/')
      break
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.header-left {
  .logo {
    display: flex;
    align-items: center;
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
    
    .el-icon {
      font-size: 24px;
      margin-right: 8px;
    }
  }
}

.header-center {
  flex: 1;
  max-width: 500px;
  margin: 0 40px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f5f7fa;
  cursor: text;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: #c0c4cc;
    background: #fff;
  }

  .search-icon {
    color: #909399;
    font-size: 14px;
    flex-shrink: 0;
  }

  .search-placeholder {
    color: #a8abb2;
    font-size: 14px;
    user-select: none;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;

  &:focus,
  &:focus-visible {
    outline: none;
  }

  .username {
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
  flex-shrink: 0;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #e4e7ed;
    border-radius: 2px;
  }
}

.sidebar-menu {
  border-right: none;
  height: 100%;

  // 缩小菜单项高度和内边距
  :deep(.el-menu-item) {
    height: 57px;
    
    line-height: 48px;
    padding: 0 18px !important;
    font-size: 14px;
  }

  // 缩小分割线上下间距
  :deep(.el-divider) {
    margin: 6px 0;
  }
}

.main-content {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: scroll;
  height: calc(100vh - 60px);
  scrollbar-gutter: stable;
}

// 页面切换过渡动画
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>