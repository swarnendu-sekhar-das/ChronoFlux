import { useQuery } from '@tanstack/react-query';
import { getActiveEvents } from '../services/api';
import { RefreshCw } from 'lucide-react';

export default function Simulator() {
  const { data: activeEvents, isLoading, isError, error } = useQuery({
    queryKey: ['activeEvents'],
    queryFn: getActiveEvents,
    refetchInterval: 3000, // Poll every 3 seconds
  });

  const hasActiveEvents = activeEvents && activeEvents.length > 0;
  
  // Immersive Gaming UI styles
  if (isLoading && !activeEvents) {
    return (
      <div className="flex h-[80vh] flex-col items-center justify-center bg-black text-emerald-500 font-mono">
        <RefreshCw className="w-12 h-12 animate-spin mb-4" />
        <h2 className="text-2xl tracking-[0.2em] uppercase animate-pulse">Initializing Simulator Core...</h2>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex h-[80vh] flex-col items-center justify-center bg-black text-red-500 font-mono">
        <h2 className="text-2xl tracking-[0.2em] uppercase">Connection Error</h2>
        <p className="mt-2 text-red-400/80">Failed to sync with Edge API. Is it online?</p>
        <p className="mt-2 text-xs text-red-400/50">{String(error)}</p>
      </div>
    );
  }

  return (
    <div className={`flex flex-col min-h-[80vh] transition-colors duration-1000 ${hasActiveEvents ? 'bg-red-950/20' : 'bg-black'} p-8 border ${hasActiveEvents ? 'border-red-900/50' : 'border-emerald-900/30'} rounded-xl relative overflow-hidden`}>
      
      {/* Background Grid/Scanlines for sci-fi feel */}
      <div className="absolute inset-0 bg-[linear-gradient(rgba(16,185,129,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(16,185,129,0.03)_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none opacity-50" />
      
      <div className="relative z-10 flex flex-col items-center justify-center flex-1">
        
        {!hasActiveEvents ? (
          <div className="text-center">
            <div className="inline-block relative">
              <div className="absolute inset-0 bg-emerald-500/20 blur-xl rounded-full animate-pulse" />
              <div className="w-4 h-4 bg-emerald-500 rounded-full mx-auto mb-6 animate-ping" />
            </div>
            <h2 className="text-3xl font-bold tracking-[0.3em] uppercase text-emerald-500 animate-pulse drop-shadow-[0_0_8px_rgba(16,185,129,0.8)]">
              Waiting For Live Events...
            </h2>
            <p className="mt-4 text-emerald-500/50 font-mono text-sm tracking-widest uppercase">
              Polling Edge API [v1] • Standby
            </p>
          </div>
        ) : (
          <div className="w-full max-w-4xl space-y-8 animate-in fade-in zoom-in duration-500">
            {[...activeEvents].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).map((event, idx) => (
              <div key={event.id} className={`bg-black/80 border border-red-500/50 rounded-xl overflow-hidden shadow-[0_0_30px_rgba(239,68,68,0.3)] backdrop-blur-sm ${idx === 0 ? 'animate-[pulse_2s_ease-in-out_infinite]' : ''}`}>
                <div className="bg-red-950/80 border-b border-red-500/50 p-4 flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <span className="text-2xl">⚔️</span>
                    <h2 className="text-2xl font-black text-red-500 tracking-wider uppercase drop-shadow-[0_0_5px_rgba(239,68,68,0.8)]">
                      {event.title ? `Event Triggered: ${event.title}` : 'RAID BOSS SPAWNED: UNKNOWN ENTITY'}
                    </h2>
                  </div>
                  <div className="px-3 py-1 bg-red-500/20 text-red-400 text-xs font-bold uppercase tracking-widest rounded animate-pulse">
                    LIVE
                  </div>
                </div>
                
                <div className="p-6">
                  <div className="mb-4 text-slate-400 font-mono text-xs uppercase tracking-wider">
                    Payload Data Received
                  </div>
                  <pre className="bg-black border border-slate-800 rounded-lg p-4 overflow-x-auto text-emerald-400 font-mono text-sm shadow-inner">
                    <code className="text-shadow-[0_0_2px_rgba(16,185,129,0.5)]">
                      {JSON.stringify(event.payload, null, 2)}
                    </code>
                  </pre>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
