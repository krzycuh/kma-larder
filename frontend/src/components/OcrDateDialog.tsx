import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { t } from '../i18n';
import type { ApproximateDate } from '../types/stock';
import type { DateCandidate } from '../utils/dateOcr';
import { formatApproximateDate } from '../utils/format';

interface OcrDateDialogProps {
  open: boolean;
  onClose: () => void;
  /** The user picked one of the recognised dates */
  onPick: (date: ApproximateDate) => void;
}

type Stage = 'idle' | 'loading' | 'recognizing' | 'done' | 'error';

/**
 * Photo of the printed date → text (Tesseract.js, on the phone) → date candidates the user confirms.
 * Nothing is saved without a tap: OCR on inkjet and embossed dates is a suggestion, not a fact.
 */
export function OcrDateDialog({ open, onClose, onPick }: OcrDateDialogProps) {
  const [stage, setStage] = useState<Stage>('idle');
  const [progress, setProgress] = useState(0);
  const [attempt, setAttempt] = useState<{ current: number; total: number } | null>(null);
  const [candidates, setCandidates] = useState<DateCandidate[]>([]);
  const [rawText, setRawText] = useState('');
  const [preview, setPreview] = useState<string | null>(null);
  const [showRaw, setShowRaw] = useState(false);
  const cancelled = useRef(false);

  useEffect(() => {
    if (open) {
      cancelled.current = false;
      setStage('idle');
      setCandidates([]);
      setRawText('');
      setShowRaw(false);
      setPreview(null);
    } else {
      cancelled.current = true;
    }
  }, [open]);

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview);
  }, [preview]);

  const onFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setPreview((old) => {
      if (old) URL.revokeObjectURL(old);
      return URL.createObjectURL(file);
    });
    setStage('loading');
    setProgress(0);
    setCandidates([]);
    try {
      const { recognizeDates } = await import('../lib/ocr');
      const result = await recognizeDates(file, (status, value, current) => {
        if (cancelled.current) return;
        setStage(status);
        setProgress(Math.round(value * 100));
        setAttempt(current ?? null);
      });
      if (cancelled.current) return;
      setRawText(result.text);
      setCandidates(result.candidates);
      setStage('done');
    } catch {
      if (!cancelled.current) setStage('error');
    }
  };

  const busy = stage === 'loading' || stage === 'recognizing';
  const inputClasses =
    'flex flex-1 cursor-pointer items-center justify-center rounded-lg px-4 py-3 text-sm font-semibold focus-within:ring-2 focus-within:ring-blue-500';

  return (
    <Dialog open={open} onClose={busy ? () => undefined : onClose} className="relative z-50">
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" />
      <div className="fixed inset-0 flex items-end justify-center p-4 sm:items-center">
        <DialogPanel className="max-h-full w-full max-w-md overflow-y-auto rounded-lg bg-white p-5 shadow-xl">
          <DialogTitle className="text-lg font-semibold text-gray-900">{t.ocr.title}</DialogTitle>
          <p className="mt-1 text-sm text-gray-600">{t.ocr.hint}</p>
          <p className="mt-1 text-xs text-gray-500">{t.ocr.orientationHint}</p>

          {preview && <img src={preview} alt="" className="mt-3 max-h-48 w-full rounded-md object-contain bg-gray-100" />}

          {busy && (
            <div className="mt-4" role="status" aria-live="polite">
              <p className="text-sm text-gray-700">
                {stage === 'loading' ? t.ocr.loadingEngine : t.ocr.reading(progress)}
                {attempt && attempt.current > 1 && <span className="ml-1 text-gray-500">{t.ocr.attempt(attempt.current)}</span>}
              </p>
              <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-gray-200">
                <div className="h-2 rounded-full bg-blue-600 transition-all" style={{ width: `${stage === 'loading' ? Math.max(5, progress) : progress}%` }} />
              </div>
            </div>
          )}

          {stage === 'error' && (
            <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-800" role="alert">
              {t.ocr.failed}
            </p>
          )}

          {stage === 'done' && (
            <div className="mt-4">
              {candidates.length === 0 ? (
                <p className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">{t.ocr.noDates}</p>
              ) : (
                <>
                  <p className="text-sm font-medium text-gray-700">{t.ocr.foundDates}</p>
                  <p className="text-xs text-gray-500">{t.ocr.pickHint}</p>
                  <ul className="mt-2 space-y-2">
                    {candidates.map((candidate) => (
                      <li key={`${candidate.date.year}-${candidate.date.month}-${candidate.date.day}`}>
                        <button
                          type="button"
                          onClick={() => onPick(candidate.date)}
                          className="flex w-full items-center justify-between gap-3 rounded-lg border border-gray-300 bg-white px-4 py-3 text-left hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                          <span>
                            <span className="block font-semibold text-gray-900">{formatApproximateDate(candidate.date)}</span>
                            <span className="block text-xs text-gray-500">„{candidate.raw}”</span>
                          </span>
                          <span className="text-sm font-medium text-blue-700">{t.ocr.use} →</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </>
              )}
              {rawText.trim() && (
                <div className="mt-3">
                  <button type="button" onClick={() => setShowRaw((v) => !v)} className="text-xs font-medium text-gray-500 underline hover:text-gray-900">
                    {t.ocr.rawText}
                  </button>
                  {showRaw && <pre className="mt-1 max-h-32 overflow-auto whitespace-pre-wrap rounded-md bg-gray-50 p-2 text-xs text-gray-700">{rawText}</pre>}
                </div>
              )}
            </div>
          )}

          <div className="mt-5 flex gap-2">
            <label className={`${inputClasses} bg-blue-600 text-white hover:bg-blue-700 ${busy ? 'pointer-events-none opacity-50' : ''}`}>
              {stage === 'done' || stage === 'error' ? t.common.retry : t.ocr.takePhoto}
              <input type="file" accept="image/*" capture="environment" className="sr-only" onChange={onFile} disabled={busy} />
            </label>
            <label className={`${inputClasses} border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 ${busy ? 'pointer-events-none opacity-50' : ''}`}>
              {t.ocr.choosePhoto}
              <input type="file" accept="image/*" className="sr-only" onChange={onFile} disabled={busy} />
            </label>
          </div>
          <div className="mt-3 flex items-center justify-between gap-3">
            <p className="text-xs text-gray-500">{t.ocr.privacy}</p>
            <button type="button" onClick={onClose} disabled={busy} className="shrink-0 rounded-md px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50">
              {t.common.close}
            </button>
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
}
