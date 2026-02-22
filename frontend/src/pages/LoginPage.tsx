import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { LightBulbIcon, SunIcon, MoonIcon } from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const { login } = useAuth();
  const { effectiveTheme, toggleTheme } = useTheme();
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.username.trim()) {
      newErrors.username = 'Benutzername ist erforderlich';
    }
    if (!formData.password) {
      newErrors.password = 'Passwort ist erforderlich';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsLoading(true);
    try {
      await login(formData);
      toast.success('Willkommen zurück!');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Anmeldung fehlgeschlagen';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* Linke Seite - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary-600 to-primary-800 p-12 flex-col justify-between">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
            <LightBulbIcon className="w-7 h-7 text-white" />
          </div>
          <span className="text-2xl font-bold text-white">IdeaBoard</span>
        </div>

        <div className="space-y-6">
          <h1 className="text-4xl font-bold text-white leading-tight">
            Verwandeln Sie Ideen in Innovation
          </h1>
          <p className="text-lg text-primary-100">
            Treten Sie Ihrem Team bei, teilen Sie, entwickeln Sie und implementieren Sie die Ideen.
          </p>
        </div>

        <p className="text-sm text-primary-200">
          GFOS Innovation Award 2026
        </p>
      </div>

      {/* Rechte Seite - Anmeldeformular */}
      <div className="flex-1 flex flex-col justify-center px-8 py-12 lg:px-16 bg-white dark:bg-gray-900">
        {/* Design-Umschalter */}
        <div className="absolute top-4 right-4">
          <button onClick={toggleTheme} className="btn-icon">
            {effectiveTheme === 'light' ? (
              <MoonIcon className="w-5 h-5" />
            ) : (
              <SunIcon className="w-5 h-5" />
            )}
          </button>
        </div>

        <div className="max-w-md w-full mx-auto">
          {/* Mobile-Logo */}
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="w-10 h-10 bg-primary-600 rounded-lg flex items-center justify-center">
              <LightBulbIcon className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900 dark:text-white">IdeaBoard</span>
          </div>

          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Willkommen zurück
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-8">
            Melden Sie sich an, um zu Ihrem Dashboard zu gelangen
          </p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="form-group">
              <label htmlFor="username" className="label">
                Benutzername
              </label>
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className={errors.username ? 'input-error' : 'input'}
                placeholder="Geben Sie Ihren Benutzernamen ein"
                autoComplete="username"
              />
              {errors.username && <p className="form-error">{errors.username}</p>}
            </div>

            <div className="form-group">
              <label htmlFor="password" className="label">
                Passwort
              </label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className={errors.password ? 'input-error' : 'input'}
                placeholder="Geben Sie Ihr Passwort ein"
                autoComplete="current-password"
              />
              {errors.password && <p className="form-error">{errors.password}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full py-3"
            >
              {isLoading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white" />
              ) : (
                'Anmelden'
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-gray-600 dark:text-gray-400">
            Haben Sie noch kein Konto?{' '}
            <Link to="/register" className="link font-medium">
              Erstelle eines
            </Link>
          </p>

          {/* Demo-Anmeldedaten */}
          <div className="mt-8 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Test-Konten:
            </p>
            <div className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
              <p><span className="font-medium">Admin:</span> admin / admin123</p>
              <p><span className="font-medium">Mitarbeiter:</span> jsmith, tjohnson / password123</p>
              <p><span className="font-medium">PM:</span> mwilson / password123</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
