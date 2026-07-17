import type { UseFormRegister, FieldErrors } from 'react-hook-form';

interface DynamicFormEngineProps {
  schemaDefinition: Record<string, string>;
  register: UseFormRegister<any>;
  errors: FieldErrors<any>;
}

export default function DynamicFormEngine({ schemaDefinition, register, errors }: DynamicFormEngineProps) {
  if (!schemaDefinition || Object.keys(schemaDefinition).length === 0) {
    return <div className="text-slate-500 text-sm italic">No schema definition found.</div>;
  }

  // Cast payload errors to a generic record to avoid TS indexing errors
  const payloadErrors = errors.payload as Record<string, any> | undefined;

  return (
    <div className="space-y-4">
      {Object.entries(schemaDefinition).map(([key, type]) => {
        // Build the correct input based on the expected type
        return (
          <div key={key}>
            <label className="block text-sm font-medium text-slate-300 mb-1 capitalize">
              {key} <span className="text-slate-500 text-xs ml-1">({type})</span>
            </label>
            
            {String(type).toLowerCase() === 'boolean' ? (
              <div className="flex items-center mt-2 h-10">
                <input
                  type="checkbox"
                  {...register(`payload.${key}`)}
                  className="w-4 h-4 text-emerald-600 bg-slate-900 border-slate-700 rounded focus:ring-emerald-500 focus:ring-2 cursor-pointer"
                />
                <span className="ml-2 text-sm text-slate-400">Enabled</span>
              </div>
            ) : String(type).toLowerCase() === 'number' ? (
              <input
                type="number"
                step="any"
                {...register(`payload.${key}`, { valueAsNumber: true })}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors"
                placeholder={`Enter a number...`}
              />
            ) : (
              <input
                type="text"
                {...register(`payload.${key}`)}
                className="w-full bg-slate-950 border border-slate-800 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors"
                placeholder={`Enter text...`}
              />
            )}
            
            {/* Display validation error if any */}
            {payloadErrors?.[key] && (
              <p className="text-red-400 text-xs mt-1">{payloadErrors[key]?.message}</p>
            )}
          </div>
        );
      })}
    </div>
  );
}
