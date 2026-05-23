import request from './request';

export const chatStream = (message) =>
  request.post('/agent/chat/stream', { message }, { responseType: 'stream' });
export const parseWorkout = (text) => request.post('/agent/parse-workout', { text });
