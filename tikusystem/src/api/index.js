// API入口文件 - 导出所有API模块
import * as authApi from './auth'
import * as bankApi from './bank'
import * as questionApi from './question'
import * as categoryApi from './category'
import * as adminApi from './admin'
import * as searchApi from './search'
import * as statsApi from './stats'
import * as practiceApi from './practice'
import * as wrongBookApi from './wrongBook'

export {
  authApi,
  bankApi,
  questionApi,
  categoryApi,
  adminApi,
  searchApi,
  statsApi,
  practiceApi,
  wrongBookApi
}

// 也可以单独导出特定函数
export * from './auth'
export * from './bank'
export * from './question'
export * from './category'
export * from './admin'
export * from './search'
export * from './stats'
export * from './practice'
export * from './wrongBook'