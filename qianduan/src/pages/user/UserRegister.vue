<template>
    <div  id = "user-register">
        <h2 class = "title">用户注册</h2>
    <a-form
    style = "max-width: 480px; margin: 0 auto;"
    :model="formState"
    name="basic"
    :label-col="{ span: 8 }"
    :wrapper-col="{ span: 16 }"
    autocomplete="off"
    @finish="handlesubmit"

    >
    <a-form-item
      label="编号"
      name="planetCode"
      :rules="[{ required: true, message: '请输入您的编号' }]"
    >
      <a-input
        v-model:value="formState.planetCode"
        placeholder="请输入您的编号"
      />
    </a-form-item>
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
       v-model:value="formState.userPassword" placeholder="请输入您的密码"/>
    </a-form-item>
    <a-form-item
      name="userPassword"
      label="确认密码"
      :rules="[{ required: true, message: '请确认您的密码'},{ min: 6, message: '密码长度不能少于6位'}]"
    >
      <a-input-password
       v-model:value="formState.checkPassword" placeholder="请确认您的密码"/>
    </a-form-item>
    <!-- <a-form-item name="remember" :wrapper-col="{ offset: 8, span: 16 }">
      <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
    </a-form-item> -->
      <a-form-item  :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">注册</a-button>
     </a-form-item>
      
    </a-form>
    </div>
</template>
     
<script lang="ts" setup>
import { userLogin, userRegister } from '@/api/user';
import { useUserLogin } from '@/store/useUesrLogin';
import { message } from 'ant-design-vue';
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
interface FormState {
    userAccount: string;
    userPassword: string;
    checkPassword: string;
    planetCode: string;
}

const formState = reactive<FormState>({
    userAccount: '',
    userPassword: '',
    checkPassword: '',
    planetCode: '',
});
const handlesubmit = async (values: any) => {

    if (formState.userPassword !== formState.checkPassword) {
        message.error('两次密码输入不一致');
        return;
    }
    const res = await userRegister(values);
    if (res.data.code === 0
        && res.data.data
    ) {
      message.success('注册成功');

      router.push({
        path:'/user/login',
        replace: true,
      });
    } else {
      message.error('注册失败'  + res.data.description);
    }

};



  //    @finishFailed="onFinishFailed"
// const onFinishFailed = (errorInfo: any) => {
//   console.log('Failed:', errorInfo);
// };

</script>

<style scoped>  
#user-register .title{

  text-align: center;
  margin-top: 20px;
  margin-bottom: 16px;
}


</style>

