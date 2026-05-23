import request from './request';

export const login = (data) => request.post('/user/login', data);
export const register = (data) => request.post('/user/register', data);
export const refreshToken = () => request.post('/user/refresh-token');
export const changePassword = (data) => request.put('/user/password', data);
export const logout = () => request.post('/user/logout');
