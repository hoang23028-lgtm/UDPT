const dt = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

export function formatDateTime(iso: string): string {
  try {
    return dt.format(new Date(iso));
  } catch {
    return iso;
  }
}
