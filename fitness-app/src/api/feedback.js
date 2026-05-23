import request from './request';

export const submitFeedback = (content) => request.post('/feedback', { content });
