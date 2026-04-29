# API 模块说明

本目录包含前端与后端交互的所有API接口定义，按照功能模块进行组织。

## 模块划分

### auth.js - 认证模块
- `login(data)` - 用户登录
- `register(data)` - 用户注册
- `getUserInfo()` - 获取用户信息
- `updateUserInfo(data)` - 更新用户信息
- `changePassword(data)` - 修改密码

### bank.js - 题库模块
- `getBanks(params)` - 获取题库列表
- `getBank(id)` - 获取单个题库详情
- `createBank(data)` - 创建题库
- `updateBank(id, data)` - 更新题库
- `deleteBank(id)` - 删除题库
- `getMyBanks(params)` - 获取我的题库
- `toggleBankFavorite(id)` - 切换题库收藏状态
- `getBankQuestions(id, params)` - 获取题库下的题目
- `getBankFavorites(params)` - 获取收藏的题库
- `toggleBankLike(id)` - 切换题库点赞状态
- `getBankStats(id)` - 获取题库统计数据
- `getPendingBanks(params)` - 获取待审核题库（管理员）
- `approveBank(id, data)` - 审核通过题库（管理员）
- `rejectBank(id, data)` - 审核拒绝题库（管理员）

### question.js - 题目模块
- `getQuestions(params)` - 获取题目列表
- `getQuestion(id)` - 获取单个题目详情
- `createQuestion(data)` - 创建题目
- `updateQuestion(id, data)` - 更新题目
- `deleteQuestion(id)` - 删除题目
- `getMyQuestions(params)` - 获取我的题目
- `toggleQuestionLike(id)` - 切换题目点赞状态
- `toggleQuestionFavorite(id)` - 切换题目收藏状态
- `getQuestionComments(id, params)` - 获取题目评论
- `addComment(questionId, data)` - 添加评论
- `deleteComment(questionId, commentId)` - 删除评论
- `getRandomQuestion(params)` - 获取随机题目
- `getPendingQuestions(params)` - 获取待审核题目（管理员）
- `approveQuestion(id, data)` - 审核通过题目（管理员）
- `rejectQuestion(id, data)` - 审核拒绝题目（管理员）

### category.js - 分类模块
- `getCategories()` - 获取分类列表
- `getCategory(id)` - 获取单个分类详情
- `createCategory(data)` - 创建分类
- `updateCategory(id, data)` - 更新分类
- `deleteCategory(id)` - 删除分类
- `getAllCategories(params)` - 获取所有分类（管理员）
- `updateCategoryStatus(id, status)` - 更新分类状态（管理员）

### admin.js - 管理员模块
- `getPendingReviews(params)` - 获取待审核内容
- `approveReview(id, data)` - 审核通过
- `rejectReview(id, data)` - 审核拒绝
- `getUsers(params)` - 获取用户列表
- `getUser(id)` - 获取单个用户信息
- `updateUserStatus(id, status)` - 更新用户状态
- `updateUserRole(id, role)` - 更新用户角色
- `deleteUser(id)` - 删除用户
- `getSystemStats()` - 获取系统统计
- `getSystemLogs(params)` - 获取系统日志
- `getDailyStats(params)` - 获取日统计
- `getWeeklyStats(params)` - 获取周统计
- `getMonthlyStats(params)` - 获取月统计

### search.js - 搜索模块
- `search(params)` - 全局搜索
- `searchBanks(params)` - 搜索题库
- `searchQuestions(params)` - 搜索题目

### stats.js - 统计模块
- `getSystemStats()` - 获取系统统计数据
- `getHotBanks(params)` - 获取热门题库
- `getLatestQuestions(params)` - 获取最新题目
- `getUserStats()` - 获取用户统计数据
- `getBankStats(bankId)` - 获取题库统计数据
- `getQuestionStats(questionId)` - 获取题目统计数据
- `getCategoryStats()` - 获取分类统计数据

## 使用示例

```javascript
import { bankApi, questionApi } from '@/api'

// 获取题库列表
const banks = await bankApi.getBanks({ page: 1, size: 10 })

// 创建新题目
await questionApi.createQuestion({
  title: '新题目',
  content: '题目内容',
  bankId: 1
})
```