<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="error-icon">
        <el-icon><WarningFilled /></el-icon>
      </div>
      <div class="error-code">404</div>
      <div class="error-title">抱歉，您访问的页面不存在</div>
      <div class="error-desc">请检查URL是否正确，或返回首页</div>
      <button class="home-btn" @click="$router.push('/')">
        <el-icon><HomeFilled /></el-icon>返回首页
      </button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.not-found-page {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  padding: 40px 20px;
}

.not-found-content {
  text-align: center;
  max-width: 400px;

  .error-icon {
    width: 100px;
    height: 100px;
    margin: 0 auto 24px;
    border-radius: 50%;
    background: linear-gradient(135deg, #f56c6c, #e85555);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12px 32px rgba(245, 108, 108, 0.3);

    .el-icon {
      font-size: 48px;
      color: #fff;
    }
  }

  .error-code {
    font-size: 72px;
    font-weight: 800;
    color: #303133;
    line-height: 1;
    letter-spacing: -2px;
    margin-bottom: 12px;
    background: linear-gradient(135deg, #303133 0%, #606266 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .error-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }

  .error-desc {
    font-size: 14px;
    color: #909399;
    margin-bottom: 32px;
  }

  .home-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 44px;
    padding: 0 28px;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    border: none;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 6px 18px rgba(64, 158, 255, 0.4);
    transition: all 0.2s;

    &:hover {
      opacity: 0.9;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(64, 158, 255, 0.5);
    }

    &:active {
      transform: translateY(0);
    }
  }
}
</style>