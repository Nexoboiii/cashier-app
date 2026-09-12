import { useState } from 'react'
import Till from './components/Till'
import Products from './components/Products'
import Report from './components/Report'

export default function App() {
  const [screen, setScreen] = useState('till')

  return (
    <>
      <nav className="nav">
        <button className={screen === 'till' ? 'on' : ''} onClick={() => setScreen('till')}>Till</button>
        <button className={screen === 'day' ? 'on' : ''} onClick={() => setScreen('day')}>Day</button>
        <button className={screen === 'products' ? 'on' : ''} onClick={() => setScreen('products')}>Products</button>
      </nav>
        {screen === 'till' && <Till />}
        {screen === 'products' && <Products />}
        {screen === 'day' && <Report />}
    </>
  )
}