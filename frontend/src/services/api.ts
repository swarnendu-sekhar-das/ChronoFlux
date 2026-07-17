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
  title: string;
  payload: any;
  status: string;
  activeFrom?: string;
  activeUntil?: string;
  createdAt: string;
}

export const getEvents = async (): Promise<Event[]> => {
  const response = await api.get('/api/admin/events');
  return response.data;
};

export const getEventById = async (id: string): Promise<Event> => {
  const response = await api.get(`/api/admin/events/${id}`);
  return response.data;
};

export const createEvent = async (data: any): Promise<Event> => {
  const response = await api.post('/api/admin/events', data);
  return response.data;
};

export interface Webhook {
  id: string;
  schemaId: string;
  targetUrl: string;
  isActive: boolean;
  createdAt: string;
}

export interface WebhookDelivery {
  id: string;
  webhookId: string;
  eventId: string;
  status: string;
  responseCode: number;
  attemptCount: number;
  lastAttemptAt: string;
  createdAt: string;
}

export const getWebhooks = async (): Promise<Webhook[]> => {
  const response = await api.get('/api/admin/webhooks');
  return response.data;
};

export const createWebhook = async (data: { schemaId: string, targetUrl: string }): Promise<Webhook> => {
  const response = await api.post('/api/admin/webhooks', data);
  return response.data;
};

export const toggleWebhook = async (id: string, isActive: boolean): Promise<Webhook> => {
  const response = await api.put(`/api/admin/webhooks/${id}/toggle`, { isActive });
  return response.data;
};

export const getWebhookDeliveries = async (): Promise<WebhookDelivery[]> => {
  const response = await api.get('/api/admin/webhooks/deliveries');
  return response.data;
};
