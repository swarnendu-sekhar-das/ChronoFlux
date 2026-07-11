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
