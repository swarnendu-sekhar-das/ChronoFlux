import { useState, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { getSchemas, createEvent } from '../services/api';
import DynamicFormEngine from '../components/DynamicFormEngine';

// Build a dynamic Zod schema based on the selected EventSchema's JSON definition
const buildZodSchema = (schemaDefinition: Record<string, string> | null) => {
  if (!schemaDefinition) return z.object({});

  const shape: Record<string, any> = {};
  
  Object.entries(schemaDefinition).forEach(([key, type]) => {
    switch (String(type).toLowerCase()) {
      case 'string':
        shape[key] = z.string().min(1, 'Required');
        break;
      case 'number':
        shape[key] = z.number();
        break;
      case 'boolean':
        shape[key] = z.boolean().default(false);
        break;
      default:
        shape[key] = z.any();
    }
  });

  return z.object(shape);
};

export default function CreateEvent() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedSchemaId, setSelectedSchemaId] = useState<string>('');

  const { data: schemas, isLoading: schemasLoading } = useQuery({
    queryKey: ['schemas'],
    queryFn: getSchemas,
  });

  const selectedSchema = useMemo(() => {
    return schemas?.find((s) => s.id === selectedSchemaId) || null;
  }, [schemas, selectedSchemaId]);

  // Dynamically create the Zod schema for the payload whenever the selected schema changes
  const dynamicPayloadSchema = useMemo(() => {
    return buildZodSchema(selectedSchema?.schemaDefinition || null);
  }, [selectedSchema]);

  // Master form schema (static fields + dynamic payload)
  const masterFormSchema = useMemo(() => z.object({
    title: z.string().min(1, 'Title is required'),
    status: z.enum(['DRAFT', 'SCHEDULED', 'ACTIVE']),
    activeFrom: z.string().optional().or(z.literal('')),
    activeUntil: z.string().optional().or(z.literal('')),
    payload: dynamicPayloadSchema,
  }), [dynamicPayloadSchema]);

  type FormValues = z.infer<typeof masterFormSchema>;

  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormValues>({
    resolver: zodResolver(masterFormSchema),
    defaultValues: {
      status: 'DRAFT',
    }
  });

  // Reset form when schema changes to clear old dynamic data and errors
  const handleSchemaChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedSchemaId(e.target.value);
    reset({
      title: '',
      status: 'DRAFT',
      activeFrom: '',
      activeUntil: '',
      payload: {},
    });
  };

  const mutation = useMutation({
    mutationFn: createEvent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      navigate('/events');
    },
  });

  const onSubmit = (data: FormValues) => {
    // Transform empty strings to null for ISO date fields before sending to API
    const payloadToSubmit = {
      schema: { id: selectedSchemaId },
      title: data.title,
      status: data.status,
      payload: data.payload,
      activeFrom: data.activeFrom ? new Date(data.activeFrom).toISOString() : null,
      activeUntil: data.activeUntil ? new Date(data.activeUntil).toISOString() : null,
    };
    mutation.mutate(payloadToSubmit);
  };

  if (schemasLoading) return <div className="p-8 text-slate-400">Loading dependencies...</div>;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Create Live Event</h2>
        <p className="text-slate-400">Schedule or activate a new event from an existing schema.</p>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-lg p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          
          {/* Top Section: Static Master Fields */}
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="block text-sm font-medium text-slate-300 mb-1">Select Schema</label>
              <select
                value={selectedSchemaId}
                onChange={handleSchemaChange}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="" disabled>Select a schema to configure payload...</option>
                {schemas?.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>

            {selectedSchemaId && (
              <>
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-slate-300 mb-1">Event Title</label>
                  <input
                    {...register('title')}
                    className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="e.g. Weekend Warrior Event"
                  />
                  {errors.title && <p className="text-red-400 text-xs mt-1">{errors.title.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Status</label>
                  <select
                    {...register('status')}
                    className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    <option value="DRAFT">Draft</option>
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="ACTIVE">Active (Immediate)</option>
                  </select>
                </div>

                <div className="col-span-2 grid grid-cols-2 gap-4 border-t border-slate-800 pt-4 mt-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Active From</label>
                    <input
                      type="datetime-local"
                      {...register('activeFrom')}
                      className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Active Until</label>
                    <input
                      type="datetime-local"
                      {...register('activeUntil')}
                      className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Bottom Section: Dynamic Payload Engine */}
          {selectedSchema && (
            <div className="mt-8 border-t border-slate-800 pt-6">
              <h3 className="text-lg font-semibold text-emerald-400 mb-4">
                Dynamic Configuration ({selectedSchema.name})
              </h3>
              
              <DynamicFormEngine 
                schemaDefinition={selectedSchema.schemaDefinition}
                register={register}
                errors={errors}
              />
            </div>
          )}

          {mutation.isError && (
            <div className="p-3 bg-red-900/50 border border-red-800 rounded text-red-200 text-sm">
              Failed to create event. Please verify backend status and payload.
            </div>
          )}

          {selectedSchemaId && (
            <div className="flex justify-end gap-3 pt-6 border-t border-slate-800">
              <button
                type="button"
                onClick={() => navigate('/events')}
                className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting || mutation.isPending}
                className="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
              >
                {isSubmitting || mutation.isPending ? 'Saving Event...' : 'Save Event'}
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
