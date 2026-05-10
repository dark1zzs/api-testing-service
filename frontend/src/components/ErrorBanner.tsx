import { useI18n } from '../i18n'

type Props = { message: string | null; onDismiss: () => void }

export function ErrorBanner({ message, onDismiss }: Props) {
  const { t } = useI18n()

  if (!message) return null
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      <button type="button" className="btn btn-ghost" onClick={onDismiss}>
        {t('actions.dismiss')}
      </button>
    </div>
  )
}
