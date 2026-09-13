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
        </div>
      </header>

      {screen === 'till' && <Till />}
      {screen === 'products' && <Products />}
      {screen === 'day' && <Report />}
    </>
  )
}