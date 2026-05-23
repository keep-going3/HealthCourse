import request from './request';

export const getBodyInfo = () => request.get('/body/info');
export const updateBodyInfo = (data) => request.put('/body/info', data);
export const recordWeight = (data) => request.post('/body/weight', data);
export const getWeightList = (months) =>
  request.get('/body/weight/list', { params: { months } });
export const updateWeight = (id, data) => request.put(`/body/weight/${id}`, data);
export const deleteWeight = (id) => request.delete(`/body/weight/${id}`);
