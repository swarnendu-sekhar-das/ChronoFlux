import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getEventById, updateEvent, archiveEvent } from '../services/api';
import { useToast } from '@/hooks/use-toast';

const getStatusColor = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'ACTIVE': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    case 'SCHEDULED': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    case 'DRAFT': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    case 'EXPIRED': return 'bg-red-500/10 text-red-400 border-red-500/20';
    case 'ARCHIVED': return 'bg-zinc-500/10 text-zinc-500 border-zinc-500/20';
    default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
  }
};

export default function EventDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const [isEditing, setIsEditing] = useState(false);
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState('DRAFT');
  const [activeFrom, setActiveFrom] = useState('');
  const [activeUntil, setActiveUntil] = useState('');
  const [payloadStr, setPayloadStr] = useState('{}');
  const [jsonError, setJsonError] = useState<string | null>(null);

  const { data: event, isLoading, error } = useQuery({
    queryKey: ['event', id],
    queryFn: () => getEventById(id!),
    enabled: !!id,
    refetchInterval: (query) => {
      if (query.state.data?.status === 'SCHEDULED') {
        return 5000;
      }
      return false;
    },
  });

  useEffect(() => {
    if (event) {
      setTitle(event.title || '');
      setStatus(event.status || 'DRAFT');
      setActiveFrom(event.activeFrom ? event.activeFrom.slice(0, 16) : '');
      setActiveUntil(event.activeUntil ? event.activeUntil.slice(0, 16) : '');
      setPayloadStr(JSON.stringify(event.payload || {}, null, 2));
    }
  }, [event]);

  const updateMutation = useMutation({
    mutationFn: (data: any) => updateEvent(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event', id] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      setIsEditing(false);
      toast({
        title: "Success",
        description: "Event updated successfully.",
      });
    },
    onError: (err: any) => {
      toast({
        title: "Error",
        description: err.response?.data?.message || "Failed to update event.",
        variant: "destructive",
      });
    },
  });

  const archiveMutation = useMutation({
    mutationFn: () => archiveEvent(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['event', id] });
      toast({
        title: "Archived",
        description: "Event has been archived successfully.",
      });
      navigate('/events');
    },
    onError: (err: any) => {
      toast({
        title: "Error",
        description: err.response?.data?.message || "Failed to archive event.",
        variant: "destructive",
      });
    },
  });

  const handleUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    setJsonError(null);
    let parsedPayload = {};
    try {
      parsedPayload = JSON.parse(payloadStr);
    } catch (err) {
      setJsonError("Invalid JSON string in payload.");
      return;
    }

    updateMutation.mutate({
      title,
      status,
      payload: parsedPayload,
      activeFrom: activeFrom ? new Date(activeFrom).toISOString() : null,
      activeUntil: activeUntil ? new Date(activeUntil).toISOString() : null,
    });
  };

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

  const isArchived = event.status === 'ARCHIVED';

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {isArchived && (
        <div className="bg-zinc-800/50 border border-zinc-700 rounded-lg px-4 py-3 flex items-center gap-3">
          <span className="text-zinc-400 text-sm">📦</span>
          <span className="text-zinc-300 text-sm font-medium">This event has been archived and is no longer visible to game clients.</span>
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-4">
            <h2 className={`text-3xl font-bold tracking-tight ${isArchived ? 'text-zinc-500' : 'text-white'}`}>{event.title}</h2>
            <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${getStatusColor(event.status)}`}>
              {event.status}
            </span>
          </div>
          <p className="text-slate-400 mt-1 font-mono text-sm">ID: {event.id}</p>
        </div>
        <div className="flex items-center gap-3">
          {!isArchived && (
            <>
              <button
                onClick={() => setIsEditing(!isEditing)}
                className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              >
                {isEditing ? 'Cancel Edit' : 'Edit Event'}
              </button>
              <button
                onClick={() => {
                  if (window.confirm('Are you sure you want to archive this event? It will no longer be visible to game clients.')) {
                    archiveMutation.mutate();
                  }
                }}
                disabled={archiveMutation.isPending}
                className="bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors disabled:opacity-50"
              >
                Archive Event
              </button>
            </>
          )}
          <Link to="/events" className="text-emerald-500 hover:text-emerald-400 text-sm font-medium transition-colors ml-2">
            &larr; Back to Events
          </Link>
        </div>
      </div>

      {isEditing ? (
        <form onSubmit={handleUpdate} className="bg-slate-900 border border-slate-800 rounded-lg p-6 space-y-6">
          <h3 className="text-lg font-bold text-white">Edit Event Details</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 uppercase mb-1">Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 uppercase mb-1">Status</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
              >
                <option value="DRAFT">DRAFT</option>
                <option value="SCHEDULED">SCHEDULED</option>
                {event.status === 'ACTIVE' && <option value="ACTIVE">ACTIVE</option>}
              </select>
            </div>
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-medium text-slate-400 uppercase">Active From</label>
                <button
                  type="button"
                  onClick={() => {
                    setStatus('SCHEDULED');
                    setActiveFrom(new Date().toISOString().slice(0, 16));
                  }}
                  className="text-xs text-emerald-400 hover:text-emerald-300 font-medium transition-colors"
                >
                  ⚡ Set to Now
                </button>
              </div>
              <input
                type="datetime-local"
                value={activeFrom}
                onChange={(e) => setActiveFrom(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 uppercase mb-1">Active Until</label>
              <input
                type="datetime-local"
                value={activeUntil}
                onChange={(e) => setActiveUntil(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase mb-1">Payload (JSON)</label>
            <textarea
              rows={8}
              value={payloadStr}
              onChange={(e) => setPayloadStr(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-md p-3 text-sm font-mono text-emerald-400 focus:outline-none focus:border-emerald-500"
            />
            {jsonError && <p className="text-red-400 text-xs mt-1">{jsonError}</p>}
          </div>

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-md text-sm font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={updateMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors disabled:opacity-50"
            >
              {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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
                  <div className="text-sm text-slate-500 italic mt-4">
                    Waiting for webhook activity...
                  </div>
               </div>
            </div>
          </div>

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
      )}
    </div>
  );
}
