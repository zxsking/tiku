<template>
  <div class="register-page">
    <div class="register-bg">
      <div class="bg-shape bg-shape--1"></div>
      <div class="bg-shape bg-shape--2"></div>
      <div class="bg-shape bg-shape--3"></div>
    </div>
    <el-card shadow="never" class="register-card">
      <template #header>
        <div class="register-header">
          <div class="logo-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <h2>注册账号</h2>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
            size="large"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码"
            show-password
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            class="submit-btn-full-width"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          已有账号？<router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      email: form.email,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error(error)
    ElMessage.error(error.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .bg-shape {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
    background: #fff;

    &--1 {
      width: 400px;
      height: 400px;
      top: -100px;
      left: -100px;
    }

    &--2 {
      width: 300px;
      height: 300px;
      bottom: -50px;
      right: -50px;
    }

    &--3 {
      width: 200px;
      height: 200px;
      top: 40%;
      right: 20%;
    }
  }
}

.register-card {
  width: 420px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;

  :deep(.el-card__header) {
    padding: 24px 24px 12px;
    border: none;
    background: transparent;
  }

  :deep(.el-card__body) {
    padding: 12px 24px 28px;
  }
}

.register-header {
  text-align: center;

  .logo-icon {
    width: 56px;
    height: 56px;
    margin: 0 auto 12px;
    border-radius: 16px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);

    .el-icon {
      font-size: 28px;
      color: #fff;
    }
  }

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    letter-spacing: -0.3px;
  }
}

.register-form {
  :deep(.el-form-item__label) {
    color: #606266;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    padding: 4px 12px;
    box-shadow: 0 0 0 1px #edf0f5 inset;

    &:hover {
      box-shadow: 0 0 0 1px #c6e0ff inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .submit-btn-full-width {
    width: 100%;
    height: 44px;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 600;
  }
}

.register-footer {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 8px;

  a {
    color: #409eff;
    font-weight: 500;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>