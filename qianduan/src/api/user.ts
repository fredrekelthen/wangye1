import myAxios from "@/request";

/**
 * 用户注册接口
 * @param params 用户注册信息，通常包含 username, password 等
 */
export const userRegister = async (params: any) => {
  return await myAxios.request({
    url: '/api/user/register',
    method: 'post',
    data: params,
  });
}

/**
 * 用户登录接口
 * @param params 用户登录凭证，通常包含 username, password
 */
export const userLogin = async (params: any) => {
  return await myAxios.request({
    url: '/api/user/login',
    method: 'post',
    data: params,
  });
}

/**
 * 获取当前已登录用户的信息
 */
export const getCurrentUser = async () => {
  return await myAxios.request({
    url: '/api/user/current',
    method: 'get',
  });
}

/**
 * 用户注销登录
 */
export const userLogout = async (params: any) => {
  return await myAxios.request({
    url: '/api/user/logout',
    method: 'post',
    data: params,
  });
}

/**
 * 获取用户列表（通常需要管理员权限）
 * @param params 查询参数，可包含分页信息如 pageNum, pageSize
 */
export const getUserList = async (params?: any) => { // 参数设为可选，因为可能不需要参数获取全部
  return await myAxios.request({
    url: '/api/user/list',
    method: 'get',
    params: params, // 注意：GET请求使用`params`传递查询参数，POST请求使用`data`
  });
}
/**
 * 删除用户
 * @param params 查询参数，可包含分页信息如 pageNum, pageSize
 */
export const deleteUser = async (id: string) => { // 参数设为可选，因为可能不需要参数获取全部
  return await myAxios.request({
    url: '/api/user/delete',
    method: 'POST',
    data: { id },
    headers: {
      'Content-Type': 'application/json'
    },
  });
}

/**
 * 修改用户密码（管理员功能）
 */
export const updateUserPassword = async (data: { id: string; userPassword: string }) => {
  return await myAxios.request({
    url: '/api/user/password',
    method: 'PUT',
    data,
  });
}