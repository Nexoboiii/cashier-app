import { useState } from 'react'
import Till from './components/Till'
import Products from './components/Products'
import Report from './components/Report'

const TABS = [
  ['till', 'Till'],
  ['day', 'Day'],
  ['products', 'Products'],
]

export default function App() {
  const [screen, setScreen] = useState('till')

  async function shutdown() {
    if (!window.confirm('Close the till? Make sure the day is closed first.')) return
    await fetch('/api/system/shutdown', { method: 'POST' }).catch(() => {})
    window.close()
  }

  return (
    <>
      <header className="topbar no-print">
        <div className="topbar-inner">
          <span className="brand"><span className="brand-dot" />Till</span>
          <nav className="nav">
            {TABS.map(([key, label]) => (
              <button key={key} className={screen === key ? 'on' : ''} onClick={() => setScreen(key)}>
                {label}
              </button>
            ))}
          </nav>
          <button className="btn-ghost shutdown" onClick={shutdown}>Close till</button>
        </div>
      </header>

      {screen === 'till' && <Till />}
      {screen === 'products' && <Products />}
      {screen === 'day' && <Report />}
    </>
  )
}