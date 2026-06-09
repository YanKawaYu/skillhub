import { createElement, type ReactNode } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { AuthMethod } from '@/api/types'

const authMethodsState = vi.hoisted(() => ({
  data: [] as AuthMethod[],
  isLoading: false,
}))

vi.mock('react-i18next', async () => {
  const actual = await vi.importActual<typeof import('react-i18next')>('react-i18next')
  return {
    ...actual,
    useTranslation: () => ({
      t: (key: string, params?: { name?: string }) =>
        key === 'loginButton.loginWith' ? `login with ${params?.name ?? ''}` : key,
    }),
  }
})

vi.mock('@/shared/ui/button', () => ({
  Button: ({ children }: { children: ReactNode }) => createElement('button', null, children),
}))

vi.mock('./use-auth-methods', () => ({
  useAuthMethods: () => authMethodsState,
}))

import { LoginButton } from './login-button'

function authMethod(provider: string, displayName: string): AuthMethod {
  return {
    id: `oauth-${provider}`,
    methodType: 'OAUTH_REDIRECT',
    provider,
    displayName,
    actionUrl: `/oauth2/authorization/${provider}`,
  }
}

describe('LoginButton', () => {
  it('exports LoginButton component', () => {
    expect(LoginButton).toBeTypeOf('function')
  })

  it('renders only tuyoo OAuth provider', () => {
    authMethodsState.data = [
      authMethod('github', 'GitHub'),
      authMethod('gitlab', 'GitLab'),
      authMethod('tuyoo', 'tuyoo'),
    ]
    authMethodsState.isLoading = false

    const html = renderToStaticMarkup(createElement(LoginButton, { returnTo: '/dashboard' }))

    expect(html).toContain('login with tuyoo')
    expect(html).not.toContain('GitHub')
    expect(html).not.toContain('GitLab')
  })
})
