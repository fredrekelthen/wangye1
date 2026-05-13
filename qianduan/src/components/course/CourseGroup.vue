<template>
  <div class="group-section">
    <div class="section-header">
      <h3 class="section-title">分组</h3>
      <a-button type="primary" @click="showCreateModal = true">
        创建分组
      </a-button>
    </div>

    <div class="groups-grid">
      <div 
        v-for="group in groups" 
        :key="group.id" 
        class="group-card"
      >
        <div class="group-header">
          <div class="group-icon">{{ group.icon }}</div>
          <div class="group-info">
            <h4 class="group-name">{{ group.name }}</h4>
            <span class="group-code">编号: {{ group.code }}</span>
          </div>
          <a-dropdown trigger="click">
            <a-button type="text" class="more-btn">
              •••
            </a-button>
            <template #overlay>
              <a-menu @click="handleGroupAction($event, group.id)">
                <a-menu-item key="join">加入分组</a-menu-item>
                <a-menu-item key="invite">邀请成员</a-menu-item>
                <a-menu-item key="view">查看详情</a-menu-item>
                <a-menu-item key="leave" v-if="group.isMember">退出分组</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        
        <p class="group-description">{{ group.description }}</p>
        
        <div class="group-members">
          <div class="member-avatars">
            <span 
              v-for="(member, idx) in group.members.slice(0, 4)" 
              :key="member.name || idx"
              class="avatar"
            >
              {{ member.avatar }}
            </span>
            <span 
              v-if="group.members.length > 4" 
              class="avatar more"
            >
              +{{ group.members.length - 4 }}
            </span>
          </div>
          <span class="member-count">{{ group.members.length }} 人</span>
        </div>

        <div class="group-stats">
          <div class="stat-item">
            <span class="stat-icon">📝</span>
            <span class="stat-label">{{ group.tasks }} 任务</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">💬</span>
            <span class="stat-label">{{ group.discussions }} 讨论</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建分组弹窗 -->
    <a-modal 
      v-model:open="showCreateModal" 
      title="创建分组" 
      :footer="null"
      width="600px"
    >
      <a-form 
        :model="newGroup" 
        layout="vertical"
        @finish="createGroup"
      >
        <a-form-item label="分组名称" name="name" required>
          <a-input 
            v-model:value="newGroup.name" 
            placeholder="请输入分组名称" 
            :maxlength="20"
            show-count
          />
        </a-form-item>
        <a-form-item label="分组编号" name="code" required>
          <a-input 
            v-model:value="newGroup.code" 
            placeholder="请输入分组编号（用于加入）" 
            :maxlength="10"
          />
        </a-form-item>
        <a-form-item label="分组描述" name="description">
          <a-textarea 
            v-model:value="newGroup.description" 
            :rows="4" 
            placeholder="请输入分组描述"
            :maxlength="200"
            show-count
          />
        </a-form-item>
        <a-form-item label="分组图标">
          <div class="icon-selector">
            <span 
              v-for="icon in iconOptions" 
              :key="icon"
              :class="['icon-option', { active: newGroup.icon === icon }]"
              @click="newGroup.icon = icon"
            >
              {{ icon }}
            </span>
          </div>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">创建</a-button>
            <a-button @click="showCreateModal = false">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';

interface Member {
  avatar: string;
  name: string;
}

interface Group {
  id: number;
  name: string;
  code: string;
  description: string;
  icon: string;
  members: Member[];
  tasks: number;
  discussions: number;
  isMember: boolean;
}

const iconOptions = ['📚','🎯','📊','💻','🔬','⚡','🧮','📐','🎓','💡','🏆','🌟'];
const showCreateModal = ref(false), currentPage = ref(1), pageSize = ref(9);
const newGroup = ref({ name: '', code: '', description: '', icon: '📚' });

const groups = ref<Group[]>([
  {
    id: 1,
    name: '电磁场学习小组',
    code: 'EMF001',
    description: '专注于电磁场理论学习的小组，定期组织讨论和分享。',
    icon: '⚡',
    members: [
      { avatar: '👨‍🎓', name: '张同学' },
      { avatar: '👩‍🎓', name: '李同学' },
      { avatar: '👨‍💻', name: '王同学' },
      { avatar: '👩‍💻', name: '赵同学' },
      { avatar: '👨‍🏫', name: '孙同学' }
    ],
    tasks: 12,
    discussions: 45,
    isMember: true
  },
  {
    id: 2,
    name: '物理建模小组',
    code: 'PHY002',
    description: '探索物理建模方法和工具，提高建模能力。',
    icon: '🧮',
    members: [
      { avatar: '👨‍🔬', name: '周同学' },
      { avatar: '👩‍🔬', name: '吴同学' },
      { avatar: '👨‍🏫', name: '郑同学' }
    ],
    tasks: 8,
    discussions: 23,
    isMember: false
  },
  {
    id: 3,
    name: '电磁波研究组',
    code: 'EMW003',
    description: '深入研究电磁波传播特性及其应用。',
    icon: '🌟',
    members: [
      { avatar: '👩‍🎓', name: '陈同学' },
      { avatar: '👨‍💻', name: '林同学' },
      { avatar: '👩‍💻', name: '黄同学' },
      { avatar: '👨‍🏫', name: '杨同学' }
    ],
    tasks: 15,
    discussions: 67,
    isMember: false
  },
  {
    id: 4,
    name: '实验数据分析组',
    code: 'EDT004',
    description: '专注于实验数据的收集、分析和可视化。',
    icon: '📊',
    members: [
      { avatar: '👨‍🎓', name: '刘同学' },
      { avatar: '👩‍🎓', name: '何同学' }
    ],
    tasks: 6,
    discussions: 18,
    isMember: false
  },
  {
    id: 5,
    name: '综合复习小组',
    code: 'REV005',
    description: '期末考试复习，共同梳理知识点。',
    icon: '💡',
    members: [
      { avatar: '👨‍💻', name: '许同学' },
      { avatar: '👩‍💻', name: '马同学' },
      { avatar: '👨‍🏫', name: '唐同学' }
    ],
    tasks: 10,
    discussions: 32,
    isMember: false
  }
]);

const handleGroupAction = ({ key }: { key: string }, groupId: number) => {
  const group = groups.value.find(g => g.id === groupId);
  
  switch (key) {
    case 'join':
      if (group && !group.isMember) {
        message.success(`成功加入分组: ${group?.name}`);
        group.isMember = true;
      } else {
        message.info('您已经是该分组成员');
      }
      break;
    case 'invite':
      message.info('邀请功能待实现');
      break;
    case 'view':
      message.info(`查看分组详情: ${group?.name}`);
      break;
    case 'leave':
      if (group && group.isMember) {
        group.isMember = false;
        message.success('已退出分组');
      }
      break;
  }
};

const createGroup = () => {
  groups.value.unshift({
    id: groups.value.length + 1,
    name: newGroup.value.name,
    code: newGroup.value.code,
    description: newGroup.value.description,
    icon: newGroup.value.icon,
    members: [],
    tasks: 0,
    discussions: 0,
    isMember: true
  });
  message.success('分组创建成功！');
  showCreateModal.value = false;
  newGroup.value = { name: '', code: '', description: '', icon: '📚' };
};
</script>

<style scoped>
.group-section {
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
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.group-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  border: 1px solid #e6e6e6;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
  cursor: pointer;
}

.group-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.group-icon {
  font-size: 48px;
  background: #fff;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.group-info {
  flex: 1;
  margin-left: 15px;
}

.group-name {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.group-code {
  font-size: 12px;
  color: #999;
}

.more-btn {
  padding: 4px;
}

.group-description {
  margin: 0 0 15px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.group-members {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.member-avatars {
  display: flex;
}

.avatar {
  width: 32px;
  height: 32px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  margin-right: -8px;
  border: 2px solid #fff;
}

.avatar.more {
  background: #f0f0f0;
  color: #999;
  font-size: 12px;
}

.member-count {
  color: #999;
  font-size: 13px;
}

.group-stats {
  display: flex;
  gap: 20px;
  padding-top: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.stat-icon {
  font-size: 16px;
}

.icon-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.icon-option {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: #f5f5f5;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-option:hover {
  border-color: #667eea;
  background: #f0f5ff;
}

.icon-option.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
</style>
