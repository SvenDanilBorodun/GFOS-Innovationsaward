import { useState, useEffect } from 'react';
import { PlusIcon, XMarkIcon, ChartBarIcon, InformationCircleIcon, EyeSlashIcon, CheckIcon } from '@heroicons/react/24/outline';
import { CheckCircleIcon } from '@heroicons/react/24/solid';
import { surveyService } from '../services/surveyService';
import { Survey } from '../types';
import { useAuth } from '../context/AuthContext';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function SurveysPage() {
  const { user } = useAuth();
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedSurvey, setSelectedSurvey] = useState<Survey | null>(null);

  useEffect(() => {
    fetchSurveys();
  }, []);

  const fetchSurveys = async () => {
    try {
      const response = await surveyService.getSurveys({ size: 50 });
      setSurveys(response.content);
    } catch (error) {
      console.error('Fehler beim Abrufen von Umfragen:', error);
      toast.error('Fehler beim Laden von Umfragen');
    } finally {
      setLoading(false);
    }
  };

  const handleVote = async (surveyId: number, optionIds: number[]) => {
    try {
      const updated = await surveyService.vote(surveyId, optionIds);
      setSurveys((prev) =>
        prev.map((s) => (s.id === surveyId ? updated : s))
      );
      toast.success('Stimme erfasst!');
    } catch (error) {
      toast.error('Fehler beim Erfassen der Stimme');
    }
  };

  const handleDelete = async (surveyId: number) => {
    if (!confirm('Diese Umfrage löschen?')) return;

    try {
      await surveyService.deleteSurvey(surveyId);
      setSurveys((prev) => prev.filter((s) => s.id !== surveyId));
      toast.success('Umfrage gelöscht');
    } catch (error) {
      toast.error('Fehler beim Löschen der Umfrage');
    }
  };

  const handleCreateSurvey = async (data: {
    question: string;
    options: string[];
  }) => {
    try {
      const survey = await surveyService.createSurvey({
        question: data.question,
        options: data.options,
      });
      setSurveys((prev) => [survey, ...prev]);
      setShowCreateModal(false);
      toast.success('Umfrage erstellt!');
    } catch (error) {
      toast.error('Fehler beim Erstellen der Umfrage');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Kopfzeile */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Umfragen</h1>
          <p className="text-gray-600 dark:text-gray-400">
            Erstellen Sie Team-Umfragen und nehmen Sie an diesen teil
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn-primary inline-flex items-center gap-2"
        >
          <PlusIcon className="w-5 h-5" />
          Umfrage erstellen
        </button>
      </div>

      {/* Umfragenliste */}
      {surveys.length === 0 ? (
        <div className="card p-12 text-center">
          <ChartBarIcon className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
            Noch keine Umfragen
          </h3>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            Erstellen Sie die erste Umfrage, um Feedback des Teams zu sammeln
          </p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn-primary"
          >
            Umfrage erstellen
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {surveys.map((survey) => (
            <SurveyCard
              key={survey.id}
              survey={survey}
              onVote={handleVote}
              onDelete={handleDelete}
              onShowDetails={() => setSelectedSurvey(survey)}
              canDelete={user?.id === survey.creator.id || user?.role === 'ADMIN'}
            />
          ))}
        </div>
      )}

      {/* Erstellen-Modal */}
      {showCreateModal && (
        <CreateSurveyModal
          onClose={() => setShowCreateModal(false)}
          onSubmit={handleCreateSurvey}
        />
      )}

      {/* Umfragedetails-Modal */}
      {selectedSurvey && (
        <SurveyDetailsModal
          survey={selectedSurvey}
          onClose={() => setSelectedSurvey(null)}
        />
      )}
    </div>
  );
}

interface SurveyCardProps {
  survey: Survey;
  onVote: (surveyId: number, optionIds: number[]) => void;
  onDelete: (surveyId: number) => void;
  onShowDetails: () => void;
  canDelete: boolean;
}

function SurveyCard({ survey, onVote, onDelete, onShowDetails, canDelete }: SurveyCardProps) {
  const [selectedOptions, setSelectedOptions] = useState<number[]>(
    survey.userVotedOptionIds || []
  );
  const hasVoted = survey.hasVoted;

  const handleOptionClick = (optionId: number) => {
    if (hasVoted) return;

    if (survey.allowMultipleVotes) {
      setSelectedOptions((prev) =>
        prev.includes(optionId)
          ? prev.filter((id) => id !== optionId)
          : [...prev, optionId]
      );
    } else {
      setSelectedOptions([optionId]);
    }
  };

  const handleVoteSubmit = () => {
    if (selectedOptions.length === 0) {
      return;
    }
    onVote(survey.id, selectedOptions);
  };

  const getPercentage = (voteCount: number) => {
    if (survey.totalVotes === 0) return 0;
    return Math.round((voteCount / survey.totalVotes) * 100);
  };

  return (
    <div className="card">
      <div className="p-5 border-b border-gray-100 dark:border-gray-700">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-1">
              {survey.question}
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              by {survey.creator.firstName} {survey.creator.lastName} •{' '}
              {format(new Date(survey.createdAt), 'MMM d')}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <button
              onClick={onShowDetails}
              className="btn-icon text-gray-400 hover:text-primary-500"
              title="Details anzeigen"
            >
              <InformationCircleIcon className="w-5 h-5" />
            </button>
            {canDelete && (
              <button
                onClick={() => onDelete(survey.id)}
                className="btn-icon text-gray-400 hover:text-error-500"
                title="Umfrage löschen"
              >
                <XMarkIcon className="w-5 h-5" />
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="p-5 space-y-3">
        {survey.options.map((option) => {
          const percentage = getPercentage(option.voteCount);
          const isSelected = selectedOptions.includes(option.id);
          const wasVotedFor = survey.userVotedOptionIds?.includes(option.id);

          return (
            <button
              key={option.id}
              onClick={() => handleOptionClick(option.id)}
              disabled={hasVoted}
              className={`w-full relative overflow-hidden rounded-lg border-2 p-3 text-left transition-all
                ${hasVoted
                  ? 'cursor-default'
                  : 'cursor-pointer hover:border-primary-300 dark:hover:border-primary-700'
                }
                ${isSelected && !hasVoted
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-gray-200 dark:border-gray-700'
                }
                ${wasVotedFor
                  ? 'border-primary-500'
                  : ''
                }
              `}
            >
              {/* Hintergrund-Fortschrittsbalken */}
              {hasVoted && (
                <div
                  className="absolute inset-y-0 left-0 bg-primary-100 dark:bg-primary-900/30 transition-all duration-500"
                  style={{ width: `${percentage}%` }}
                />
              )}

              <div className="relative flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {wasVotedFor && (
                    <CheckCircleIcon className="w-5 h-5 text-primary-600" />
                  )}
                  <span className="font-medium text-gray-900 dark:text-white">
                    {option.optionText}
                  </span>
                </div>
                {hasVoted && (
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
                    {percentage}% ({option.voteCount})
                  </span>
                )}
              </div>
            </button>
          );
        })}
      </div>

      <div className="px-5 py-3 border-t border-gray-100 dark:border-gray-700 flex items-center justify-between">
        <span className="text-sm text-gray-500 dark:text-gray-400">
          {survey.totalVotes} {survey.totalVotes === 1 ? 'Stimme' : 'Stimmen'}
        </span>
        {!hasVoted && (
          <button
            onClick={handleVoteSubmit}
            disabled={selectedOptions.length === 0}
            className="btn-primary text-sm py-1.5"
          >
            Abstimmen
          </button>
        )}
      </div>
    </div>
  );
}

interface CreateSurveyModalProps {
  onClose: () => void;
  onSubmit: (data: { question: string; options: string[] }) => void;
}

function CreateSurveyModal({ onClose, onSubmit }: CreateSurveyModalProps) {
  const [question, setQuestion] = useState('');
  const [options, setOptions] = useState(['', '']);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleAddOption = () => {
    if (options.length < 6) {
      setOptions([...options, '']);
    }
  };

  const handleRemoveOption = (index: number) => {
    if (options.length > 2) {
      setOptions(options.filter((_, i) => i !== index));
    }
  };

  const handleOptionChange = (index: number, value: string) => {
    const newOptions = [...options];
    newOptions[index] = value;
    setOptions(newOptions);
  };

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!question.trim()) {
      newErrors.question = 'Frage ist erforderlich';
    }

    const validOptions = options.filter((o) => o.trim());
    if (validOptions.length < 2) {
      newErrors.options = 'Mindestens 2 Optionen sind erforderlich';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      question: question.trim(),
      options: options.filter((o) => o.trim()),
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Umfrage erstellen
          </h2>
          <button onClick={onClose} className="btn-icon">
            <XMarkIcon className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="form-group">
            <label htmlFor="question" className="label">
              Frage
            </label>
            <input
              type="text"
              id="question"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              className={errors.question ? 'input-error' : 'input'}
              placeholder="Was möchten Sie fragen?"
            />
            {errors.question && <p className="form-error">{errors.question}</p>}
          </div>

          <div className="form-group">
            <label className="label">Optionen</label>
            <div className="space-y-2">
              {options.map((option, index) => (
                <div key={index} className="flex gap-2">
                  <input
                    type="text"
                    value={option}
                    onChange={(e) => handleOptionChange(index, e.target.value)}
                    className="input"
                    placeholder={`Option ${index + 1}`}
                  />
                  {options.length > 2 && (
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(index)}
                      className="btn-icon text-gray-400 hover:text-error-500"
                    >
                      <XMarkIcon className="w-5 h-5" />
                    </button>
                  )}
                </div>
              ))}
            </div>
            {errors.options && <p className="form-error">{errors.options}</p>}
            {options.length < 6 && (
              <button
                type="button"
                onClick={handleAddOption}
                className="text-sm text-primary-600 dark:text-primary-400 hover:underline mt-2"
              >
                + Option hinzufügen
              </button>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <button type="button" onClick={onClose} className="btn-secondary">
              Abbrechen
            </button>
            <button type="submit" className="btn-primary">
              Umfrage erstellen
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface SurveyDetailsModalProps {
  survey: Survey;
  onClose: () => void;
}

function SurveyDetailsModal({ survey, onClose }: SurveyDetailsModalProps) {
  const getPercentage = (voteCount: number) => {
    if (survey.totalVotes === 0) return 0;
    return Math.round((voteCount / survey.totalVotes) * 100);
  };

  const getWinningOption = () => {
    if (survey.options.length === 0 || survey.totalVotes === 0) return null;
    return survey.options.reduce((max, option) =>
      option.voteCount > max.voteCount ? option : max
    );
  };

  const winningOption = getWinningOption();

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content p-6 max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Umfragedetails
          </h2>
          <button onClick={onClose} className="btn-icon">
            <XMarkIcon className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-6">
          {/* Frage */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {survey.question}
            </h3>
            {survey.description && (
              <p className="text-gray-600 dark:text-gray-400">
                {survey.description}
              </p>
            )}
          </div>

          {/* Umfrageinformationen */}
          <div className="grid grid-cols-2 gap-4">
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-sm text-gray-500 dark:text-gray-400">Erstellt von</p>
              <p className="font-medium text-gray-900 dark:text-white">
                {survey.creator.firstName} {survey.creator.lastName}
              </p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-sm text-gray-500 dark:text-gray-400">Gesamtstimmen</p>
              <p className="font-medium text-gray-900 dark:text-white">
                {survey.totalVotes}
              </p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-sm text-gray-500 dark:text-gray-400">Erstellt</p>
              <p className="font-medium text-gray-900 dark:text-white">
                {format(new Date(survey.createdAt), 'd. MMM yyyy')}
              </p>
            </div>
            <div className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <p className="text-sm text-gray-500 dark:text-gray-400">Status</p>
              <p className="font-medium text-gray-900 dark:text-white">
                {survey.isActive ? 'Aktiv' : 'Geschlossen'}
              </p>
            </div>
          </div>

          {/* Umfrageeinstellungen */}
          <div className="flex flex-wrap gap-2">
            {survey.isAnonymous && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 dark:bg-gray-700 rounded-full text-sm text-gray-700 dark:text-gray-300">
                <EyeSlashIcon className="w-4 h-4" />
                Anonyme Abstimmung
              </span>
            )}
            {survey.allowMultipleVotes && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 dark:bg-gray-700 rounded-full text-sm text-gray-700 dark:text-gray-300">
                <CheckIcon className="w-4 h-4" />
                Mehrfachauswahl erlaubt
              </span>
            )}
            {survey.expiresAt && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 dark:bg-gray-700 rounded-full text-sm text-gray-700 dark:text-gray-300">
                Läuft ab: {format(new Date(survey.expiresAt), 'd. MMM yyyy')}
              </span>
            )}
          </div>

          {/* Ergebnisaufschlüsselung */}
          <div>
            <h4 className="font-medium text-gray-900 dark:text-white mb-3">
              Ergebnisaufschlüsselung
            </h4>
            <div className="space-y-3">
              {survey.options.map((option) => {
                const percentage = getPercentage(option.voteCount);
                const isWinner = winningOption && option.id === winningOption.id && survey.totalVotes > 0;

                return (
                  <div key={option.id} className="relative">
                    <div className="flex items-center justify-between mb-1">
                      <span className={`text-sm ${isWinner ? 'font-semibold text-primary-600 dark:text-primary-400' : 'text-gray-700 dark:text-gray-300'}`}>
                        {option.optionText}
                        {isWinner && ' (Führend)'}
                      </span>
                      <span className="text-sm text-gray-500 dark:text-gray-400">
                        {option.voteCount} Stimmen ({percentage}%)
                      </span>
                    </div>
                    <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          isWinner ? 'bg-primary-500' : 'bg-gray-400 dark:bg-gray-500'
                        }`}
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Ihr Abstimmungsstatus */}
          {survey.hasVoted && (
            <div className="p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg border border-primary-200 dark:border-primary-800">
              <p className="text-sm text-primary-700 dark:text-primary-300 flex items-center gap-2">
                <CheckCircleIcon className="w-5 h-5" />
                Sie haben an dieser Umfrage teilgenommen
              </p>
            </div>
          )}
        </div>

        <div className="flex justify-end mt-6">
          <button onClick={onClose} className="btn-primary">
            Schließen
          </button>
        </div>
      </div>
    </div>
  );
}
