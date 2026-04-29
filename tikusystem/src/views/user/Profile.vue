<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never" class="section-card">
          <div class="user-info">
            <!-- 头像区域 -->
            <div class="avatar-wrap" @click="triggerUpload">
              <img v-if="avatarPreview || userStore.userInfo?.avatar"
                :src="avatarPreview || userStore.userInfo?.avatar"
                class="user-avatar user-avatar--img"
              />
              <div v-else class="user-avatar">
                {{ userStore.userInfo?.username?.charAt(0).toUpperCase() || '用' }}
              </div>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
            <!-- 隐藏的文件选择器 -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              style="display:none"
              @change="onFileChange"
            />
            <!-- 有待上传的图片时显示操作按钮 -->
            <div v-if="avatarPreview" class="avatar-actions">
              <button class="avatar-btn avatar-btn--confirm" :disabled="avatarUploading" @click="saveAvatar">
                {{ avatarUploading ? '保存中...' : '保存头像' }}
              </button>
              <button class="avatar-btn avatar-btn--cancel" @click="cancelAvatar">取消</button>
            </div>
            <h3 class="username">{{ userStore.userInfo?.username }}</h3>
            <p class="user-email">{{ userStore.userInfo?.email }}</p>
            <div class="user-stats">
              <div class="stat-item" @click="$router.push('/user/banks')">
                <div class="stat-value">{{ userStats.bankCount }}</div>
                <div class="stat-label">题库</div>
              </div>
              <div class="stat-item" @click="$router.push('/user/questions')">
                <div class="stat-value">{{ userStats.questionCount }}</div>
                <div class="stat-label">题目</div>
              </div>
              <div class="stat-item" @click="$router.push('/user/favorites')">
                <div class="stat-value">{{ userStats.favoriteCount }}</div>
                <div class="stat-label">收藏</div>
              </div>
              <div class="stat-item" @click="$router.push('/user/following')">
                <div class="stat-value">{{ userStats.followingCount }}</div>
                <div class="stat-label">关注</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #409eff"></span>
              <span class="header-title">个人信息</span>
            </div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="profile-form">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="form.bio" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <button type="button" class="submit-btn" :disabled="loading" @click="updateProfile">{{ loading ? '保存中...' : '保存修改' }}</button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #f56c6c"></span>
              <span class="header-title">修改密码</span>
            </div>
          </template>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <button type="button" class="submit-btn submit-btn--danger" :disabled="loading" @click="changePassword">{{ loading ? '提交中...' : '修改密码' }}</button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)

// 头像相关
const fileInputRef = ref()
const avatarPreview = ref('')
const avatarUploading = ref(false)

function triggerUpload() {
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    // 用 Canvas 压缩到 200x200 以内，质量 0.85
    const img = new Image()
    img.onload = () => {
      const MAX = 200
      let w = img.width, h = img.height
      if (w > h) { if (w > MAX) { h = Math.round(h * MAX / w); w = MAX } }
      else        { if (h > MAX) { w = Math.round(w * MAX / h); h = MAX } }
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      canvas.getContext('2d').drawImage(img, 0, 0, w, h)
      avatarPreview.value = canvas.toDataURL('image/jpeg', 0.85)
    }
    img.src = ev.target.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function saveAvatar() {
  if (!avatarPreview.value) return
  avatarUploading.value = true
  try {
    await userStore.updateInfo({ avatar: avatarPreview.value })
    ElNotification({ title: '头像更新成功', message: '新头像已保存', type: 'success', duration: 3000 })
    avatarPreview.value = ''
  } catch (error) {
    ElMessage.error(error.message || '头像保存失败')
  } finally {
    avatarUploading.value = false
  }
}

function cancelAvatar() {
  avatarPreview.value = ''
}

const userStats = ref({
  bankCount: 0,
  questionCount: 0,
  favoriteCount: 0,
  followingCount: 0
})

const formRef = ref()
const form = reactive({
  username: '',
  email: '',
  bio: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function updateProfile() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.updateInfo(form)
    ElNotification({
      title: '保存成功',
      message: '个人信息已更新',
      type: 'success',
      duration: 3000
    })
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function changePassword() {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmNewPassword: passwordForm.confirmPassword
    })
    ElNotification({
      title: '密码修改成功',
      message: '请使用新密码重新登录',
      type: 'success',
      duration: 4000
    })
    passwordFormRef.value.resetFields()
  } catch (error) {
    ElNotification({
      title: '修改失败',
      message: error.message || '请检查当前密码是否正确',
      type: 'error',
      duration: 4000
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  userStore.fetchUserInfo().then(() => {
    form.username = userStore.userInfo?.username || ''
    form.email = userStore.userInfo?.email || ''
    form.bio = userStore.userInfo?.bio || ''

    userStats.value = {
      bankCount: userStore.userInfo?.bankCount || 0,
      questionCount: userStore.userInfo?.questionCount || 0,
      favoriteCount: userStore.userInfo?.favoriteCount || 0,
      followingCount: userStore.userInfo?.followingCount || 0
    }
  })
})
</script>

<style lang="scss" scoped>
.profile-page {
  padding: 4px 0;
}

.section-card {
  margin-bottom: 20px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f5;
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-dot {
  width: 4px;
  height: 18px;
  border-radius: 4px;
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  text-align: center;
  padding: 20px 0;

  .avatar-wrap {
    position: relative;
    width: 80px;
    height: 80px;
    margin: 0 auto 12px;
    cursor: pointer;
    border-radius: 20px;
    overflow: hidden;

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .user-avatar {
    width: 80px;
    height: 80px;
    border-radius: 20px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 32px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6px 18px rgba(64, 158, 255, 0.35);

    &--img {
      width: 80px;
      height: 80px;
      object-fit: cover;
      display: block;
    }
  }

  .avatar-overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s;
    color: #fff;
    font-size: 11px;
    border-radius: 20px;

    .el-icon { font-size: 18px; }
  }

  .avatar-actions {
    display: flex;
    gap: 8px;
    justify-content: center;
    margin-bottom: 12px;
  }

  .avatar-btn {
    height: 28px;
    padding: 0 12px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    border: none;
    transition: opacity 0.15s;

    &--confirm {
      background: #409eff;
      color: #fff;
      &:hover { opacity: 0.88; }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
    }

    &--cancel {
      background: #f0f0f0;
      color: #606266;
      &:hover { background: #e0e0e0; }
    }
  }

  .username {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .user-email {
    color: #909399;
    font-size: 13px;
    margin: 0 0 20px;
  }

  .user-stats {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    padding: 20px 20px 0;
    border-top: 1px solid #edf0f5;

    .stat-item {
      padding: 16px;
      background: #f7f9fc;
      border-radius: 10px;
      border: 1px solid #edf0f5;
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s, transform 0.15s;

      &:hover {
        background: #e8f4ff;
        border-color: #c6e0ff;
        transform: translateY(-2px);
      }

      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: #303133;
        margin-bottom: 4px;
      }

      .stat-label {
        color: #909399;
        font-size: 12px;
      }
    }
  }
}

.profile-form {
  max-width: 500px;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }

  :deep(.el-textarea__inner) {
    border-radius: 8px;
  }
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  padding: 0 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  color: #fff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: opacity 0.2s, transform 0.15s;

  &:hover { opacity: 0.88; }
  &:active { transform: scale(0.97); }

  &--danger {
    background: linear-gradient(135deg, #f56c6c, #e85555);
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
  }
}
</style>