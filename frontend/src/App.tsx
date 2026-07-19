import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/toaster';
import { ErrorBoundary } from './components/ErrorBoundary';
import Layout from './pages/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Schemas from './pages/Schemas';
import CreateSchema from './pages/CreateSchema';
import Events from './pages/Events';
import CreateEvent from './pages/CreateEvent';
import EventDetail from './pages/EventDetail';
import Webhooks from './pages/Webhooks';
import Placeholder from './pages/Placeholder';
import Simulator from './pages/Simulator';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ErrorBoundary>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<Layout />}>
              <Route index element={<Dashboard />} />
              <Route path="schemas" element={<Schemas />} />
              <Route path="schemas/new" element={<CreateSchema />} />
              <Route path="events" element={<Events />} />
              <Route path="events/new" element={<CreateEvent />} />
              <Route path="events/:id" element={<EventDetail />} />
              <Route path="webhooks" element={<Webhooks />} />
              <Route path="simulator" element={<Simulator />} />
              <Route path="*" element={<Placeholder title="404" />} />
            </Route>
          </Routes>
        </BrowserRouter>
        <Toaster />
      </ErrorBoundary>
    </QueryClientProvider>
  );
}

export default App;
