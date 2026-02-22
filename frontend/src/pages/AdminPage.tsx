import { useState, useEffect } from 'react';
import { Routes, Route, NavLink, Navigate } from 'react-router-dom';
import {
  UserGroupIcon,
  DocumentTextIcon,
  ClipboardDocumentListIcon,
  ArrowDownTrayIcon,
} from '@heroicons/react/24/outline';
import { format } from 'date-fns';
import api from '../services/api';
import { User, AuditLog } from '../types';
import toast from 'react-hot-toast';

export default function AdminPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Admin-Panel</h1>
        <p className="text-gray-600 dark:text-gray-400">
          Verwalten Sie Benutzer, sehen Sie Audit-Logs ein und exportieren Sie Daten
        </p>
      </div>

      {/* Navigations-Reiter */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-700">
        <NavLink
          to="/admin/users"
          className={({ isActive }) =>
            `px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
              isActive
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`
          }
        >
          <div className="flex items-center gap-2">
            <UserGroupIcon className="w-4 h-4" />
            Benutzer
          </div>
        </NavLink>
        <NavLink
          to="/admin/audit"
          className={({ isActive }) =>
            `px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
              isActive
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`
          }
        >
          <div className="flex items-center gap-2">
            <ClipboardDocumentListIcon className="w-4 h-4" />
            Audit-Logs
          </div>
        </NavLink>
        <NavLink
          to="/admin/export"
          className={({ isActive }) =>
            `px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
              isActive
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`
          }
        >
          <div className="flex items-center gap-2">
            <ArrowDownTrayIcon className="w-4 h-4" />
            Export
          </div>
        </NavLink>
      </div>

      {/* Inhalt */}
      <Routes>
        <Route path="/" element={<Navigate to="/admin/users" replace />} />
        <Route path="/users" element={<UsersManagement />} />
        <Route path="/audit" element={<AuditLogs />} />
        <Route path="/export" element={<ExportData />} />
      </Routes>
    </div>
  );
}

function UsersManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await api.get<User[]>('/users');
      setUsers(response.data);
    } catch (error) {
      toast.error('Fehler beim Laden von Benutzern');
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (userId: number, newRole: string) => {
    try {
      await api.put(`/users/${userId}/role`, { role: newRole });
      setUsers((prev) =>
        prev.map((u) =>
          u.id === userId ? { ...u, role: newRole as User['role'] } : u
        )
      );
      toast.success('Rolle aktualisiert');
    } catch (error) {
      toast.error('Fehler beim Aktualisieren der Rolle');
    }
  };

  const handleToggleActive = async (userId: number, isActive: boolean) => {
    try {
      await api.put(`/users/${userId}/status`, { isActive: !isActive });
      setUsers((prev) =>
        prev.map((u) =>
          u.id === userId ? { ...u, isActive: !isActive } : u
        )
      );
      toast.success(isActive ? 'Benutzer deaktiviert' : 'Benutzer aktiviert');
    } catch (error) {
      toast.error('Fehler beim Aktualisieren des Benutzerstatus');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="card overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-700/50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Benutzer
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Rolle
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Level
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Beigetreten
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Aktionen
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/30">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center gap-3">
                    <div className="avatar-sm">
                      {user.firstName?.[0]}{user.lastName?.[0]}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">
                        {user.firstName} {user.lastName}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {user.email}
                      </p>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <select
                    value={user.role}
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    className="input py-1 text-sm"
                  >
                    <option value="EMPLOYEE">Mitarbeiter</option>
                    <option value="PROJECT_MANAGER">Projektmanager</option>
                    <option value="ADMIN">Administrator</option>
                  </select>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="text-sm text-gray-900 dark:text-white">
                    Level {user.level} ({user.xpPoints} XP)
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span
                    className={`badge ${
                      user.isActive ? 'badge-success' : 'badge-error'
                    }`}
                  >
                    {user.isActive ? 'Aktiv' : 'Inaktiv'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  {format(new Date(user.createdAt), 'MMM d, yyyy')}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <button
                    onClick={() => handleToggleActive(user.id, user.isActive)}
                    className={`text-sm font-medium ${
                      user.isActive
                        ? 'text-error-600 hover:text-error-700'
                        : 'text-success-600 hover:text-success-700'
                    }`}
                  >
                    {user.isActive ? 'Deaktivieren' : 'Aktivieren'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AuditLogs() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      const response = await api.get<AuditLog[]>('/audit-logs');
      setLogs(response.data);
    } catch (error) {
      toast.error('Fehler beim Laden von Audit-Logs');
    } finally {
      setLoading(false);
    }
  };

  const getActionColor = (action: string) => {
    switch (action) {
      case 'CREATE':
        return 'badge-success';
      case 'UPDATE':
        return 'badge-warning';
      case 'DELETE':
        return 'badge-error';
      case 'STATUS_CHANGE':
        return 'badge-primary';
      default:
        return 'badge-gray';
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="card overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-700/50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Zeitstempel
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Benutzer
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Aktion
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Entität
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Details
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {logs.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                  Keine Audit-Logs gefunden
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/30">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                    {format(new Date(log.createdAt), 'MMM d, yyyy HH:mm:ss')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="text-sm text-gray-900 dark:text-white">
                      {log.user ? `${log.user.firstName} ${log.user.lastName}` : 'System'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`badge ${getActionColor(log.action)}`}>
                      {log.action}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {log.entityType} #{log.entityId}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400 max-w-md truncate">
                    {log.newValue ? JSON.stringify(log.newValue).slice(0, 100) : '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ExportData() {
  const [exporting, setExporting] = useState<string | null>(null);

  const handleExport = async (type: string, format: string) => {
    setExporting(`${type}-${format}`);
    try {
      const response = await api.get(`/export/${type}/${format}`, {
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(response.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${type}-export-${new Date().toISOString().split('T')[0]}.${format}`;
      a.click();
      window.URL.revokeObjectURL(url);

      toast.success('Export heruntergeladen');
    } catch (error) {
      toast.error('Fehler beim Export');
    } finally {
      setExporting(null);
    }
  };

  const exportOptions = [
    {
      title: 'Statistikbericht',
      description: 'KPIs und Dashboard-Statistiken exportieren',
      type: 'statistics',
      formats: ['csv', 'pdf'],
    },
    {
      title: 'Ideen-Daten',
      description: 'Alle Ideen mit Details exportieren',
      type: 'ideas',
      formats: ['csv'],
    },
    {
      title: 'Benutzeraktivität',
      description: 'Benutzeraktivität und Beiträge exportieren',
      type: 'users',
      formats: ['csv'],
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {exportOptions.map((option) => (
        <div key={option.type} className="card p-6">
          <div className="flex items-start gap-3 mb-4">
            <div className="p-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <DocumentTextIcon className="w-6 h-6 text-primary-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 dark:text-white">
                {option.title}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {option.description}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            {option.formats.map((format) => (
              <button
                key={format}
                onClick={() => handleExport(option.type, format)}
                disabled={exporting === `${option.type}-${format}`}
                className="btn-secondary text-sm flex items-center gap-2"
              >
                {exporting === `${option.type}-${format}` ? (
                  <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-current" />
                ) : (
                  <ArrowDownTrayIcon className="w-4 h-4" />
                )}
                {format.toUpperCase()}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
