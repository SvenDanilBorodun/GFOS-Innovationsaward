import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useDropzone } from 'react-dropzone';
import {
  XMarkIcon,
  PaperClipIcon,
  ArrowLeftIcon,
  DocumentIcon,
  PhotoIcon,
  DocumentTextIcon,
  TableCellsIcon,
  CloudArrowUpIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
  PlusIcon,
  ListBulletIcon
} from '@heroicons/react/24/outline';
import { ideaService } from '../services/ideaService';
import { Idea } from '../types';
import toast from 'react-hot-toast';

const CATEGORIES = [
  'Technologie',
  'Nachhaltigkeit',
  'HR',
  'Kundenerlebnis',
  'Betrieb',
  'Marketing',
  'Finanzen',
  'Sonstige',
];

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

// Dateityp-Konfigurationen
const FILE_TYPE_CONFIG: Record<string, { icon: React.ComponentType<{ className?: string }>; color: string; label: string }> = {
  'image/jpeg': { icon: PhotoIcon, color: 'text-blue-500', label: 'Bild' },
  'image/png': { icon: PhotoIcon, color: 'text-blue-500', label: 'Bild' },
  'image/gif': { icon: PhotoIcon, color: 'text-blue-500', label: 'Bild' },
  'image/webp': { icon: PhotoIcon, color: 'text-blue-500', label: 'Bild' },
  'application/pdf': { icon: DocumentTextIcon, color: 'text-red-500', label: 'PDF' },
  'application/msword': { icon: DocumentIcon, color: 'text-blue-600', label: 'Word' },
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': { icon: DocumentIcon, color: 'text-blue-600', label: 'Word' },
  'application/vnd.ms-excel': { icon: TableCellsIcon, color: 'text-green-600', label: 'Excel' },
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': { icon: TableCellsIcon, color: 'text-green-600', label: 'Excel' },
  'text/plain': { icon: DocumentTextIcon, color: 'text-gray-500', label: 'Text' },
  'text/csv': { icon: TableCellsIcon, color: 'text-green-500', label: 'CSV' },
};

interface FileWithPreview extends File {
  preview?: string;
}

interface UploadStatus {
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
}

export default function CreateIdeaPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const [loading, setLoading] = useState(false);
  const [loadingIdea, setLoadingIdea] = useState(isEditing);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
    tags: [] as string[],
  });
  const [tagInput, setTagInput] = useState('');
  const [checklistItems, setChecklistItems] = useState<string[]>(['']);
  const [files, setFiles] = useState<FileWithPreview[]>([]);
  const [uploadStatuses, setUploadStatuses] = useState<Map<string, UploadStatus>>(new Map());
  const [existingFiles, setExistingFiles] = useState<{ id: number; name: string; mimeType?: string }[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (isEditing) {
      fetchIdea();
    }
  }, [id]);

  const fetchIdea = async () => {
    try {
      const idea = await ideaService.getIdea(Number(id));
      setFormData({
        title: idea.title,
        description: idea.description,
        category: idea.category,
        tags: idea.tags,
      });
      setExistingFiles(
        idea.attachments.map((a) => ({ id: a.id, name: a.originalName, mimeType: a.mimeType }))
      );
    } catch (error) {
      toast.error('Fehler beim Laden der Idee');
      navigate('/ideas');
    } finally {
      setLoadingIdea(false);
    }
  };

  // Vorschaubilder bereinigen, wenn die Komponente entfernt wird
  useEffect(() => {
    return () => {
      files.forEach(file => {
        if (file.preview) URL.revokeObjectURL(file.preview);
      });
    };
  }, [files]);

  const onDrop = useCallback((acceptedFiles: File[], rejectedFiles: any[]) => {
    // Abgelehnte Dateien verarbeiten
    rejectedFiles.forEach(({ file, errors }) => {
      const errorMessages = errors.map((e: any) => {
        if (e.code === 'file-too-large') return 'Datei ist zu groß (max. 10MB)';
        if (e.code === 'file-invalid-type') return 'Dateityp wird nicht unterstützt';
        return e.message;
      }).join(', ');
      toast.error(`${file.name}: ${errorMessages}`);
    });

    // Akzeptierte Dateien mit Vorschaubildern verarbeiten
    const filesWithPreviews = acceptedFiles.map(file => {
      const fileWithPreview = file as FileWithPreview;
      if (file.type.startsWith('image/')) {
        fileWithPreview.preview = URL.createObjectURL(file);
      }
      return fileWithPreview;
    });

    setFiles(prev => [...prev, ...filesWithPreviews]);
  }, []);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.png', '.jpg', '.jpeg', '.gif', '.webp'],
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'text/plain': ['.txt'],
      'text/csv': ['.csv'],
    },
    maxSize: MAX_FILE_SIZE,
    multiple: true,
  });

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.title.trim()) {
      newErrors.title = 'Titel erforderlich';
    } else if (formData.title.length > 200) {
      newErrors.title = 'Titel muss weniger als 200 Zeichen sein';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Beschreibung erforderlich';
    }

    if (!formData.category) {
      newErrors.category = 'Bitte wählen Sie eine Kategorie';
    }

    // Validiere Checklistenelemente (nur bei neuen Ideen)
    if (!isEditing) {
      const validItems = checklistItems.filter(item => item.trim() !== '');
      if (validItems.length === 0) {
        newErrors.checklist = 'Mindestens ein To-do ist erforderlich';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    try {
      let idea: Idea;

      if (isEditing) {
        idea = await ideaService.updateIdea(Number(id), formData);
      } else {
        const validChecklistItems = checklistItems.filter(item => item.trim() !== '');
        idea = await ideaService.createIdea({
          ...formData,
          checklistItems: validChecklistItems,
        });
      }

      // Neue Dateien mit Fortschrittsanzeige hochladen
      if (files.length > 0) {
        setIsUploading(true);
        const newStatuses = new Map<string, UploadStatus>();
        files.forEach(file => newStatuses.set(file.name, { status: 'pending' }));
        setUploadStatuses(newStatuses);

        let successCount = 0;
        let errorCount = 0;

        for (const file of files) {
          setUploadStatuses(prev => {
            const updated = new Map(prev);
            updated.set(file.name, { status: 'uploading' });
            return updated;
          });

          try {
            await ideaService.uploadFile(idea.id, file);
            setUploadStatuses(prev => {
              const updated = new Map(prev);
              updated.set(file.name, { status: 'success' });
              return updated;
            });
            successCount++;
          } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Upload fehlgeschlagen';
            setUploadStatuses(prev => {
              const updated = new Map(prev);
              updated.set(file.name, { status: 'error', error: errorMessage });
              return updated;
            });
            errorCount++;
          }
        }

        setIsUploading(false);

        if (errorCount > 0) {
          toast.error(`${errorCount} Datei(en) konnte(n) nicht hochgeladen werden`);
        }
        if (successCount > 0) {
          toast.success(`${successCount} Datei(en) erfolgreich hochgeladen`);
        }
      }

      toast.success(isEditing ? 'Idee aktualisiert!' : 'Idee eingereicht!');
      navigate(`/ideas/${idea.id}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Fehler beim Speichern der Idee';
      toast.error(message);
    } finally {
      setLoading(false);
      setIsUploading(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleAddTag = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      const tag = tagInput.trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
      if (tag && !formData.tags.includes(tag) && formData.tags.length < 5) {
        setFormData((prev) => ({ ...prev, tags: [...prev.tags, tag] }));
      }
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData((prev) => ({
      ...prev,
      tags: prev.tags.filter((tag) => tag !== tagToRemove),
    }));
  };

  const handleRemoveFile = (index: number) => {
    const fileToRemove = files[index];
    if (fileToRemove.preview) {
      URL.revokeObjectURL(fileToRemove.preview);
    }
    setFiles((prev) => prev.filter((_, i) => i !== index));
    setUploadStatuses(prev => {
      const updated = new Map(prev);
      updated.delete(fileToRemove.name);
      return updated;
    });
  };

  const getFileIcon = (mimeType?: string) => {
    if (!mimeType) return { Icon: PaperClipIcon, color: 'text-gray-400' };
    const config = FILE_TYPE_CONFIG[mimeType];
    if (config) return { Icon: config.icon, color: config.color };
    if (mimeType.startsWith('image/')) return { Icon: PhotoIcon, color: 'text-blue-500' };
    return { Icon: DocumentIcon, color: 'text-gray-400' };
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const handleRemoveExistingFile = async (fileId: number) => {
    try {
      await ideaService.deleteFile(Number(id), fileId);
      setExistingFiles((prev) => prev.filter((f) => f.id !== fileId));
      toast.success('Datei entfernt');
    } catch (error) {
      toast.error('Fehler beim Entfernen der Datei');
    }
  };

  if (loadingIdea) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Kopfzeile */}
      <div className="mb-6">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors mb-4"
        >
          <ArrowLeftIcon className="w-4 h-4" />
          Zurück
        </button>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {isEditing ? 'Idee bearbeiten' : 'Neue Idee einreichen'}
        </h1>
        <p className="text-gray-600 dark:text-gray-400">
          {isEditing
            ? 'Aktualisieren Sie Ihre Innovationsidee'
            : 'Teilen Sie Ihre Innovationsidee mit dem Team'}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="card p-6 space-y-6">
        {/* Titel */}
        <div className="form-group">
          <label htmlFor="title" className="label">
            Titel <span className="text-error-500">*</span>
          </label>
          <input
            type="text"
            id="title"
            name="title"
            value={formData.title}
            onChange={handleChange}
            className={errors.title ? 'input-error' : 'input'}
            placeholder="Ein klarer, prägnanter Titel für Ihre Idee"
          />
          {errors.title && <p className="form-error">{errors.title}</p>}
        </div>

        {/* Kategorie */}
        <div className="form-group">
          <label htmlFor="category" className="label">
            Kategorie <span className="text-error-500">*</span>
          </label>
          <select
            id="category"
            name="category"
            value={formData.category}
            onChange={handleChange}
            className={errors.category ? 'input-error' : 'input'}
          >
            <option value="">Wählen Sie eine Kategorie</option>
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
          {errors.category && <p className="form-error">{errors.category}</p>}
        </div>

        {/* Beschreibung */}
        <div className="form-group">
          <label htmlFor="description" className="label">
            Beschreibung <span className="text-error-500">*</span>
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows={6}
            className={errors.description ? 'input-error' : 'input'}
            placeholder="Beschreiben Sie Ihre Idee ausführlich. Welches Problem löst sie? Welche Vorteile würde sie bringen?"
          />
          {errors.description && <p className="form-error">{errors.description}</p>}
        </div>

        {/* Tags */}
        <div className="form-group">
          <label htmlFor="tags" className="label">
            Tags (max. 5)
          </label>
          <div className="flex flex-wrap gap-2 mb-2">
            {formData.tags.map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center gap-1 px-2 py-1 bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 rounded-full text-sm"
              >
                #{tag}
                <button
                  type="button"
                  onClick={() => handleRemoveTag(tag)}
                  className="hover:text-primary-900 dark:hover:text-primary-100"
                >
                  <XMarkIcon className="w-4 h-4" />
                </button>
              </span>
            ))}
          </div>
          <input
            type="text"
            id="tags"
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={handleAddTag}
            className="input"
            placeholder="Tag eingeben und Enter drücken"
            disabled={formData.tags.length >= 5}
          />
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
            Enter oder Komma drücken, um ein Tag hinzuzufügen
          </p>
        </div>

        {/* To-dos / Checkliste (nur bei neuen Ideen) */}
        {!isEditing && (
          <div className="form-group">
            <label className="label flex items-center gap-2">
              <ListBulletIcon className="w-4 h-4" />
              To-dos <span className="text-error-500">*</span>
            </label>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">
              Definieren Sie die Schritte zur Umsetzung Ihrer Idee (mindestens 1)
            </p>

            <div className="space-y-2 mb-3">
              {checklistItems.map((item, index) => (
                <div key={index} className="flex items-center gap-2">
                  <input
                    type="text"
                    value={item}
                    onChange={(e) => {
                      const newItems = [...checklistItems];
                      newItems[index] = e.target.value;
                      setChecklistItems(newItems);
                    }}
                    placeholder={`To-do ${index + 1}`}
                    maxLength={200}
                    className={`input flex-1 ${errors.checklist && index === 0 && !item.trim() ? 'border-error-500' : ''}`}
                  />
                  {checklistItems.length > 1 && (
                    <button
                      type="button"
                      onClick={() => {
                        setChecklistItems(checklistItems.filter((_, i) => i !== index));
                      }}
                      className="p-2 text-gray-400 hover:text-error-500 hover:bg-error-50 dark:hover:bg-error-900/20 rounded-lg transition-colors"
                      title="To-do entfernen"
                    >
                      <XMarkIcon className="w-5 h-5" />
                    </button>
                  )}
                </div>
              ))}
            </div>

            <button
              type="button"
              onClick={() => setChecklistItems([...checklistItems, ''])}
              className="btn-secondary flex items-center gap-2 text-sm"
              disabled={checklistItems.length >= 20}
            >
              <PlusIcon className="w-4 h-4" />
              To-do hinzufügen
            </button>

            {errors.checklist && <p className="form-error mt-2">{errors.checklist}</p>}
          </div>
        )}

        {/* Datei-Upload */}
        <div className="form-group">
          <label className="label">Anhänge</label>
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">
            Fügen Sie Dokumente, Bilder oder Tabellen hinzu, um Ihre Idee zu unterstützen
          </p>

          {/* Vorhandene Dateien */}
          {existingFiles.length > 0 && (
            <div className="space-y-2 mb-4">
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                Aktuelle Anhänge
              </p>
              {existingFiles.map((file) => {
                const { Icon, color } = getFileIcon(file.mimeType);
                return (
                  <div
                    key={file.id}
                    className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`p-2 rounded-lg bg-white dark:bg-gray-800 ${color}`}>
                        <Icon className="w-5 h-5" />
                      </div>
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300 truncate max-w-[200px]">
                        {file.name}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveExistingFile(file.id)}
                      className="p-1 text-gray-400 hover:text-error-500 hover:bg-error-50 dark:hover:bg-error-900/20 rounded transition-colors"
                      title="Datei entfernen"
                    >
                      <XMarkIcon className="w-5 h-5" />
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {/* Neue Dateien */}
          {files.length > 0 && (
            <div className="space-y-2 mb-4">
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                Hochzuladende Dateien ({files.length})
              </p>
              <div className="grid gap-2">
                {files.map((file, index) => {
                  const { Icon, color } = getFileIcon(file.type);
                  const uploadStatus = uploadStatuses.get(file.name);
                  const isImage = file.type.startsWith('image/');

                  return (
                    <div
                      key={`${file.name}-${index}`}
                      className={`flex items-center justify-between p-3 rounded-lg border transition-all
                        ${uploadStatus?.status === 'error'
                          ? 'bg-error-50 dark:bg-error-900/20 border-error-300 dark:border-error-700'
                          : uploadStatus?.status === 'success'
                          ? 'bg-success-50 dark:bg-success-900/20 border-success-300 dark:border-success-700'
                          : 'bg-gray-50 dark:bg-gray-700/50 border-gray-200 dark:border-gray-600'
                        }`}
                    >
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        {/* Bildvorschau oder Datei-Symbol */}
                        {isImage && file.preview ? (
                          <div className="w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100 dark:bg-gray-800">
                            <img
                              src={file.preview}
                              alt={file.name}
                              className="w-full h-full object-cover"
                            />
                          </div>
                        ) : (
                          <div className={`p-2 rounded-lg bg-white dark:bg-gray-800 flex-shrink-0 ${color}`}>
                            <Icon className="w-5 h-5" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 truncate">
                            {file.name}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            {formatFileSize(file.size)}
                            {uploadStatus?.error && (
                              <span className="text-error-600 dark:text-error-400 ml-2">
                                - {uploadStatus.error}
                              </span>
                            )}
                          </p>
                        </div>
                      </div>

                      {/* Statusanzeigen */}
                      <div className="flex items-center gap-2 ml-2">
                        {uploadStatus?.status === 'uploading' && (
                          <div className="animate-spin rounded-full h-5 w-5 border-2 border-primary-500 border-t-transparent" />
                        )}
                        {uploadStatus?.status === 'success' && (
                          <CheckCircleIcon className="w-5 h-5 text-success-500" />
                        )}
                        {uploadStatus?.status === 'error' && (
                          <ExclamationCircleIcon className="w-5 h-5 text-error-500" />
                        )}
                        {(!uploadStatus || uploadStatus.status === 'pending') && !isUploading && (
                          <button
                            type="button"
                            onClick={() => handleRemoveFile(index)}
                            className="p-1 text-gray-400 hover:text-error-500 hover:bg-error-50 dark:hover:bg-error-900/20 rounded transition-colors"
                            title="Datei entfernen"
                          >
                            <XMarkIcon className="w-5 h-5" />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Ablage-Bereich */}
          <div
            {...getRootProps()}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all
              ${isDragReject
                ? 'border-error-500 bg-error-50 dark:bg-error-900/20'
                : isDragActive
                ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 scale-[1.02]'
                : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-800/50'
              }`}
          >
            <input {...getInputProps()} />
            <div className="flex flex-col items-center">
              <div className={`p-3 rounded-full mb-3 transition-colors
                ${isDragActive
                  ? 'bg-primary-100 dark:bg-primary-900/50'
                  : 'bg-gray-100 dark:bg-gray-800'
                }`}>
                <CloudArrowUpIcon className={`w-8 h-8 transition-colors
                  ${isDragActive
                    ? 'text-primary-600 dark:text-primary-400'
                    : 'text-gray-400'
                  }`}
                />
              </div>
              <p className="text-gray-700 dark:text-gray-300 font-medium">
                {isDragReject
                  ? 'Dateityp wird nicht unterstützt'
                  : isDragActive
                  ? 'Dateien hier ablegen...'
                  : 'Dateien hier ablegen'}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                oder <span className="text-primary-600 dark:text-primary-400 font-medium">durchsuchen</span> zum Auswählen
              </p>
              <div className="flex flex-wrap justify-center gap-2 mt-4">
                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                  <PhotoIcon className="w-3 h-3 mr-1" /> Bilder
                </span>
                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                  <DocumentTextIcon className="w-3 h-3 mr-1" /> PDF
                </span>
                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                  <DocumentIcon className="w-3 h-3 mr-1" /> Word
                </span>
                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                  <TableCellsIcon className="w-3 h-3 mr-1" /> Excel
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-3">
                Max. 10MB pro Datei
              </p>
            </div>
          </div>
        </div>

        {/* Schaltflächen absenden */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="btn-secondary"
            disabled={loading || isUploading}
          >
            Abbrechen
          </button>
          <button
            type="submit"
            disabled={loading || isUploading}
            className="btn-primary min-w-[140px]"
          >
            {loading || isUploading ? (
              <div className="flex items-center gap-2">
                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                <span>{isUploading ? 'Wird hochgeladen...' : 'Wird gespeichert...'}</span>
              </div>
            ) : isEditing ? (
              'Idee aktualisieren'
            ) : (
              'Idee einreichen'
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
