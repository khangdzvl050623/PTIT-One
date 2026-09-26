import type { ReactNode } from 'react'
import { BrowserRouter } from 'react-router-dom'

import { AuthProvider } from '@/features/auth'

export interface AppProvidersProps {
  children: ReactNode
}

/**
 * Gom mọi provider toàn cục vào một chỗ (router, theme, error boundary...).
 * Thêm provider mới thì thêm ở đây, không rải rác trong từng màn hình.
 */
export function AppProviders({ children }: AppProvidersProps) {
  return (
    <BrowserRouter>
      <AuthProvider>{children}</AuthProvider>
    </BrowserRouter>
  )
}
