<template>
    <div id="user-login">
        <h2 class="title">{{ loginType === 'student' ? '学生登录' : '管理员登录' }}</h2>

        <!-- 登录类型切换 -->
        <div class="login-type-switch">
            <a-radio-group v-model:value="loginType" button-style="solid">
                <a-radio-button value="student">学生登录</a-radio-button>
                <a-radio-button value="admin">管理员登录</a-radio-button>
            </a-radio-group>
        </div>

    <a-form
    style="max-width: 480px; margin: 0 auto;"
    :model="formState"
    name="basic"
    :label-col="{ span: 8 }"
    :wrapper-col="{ span: 16 }"
    autocomplete="off"
    @finish="handlesubmit"
    >
    <a-form-item
      label="账号"
      name="userAccount"
      :rules="[{ required: true, message: '请输入您的账号' }]"
    >
      <a-input
       v-model:value="formState.userAccount"
      placeholder="请输入您的账号"
       />
    </a-form-item>

    <a-form-item
      name="userPassword"
      label="密码"
      :rules="[{ required: true, message: '请输入您的密码'},{ min: 6, message: '密码长度不能少于6位'}]"
    >
      <a-input-password
       v-model:value="formState.userPassword"
      placeholder="请输入您的密码"
       />
    </a-form-item>

    <!-- <a-form-item name="remember" :wrapper-col="{ offset: 8, span: 16 }">
      <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
    </a-form-item> -->

    <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
      <a-button type="primary" html-type="submit">登录</a-button>
    </a-form-item>
    </a-form>
    </div>
</template>
<script lang="ts" setup>
import { userLogin } from '@/api/user';
import { useUserLogin } from '@/store/useUesrLogin';
import { message } from 'ant-design-vue';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const loginType = ref('student'); // 默认为学生登录

interface FormState {
  userAccount: string;
  userPassword: string;
}

const formState = reactive<FormState>({
  userAccount: '',
  userPassword: '',
});
const handlesubmit = async (values: any) => {
  const res = await userLogin(values);
    if (res.data.code === 0) {
      await loginUser.fetchLoginUser(res.data.data);

      // 检查登录类型是否匹配
      if (loginType.value === 'admin' && res.data.data.role !== 'ADMIN') {
        message.error('您不是管理员账号');
        return;
      }
      if (loginType.value === 'student' && res.data.data.role === 'ADMIN') {
        message.warning('管理员账号请使用管理员登录');
        return;
      }

      message.success('登录成功');
      router.push({
        path:'/',
        replace: true,
      });
    } else {
      message.error('登录失败，请检查账号或密码');
    }
};

const loginUser = useUserLogin();

  //    @finishFailed="onFinishFailed"
// const onFinishFailed = (errorInfo: any) => {
//   console.log('Failed:', errorInfo);
// };

</script>

<style scoped>
#user-login .title {
  text-align: center;
  margin-top: 20px;
  margin-bottom: 16px;
}

.login-type-switch {
  text-align: center;
  margin-bottom: 20px;
}
</style>

