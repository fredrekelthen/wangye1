<template>
  <div id="user-manage">
    <a-card title="学生管理" class="manage-card">
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag :color="record.userRole === 'admin' || record.role === 'ADMIN' ? 'red' : 'blue'">
              {{ record.userRole === 'admin' || record.role === 'ADMIN' ? '管理员' : '学生' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showEditModal(record)">修改密码</a-button>
            <a-popconfirm
              title="确定要删除该用户吗？删除后不可恢复！"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 修改密码弹窗 -->
    <a-modal v-model:open="editVisible" title="修改密码" @ok="handleUpdatePassword" ok-text="确认" cancel-text="取消">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="账号">
          <span>{{ editingUser?.userAccount }}</span>
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="newPassword" placeholder="请输入新密码（至少6位）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import myAxios from '@/request'

const loading = ref(false)
const users = ref<any[]>([])
const editVisible = ref(false)
const editingUser = ref<any>(null)
const newPassword = ref('')

const columns = [
  {
    title: '账号',
    dataIndex: 'userAccount',
    key: 'userAccount',
    width: 150,
  },
  {
    title: '密码',
    dataIndex: 'userPassword',
    key: 'userPassword',
    width: 150,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName',
    width: 150,
  },
  {
    title: '角色',
    key: 'role',
    width: 100,
  },
  {
    title: '注册时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right' as const,
  },
]

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 获取用户列表（管理员接口）
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await myAxios.get('/api/admin/users')
    if (res.data.code === 0) {
      const data = res.data.data
      // 支持分页结果或直接数组
      if (data && data.content && Array.isArray(data.content)) {
        users.value = data.content
        pagination.value.total = data.totalElements || data.content.length
      } else if (Array.isArray(data)) {
        users.value = data
        pagination.value.total = data.length
      } else {
        users.value = []
        pagination.value.total = 0
      }
    } else {
      message.error(res.data.message || '获取学生列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  fetchUsers()
}

// 显示编辑弹窗
const showEditModal = (record: any) => {
  editingUser.value = record
  newPassword.value = ''
  editVisible.value = true
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!editingUser.value) return
  if (!newPassword.value || newPassword.value.length < 6) {
    message.warning('密码长度不能少于6位')
    return
  }
  try {
    const res = await myAxios.put('/api/user/password', {
      id: editingUser.value.id,
      userPassword: newPassword.value,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      editVisible.value = false
      fetchUsers()
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    message.error('修改密码失败')
  }
}

// 删除用户（管理员接口）
const handleDelete = async (userId: string) => {
  try {
    const res = await myAxios.delete(`/api/admin/user/${userId}`)
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchUsers()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    message.error('删除用户失败')
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
#user-manage {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.manage-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
