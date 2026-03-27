import api from './axios';

export const getMyWeeklyReport = (date) =>
    api.get('/reports/me/weekly', { params: { date } });

export const getChildWeeklyReport = (childId, date) =>
    api.get(`/reports/children/${childId}/weekly`, { params: { date } });
