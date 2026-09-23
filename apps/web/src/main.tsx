import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import '@/styles/global.scss'

import { App } from '@/App'

const container = document.getElementById('root')

if (!container) {
  throw new Error('Không tìm thấy phần tử #root trong index.html')
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
