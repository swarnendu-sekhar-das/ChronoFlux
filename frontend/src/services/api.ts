import axios from 'axios';

export const api = axios.create({
  // Vite proxy will redirect to backend
  baseURL: '',
  withCredentials: true,
});

export interface Schema {
  id: string;
  name: string;
  description?: string;
  schemaDefinition: any;
  createdAt: string;
}

export const getSchemas = async (): Promise<Schema[]> => {
  const response = await api.get('/api/admin/schemas');
  return response.data;
};

export const createSchema = async (data: { name: string, description: string, schemaDefinition: any }): Promise<Schema> => {
  const response = await api.post('/api/admin/schemas', data);
  return response.data;
};

export interface Event {
  id: string;
  schemaId: string;
  schemaName?: string;
  status: string;
  activeFrom?: string;
  activeUntil?: string;
  createdAt: string;
}

export const getEvents = async (): Promise<Event[]> => {
  const response = await api.get('/api/admin/events');
  return response.data;
};
