import { useQuery } from '@tanstack/react-query';
import { getSystemStatus, getWebhookDeliveries } from '../services/api';
import { Activity, Radio, Users, Webhook } from 'lucide-react';

export default function Dashboard() {
  const { data: status, isLoading: statusLoading } = useQuery({
    queryKey: ['systemStatus'],
    queryFn: getSystemStatus,
    refetchInterval: 5000, // Poll every 5s
  });

  const { data: deliveries, isLoading: deliveriesLoading } = useQuery({
    queryKey: ['webhookDeliveries'],
    queryFn: () => getWebhookDeliveries(0, 50),
    refetchInterval: 5000, // Poll every 5s
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-slate-100">System Dashboard</h1>
        {status?.status === 'HEALTHY' && (
          <div className="flex items-center gap-2 text-emerald-400 text-sm bg-emerald-400/10 px-3 py-1 rounded-full border border-emerald-400/20">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            SYSTEM HEALTHY
          </div>
        )}
      </div>

      {/* System Status Panel (Stat Cards) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-blue-500/10 rounded-lg text-blue-400">
              <Radio size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Active Events</p>
              <p className="text-2xl font-bold text-slate-100">
                {statusLoading ? '-' : status?.activeEvents || 0}
              </p>
            </div>
          </div>
        </div>
        
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-amber-500/10 rounded-lg text-amber-400">
              <Webhook size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Pending Webhooks</p>
              <p className="text-2xl font-bold text-slate-100">
                {statusLoading ? '-' : status?.pendingWebhooks || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-purple-500/10 rounded-lg text-purple-400">
              <Users size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Registered Admins</p>
              <p className="text-2xl font-bold text-slate-100">
                {statusLoading ? '-' : status?.totalUsers || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Activity Feed */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-800 flex items-center gap-2">
          <Activity size={20} className="text-slate-400" />
          <h2 className="text-lg font-semibold text-slate-200">Live Activity Feed</h2>
        </div>
        <div className="p-0 max-h-[500px] overflow-y-auto">
          {deliveriesLoading ? (
            <div className="p-8 text-center text-slate-500">Loading activity...</div>
          ) : deliveries?.content && deliveries.content.length > 0 ? (
            <ul className="divide-y divide-slate-800/50">
              {deliveries.content.slice(0, 50).map((d: any) => (
                <li key={d.id} className="p-4 hover:bg-slate-800/30 transition-colors flex items-start gap-4">
                  <div className={`mt-1 p-1.5 rounded-full ${
                    d.status === 'SUCCESS' ? 'bg-emerald-500/20 text-emerald-400' :
                    d.status === 'FAILED' ? 'bg-red-500/20 text-red-400' :
                    'bg-amber-500/20 text-amber-400'
                  }`}>
                    <Webhook size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-200">
                      Webhook {d.status === 'SUCCESS' ? 'Delivered' : d.status === 'FAILED' ? 'Failed' : 'Pending'}
                    </p>
                    <p className="text-xs text-slate-500 truncate mt-0.5">
                      Webhook ID: {d.webhookId} | Event: {d.eventId}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs font-mono px-2 py-1 rounded-md ${
                      d.responseCode >= 200 && d.responseCode < 300 ? 'bg-emerald-500/10 text-emerald-400' :
                      d.responseCode === 0 ? 'bg-slate-800 text-slate-400' :
                      'bg-red-500/10 text-red-400'
                    }`}>
                      {d.responseCode === 0 ? 'ERR' : d.responseCode}
                    </span>
                    <p className="text-[10px] text-slate-500 mt-1">
                      {new Date(d.lastAttemptAt || d.createdAt).toLocaleTimeString()}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className="p-8 text-center text-slate-500">No recent activity</div>
          )}
        </div>
      </div>
    </div>
  );
}
