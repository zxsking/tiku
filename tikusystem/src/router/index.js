import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/store/user'

NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/public/Home.vue') },
      { path: 'banks', name: 'BankList', component: () => import('@/views/public/BankList.vue') },
      { path: 'banks/:id', name: 'BankDetail', component: () => import('@/views/public/BankDetail.vue') },
      { path: 'questions', name: 'QuestionList', component: () => import('@/views/public/QuestionList.vue') },
      { path: 'questions/:id', name: 'QuestionDetail', component: () => import('@/views/public/QuestionDetail.vue') },
      { path: 'users/:id', name: 'UserProfile', component: () => import('@/views/public/UserProfile.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/public/Search.vue') },
      { path: 'categories', name: 'Categories', component: () => import('@/views/public/Categories.vue') },
      {
        path: 'user',
        meta: { requiresAuth: true },
        children: [
          { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue') },
          { path: 'banks', name: 'MyBanks', component: () => import('@/views/user/MyBanks.vue') },
          { path: 'questions', name: 'MyQuestions', component: () => import('@/views/user/MyQuestions.vue') },
          { path: 'favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue') },
          { path: 'following', name: 'Following', component: () => import('@/views/user/Following.vue') },
          { path: 'banks/create', name: 'BankCreate', component: () => import('@/views/user/BankEdit.vue') },
          { path: 'banks/:id/edit', name: 'BankEdit', component: () => import('@/views/user/BankEdit.vue') },
          { path: 'questions/create', name: 'QuestionCreate', component: () => import('@/views/user/QuestionEdit.vue') },
          { path: 'questions/:id/edit', name: 'QuestionEdit', component: () => import('@/views/user/QuestionEdit.vue') },
          { path: 'ai-import', name: 'AiImport', component: () => import('@/views/user/AiImport.vue') },
          { path: 'practice/start/:bankId?', name: 'PracticeStart', component: () => import('@/views/user/PracticeStart.vue') },
          { path: 'practice/exam/:sessionId', name: 'PracticeExam', component: () => import('@/views/user/PracticeExam.vue') },
          { path: 'practice/result/:sessionId', name: 'PracticeResult', component: () => import('@/views/user/PracticeResult.vue') },
          { path: 'practice/history', name: 'PracticeHistory', component: () => import('@/views/user/PracticeHistory.vue') },
          { path: 'wrong-book', name: 'WrongBook', component: () => import('@/views/user/WrongBook.vue') },
          { path: 'wrong-book/detail/:id', name: 'WrongBookDetail', component: () => import('@/views/user/WrongBookDetail.vue') },
          { path: 'wrong-book/review', name: 'WrongBookReview', component: () => import('@/views/user/WrongBookReview.vue') },
          { path: 'wrong-book/stats', name: 'WrongBookStats', component: () => import('@/views/user/WrongBookStats.vue') }
        ]
      },
      {
        path: 'admin',
        meta: { requiresAuth: true, requiresAdmin: true },
        children: [
          { path: 'review', name: 'ReviewCenter', component: () => import('@/views/admin/ReviewCenter.vue') },
          { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
          { path: 'categories', name: 'CategoryManage', component: () => import('@/views/admin/CategoryManage.vue') }
        ]
      }
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/public/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/public/Register.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/public/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()

  // 等待用户信息初始化完成（有 token 时会静默拉取最新 userInfo），再做权限判断
  await userStore.ready()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router