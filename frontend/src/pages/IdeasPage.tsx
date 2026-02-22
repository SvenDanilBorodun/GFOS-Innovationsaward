import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  MagnifyingGlassIcon,
  FunnelIcon,
  HeartIcon,
  ChatBubbleLeftIcon,
  PlusIcon,
} from '@heroicons/react/24/outline';
import { HeartIcon as HeartSolidIcon } from '@heroicons/react/24/solid';
import { ideaService } from '../services/ideaService';
import { Idea, IdeaStatus, LikeStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

const CATEGORIES = [
  'Alle',
  'Technologie',
  'Nachhaltigkeit',
  'Personalwesen',
  'Kundenerlebnis',
  'Betrieb',
  'Marketing',
  'Finanzen',
];

const STATUSES: { label: string; value: IdeaStatus | '' }[] = [
  { label: 'Alle Status', value: '' },
  { label: 'Konzept', value: 'CONCEPT' },
  { label: 'In Bearbeitung', value: 'IN_PROGRESS' },
  { label: 'Abgeschlossen', value: 'COMPLETED' },
];

export default function IdeasPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [ideas, setIdeas] = useState<Idea[]>([]);
  const [loading, setLoading] = useState(true);
  const [likeStatus, setLikeStatus] = useState<LikeStatus | null>(null);
  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [category, setCategory] = useState(searchParams.get('category') || 'Alle');
  const [status, setStatus] = useState<IdeaStatus | ''>(
    (searchParams.get('status') as IdeaStatus) || ''
  );
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchIdeas();
    fetchLikeStatus();
  }, [category, status, page]);

  const fetchIdeas = async () => {
    setLoading(true);
    try {
      const filter = {
        page,
        size: 12,
        category: category !== 'Alle' ? category : undefined,
        status: status || undefined,
        search: search || undefined,
        sort: 'createdAt',
        direction: 'DESC' as const,
      };
      const response = await ideaService.getIdeas(filter);
      setIdeas(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Fehler beim Laden der Ideen:', error);
      toast.error('Fehler beim Laden der Ideen');
    } finally {
      setLoading(false);
    }
  };

  const fetchLikeStatus = async () => {
    try {
      const status = await ideaService.getLikeStatus();
      setLikeStatus(status);
    } catch (error) {
      console.error('Failed to fetch like status:', error);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchIdeas();
    setSearchParams((params) => {
      if (search) params.set('search', search);
      else params.delete('search');
      return params;
    });
  };

  const handleLike = async (ideaId: number, isLiked: boolean, authorId: number) => {
    // Benutzer daran hindern, ihre eigenen Ideen zu liken
    if (user && user.id === authorId) {
      toast.error('Sie können Ihre eigene Idee nicht liken');
      return;
    }

    if (!isLiked && likeStatus && likeStatus.remainingLikes <= 0) {
      toast.error('Diese Woche keine Likes mehr übrig!');
      return;
    }

    try {
      if (isLiked) {
        await ideaService.unlikeIdea(ideaId);
      } else {
        await ideaService.likeIdea(ideaId);
      }
      setIdeas((prev) =>
        prev.map((idea) =>
          idea.id === ideaId
            ? {
                ...idea,
                isLikedByCurrentUser: !isLiked,
                likeCount: isLiked ? idea.likeCount - 1 : idea.likeCount + 1,
              }
            : idea
        )
      );
      fetchLikeStatus();
    } catch (error) {
      console.error('Fehler beim Liken/Unliken:', error);
      toast.error('Fehler beim Aktualisieren des Likes');
    }
  };

  const getStatusClass = (status: IdeaStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'status-completed';
      case 'IN_PROGRESS':
        return 'status-in-progress';
      default:
        return 'status-concept';
    }
  };

  return (
    <div className="space-y-6">
      {/* Kopfzeile */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Ideen</h1>
          <p className="text-gray-600 dark:text-gray-400">
            Stöbern Sie und tragen Sie zu Innovationsideen bei
          </p>
        </div>
        <Link to="/ideas/new" className="btn-primary inline-flex items-center gap-2">
          <PlusIcon className="w-5 h-5" />
          Neue Idee
        </Link>
      </div>

      {/* Like-Status-Banner */}
      {likeStatus && (
        <div className="card p-4 flex items-center justify-between bg-gradient-to-r from-primary-50 to-secondary-50 dark:from-primary-900/20 dark:to-secondary-900/20">
          <div className="flex items-center gap-3">
            <HeartIcon className="w-6 h-6 text-red-500" />
            <div>
              <p className="font-medium text-gray-900 dark:text-white">
                {likeStatus.remainingLikes} Likes verbleibend diese Woche
              </p>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Setzt sich jeden Sonntag um Mitternacht zurück
              </p>
            </div>
          </div>
          <div className="flex gap-1">
            {[...Array(3)].map((_, i) => (
              <HeartSolidIcon
                key={i}
                className={`w-5 h-5 ${
                  i < likeStatus.remainingLikes ? 'text-red-500' : 'text-gray-300 dark:text-gray-600'
                }`}
              />
            ))}
          </div>
        </div>
      )}

      {/* Filter */}
      <div className="card p-4">
        <div className="flex flex-col lg:flex-row gap-4">
          {/* Suche */}
          <form onSubmit={handleSearch} className="flex-1">
            <div className="relative">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Ideen suchen..."
                className="input pl-10"
              />
            </div>
          </form>

          {/* Kategorie-Filter */}
          <div className="flex items-center gap-2">
            <FunnelIcon className="w-5 h-5 text-gray-400" />
            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setPage(0);
                setSearchParams((params) => {
                  if (e.target.value !== 'Alle') params.set('category', e.target.value);
                  else params.delete('category');
                  return params;
                });
              }}
              className="input w-40"
            >
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          {/* Status-Filter */}
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as IdeaStatus | '');
              setPage(0);
              setSearchParams((params) => {
                if (e.target.value) params.set('status', e.target.value);
                else params.delete('status');
                return params;
              });
            }}
            className="input w-40"
          >
            {STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Ideen-Raster */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="card p-5 space-y-4">
              <div className="skeleton h-6 w-3/4" />
              <div className="skeleton h-4 w-full" />
              <div className="skeleton h-4 w-2/3" />
              <div className="flex gap-2">
                <div className="skeleton h-6 w-20" />
                <div className="skeleton h-6 w-20" />
              </div>
            </div>
          ))}
        </div>
      ) : ideas.length === 0 ? (
        <div className="card p-12 text-center">
          <LightBulbIcon className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
            Keine Ideen gefunden
          </h3>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            {search || category !== 'Alle' || status
              ? 'Versuchen Sie, Ihre Filter anzupassen'
              : 'Seien Sie der Erste, der eine Idee einreicht!'}
          </p>
          <Link to="/ideas/new" className="btn-primary">
            Idee einreichen
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {ideas.map((idea) => (
            <article key={idea.id} className="card-hover overflow-hidden group">
              <Link to={`/ideas/${idea.id}`} className="block p-5">
                <div className="flex items-start justify-between mb-3">
                  <span className={getStatusClass(idea.status)}>
                    {idea.status.replace('_', ' ')}
                  </span>
                  {idea.isFeatured && (
                    <span className="badge-primary">Hervorgehoben</span>
                  )}
                </div>

                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors truncate-2">
                  {idea.title}
                </h3>

                <p className="text-gray-600 dark:text-gray-400 text-sm mb-4 truncate-3">
                  {idea.description}
                </p>

                {/* Tags */}
                <div className="flex flex-wrap gap-1 mb-4">
                  <span className="badge-gray">{idea.category}</span>
                  {idea.tags.slice(0, 2).map((tag) => (
                    <span key={tag} className="badge-primary">
                      {tag}
                    </span>
                  ))}
                  {idea.tags.length > 2 && (
                    <span className="badge-gray">+{idea.tags.length - 2}</span>
                  )}
                </div>

                {/* Fortschrittsbalken für Ideen in Bearbeitung */}
                {idea.status === 'IN_PROGRESS' && (
                  <div className="mb-4">
                    <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
                      <span>Fortschritt</span>
                      <span>{idea.progressPercentage}%</span>
                    </div>
                    <div className="progress-bar">
                      <div
                        className="progress-bar-fill bg-primary-500"
                        style={{ width: `${idea.progressPercentage}%` }}
                      />
                    </div>
                  </div>
                )}

                {/* Autor und Datum */}
                <div className="flex items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
                  <div className="avatar-sm">
                    {idea.author.firstName?.[0]}{idea.author.lastName?.[0]}
                  </div>
                  <span>
                    {idea.author.firstName} {idea.author.lastName}
                  </span>
                  <span className="text-gray-300 dark:text-gray-600">|</span>
                  <span>{format(new Date(idea.createdAt), 'MMM d')}</span>
                </div>
              </Link>

              {/* Aktionen-Fußzeile */}
              <div className="px-5 py-3 border-t border-gray-100 dark:border-gray-700 flex items-center justify-between">
                {user?.id === idea.author.id ? (
                  <span className="flex items-center gap-1.5 text-sm text-gray-400 dark:text-gray-500 cursor-not-allowed" title="Sie können Ihre eigene Idee nicht liken">
                    <HeartIcon className="w-5 h-5" />
                    {idea.likeCount}
                  </span>
                ) : (
                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      handleLike(idea.id, idea.isLikedByCurrentUser || false, idea.author.id);
                    }}
                    className={`flex items-center gap-1.5 text-sm font-medium transition-colors ${
                      idea.isLikedByCurrentUser
                        ? 'text-red-500'
                        : 'text-gray-500 dark:text-gray-400 hover:text-red-500'
                    }`}
                  >
                    {idea.isLikedByCurrentUser ? (
                      <HeartSolidIcon className="w-5 h-5" />
                    ) : (
                      <HeartIcon className="w-5 h-5" />
                    )}
                    {idea.likeCount}
                  </button>
                )}

                <div className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400">
                  <ChatBubbleLeftIcon className="w-5 h-5" />
                  {idea.commentCount}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      {/* Paginierung */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="btn-secondary disabled:opacity-50"
          >
            Zurück
          </button>
          <span className="px-4 text-gray-600 dark:text-gray-400">
            Seite {page + 1} von {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="btn-secondary disabled:opacity-50"
          >
            Weiter
          </button>
        </div>
      )}
    </div>
  );
}

function LightBulbIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 18v-5.25m0 0a6.01 6.01 0 001.5-.189m-1.5.189a6.01 6.01 0 01-1.5-.189m3.75 7.478a12.06 12.06 0 01-4.5 0m3.75 2.383a14.406 14.406 0 01-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 10-7.517 0c.85.493 1.509 1.333 1.509 2.316V18" />
    </svg>
  );
}
