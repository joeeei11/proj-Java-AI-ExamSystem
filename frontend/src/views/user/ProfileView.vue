<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 头像卡片 -->
      <el-col :span="8">
        <el-card class="avatar-card" shadow="never">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
            >
              <el-avatar :size="100" :src="form.avatarUrl" class="avatar-img">
                {{ authStore.displayName.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon size="20"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
            <h3 class="display-name">{{ form.nickname || authStore.displayName }}</h3>
            <el-tag :type="authStore.isAdmin ? 'danger' : 'primary'" size="small">
              {{ authStore.isAdmin ? '管理员' : '学员' }}
            </el-tag>
            <div class="streak-badge" v-if="form.studyStreak > 0">
              <el-icon><Sunny /></el-icon>
              <span>连续打卡 {{ form.studyStreak }} 天</span>
            </div>
          </div>
        </el-card>

        <!-- 打卡卡片 -->
        <el-card class="checkin-card" shadow="never">
          <template #header>
            <span class="card-title">每日打卡</span>
          </template>
          <div class="checkin-content">
            <p class="checkin-tip">{{ checkedInToday ? '今日已打卡 ✓' : '今天还未打卡' }}</p>
            <el-button
              type="warning"
              :disabled="checkedInToday || checkInLoading"
              :loading="checkInLoading"
              @click="handleCheckIn"
              class="checkin-btn"
            >
              {{ checkedInToday ? '已打卡' : '立即打卡' }}
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 个人信息编辑 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">基本信息</span>
          </template>
          <el-form
            ref="profileFormRef"
            :model="form"
            :rules="profileRules"
            label-width="90px"
            label-position="left"
          >
            <el-form-item label="用户名">
              <el-input :value="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="设置昵称" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="绑定邮箱（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSaveProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card shadow="never" style="margin-top: 20px">
          <template #header>
            <span class="card-title">修改密码</span>
          </template>
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            label-position="left"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 8 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, changePassword as apiChangePassword, uploadAvatar, checkIn, getProfile } from '@/api/user'

const authStore = useAuthStore()

// ===== 基本信息表单 =====
const profileFormRef = ref<FormInstance>()
const saveLoading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  avatarUrl: '',
  studyStreak: 0,
})

const profileRules: FormRules = {
  nickname: [{ max: 50, message: '昵称最长 50 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

// ===== 密码表单 =====
const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

// ===== 打卡 =====
const checkedInToday = ref(false)
const checkInLoading = ref(false)

// ===== 初始化 =====
onMounted(async () => {
  const res = await getProfile()
  const data = res.data
  form.username = data.username
  form.nickname = data.nickname || ''
  form.avatarUrl = data.avatarUrl || ''
  form.studyStreak = data.studyStreak || 0
  form.email = data.email || ''
})

// ===== 方法 =====

async function handleSaveProfile() {
  if (!await profileFormRef.value?.validate().catch(() => false)) return

  saveLoading.value = true
  try {
    const res = await updateProfile({ nickname: form.nickname, email: form.email })
    authStore.updateUserInfo({ nickname: res.data.nickname, avatarUrl: res.data.avatarUrl })
    ElMessage.success('保存成功')
  } finally {
    saveLoading.value = false
  }
}

async function handleChangePassword() {
  if (!await pwdFormRef.value?.validate().catch(() => false)) return

  pwdLoading.value = true
  try {
    await apiChangePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdFormRef.value?.resetFields()
    await authStore.logout()
  } finally {
    pwdLoading.value = false
  }
}

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  try {
    const res = await uploadAvatar(options.file as File)
    form.avatarUrl = res.data.avatarUrl
    authStore.updateUserInfo({ avatarUrl: res.data.avatarUrl })
    ElMessage.success('头像更新成功')
  } catch {
    // 错误已由 request.ts 拦截器处理
  }
}

async function handleCheckIn() {
  checkInLoading.value = true
  try {
    const res = await checkIn()
    form.studyStreak = res.data.studyStreak
    authStore.updateUserInfo({ studyStreak: res.data.studyStreak })
    checkedInToday.value = true
    ElMessage.success(`打卡成功！连续打卡 ${res.data.studyStreak} 天`)
  } finally {
    checkInLoading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.avatar-card,
.checkin-card {
  margin-bottom: 20px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  gap: 12px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
}

.avatar-img {
  font-size: 40px;
  font-weight: 600;
  background: var(--el-color-primary);
  color: #fff;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-uploader:hover .avatar-overlay {
  opacity: 1;
}

.display-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.streak-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
  font-size: 14px;
  font-weight: 500;
}

.checkin-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.checkin-tip {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.checkin-btn {
  width: 100%;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
</style>
