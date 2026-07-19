import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getEventById } from '../services/api';

const getStatusColor = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'ACTIVE': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    case 'SCHEDULED': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    case 'DRAFT': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    case 'EXPIRED': return 'bg-red-500/10 text-red-400 border-red-500/20';
    default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
  }
};

export default function EventDetail() {
  const { id } = useParams<{ id: string }>();

  const { data: event, isLoading, error } = useQuery({
    queryKey: ['event', id],
    queryFn: () => getEventById(id!),
    enabled: !!id,
    // Smart Polling: If the event is scheduled, check every 5 seconds to see if it flipped to ACTIVE.
    refetchInterval: (query) => {
      if (query.state.data?.status === 'SCHEDULED') {
        return 5000;
      }
      return false; // Stop polling once active, expired, or draft
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 animate-pulse">
        <div className="h-10 bg-slate-800 rounded w-1/3"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="col-span-1 h-64 bg-slate-900 border border-slate-800 rounded-lg"></div>
          <div className="col-span-1 md:col-span-2 h-64 bg-slate-950 border border-slate-800 rounded-lg"></div>
        </div>
      </div>
    );
  }
  if (error || !event) return <div className="p-8 text-red-400">Error loading event details.</div>;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-4">
            <h2 className="text-3xl font-bold tracking-tight text-white">{event.title}</h2>
            <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${getStatusColor(event.status)}`}>
              {event.status}
            </span>
          </div>
          <p className="text-slate-400 mt-1 font-mono text-sm">ID: {event.id}</p>
        </div>
        <Link to="/events" className="text-emerald-500 hover:text-emerald-400 text-sm font-medium transition-colors">
          &larr; Back to Events
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Column: Metadata */}
        <div className="col-span-1 space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-lg p-5 space-y-4">
            <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-2">Schedule</h3>
            
            <div>
              <p className="text-xs text-slate-500 mb-1">Active From</p>
              <p className="text-sm text-slate-200">
                {event.activeFrom ? new Date(event.activeFrom).toLocaleString() : 'Not Scheduled'}
              </p>
            </div>
            
            <div>
              <p className="text-xs text-slate-500 mb-1">Active Until</p>
              <p className="text-sm text-slate-200">
                {event.activeUntil ? new Date(event.activeUntil).toLocaleString() : 'Indefinite'}
              </p>
            </div>

            <div className="pt-4 border-t border-slate-800">
              <p className="text-xs text-slate-500 mb-1">Created At</p>
              <p className="text-sm text-slate-400">
                {new Date(event.createdAt).toLocaleString()}
              </p>
            </div>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-lg p-5">
             <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-2">Activity Feed</h3>
             <div className="space-y-3">
                <div className="text-sm text-slate-400">
                  <span className="text-emerald-400 mr-2">•</span>Event Created
                </div>
                {/* Webhook logs will go here on Day 10 */}
                <div className="text-sm text-slate-500 italic mt-4">
                  Waiting for webhook activity...
                </div>
             </div>
          </div>
        </div>

        {/* Right Column: Payload */}
        <div className="col-span-1 md:col-span-2">
          <div className="bg-slate-950 border border-slate-800 rounded-lg overflow-hidden h-full">
            <div className="bg-slate-900/50 border-b border-slate-800 px-4 py-3">
              <h3 className="text-sm font-semibold text-slate-300">Live JSON Payload</h3>
            </div>
            <div className="p-4 overflow-auto">
              <pre className="text-emerald-400 font-mono text-sm leading-relaxed">
                {JSON.stringify((event as any).payload, null, 2)}
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
