import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getEvents, archiveEvent } from '../services/api';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';

const getStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case 'ACTIVE': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    case 'SCHEDULED': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    case 'DRAFT': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    case 'EXPIRED': return 'bg-red-500/10 text-red-400 border-red-500/20';
    case 'ARCHIVED': return 'bg-zinc-500/10 text-zinc-500 border-zinc-500/20';
    default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
  }
};

export default function Events() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [showArchived, setShowArchived] = useState(false);
  const [page, setPage] = useState(0);

  const { data: pageData, isLoading, error } = useQuery({
    queryKey: ['events', showArchived, page],
    queryFn: () => getEvents(showArchived, page, 10),
  });

  const events = pageData?.content;

  const archiveMutation = useMutation({
    mutationFn: archiveEvent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast({
        title: "Archived",
        description: "Event has been archived successfully.",
      });
    },
    onError: (err: any) => {
      toast({
        title: "Error",
        description: err.response?.data?.message || "Failed to archive event.",
        variant: "destructive",
      });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Live Events</h2>
          <p className="text-slate-400">Monitor and manage all scheduled and active events.</p>
        </div>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-400 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={showArchived}
              onChange={(e) => setShowArchived(e.target.checked)}
              className="rounded border-slate-600 bg-slate-800 text-emerald-500 focus:ring-emerald-500/20"
            />
            Show Archived
          </label>
          <Link to="/events/new" className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">
            Create Event
          </Link>
        </div>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-8 w-full bg-slate-800" />
            <Skeleton className="h-12 w-full bg-slate-800" />
            <Skeleton className="h-12 w-full bg-slate-800" />
            <Skeleton className="h-12 w-full bg-slate-800" />
          </div>
        ) : error ? (
          <div className="p-8 text-center text-red-400">Failed to load events. Is the backend running?</div>
        ) : !events || events.length === 0 ? (
          <div className="p-8 text-center text-slate-400">No events found.</div>
        ) : (
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-950/50 text-xs uppercase text-slate-500 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4 font-medium">Event</th>
                <th className="px-6 py-4 font-medium">Schema</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium">Active From</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {events?.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-slate-500">
                    No live events found. Create one to get started.
                  </td>
                </tr>
              ) : (
                events?.map((event) => (
                  <tr key={event.id} className={`border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors ${event.status === 'ARCHIVED' ? 'opacity-60' : ''}`}>
                    <td className="px-6 py-4">
                      <Link to={`/events/${event.id}`} className="font-medium text-slate-200 hover:text-emerald-400 transition-colors">
                        {event.title || 'Untitled Event'}
                      </Link>
                      <div className="text-xs text-slate-500 mt-1">{event.id}</div>
                    </td>
                    <td className="px-6 py-4 text-slate-400">
                      {event.schemaName || event.schemaId}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${getStatusColor(event.status)}`}>
                        {event.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-400">
                      {event.activeFrom ? new Date(event.activeFrom).toLocaleString() : '-'}
                    </td>
                    <td className="px-6 py-4 text-right space-x-3">
                      <Link to={`/events/${event.id}`} className="text-emerald-500 hover:text-emerald-400 text-sm font-medium">
                        View Details
                      </Link>
                      {event.status !== 'ARCHIVED' && (
                        <button
                          onClick={() => archiveMutation.mutate(event.id)}
                          disabled={archiveMutation.isPending}
                          className="text-amber-400 hover:text-amber-300 text-xs font-medium px-2 py-1 rounded hover:bg-amber-500/10 transition-colors disabled:opacity-50"
                        >
                          Archive
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
        {pageData && pageData.totalPages > 0 && (
          <div className="flex justify-between items-center px-6 py-4 border-t border-slate-800">
            <button 
              disabled={pageData.number === 0} 
              onClick={() => setPage(p => p - 1)}
              className="text-emerald-500 disabled:opacity-50 text-sm font-medium hover:text-emerald-400 transition-colors">
              &larr; Previous
            </button>
            <span className="text-slate-400 text-sm">
              Page {pageData.number + 1} of {pageData.totalPages}
            </span>
            <button 
              disabled={pageData.number >= pageData.totalPages - 1} 
              onClick={() => setPage(p => p + 1)}
              className="text-emerald-500 disabled:opacity-50 text-sm font-medium hover:text-emerald-400 transition-colors">
              Next &rarr;
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
