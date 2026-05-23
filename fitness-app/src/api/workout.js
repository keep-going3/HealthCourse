import request from './request';

export const saveWorkout = (data) => request.post('/workout/save', data);
export const updateWorkout = (data) => request.put('/workout/update', data);
export const getWorkoutDetail = (date) => request.get('/workout/detail', { params: { date } });
export const getWorkoutCalendar = (year, month) =>
  request.get('/workout/calendar', { params: { year, month } });
export const deleteWorkout = (sessionId) => request.delete(`/workout/${sessionId}`);

export const getExerciseLibrary = () => request.get('/exercise/library');
export const getExerciseLibraryGroup = () => request.get('/exercise/library/group');
export const addCustomExercise = (data) => request.post('/exercise/custom', data);
export const updateCustomExercise = (id, data) => request.put(`/exercise/custom/${id}`, data);
export const deleteCustomExercise = (id) => request.delete(`/exercise/custom/${id}`);
