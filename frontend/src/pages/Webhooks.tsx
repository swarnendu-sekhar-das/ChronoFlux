import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  getWebhooks, 
  createWebhook, 
  toggleWebhook, 
  getWebhookDeliveries, 
  getSchemas 
} from '../services/api';

export default function Webhooks() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [targetUrl, setTargetUrl] = useState('');
  const [schemaId, setSchemaId] = useState('');

  const { data: webhooks, isLoading: isLoadingWebhooks } = useQuery({
    queryKey: ['webhooks'],
    queryFn: getWebhooks
  });

  const { data: schemas } = useQuery({
    queryKey: ['schemas'],
    queryFn: getSchemas
  });

  const { data: deliveries, isLoading: isLoadingDeliveries } = useQuery({
    queryKey: ['webhookDeliveries'],
    queryFn: getWebhookDeliveries,
    refetchInterval: 5000 // Poll deliveries to see results
  });

  const createMutation = useMutation({
    mutationFn: createWebhook,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
      setIsModalOpen(false);
      setTargetUrl('');
      setSchemaId('');
    }
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string, isActive: boolean }) => toggleWebhook(id, isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (targetUrl && schemaId) {
      createMutation.mutate({ schemaId, targetUrl });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight text-white">Webhooks</h2>
          <p className="text-slate-400 mt-1">Manage game server webhook registrations and monitor deliveries.</p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-md font-medium transition-colors"
        >
          Register Webhook
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Webhooks List */}
        <div className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-800 flex justify-between items-center">
            <h3 className="text-lg font-medium text-slate-200">Registered Endpoints</h3>
          </div>
          
          <div className="p-0 overflow-auto max-h-[500px]">
            {isLoadingWebhooks ? (
               <div className="p-6 text-slate-400">Loading webhooks...</div>
            ) : webhooks?.length === 0 ? (
               <div className="p-6 text-slate-500 text-center">No webhooks registered.</div>
            ) : (
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-950/50 text-xs uppercase text-slate-500 border-b border-slate-800">
                  <tr>
                    <th className="px-6 py-3 font-medium">Target URL</th>
                    <th className="px-6 py-3 font-medium">Schema</th>
                    <th className="px-6 py-3 font-medium text-right">Active</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/50">
                  {webhooks?.map(wh => {
                    const schema = schemas?.find(s => s.id === wh.schemaId);
                    return (
                      <tr key={wh.id} className="hover:bg-slate-800/20">
                        <td className="px-6 py-4 font-mono text-xs text-emerald-400 truncate max-w-[200px]" title={wh.targetUrl}>
                          {wh.targetUrl}
                        </td>
                        <td className="px-6 py-4">{schema ? schema.name : 'Unknown Schema'}</td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => toggleMutation.mutate({ id: wh.id, isActive: !wh.isActive })}
                            className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors focus:outline-none ${wh.isActive ? 'bg-emerald-500' : 'bg-slate-600'}`}
                          >
                            <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${wh.isActive ? 'translate-x-5' : 'translate-x-1'}`} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Delivery Logs */}
        <div className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-800 flex justify-between items-center">
            <h3 className="text-lg font-medium text-slate-200">Delivery Logs</h3>
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
          </div>
          <div className="p-0 overflow-auto max-h-[500px]">
             {isLoadingDeliveries ? (
               <div className="p-6 text-slate-400">Loading delivery logs...</div>
            ) : deliveries?.length === 0 ? (
               <div className="p-6 text-slate-500 text-center">No deliveries attempted yet.</div>
            ) : (
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-950/50 text-xs uppercase text-slate-500 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Code</th>
                    <th className="px-4 py-3 font-medium">Attempts</th>
                    <th className="px-4 py-3 font-medium">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/50">
                  {deliveries?.map(del => (
                    <tr key={del.id} className="hover:bg-slate-800/20">
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 text-xs font-semibold rounded ${
                          del.status === 'SUCCESS' ? 'bg-emerald-500/10 text-emerald-400' :
                          del.status === 'FAILED' ? 'bg-red-500/10 text-red-400' :
                          'bg-amber-500/10 text-amber-400'
                        }`}>
                          {del.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-mono text-xs text-slate-400">{del.responseCode || '-'}</td>
                      <td className="px-4 py-3">{del.attemptCount}</td>
                      <td className="px-4 py-3 text-xs text-slate-400">
                        {del.lastAttemptAt ? new Date(del.lastAttemptAt).toLocaleString() : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* Registration Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-lg shadow-xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-800">
              <h3 className="text-lg font-medium text-slate-200">Register Webhook</h3>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Event Schema</label>
                <select
                  required
                  value={schemaId}
                  onChange={e => setSchemaId(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="" disabled>Select a schema...</option>
                  {schemas?.map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Target URL</label>
                <input
                  type="url"
                  required
                  value={targetUrl}
                  onChange={e => setTargetUrl(e.target.value)}
                  placeholder="https://game-server.com/webhook"
                  className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500 placeholder:text-slate-600"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-md font-medium text-sm transition-colors disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Saving...' : 'Save Webhook'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
