import axios from 'axios';

export const api = axios.create({
  // Vite proxy will redirect to backend
  baseURL: '',
  withCredentials: true,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export interface Schema {
  id: string;
  name: string;
  description?: string;
  schemaDefinition: any;
  archived?: boolean;
  createdAt: string;
}

export const getSchemas = async (includeArchived?: boolean | any): Promise<Schema[]> => {
  const include = typeof includeArchived === 'boolean' ? includeArchived : false;
  const response = await api.get('/api/admin/schemas', { params: { includeArchived: include } });
  return response.data;
};

export const createSchema = async (data: { name: string, description: string, schemaDefinition: any }): Promise<Schema> => {
  const response = await api.post('/api/admin/schemas', data);
  return response.data;
};

export const archiveSchema = async (id: string): Promise<Schema> => {
  const response = await api.put(`/api/admin/schemas/${id}/archive`);
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

export const getEvents = async (includeArchived?: boolean | any): Promise<Event[]> => {
  const include = typeof includeArchived === 'boolean' ? includeArchived : false;
  const response = await api.get('/api/admin/events', { params: { includeArchived: include } });
  return response.data.content || [];
};

export const getEventById = async (id: string): Promise<Event> => {
  const response = await api.get(`/api/admin/events/${id}`);
  return response.data;
};

export const getActiveEvents = async (): Promise<Event[]> => {
  const response = await api.get('/api/v1/events/active');
  return response.data.content || [];
};

export const createEvent = async (data: any): Promise<Event> => {
  const response = await api.post('/api/admin/events', data);
  return response.data;
};

export const updateEvent = async (id: string, data: any): Promise<Event> => {
  const response = await api.put(`/api/admin/events/${id}`, data);
  return response.data;
};

export const archiveEvent = async (id: string): Promise<Event> => {
  const response = await api.put(`/api/admin/events/${id}/archive`);
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

export interface SystemStatus {
  activeEvents: number;
  pendingWebhooks: number;
  totalUsers: number;
  status: string;
}

export const getSystemStatus = async (): Promise<SystemStatus> => {
  const response = await api.get('/api/admin/system/status');
  return response.data;
};
