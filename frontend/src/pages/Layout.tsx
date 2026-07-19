import { Link, Outlet } from 'react-router-dom';
import { Database, Calendar, Webhook, Settings, Gamepad2, Activity } from 'lucide-react';
import { api } from '../services/api';

export default function Layout() {

  return (
    <div className="flex h-screen bg-slate-950 text-slate-50 font-sans">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col">
        <div className="p-6 border-b border-slate-800">
          <h1 className="text-xl font-bold tracking-wider text-emerald-400">CHRONOFLUX</h1>
          <p className="text-xs text-slate-500 uppercase tracking-widest mt-1">Live Ops Engine</p>
        </div>
        <nav className="flex-1 p-4 space-y-2">
          <Link to="/" className="flex items-center gap-3 px-4 py-3 text-slate-300 hover:bg-slate-800 hover:text-white rounded-lg transition-colors">
            <Activity size={20} />
            Dashboard
          </Link>
          <Link to="/schemas" className="flex items-center gap-3 px-4 py-3 text-slate-300 hover:bg-slate-800 hover:text-white rounded-lg transition-colors">
            <Database size={20} />
            Schemas
          </Link>
          <Link to="/events" className="flex items-center gap-3 px-4 py-3 text-slate-300 hover:bg-slate-800 hover:text-white rounded-lg transition-colors">
            <Calendar size={20} />
            Events
          </Link>
          <Link to="/webhooks" className="flex items-center gap-3 px-4 py-3 text-slate-300 hover:bg-slate-800 hover:text-white rounded-lg transition-colors">
            <Webhook size={20} />
            Webhooks
          </Link>
          <Link to="/simulator" className="flex items-center gap-3 px-4 py-3 text-emerald-400 hover:bg-slate-800 hover:text-emerald-300 rounded-lg transition-colors bg-emerald-950/20">
            <Gamepad2 size={20} />
            Simulator UI
          </Link>
          <Link to="/settings" className="flex items-center gap-3 px-4 py-3 text-slate-300 hover:bg-slate-800 hover:text-white rounded-lg transition-colors">
            <Settings size={20} />
            Settings
          </Link>
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="h-16 bg-slate-900 border-b border-slate-800 flex items-center justify-between px-8">
          <h2 className="text-lg font-semibold text-slate-200">Admin Control Room</h2>
          <button 
            onClick={async () => {
              try {
                await api.post('/api/admin/auth/signout');
                window.location.href = '/login';
              } catch (e) {
                console.error(e);
              }
            }}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-sm font-medium rounded-lg transition-colors"
          >
            Sign Out
          </button>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-8 bg-slate-950">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
