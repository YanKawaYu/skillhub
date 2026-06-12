/** @vitest-environment jsdom */

import { createElement, type MouseEvent } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from './dialog'

describe('Dialog components', () => {
  it('exports all dialog sub-components', () => {
    expect(Dialog).toBeDefined()
    expect(DialogTrigger).toBeDefined()
    expect(DialogContent).toBeDefined()
    expect(DialogHeader).toBeDefined()
    expect(DialogFooter).toBeDefined()
    expect(DialogTitle).toBeDefined()
    expect(DialogDescription).toBeDefined()
  })

  it('sets displayName on forwardRef components', () => {
    expect(DialogTrigger.displayName).toBe('DialogTrigger')
    expect(DialogContent.displayName).toBe('DialogContent')
    expect(DialogTitle.displayName).toBe('DialogTitle')
    expect(DialogDescription.displayName).toBe('DialogDescription')
  })

  it('sets displayName on function components', () => {
    expect(DialogHeader.displayName).toBe('DialogHeader')
    expect(DialogFooter.displayName).toBe('DialogFooter')
  })

  it('preserves child click handlers when rendered as child', () => {
    const parentClick = vi.fn()
    const childClick = vi.fn((event: MouseEvent<HTMLButtonElement>) => {
      event.stopPropagation()
    })

    render(createElement(
      'div',
      { onClick: parentClick },
      createElement(
        Dialog,
        null,
        createElement(
          DialogTrigger,
          { asChild: true },
          createElement('button', { type: 'button', onClick: childClick }, 'Open dialog'),
        ),
        createElement(DialogContent, null, 'Dialog content'),
      ),
    ))

    fireEvent.click(screen.getByRole('button', { name: 'Open dialog' }))

    expect(childClick).toHaveBeenCalledTimes(1)
    expect(parentClick).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog')).toBeDefined()
  })
})
