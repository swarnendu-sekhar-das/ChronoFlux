import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getSchemas } from '../services/api';
import { Skeleton } from '@/components/ui/skeleton';

export default function Schemas() {
  const { data: schemas, isLoading, error } = useQuery({
    queryKey: ['schemas'],
    queryFn: getSchemas,
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Event Schemas</h2>
          <p className="text-slate-400">Manage your dynamic event definitions.</p>
        </div>
        <Link 
          to="/schemas/new" 
          className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
        >
          Create Schema
        </Link>
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
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {schemas.map((schema) => (
                <tr key={schema.id} className="hover:bg-slate-800/50 transition-colors">
                  <td className="px-6 py-4 font-medium text-slate-200">{schema.name}</td>
                  <td className="px-6 py-4">{schema.description || '-'}</td>
                  <td className="px-6 py-4 text-slate-400">
                    {new Date(schema.createdAt).toLocaleDateString()}
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
