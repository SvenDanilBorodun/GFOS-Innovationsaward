import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { TrophyIcon, SparklesIcon } from '@heroicons/react/24/solid';
import { ideaService } from '../services/ideaService';
import { authService } from '../services/authService';
import userService from '../services/userService';
import { Idea, UserBadge, Badge } from '../types';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

// Synchronisiert mit backend GamificationService.LEVEL_THRESHOLDS
const LEVEL_THRESHOLDS = [0, 100, 300, 600, 1000, 1500, 2500, 4000, 6000, 10000];

const LEVEL_NAMES = [
  'Anfänger',
  'Mitwirkender',
  'Innovator',
  'Experte',
  'Meister',
  'Legende',
  'Champion',
  'Held',
  'Titan',
  'Legendär',
];

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [myIdeas, setMyIdeas] = useState<Idea[]>([]);
  const [earnedBadges, setEarnedBadges] = useState<UserBadge[]>([]);
  const [allBadges, setAllBadges] = useState<Badge[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
  });

  useEffect(() => {
    fetchProfileData();
  }, []);

  const fetchProfileData = async () => {
    try {
      const [ideasResponse, userBadges, badges] = await Promise.all([
        ideaService.getIdeas({
          authorId: user?.id,
          size: 10,
          sort: 'createdAt',
          direction: 'DESC',
        }),
        userService.getCurrentUserBadges(),
        userService.getAllBadges(),
      ]);
      setMyIdeas(ideasResponse.content);
      setEarnedBadges(userBadges);
      setAllBadges(badges);
    } catch (error) {
      console.error('Fehler beim Abrufen von Profildaten:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async () => {
    try {
      const updated = await authService.updateProfile(formData);
      updateUser(updated);
      setEditing(false);
      toast.success('Profil aktualisiert');
    } catch (error) {
      toast.error('Fehler beim Aktualisieren des Profils');
    }
  };

  const getNextLevelXp = () => {
    const currentLevel = user?.level || 1;
    if (currentLevel >= LEVEL_THRESHOLDS.length) {
      return LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
    }
    return LEVEL_THRESHOLDS[currentLevel];
  };

  const getCurrentLevelXp = () => {
    const currentLevel = user?.level || 1;
    return LEVEL_THRESHOLDS[currentLevel - 1] || 0;
  };

  const getLevelProgress = () => {
    const currentXp = user?.xpPoints || 0;
    const currentLevelXp = getCurrentLevelXp();
    const nextLevelXp = getNextLevelXp();
    const progressXp = currentXp - currentLevelXp;
    const requiredXp = nextLevelXp - currentLevelXp;
    return Math.min(100, Math.round((progressXp / requiredXp) * 100));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Profil-Kopfzeile */}
      <div className="card p-6">
        <div className="flex flex-col sm:flex-row gap-6">
          {/* Avatar */}
          <div className="flex-shrink-0">
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-primary-500 to-secondary-500 flex items-center justify-center text-white text-3xl font-bold">
              {user?.firstName?.[0]}{user?.lastName?.[0]}
            </div>
          </div>

          {/* Info */}
          <div className="flex-1">
            {editing ? (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">Vorname</label>
                    <input
                      type="text"
                      value={formData.firstName}
                      onChange={(e) =>
                        setFormData({ ...formData, firstName: e.target.value })
                      }
                      className="input"
                    />
                  </div>
                  <div>
                    <label className="label">Nachname</label>
                    <input
                      type="text"
                      value={formData.lastName}
                      onChange={(e) =>
                        setFormData({ ...formData, lastName: e.target.value })
                      }
                      className="input"
                    />
                  </div>
                </div>
                <div>
                  <label className="label">E-Mail</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) =>
                      setFormData({ ...formData, email: e.target.value })
                    }
                    className="input"
                  />
                </div>
                <div className="flex gap-2">
                  <button onClick={handleSaveProfile} className="btn-primary">
                    Änderungen speichern
                  </button>
                  <button
                    onClick={() => setEditing(false)}
                    className="btn-secondary"
                  >
                    Abbrechen
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-start justify-between">
                  <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                      {user?.firstName} {user?.lastName}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400">
                      @{user?.username} • {user?.email}
                    </p>
                    <span className={`inline-block mt-2 badge ${
                      user?.role === 'ADMIN'
                        ? 'badge-error'
                        : user?.role === 'PROJECT_MANAGER'
                        ? 'badge-warning'
                        : 'badge-primary'
                    }`}>
                      {user?.role?.replace('_', ' ')}
                    </span>
                  </div>
                  <button
                    onClick={() => setEditing(true)}
                    className="btn-secondary text-sm"
                  >
                    Profil bearbeiten
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Meine Ideen */}
      <div className="card">
        <div className="p-5 border-b border-gray-100 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Meine Ideen ({myIdeas.length})
          </h2>
        </div>
        <div className="divide-y divide-gray-100 dark:divide-gray-700">
          {myIdeas.length === 0 ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">
              Sie haben noch keine Ideen eingereicht
            </div>
          ) : (
            myIdeas.map((idea) => (
              <a
                key={idea.id}
                href={`/ideas/${idea.id}`}
                className="block p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-900 dark:text-white truncate">
                      {idea.title}
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {format(new Date(idea.createdAt), 'MMM d, yyyy')}
                    </p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className={`badge ${
                      idea.status === 'COMPLETED'
                        ? 'badge-success'
                        : idea.status === 'IN_PROGRESS'
                        ? 'badge-warning'
                        : 'badge-gray'
                    }`}>
                      {idea.status.replace('_', ' ')}
                    </span>
                    <span className="text-sm text-gray-500">
                      {idea.likeCount} Likes
                    </span>
                  </div>
                </div>
              </a>
            ))
          )}
        </div>
      </div>

      {/* Level und XP */}
      <div className="card p-6">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-16 h-16 rounded-full bg-gradient-to-br from-yellow-400 to-orange-500 flex items-center justify-center">
            <span className="text-2xl font-bold text-white">{user?.level}</span>
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">
                {LEVEL_NAMES[Math.min((user?.level || 1) - 1, LEVEL_NAMES.length - 1)]}
              </h2>
              <SparklesIcon className="w-5 h-5 text-yellow-500" />
            </div>
            <p className="text-gray-500 dark:text-gray-400">
              {user?.xpPoints} XP total
            </p>
          </div>
        </div>

        {/* Fortschritt zum nächsten Level */}
        <div>
          <div className="flex justify-between text-sm mb-2">
            <span className="text-gray-600 dark:text-gray-400">
              Fortschritt zu Level {(user?.level || 1) + 1}
            </span>
            <span className="font-medium text-gray-900 dark:text-white">
              {user?.xpPoints} / {getNextLevelXp()} XP
            </span>
          </div>
          <div className="progress-bar h-3">
            <div
              className="progress-bar-fill bg-gradient-to-r from-yellow-400 to-orange-500"
              style={{ width: `${getLevelProgress()}%` }}
            />
          </div>
        </div>

        {/* XP-Aufschlüsselung */}
        <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg text-center">
            <p className="text-2xl font-bold text-primary-600">+50</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">pro Idee</p>
          </div>
          <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg text-center">
            <p className="text-2xl font-bold text-red-500">+10</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">pro erhaltenen Like</p>
          </div>
          <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg text-center">
            <p className="text-2xl font-bold text-blue-500">+5</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">pro Kommentar</p>
          </div>
          <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg text-center">
            <p className="text-2xl font-bold text-green-500">+100</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">Idee abgeschlossen</p>
          </div>
        </div>
      </div>

      {/* Abzeichen */}
      <div className="card p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
          <TrophyIcon className="w-5 h-5 text-yellow-500" />
          Abzeichen ({earnedBadges.length}/{allBadges.length} verdient)
        </h2>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {allBadges.length > 0 ? (
            allBadges.map((badge) => {
              const earnedBadge = earnedBadges.find(
                (eb) => eb.badge.id === badge.id
              );
              return (
                <BadgeCard
                  key={badge.id}
                  name={badge.displayName || badge.name}
                  description={badge.description}
                  earned={!!earnedBadge}
                  earnedAt={earnedBadge?.earnedAt}
                />
              );
            })
          ) : (
            <>
              {/* Fallback-Abzeichen, wenn keine Abzeichen im System vorhanden sind */}
              <BadgeCard
                name="Erste Idee"
                description="Reichen Sie Ihre erste Idee ein"
                earned={earnedBadges.some((b) => b.badge.name === 'first_idea')}
              />
              <BadgeCard
                name="Beliebt"
                description="Erhalten Sie 10 Likes für eine Idee"
                earned={earnedBadges.some((b) => b.badge.name === 'popular')}
              />
              <BadgeCard
                name="Kommentator"
                description="Hinterlassen Sie 50 Kommentare"
                earned={earnedBadges.some((b) => b.badge.name === 'commentator')}
              />
              <BadgeCard
                name="Unterstützer"
                description="Verwenden Sie alle Likes 4 Wochen hintereinander"
                earned={earnedBadges.some((b) => b.badge.name === 'supporter')}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}

interface BadgeCardProps {
  name: string;
  description: string;
  earned: boolean;
  earnedAt?: string;
}

function BadgeCard({ name, description, earned, earnedAt }: BadgeCardProps) {
  return (
    <div
      className={`p-4 rounded-lg border-2 text-center transition-all ${
        earned
          ? 'border-yellow-400 bg-yellow-50 dark:bg-yellow-900/20'
          : 'border-gray-200 dark:border-gray-700 opacity-50'
      }`}
    >
      <div
        className={`w-12 h-12 rounded-full mx-auto mb-2 flex items-center justify-center ${
          earned
            ? 'bg-yellow-400 text-white'
            : 'bg-gray-200 dark:bg-gray-700 text-gray-400'
        }`}
      >
        <TrophyIcon className="w-6 h-6" />
      </div>
      <h3 className="font-medium text-gray-900 dark:text-white text-sm">
        {name}
      </h3>
      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
        {description}
      </p>
      {earned && earnedAt && (
        <p className="text-xs text-yellow-600 dark:text-yellow-400 mt-1">
          Verdient am {format(new Date(earnedAt), 'd. MMM yyyy')}
        </p>
      )}
    </div>
  );
}
