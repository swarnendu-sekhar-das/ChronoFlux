import { useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createSchema } from '../services/api';
import { useToast } from '@/hooks/use-toast';

type SchemaFormData = {
  name: string;
  description: string;
  schemaDefinitionStr: string;
};

export default function CreateSchema() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } = useForm<SchemaFormData>();

  const mutation = useMutation({
    mutationFn: createSchema,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schemas'] });
      toast({
        title: "Schema Created",
        description: "Your new schema was successfully saved.",
      });
      navigate('/schemas');
    },
    onError: () => {
      toast({
        variant: "destructive",
        title: "Error",
        description: "Failed to create schema.",
      });
    }
  });

  const onSubmit = (data: SchemaFormData) => {
    try {
      // Validate JSON safely before sending
      const parsedSchema = JSON.parse(data.schemaDefinitionStr);
      
      mutation.mutate({
        name: data.name,
        description: data.description,
        schemaDefinition: parsedSchema,
      });
    } catch (e) {
      setError('schemaDefinitionStr', {
        type: 'manual',
        message: 'Invalid JSON format. Please check your syntax.',
      });
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Create Schema</h2>
        <p className="text-slate-400">Define a new dynamic event type.</p>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-lg p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">Schema Name</label>
            <input
              {...register('name', { required: 'Name is required' })}
              className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="e.g. Flash Sale"
            />
            {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">Description</label>
            <input
              {...register('description')}
              className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="Short description..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">JSON Schema Definition</label>
            <textarea
              {...register('schemaDefinitionStr', { required: 'Schema Definition is required' })}
              className="w-full h-48 bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder={'{\n  "title": "String",\n  "discount": "Number"\n}'}
            />
            {errors.schemaDefinitionStr && <p className="text-red-400 text-xs mt-1">{errors.schemaDefinitionStr.message}</p>}
          </div>

          {mutation.isError && (
            <div className="p-3 bg-red-900/50 border border-red-800 rounded text-red-200 text-sm">
              Failed to create schema. Please check if the backend is running.
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={() => navigate('/')}
              className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || mutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
            >
              {isSubmitting || mutation.isPending ? 'Saving...' : 'Save Schema'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
