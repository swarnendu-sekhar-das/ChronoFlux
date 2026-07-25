import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getSchemas, archiveSchema } from '../services/api';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';

export default function Schemas() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [showArchived, setShowArchived] = useState(false);

  const { data: schemas, isLoading, error } = useQuery({
    queryKey: ['schemas', showArchived],
    queryFn: () => getSchemas(showArchived),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveSchema,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schemas'] });
      toast({
        title: "Archived",
        description: "Schema has been archived successfully.",
      });
    },
    onError: (err: any) => {
      toast({
        title: "Error",
        description: err.response?.data?.message || "Failed to archive schema.",
        variant: "destructive",
      });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Event Schemas</h2>
          <p className="text-slate-400">Manage your dynamic event definitions.</p>
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
          <Link 
            to="/schemas/new" 
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
          >
            Create Schema
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
          <div className="p-8 text-center text-red-400">Failed to load schemas. Is the backend running?</div>
        ) : !schemas || schemas.length === 0 ? (
          <div className="p-8 text-center text-slate-400">No schemas found. Create one to get started.</div>
        ) : (
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-950/50 text-xs uppercase text-slate-500 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4 font-medium">Name</th>
                <th className="px-6 py-4 font-medium">Description</th>
                <th className="px-6 py-4 font-medium">Created At</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {schemas.map((schema) => (
                <tr key={schema.id} className={`hover:bg-slate-800/50 transition-colors ${schema.archived ? 'opacity-60' : ''}`}>
                  <td className="px-6 py-4 font-medium text-slate-200">
                    {schema.name}
                    {schema.archived && (
                      <span className="ml-2 px-2 py-0.5 text-xs font-medium rounded-full bg-zinc-500/10 text-zinc-500 border border-zinc-500/20">
                        Archived
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4">{schema.description || '-'}</td>
                  <td className="px-6 py-4 text-slate-400">
                    {new Date(schema.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-right">
                    {!schema.archived && (
                      <button
                        onClick={() => archiveMutation.mutate(schema.id)}
                        disabled={archiveMutation.isPending}
                        className="text-amber-400 hover:text-amber-300 text-xs font-medium px-2 py-1 rounded hover:bg-amber-500/10 transition-colors disabled:opacity-50"
                      >
                        Archive
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
