import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

const myAxios: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000, // 建议适当延长超时时间
  withCredentials: true,
});

// 请求拦截器 - 添加token等认证信息
myAxios.interceptors.request.use(
  function  (config: InternalAxiosRequestConfig)  {
    // 在发送请求之前做些什么
    return config;
  },
);

// 响应拦截器 - 统一错误处理
myAxios.interceptors.response.use(
  function(response) {
    // 对响应数据做点什么
    console.log(response);
    const {data} = response;
    console.log(data);
    if(data.code === 40100){
      // 跳转到登录页面
      if(
        !response.request.responseURL.includes('user/current') &&
       !window.location.href.includes('/user/login')
      ){
        window.location.href = '/user/login?redirect=${window.location.href}';
      }

    }
    return response;
  },

  function(error) {
  // 对响应错误做点什么
    return Promise.reject(error);
  }
);

export default myAxios;