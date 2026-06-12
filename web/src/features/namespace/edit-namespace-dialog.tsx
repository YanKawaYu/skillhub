import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Namespace, NamespaceReviewPolicy } from '@/api/types'
import { useUpdateNamespace } from '@/shared/hooks/use-namespace-queries'
import { toast } from '@/shared/lib/toast'
import { Button } from '@/shared/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/shared/ui/dialog'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import { Textarea } from '@/shared/ui/textarea'

const REVIEW_POLICIES: NamespaceReviewPolicy[] = ['AUTO_APPROVE', 'FIRST_PUBLISH_ONLY', 'EVERY_PUBLISH']

const REVIEW_POLICY_LABEL_KEYS: Record<NamespaceReviewPolicy, string> = {
  AUTO_APPROVE: 'namespaceEdit.reviewPolicyAutoApprove',
  FIRST_PUBLISH_ONLY: 'namespaceEdit.reviewPolicyFirstPublishOnly',
  EVERY_PUBLISH: 'namespaceEdit.reviewPolicyEveryPublish',
}

const REVIEW_POLICY_HINT_KEYS: Record<NamespaceReviewPolicy, string> = {
  AUTO_APPROVE: 'namespaceEdit.reviewPolicyAutoApproveHint',
  FIRST_PUBLISH_ONLY: 'namespaceEdit.reviewPolicyFirstPublishOnlyHint',
  EVERY_PUBLISH: 'namespaceEdit.reviewPolicyEveryPublishHint',
}

interface EditNamespaceDialogProps {
  namespace: Namespace
  children: React.ReactNode
}

export function EditNamespaceDialog({ namespace, children }: EditNamespaceDialogProps) {
  const { t } = useTranslation()
  const updateMutation = useUpdateNamespace()
  const [open, setOpen] = useState(false)
  const [displayName, setDisplayName] = useState(namespace.displayName)
  const [description, setDescription] = useState(namespace.description ?? '')
  const [reviewPolicy, setReviewPolicy] = useState<NamespaceReviewPolicy>(
    namespace.reviewPolicy ?? 'FIRST_PUBLISH_ONLY',
  )
  const [displayNameError, setDisplayNameError] = useState<string | null>(null)

  const resetDialog = () => {
    setDisplayName(namespace.displayName)
    setDescription(namespace.description ?? '')
    setReviewPolicy(namespace.reviewPolicy ?? 'FIRST_PUBLISH_ONLY')
    setDisplayNameError(null)
    updateMutation.reset()
  }

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (!nextOpen) {
      resetDialog()
    }
  }

  const handleSave = async () => {
    const trimmedDisplayName = displayName.trim()
    if (!trimmedDisplayName) {
      setDisplayNameError(t('namespaceEdit.displayNameRequired'))
      return
    }

    try {
      await updateMutation.mutateAsync({
        slug: namespace.slug,
        displayName: trimmedDisplayName,
        description: description.trim(),
        reviewPolicy,
      })
      toast.success(t('namespaceEdit.saveSuccess'))
      setOpen(false)
    } catch (error) {
      toast.error(t('namespaceEdit.saveErrorTitle'), error instanceof Error ? error.message : '')
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader className="text-center sm:text-center">
          <DialogTitle className="text-center">{t('namespaceEdit.dialogTitle')}</DialogTitle>
          <DialogDescription className="text-center">@{namespace.slug}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="edit-display-name">{t('namespaceEdit.displayNameLabel')}</Label>
            <Input
              id="edit-display-name"
              value={displayName}
              onChange={(event) => {
                setDisplayName(event.target.value)
                if (displayNameError) setDisplayNameError(null)
              }}
              aria-invalid={displayNameError ? 'true' : 'false'}
            />
            {displayNameError ? <p className="text-xs text-red-600">{displayNameError}</p> : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-description">{t('namespaceEdit.descriptionLabel')}</Label>
            <Textarea
              id="edit-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={3}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-review-policy">{t('namespaceEdit.reviewPolicyLabel')}</Label>
            <Select
              value={reviewPolicy}
              onValueChange={(value) => setReviewPolicy(value as NamespaceReviewPolicy)}
            >
              <SelectTrigger id="edit-review-policy">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {REVIEW_POLICIES.map((policy) => (
                  <SelectItem key={policy} value={policy}>
                    {t(REVIEW_POLICY_LABEL_KEYS[policy])}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">{t(REVIEW_POLICY_HINT_KEYS[reviewPolicy])}</p>
          </div>
        </div>

        {updateMutation.error ? (
          <p className="text-sm text-red-600">{updateMutation.error.message}</p>
        ) : null}

        <DialogFooter className="sm:justify-center sm:space-x-3">
          <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
            {t('dialog.cancel')}
          </Button>
          <Button type="button" onClick={handleSave} disabled={updateMutation.isPending}>
            {updateMutation.isPending ? t('namespaceEdit.saving') : t('namespaceEdit.saveAction')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
