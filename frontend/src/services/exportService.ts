import api from './api';

const exportService = {
  async exportIdeasCsv(): Promise<void> {
    const response = await api.get('/export/ideas/csv', {
      responseType: 'blob'
    });
    downloadFile(response.data, 'ideas.csv', 'text/csv');
  },

  async exportStatisticsCsv(): Promise<void> {
    const response = await api.get('/export/statistics/csv', {
      responseType: 'blob'
    });
    downloadFile(response.data, 'statistics.csv', 'text/csv');
  },

  async exportStatisticsPdf(): Promise<void> {
    const response = await api.get('/export/statistics/pdf', {
      responseType: 'blob'
    });
    downloadFile(response.data, 'statistics.pdf', 'application/pdf');
  }
};

function downloadFile(data: Blob, filename: string, mimeType: string): void {
  const blob = new Blob([data], { type: mimeType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

export default exportService;
