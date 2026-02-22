import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { LightBulbIcon, SunIcon, MoonIcon } from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';

export default function RegisterPage() {
  const { register } = useAuth();
  const { effectiveTheme, toggleTheme } = useTheme();
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.username.trim()) {
      newErrors.username = 'Benutzername ist erforderlich';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Benutzername muss mindestens 3 Zeichen lang sein';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'E-Mail ist erforderlich';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Bitte geben Sie eine gültige E-Mail ein';
    }

    if (!formData.password) {
      newErrors.password = 'Passwort ist erforderlich';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Passwort muss mindestens 8 Zeichen lang sein';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwörter stimmen nicht überein';
    }

    if (!formData.firstName.trim()) {
      newErrors.firstName = 'Vorname ist erforderlich';
    }

    if (!formData.lastName.trim()) {
      newErrors.lastName = 'Nachname ist erforderlich';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsLoading(true);
    try {
      await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
      });
      toast.success('Konto erfolgreich erstellt!');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Registrierung fehlgeschlagen';
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
            Treten Sie der Innovationsgemeinde bei
          </h1>
          <p className="text-lg text-primary-100">
            Erstellen Sie Ihr Konto und tragen Sie Ideen bei, die einen echten Unterschied machen können.
          </p>
          <ul className="space-y-3 text-primary-100">
            <li className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-sm">1</span>
              Ihre innovativen Ideen einreichen
            </li>
            <li className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-sm">2</span>
              Mit Kollegen zusammenarbeiten
            </li>
            <li className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-sm">3</span>
              Abzeichen verdienen und Level aufsteigen
            </li>
          </ul>
        </div>

        <p className="text-sm text-primary-200">
          GFOS Innovation Award 2026
        </p>
      </div>

      {/* Rechte Seite - Registrierungsformular */}
      <div className="flex-1 flex flex-col justify-center px-8 py-12 lg:px-16 bg-white dark:bg-gray-900 overflow-y-auto">
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
            Erstellen Sie Ihr Konto
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-8">
            Füllen Sie Ihre Daten aus, um zu beginnen
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="form-group">
                <label htmlFor="firstName" className="label">
                  Vorname
                </label>
                <input
                  type="text"
                  id="firstName"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className={errors.firstName ? 'input-error' : 'input'}
                  placeholder="John"
                />
                {errors.firstName && <p className="form-error">{errors.firstName}</p>}
              </div>

              <div className="form-group">
                <label htmlFor="lastName" className="label">
                  Nachname
                </label>
                <input
                  type="text"
                  id="lastName"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  className={errors.lastName ? 'input-error' : 'input'}
                  placeholder="Doe"
                />
                {errors.lastName && <p className="form-error">{errors.lastName}</p>}
              </div>
            </div>

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
                placeholder="johndoe"
                autoComplete="username"
              />
              {errors.username && <p className="form-error">{errors.username}</p>}
            </div>

            <div className="form-group">
              <label htmlFor="email" className="label">
                E-Mail
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className={errors.email ? 'input-error' : 'input'}
                placeholder="john.doe@company.com"
                autoComplete="email"
              />
              {errors.email && <p className="form-error">{errors.email}</p>}
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
                placeholder="Mindestens 8 Zeichen"
                autoComplete="new-password"
              />
              {errors.password && <p className="form-error">{errors.password}</p>}
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword" className="label">
                Passwort bestätigen
              </label>
              <input
                type="password"
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className={errors.confirmPassword ? 'input-error' : 'input'}
                placeholder="Wiederholen Sie Ihr Passwort"
                autoComplete="new-password"
              />
              {errors.confirmPassword && <p className="form-error">{errors.confirmPassword}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full py-3 mt-2"
            >
              {isLoading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white" />
              ) : (
                'Konto erstellen'
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-gray-600 dark:text-gray-400">
            Haben Sie bereits ein Konto?{' '}
            <Link to="/login" className="link font-medium">
              Melden Sie sich an
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
