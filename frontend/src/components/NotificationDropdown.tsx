import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { de } from 'date-fns/locale';
import {
  HeartIcon,
  ChatBubbleLeftIcon,
  FaceSmileIcon,
  ArrowPathIcon,
  TrophyIcon,
  ArrowTrendingUpIcon,
  EnvelopeIcon,
} from '@heroicons/react/24/outline';
import { CheckIcon } from '@heroicons/react/24/solid';
import { dashboardService } from '../services/dashboardService';
import { Notification, NotificationType } from '../types';

interface NotificationDropdownProps {
  onClose: () => void;
}

const notificationIcons: Record<NotificationType, React.ComponentType<{ className?: string }>> = {
  LIKE: HeartIcon,
  COMMENT: ChatBubbleLeftIcon,
  REACTION: FaceSmileIcon,
  STATUS_CHANGE: ArrowPathIcon,
  BADGE_EARNED: TrophyIcon,
  LEVEL_UP: ArrowTrendingUpIcon,
  MENTION: ChatBubbleLeftIcon,
  MESSAGE: EnvelopeIcon,
};

const notificationColors: Record<NotificationType, string> = {
  LIKE: 'text-red-500 bg-red-50 dark:bg-red-900/20',
  COMMENT: 'text-blue-500 bg-blue-50 dark:bg-blue-900/20',
  REACTION: 'text-yellow-500 bg-yellow-50 dark:bg-yellow-900/20',
  STATUS_CHANGE: 'text-purple-500 bg-purple-50 dark:bg-purple-900/20',
  BADGE_EARNED: 'text-amber-500 bg-amber-50 dark:bg-amber-900/20',
  LEVEL_UP: 'text-green-500 bg-green-50 dark:bg-green-900/20',
  MENTION: 'text-indigo-500 bg-indigo-50 dark:bg-indigo-900/20',
  MESSAGE: 'text-teal-500 bg-teal-50 dark:bg-teal-900/20',
};

export default function NotificationDropdown({ onClose }: NotificationDropdownProps) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const data = await dashboardService.getNotifications();
        setNotifications(data);
      } catch (error) {
        console.error('Failed to fetch notifications:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchNotifications();
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  const handleMarkAsRead = async (id: number) => {
    try {
      await dashboardService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await dashboardService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <div
      ref={dropdownRef}
      className="absolute right-0 mt-2 w-80 sm:w-96 bg-white dark:bg-gray-800 rounded-xl shadow-material-3
                 border border-gray-100 dark:border-gray-700 overflow-hidden animate-scale-in"
    >
      {/* Kopfzeile */}
      <div className="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
        <h3 className="font-semibold text-gray-900 dark:text-white">
          Benachrichtigungen
          {unreadCount > 0 && (
            <span className="ml-2 text-xs bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 px-2 py-0.5 rounded-full">
              {unreadCount} neu
            </span>
          )}
        </h3>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllAsRead}
            className="text-xs text-primary-600 dark:text-primary-400 hover:underline"
          >
            Alle als gelesen markieren
          </button>
        )}
      </div>

      {/* Benachrichtigungsliste */}
      <div className="max-h-96 overflow-y-auto">
        {loading ? (
          <div className="p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-600 mx-auto" />
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">
            Noch keine Benachrichtigungen
          </div>
        ) : (
          <ul className="divide-y divide-gray-100 dark:divide-gray-700">
            {notifications.slice(0, 10).map((notification) => {
              const Icon = notificationIcons[notification.type];
              const colorClass = notificationColors[notification.type];

              return (
                <li
                  key={notification.id}
                  className={`relative hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors ${!notification.isRead ? 'bg-primary-50/50 dark:bg-primary-900/10' : ''
                    }`}
                >
                  <Link
                    to={notification.link || '#'}
                    onClick={() => {
                      if (!notification.isRead) handleMarkAsRead(notification.id);
                      onClose();
                    }}
                    className="flex gap-3 px-4 py-3"
                  >
                    <div className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${colorClass}`}>
                      <Icon className="w-5 h-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 dark:text-white">
                        {notification.title}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                        {notification.message}
                      </p>
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                        {format(new Date(notification.createdAt), 'd. MMM, HH:mm', { locale: de })}
                      </p>
                    </div>
                    {!notification.isRead && (
                      <button
                        onClick={(e) => {
                          e.preventDefault();
                          handleMarkAsRead(notification.id);
                        }}
                        className="flex-shrink-0 p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full"
                        title="Als gelesen markieren"
                      >
                        <CheckIcon className="w-4 h-4 text-gray-400" />
                      </button>
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {/* Fußzeile */}
      {notifications.length > 10 && (
        <div className="px-4 py-2 border-t border-gray-100 dark:border-gray-700 text-center">
          <Link
            to="/notifications"
            onClick={onClose}
            className="text-sm text-primary-600 dark:text-primary-400 hover:underline"
          >
            Alle Benachrichtigungen anzeigen
          </Link>
        </div>
      )}
    </div>
  );
}
