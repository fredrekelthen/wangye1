<template>
  <div class="content-section">
    <div class="section-header">
      <h3 class="section-title">{{ currentView === 'graph' ? '知识图谱' : '学习内容' }}</h3>
      <div class="header-actions">
        <a-button
          type="primary"
          size="small"
          @click="switchView"
        >
          {{ currentView === 'graph' ? '查看资料' : '查看知识图谱' }}
        </a-button>
      </div>
    </div>

    <!-- 知识图谱视图 -->
    <KnowledgeGraph
      v-if="currentView === 'graph'"
      :chapter-id="selectedChapterId"
      @node-click="handleGraphNodeClick"
      ref="knowledgeGraphRef"
    />

    <!-- 当前选中单元的资料 -->
    <div v-if="currentView === 'materials' && selectedCourse" class="selected-course-material">
      <div class="course-info">
        <div class="course-path">
          <strong>{{ selectedCourse.chapterTitle }}</strong>
          <template v-if="selectedCourse.lessonTitle">
            <span class="separator">/</span>
            <strong>{{ selectedCourse.lessonTitle }}</strong>
          </template>
          <template v-if="selectedCourse.subsectionTitle">
            <span class="separator">/</span>
            <strong>{{ selectedCourse.subsectionTitle }}</strong>
          </template>
        </div>
        <div v-if="courseMaterials.length > 0" class="material-count">
          该小节共有 <strong>{{ courseMaterials.length }}</strong> 个资料
        </div>
        <div v-else class="material-count">
          该小节暂无上传资料
        </div>
      </div>

      <!-- 小节介绍 -->
      <div v-if="selectedCourse.subsectionTitle" class="lesson-intro">
        <h4 class="lesson-intro-title">{{ selectedCourse.subsectionTitle }}</h4>
        <p class="lesson-intro-desc">{{ selectedCourse.description || '暂无介绍内容' }}</p>
      </div>

      <!-- 视频播放器 -->
      <div v-if="currentVideo" class="video-player-container">
        <div class="video-player-header">
          <h4 class="video-title">{{ currentVideo.fileName }}</h4>
          <a-button
            type="default"
            size="small"
            @click="closeVideoPlayer"
          >
            关闭播放器
          </a-button>
        </div>
        <div class="video-wrapper">
          <video
            ref="videoRef"
            :src="videoSrc"
            controls
            width="100%"
            class="video-element"
            @error="handleVideoError"
          >
            您的浏览器不支持视频播放。
          </video>
        </div>
      </div>

      <!-- DOCX 文档预览 -->
      <div v-if="currentDocx" class="docx-preview-container">
        <div class="docx-preview-header">
          <h4 class="docx-title">{{ currentDocx.fileName }}</h4>
          <a-button
            type="default"
            size="small"
            @click="closeDocxPreview"
          >
            关闭预览
          </a-button>
        </div>
        <div class="docx-wrapper">
          <iframe
            :src="docxPreviewUrl"
            width="100%"
            height="600px"
            frameborder="0"
            class="docx-iframe"
          />
        </div>
        <div class="docx-fallback">
          <p class="fallback-tip">如果预览加载失败，请下载后使用 Word 查看</p>
          <a-button
            type="primary"
            size="small"
            @click="downloadDocx"
          >
            下载文档
          </a-button>
        </div>
      </div>

      <!-- 资料列表 -->
      <div v-if="courseMaterials.length > 0" class="materials-grid">
        <div v-for="material in courseMaterials" :key="material.id" class="material-card">
          <div class="material-icon" :style="{ background: getIconBg(material.type) }">
            {{ getFileIcon(material.type) }}
          </div>
          <div class="material-info">
            <h5 class="material-title">{{ material.fileName }}</h5>
            <p class="material-type">{{ material.type }}</p>
            <a-button
              type="primary"
              size="small"
              @click="viewMaterial(material)"
            >
              {{ material.type === '视频' ? '播放' : material.type === 'DOCX' ? '预览' : '下载' }}
            </a-button>
          </div>
        </div>
      </div>

      <!-- 无资料提示 -->
      <a-empty
        v-else
        description="暂无上传资料"
        style="margin-top: 40px"
      />
    </div>

    <!-- 默认提示：未选择课程时显示 -->
    <div v-if="currentView === 'materials' && !selectedCourse" class="no-course-selected">
      <a-empty description="请从左侧课程树选择要学习的章节" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';
import KnowledgeGraph from './KnowledgeGraph.vue';
import myAxios from '@/request';

// 视图状态: 'graph' - 知识图谱, 'materials' - 课程资料
const currentView = ref<'graph' | 'materials'>('graph');
const selectedChapterId = ref<number | undefined>(undefined);
const knowledgeGraphRef = ref<InstanceType<typeof KnowledgeGraph> | null>(null);

// 当前选中的学习单元：章 -> 节 -> （可选）小节
interface LessonSelection {
  chapterTitle: string;
  lessonTitle: string;
  subsectionTitle?: string;
  description?: string;
}
const selectedCourse = ref<LessonSelection | null>(null);

// 当前播放的视频
const currentVideo = ref<any>(null);

// 当前预览的DOCX文档
const currentDocx = ref<any>(null);

// 视频元素引用
const videoRef = ref<HTMLVideoElement | null>(null);

// 学习资料（统一归一化：{id,fileName,type,fileUrl,chapter,lesson,fileSize}）
const uploadedMaterials = ref<any[]>([]);

const normalizeMaterials = (list: any[]) => (list || []).map((m: any) => ({
  id: m.id || m._id,
  fileName: m.fileName,
  fileType: m.fileType,
  type: m.materialType || m.fileType || m.type,
  chapter: m.chapterTitle ?? m.chapter,
  lesson: m.lessonTitle ?? m.lesson,
  fileUrl: m.filePath || m.fileUrl,
  fileSize: m.fileSize,
}));

// 加载学习资料：优先后端资料库（真实、最新），失败时回退 localStorage 缓存
const loadUploadedMaterials = async () => {
  try {
    const res = await myAxios.get('/api/materials/list');
    if (res.data?.success) {
      uploadedMaterials.value = normalizeMaterials(res.data.materials);
      return;
    }
  } catch (e) {
    // 后端不可用，回退本地缓存
  }
  const stored = localStorage.getItem('uploadedMaterials');
  try {
    uploadedMaterials.value = stored ? normalizeMaterials(JSON.parse(stored)) : [];
  } catch {
    uploadedMaterials.value = [];
  }
};

// 当前选中单元的资料（按 章 + 节 匹配；资料粒度在“节”一级）
const courseMaterials = computed(() => {
  if (!selectedCourse.value) return [];
  return uploadedMaterials.value.filter(m =>
    m.chapter === selectedCourse.value!.chapterTitle &&
    (!selectedCourse.value!.lessonTitle || m.lesson === selectedCourse.value!.lessonTitle)
  );
});

// 视频源（处理Base64数据）
const videoSrc = computed(() => {
  if (!currentVideo.value) return '';
  const url = currentVideo.value.fileUrl;
  // 如果不是Base64格式，直接返回
  if (!url.startsWith('data:')) {
    return url;
  }
  // 确保Base64格式正确
  if (url.startsWith('data:video/mp4')) {
    return url;
  }
  // 尝试修正为正确的视频Base64格式
  const base64Data = url.split(',')[1];
  return `data:video/mp4;base64,${base64Data}`;
});

// DOCX预览URL（使用Office Online预览）
const docxPreviewUrl = computed(() => {
  if (!currentDocx.value) return '';
  // 使用Microsoft Office Online预览服务
  const url = currentDocx.value.fileUrl;
  if (url.startsWith('data:')) {
    // 将Base64转换为Blob后使用预览服务
    return 'about:blank'; // Base64无法直接预览，使用fallback
  }
  return `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(url)}`;
});

// 获取文件图标
const getFileIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    'PDF': '📄',
    'DOCX': '📝',
    '视频': '🎬',
    'PPT': '📊'
  };
  return iconMap[type] || '📁';
};

// 获取图标背景
const getIconBg = (type: string) => {
  const bgMap: Record<string, string> = {
    'PDF': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'DOCX': 'linear-gradient(135deg, #4ecdc4 0%, #44bd32 100%)',
    '视频': 'linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%)',
    'PPT': 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
  };
  return bgMap[type] || 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)';
};

// 查看资料
const viewMaterial = (material: any) => {
  if (material.type === '视频') {
    // 在当前页面播放视频
    currentVideo.value = material;
    // 关闭其他预览
    currentDocx.value = null;
    message.success('开始播放：' + material.fileName);

    // 滚动到视频播放器
    setTimeout(() => {
      const videoPlayer = document.querySelector('.video-player-container');
      if (videoPlayer) {
        videoPlayer.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
  } else if (material.type === 'DOCX') {
    // 预览DOCX文档
    currentDocx.value = material;
    // 关闭其他播放
    currentVideo.value = null;
    message.success('开始预览：' + material.fileName);

    // 滚动到DOCX预览区域
    setTimeout(() => {
      const docxPreview = document.querySelector('.docx-preview-container');
      if (docxPreview) {
        docxPreview.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
  } else {
    // 下载文档（PDF、PPT）
    const link = document.createElement('a');
    link.href = material.fileUrl;
    link.download = material.fileName;
    link.click();
    message.success('开始下载：' + material.fileName);
  }
};

// 视频加载错误处理
const handleVideoError = (e: any) => {
  message.error('视频加载失败，请检查文件格式或重新上传');
  console.error('视频加载错误：', e);
};

// 下载DOCX文档
const downloadDocx = () => {
  if (!currentDocx.value) return;

  const link = document.createElement('a');
  link.href = currentDocx.value.fileUrl;
  link.download = currentDocx.value.fileName;
  link.click();
  message.success('开始下载：' + currentDocx.value.fileName);
};

// 关闭视频播放器
const closeVideoPlayer = () => {
  if (videoRef.value) {
    videoRef.value.pause();
  }
  currentVideo.value = null;
};

// 关闭DOCX预览
const closeDocxPreview = () => {
  currentDocx.value = null;
};

// 处理知识图谱节点点击
const handleGraphNodeClick = (nodeData: any) => {
  if (nodeData.type === 'chapter') {
    // 点击章节节点，显示该章节的知识图谱
    selectedChapterId.value = nodeData.chapterId;
    currentView.value = 'graph';
    message.info(`已切换到: ${nodeData.chapterTitle}`);
  } else if (nodeData.type === 'lesson') {
    // 点击课程节点，显示该课程的资料
    selectedCourse.value = {
      chapterTitle: nodeData.chapter || '',
      lessonTitle: nodeData.lesson || '',
    };
    loadUploadedMaterials();
    currentView.value = 'materials';
    message.success(`已切换到: ${nodeData.lesson}`);
  } else if (nodeData.type === 'course-view') {
    // 返回课程视图
    selectedChapterId.value = undefined;
    currentView.value = 'graph';
  }
};

// 切换视图
const switchView = () => {
  if (currentView.value === 'graph') {
    currentView.value = 'materials';
  } else {
    currentView.value = 'graph';
  }
};

// 监听课程选中事件（来自知识图谱节点点击：chapter=章标题, lesson=节标题）
const handleCourseSelected = (e: any) => {
  const d = e.detail || {};
  selectedCourse.value = {
    chapterTitle: d.chapter || '',
    lessonTitle: d.lesson || '',
  };
  loadUploadedMaterials();
  currentView.value = 'materials';
};

// 监听小节选中事件（来自 CourseTree 点击小节：关闭图谱，展示介绍与资料）
const handleSubsectionSelected = (e: any) => {
  const d = e.detail || {};
  selectedCourse.value = {
    chapterTitle: d.chapter || '',
    lessonTitle: d.section || '',
    subsectionTitle: d.subsection || '',
    description: d.description || '',
  };
  loadUploadedMaterials();
  currentView.value = 'materials';
};

// 监听章节选中事件（来自 CourseTree）
const handleChapterSelected = (e: any) => {
  const chapterTitle = e.detail.chapter;
  
  // 提取章节编号
  const match = chapterTitle.match(/第(\d+)章/);
  if (match) {
    const chapterIndex = parseInt(match[1]);
    selectedChapterId.value = chapterIndex;
    currentView.value = 'graph';
    message.info(`已选中章节: ${chapterTitle}`);
  }
};

// 组件挂载时加载资料并添加监听
onMounted(() => {
  loadUploadedMaterials();
  window.addEventListener('course-selected', handleCourseSelected);
  window.addEventListener('subsection-selected', handleSubsectionSelected);
  window.addEventListener('chapter-selected', handleChapterSelected);
});

// 组件卸载时移除监听
onUnmounted(() => {
  window.removeEventListener('course-selected', handleCourseSelected);
  window.removeEventListener('subsection-selected', handleSubsectionSelected);
  window.removeEventListener('chapter-selected', handleChapterSelected);
});
</script>

<style scoped>
.content-section {
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  padding: 20px;
  min-height: 420px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e6e6e6;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
}

/* 知识图谱样式 */
.knowledge-graph {
  min-height: 500px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

/* 课程资料样式 */
.selected-course-material {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.course-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.course-path {
  font-size: 16px;
  margin-bottom: 8px;
}

.separator {
  margin: 0 8px;
  color: #999;
}

.material-count {
  font-size: 14px;
  color: #666;
}

/* 小节介绍 */
.lesson-intro {
  background: #f0f7ff;
  border: 1px solid #d6e9ff;
  border-left: 4px solid #1890ff;
  border-radius: 6px;
  padding: 14px 16px;
  margin-bottom: 20px;
}

.lesson-intro-title {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.lesson-intro-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #334155;
}

/* 视频播放器样式 */
.video-player-container {
  margin-bottom: 20px;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  overflow: hidden;
}

.video-player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e6e6e6;
}

.video-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.video-wrapper {
  background: #000;
}

.video-element {
  display: block;
  max-height: 500px;
}

/* DOCX预览样式 */
.docx-preview-container {
  margin-bottom: 20px;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  overflow: hidden;
}

.docx-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e6e6e6;
}

.docx-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.docx-wrapper {
  background: #fff;
}

.docx-iframe {
  display: block;
}

.docx-fallback {
  padding: 20px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #e6e6e6;
}

.fallback-tip {
  margin: 0 0 12px 0;
  color: #999;
  font-size: 14px;
}

/* 资料列表样式 */
.materials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.material-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  transition: all 0.3s;
}

.material-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #1890ff;
}

.material-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-type {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #999;
}

/* 无课程选中提示 */
.no-course-selected {
  padding: 60px 20px;
  text-align: center;
  background: #fafafa;
  border-radius: 8px;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
