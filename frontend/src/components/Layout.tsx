import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import {
  HomeIcon,
  LightBulbIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  ArrowRightOnRectangleIcon,
  SunIcon,
  MoonIcon,
  Bars3Icon,
  XMarkIcon,
  BellIcon,
  ChatBubbleLeftRightIcon,
} from '@heroicons/react/24/outline';
import NotificationDropdown from './NotificationDropdown';

const navigation = [
  { name: 'Übersicht', href: '/dashboard', icon: HomeIcon },
  { name: 'Ideen', href: '/ideas', icon: LightBulbIcon },
  { name: 'Nachrichten', href: '/messages', icon: ChatBubbleLeftRightIcon },
  { name: 'Umfragen', href: '/surveys', icon: ChartBarIcon },
];

const adminNavigation = [
  { name: 'Admin', href: '/admin', icon: Cog6ToothIcon },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const { effectiveTheme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = user?.role === 'ADMIN';

  const getLevelColor = (level: number) => {
    if (level >= 5) return 'text-yellow-500';
    if (level >= 3) return 'text-primary-500';
    return 'text-gray-500';
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Mobiler Seitenleisten-Hintergrund */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Seitenleiste */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-800 shadow-material-2
                    transform transition-transform duration-300 ease-in-out lg:translate-x-0
                    ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="h-full flex flex-col">
          {/* Logo */}
          <div className="h-16 flex items-center justify-between px-4 border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <LightBulbIcon className="w-5 h-5 text-white" />
              </div>
              <span className="font-bold text-lg text-gray-900 dark:text-white">IdeaBoard</span>
            </div>
            <button
              onClick={() => setSidebarOpen(false)}
              className="lg:hidden btn-icon"
              title="Schließen"
            >
              <XMarkIcon className="w-6 h-6" />
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
            {navigation.map((item) => (
              <NavLink
                key={item.name}
                to={item.href}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-lg font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-400'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`
                }
                onClick={() => setSidebarOpen(false)}
              >
                <item.icon className="w-5 h-5" />
                {item.name}
              </NavLink>
            ))}

            {isAdmin && (
              <>
                <div className="my-4 border-t border-gray-200 dark:border-gray-700" />
                {adminNavigation.map((item) => (
                  <NavLink
                    key={item.name}
                    to={item.href}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-lg font-medium transition-colors ${
                        isActive
                          ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-400'
                          : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                      }`
                    }
                    onClick={() => setSidebarOpen(false)}
                  >
                    <item.icon className="w-5 h-5" />
                    {item.name}
                  </NavLink>
                ))}
              </>
            )}
          </nav>

          {/* Benutzer-Bereich */}
          <div className="p-4 border-t border-gray-200 dark:border-gray-700">
            <NavLink
              to="/profile"
              className={({ isActive }) =>
                `flex items-center gap-3 mb-3 px-3 py-2 -mx-3 rounded-lg transition-colors ${
                  isActive
                    ? 'bg-primary-50 dark:bg-primary-900/20'
                    : 'hover:bg-gray-100 dark:hover:bg-gray-700'
                }`
              }
              onClick={() => setSidebarOpen(false)}
            >
              <div className="avatar-md">
                {user?.firstName?.[0]}{user?.lastName?.[0]}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className={`text-xs font-medium ${getLevelColor(user?.level || 1)}`}>
                  Stufe {user?.level} • {user?.xpPoints} XP
                </p>
              </div>
            </NavLink>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-600 dark:text-gray-400
                         hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            >
              <ArrowRightOnRectangleIcon className="w-5 h-5" />
              Abmelden
            </button>
          </div>
        </div>
      </aside>

      {/* Hauptinhalt */}
      <div className="lg:pl-64">
        {/* Obere Kopfzeile */}
        <header className="sticky top-0 z-30 h-16 bg-white dark:bg-gray-800 shadow-material-1">
          <div className="h-full flex items-center justify-between px-4">
            {/* Mobile Menü-Schaltfläche */}
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden btn-icon"
            >
              <Bars3Icon className="w-6 h-6" />
            </button>

            {/* Abstandshalter */}
            <div className="flex-1" />

            {/* Rechte Aktionen */}
            <div className="flex items-center gap-2">
              {/* Design-Umschaltung */}
              <button
                onClick={toggleTheme}
                className="btn-icon"
                title={`Wechsel zu ${effectiveTheme === 'light' ? 'dunklem' : 'hellem'} Modus`}
              >
                {effectiveTheme === 'light' ? (
                  <MoonIcon className="w-5 h-5" />
                ) : (
                  <SunIcon className="w-5 h-5" />
                )}
              </button>

              {/* Benachrichtigungen */}
              <div className="relative">
                <button
                  onClick={() => setShowNotifications(!showNotifications)}
                  className="btn-icon relative"
                >
                  <BellIcon className="w-5 h-5" />
                  {/* Benachrichtigungsabzeichen - wird dynamisch */}
                </button>
                {showNotifications && (
                  <NotificationDropdown onClose={() => setShowNotifications(false)} />
                )}
              </div>

              {/* Neue Ideen-Schaltfläche */}
              <NavLink to="/ideas/new" className="btn-primary hidden sm:flex">
                Neue Idee
              </NavLink>
            </div>
          </div>
        </header>

        {/* Seiteninhalt */}
        <main className="p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
