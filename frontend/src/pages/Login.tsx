import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

export default function Login() {
  const navigate = useNavigate();

  const handleSuccess = async (credentialResponse: any) => {
    try {
      await api.post('/api/admin/auth/callback', {
        idToken: credentialResponse.credential
      });
      navigate('/');
    } catch (err) {
      console.error('Login failed', err);
    }
  };

  return (
    <div className="flex h-screen w-full items-center justify-center bg-slate-950 font-sans">
      <div className="flex flex-col items-center gap-8 bg-slate-900 p-12 rounded-2xl border border-slate-800 shadow-2xl">
        <div className="text-center">
          <h1 className="text-4xl font-bold tracking-wider text-emerald-400">CHRONOFLUX</h1>
          <p className="text-sm text-slate-500 uppercase tracking-widest mt-2">Admin Control Room</p>
        </div>
        
        <div className="w-full flex justify-center mt-4">
          <GoogleLogin
            onSuccess={handleSuccess}
            onError={() => console.error('Login Failed')}
            useOneTap
            shape="rectangular"
            theme="filled_blue"
            size="large"
          />
        </div>
        
        <p className="text-xs text-slate-600 mt-4">
          Requires a ChronoFlux Admin Google Workspace account.
        </p>
      </div>
    </div>
  );
}
