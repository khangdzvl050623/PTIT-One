import { AppProviders } from '@/app/providers'
import { AppRoutes } from '@/app/router'

export function App() {
  return (
    <AppProviders>
      <AppRoutes />
    </AppProviders>
  )
}
