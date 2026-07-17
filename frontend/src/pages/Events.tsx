import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getEvents } from '../services/api';

const getStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case 'ACTIVE': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    case 'SCHEDULED': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    case 'DRAFT': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    case 'EXPIRED': return 'bg-red-500/10 text-red-400 border-red-500/20';
    default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
  }
};

export default function Events() {
  const { data: events, isLoading, error } = useQuery({
    queryKey: ['events'],
    queryFn: getEvents,
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Live Events</h2>
          <p className="text-slate-400">Monitor and manage all scheduled and active events.</p>
        </div>
        <Link to="/events/new" className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">
          Create Event
        </Link>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-slate-400">Loading events...</div>
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
                <th className="px-6 py-4 font-medium"></th>
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
                  <tr key={event.id} className="border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors">
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
                    <td className="px-6 py-4 text-right">
                      <Link to={`/events/${event.id}`} className="text-emerald-500 hover:text-emerald-400 text-sm font-medium">
                        View Details
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
