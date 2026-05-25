<template>
  <div class="home-page">
    <!-- 顶部横幅 -->
    <header class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">欢迎来到课程学习平台</h1>
        <p class="hero-subtitle">探索电磁场课程的精彩世界，开启您的学习之旅</p>
        <button class="hero-button" @click="navigateToCourse">开始学习</button>
      </div>
      <div class="hero-decoration">
        <div class="floating-circle" style="width: 80px; height: 80px; top: 10%; left: 10%;"></div>
        <div class="floating-circle" style="width: 120px; height: 120px; top: 60%; right: 15%;"></div>
        <div class="floating-circle" style="width: 60px; height: 60px; bottom: 15%; left: 25%;"></div>
      </div>
    </header>

    <!-- 平台特色 -->
    <section class="features-section">
      <div class="section-header">
        <h2 class="section-title">平台特色</h2>
        <p class="section-subtitle">为您提供全方位的学习体验</p>
      </div>

      <div class="feature-grid">
        <div class="feature-card" @click="navigateToCourse">
          <div class="feature-icon">📚</div>
          <h3 class="feature-title">课程学习</h3>
          <p class="feature-desc">系统化的电磁场课程内容，从基础到进阶，循序渐进</p>
          <div class="feature-arrow">→</div>
        </div>
        <div class="feature-card" @click="openChat">
          <div class="feature-icon">🎯</div>
          <h3 class="feature-title">智能问答</h3>
          <p class="feature-desc">AI助手实时解答学习问题，提供专业指导</p>
          <div class="feature-arrow">→</div>
        </div>
        <div class="feature-card" @click="navigateToCourse">
          <div class="feature-icon">📊</div>
          <h3 class="feature-title">图表生成</h3>
          <p class="feature-desc">自动生成电磁场示意图，直观理解复杂概念</p>
          <div class="feature-arrow">→</div>
        </div>
        <div class="feature-card" @click="navigateToCourse">
          <div class="feature-icon">💻</div>
          <h3 class="feature-title">互动学习</h3>
          <p class="feature-desc">丰富的互动学习体验，激发学习兴趣</p>
          <div class="feature-arrow">→</div>
        </div>
      </div>
    </section>

    <!-- 快速入口 -->
    <section class="quick-access-section">
      <div class="section-header">
        <h2 class="section-title">快速入口</h2>
        <p class="section-subtitle">快速访问常用功能</p>
      </div>

      <div class="quick-access-grid">
        <button class="quick-access-button" @click="navigateToCourse">
          <span class="button-icon">📖</span>
          <span class="button-text">进入课程</span>
        </button>
        <button class="quick-access-button" @click="openChat">
          <span class="button-icon">💬</span>
          <span class="button-text">咨询助手</span>
        </button>
        <button class="quick-access-button" @click="navigateToCourse">
          <span class="button-icon">📝</span>
          <span class="button-text">查看笔记</span>
        </button>
        <button class="quick-access-button" @click="navigateToCourse">
          <span class="button-icon">🏆</span>
          <span class="button-text">进度追踪</span>
        </button>
      </div>
    </section>

    <!-- 统计信息 -->
    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-number">50+</div>
          <div class="stat-label">课程章节</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">100+</div>
          <div class="stat-label">练习题目</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">24/7</div>
          <div class="stat-label">AI助手支持</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">∞</div>
          <div class="stat-label">学习资源</div>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="page-footer">
      <p>© 2024 电磁场课程学习平台 - 智能教育，开启未来</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();
const navigateToCourse = () => router.push('/course');
const openChat = () => {
  const event = new CustomEvent('openAgentChat');
  window.dispatchEvent(event);
};
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  color: #333;
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
}

/* 顶部横幅 */
.hero-section {
  position: relative;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 80px 20px;
  text-align: center;
  overflow: hidden;
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 48px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 20px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 40px;
  line-height: 1.6;
}

.hero-button {
  background: #fff;
  color: #007BFF;
  border: none;
  padding: 15px 40px;
  font-size: 18px;
  font-weight: bold;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.hero-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  color: #00B894;
}

.hero-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.floating-circle {
  position: absolute;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

/* 平台特色 */
.features-section {
  background: #fff;
  padding: 60px 20px;
}

.section-header {
  text-align: center;
  margin-bottom: 50px;
}

.section-title {
  font-size: 32px;
  font-weight: bold;
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 10px;
}

.section-subtitle {
  font-size: 16px;
  color: #666;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: linear-gradient(135deg, #F0F9F7 0%, #D4EDDA 100%);
  padding: 30px;
  border-radius: 16px;
  text-align: center;
  transition: all 0.4s;
  cursor: pointer;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255,255, 0.3) 0%, transparent 70%);
  transform: scale(0);
  transition: transform 0.6s;
}

.feature-card:hover::before {
  transform: scale(1);
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 30px rgba(0, 123, 255, 0.15);
  border-color: #007BFF;
}

.feature-icon {
  font-size: 64px;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.feature-title {
  font-size: 20px;
  font-weight: bold;
  color: #007BFF;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.feature-desc {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin: 0;
  position: relative;
  z-index: 1;
}

.feature-arrow {
  font-size: 24px;
  color: #00B894;
  margin-top: 15px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s;
  position: relative;
  z-index: 1;
}

.feature-card:hover .feature-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* 快速入口 */
.quick-access-section {
  background: #F0F9F7;
  padding: 60px 20px;
}

.quick-access-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.quick-access-button {
  background: #fff;
  border: 2px solid #e0e0e0;
  padding: 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.quick-access-button:hover {
  border-color: #007BFF;
  background: linear-gradient(135deg, #007BFF 0%, #00B894 100%);
  color: #fff;
}

.button-icon {
  font-size: 36px;
}

.button-text {
  font-size: 16px;
  font-weight: 500;
}

/* 统计信息 */
.stats-section {
  background: linear-gradient(135deg, #00B894 0%, #55E6C1 100%);
  padding: 60px 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.stat-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  padding: 30px;
  border-radius: 16px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s;
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.stat-number {
  font-size: 48px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
}

/* 页脚 */
.page-footer {
  background: #007BFF;
  color: #fff;
  text-align: center;
  padding: 20px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .quick-access-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .quick-access-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
