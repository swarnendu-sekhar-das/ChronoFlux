import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './pages/Layout';
import Schemas from './pages/Schemas';
import CreateSchema from './pages/CreateSchema';
import Events from './pages/Events';
import CreateEvent from './pages/CreateEvent';
import EventDetail from './pages/EventDetail';
import Placeholder from './pages/Placeholder';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Schemas />} />
            <Route path="schemas/new" element={<CreateSchema />} />
            <Route path="events" element={<Events />} />
            <Route path="events/new" element={<CreateEvent />} />
            <Route path="events/:id" element={<EventDetail />} />
            <Route path="webhooks" element={<Placeholder title="Webhooks" />} />
            <Route path="settings" element={<Placeholder title="Settings" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
